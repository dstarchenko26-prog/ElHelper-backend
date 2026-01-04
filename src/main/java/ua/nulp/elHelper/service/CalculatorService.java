package ua.nulp.elHelper.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.nulp.elHelper.entity.calculation.Calculation;
import ua.nulp.elHelper.entity.calculation.Formula;
import ua.nulp.elHelper.repository.CalculationRepo;
import ua.nulp.elHelper.repository.FormulaRepo;
import ua.nulp.elHelper.repository.ProjectRepo;
import ua.nulp.elHelper.service.dto.calculation.calculation.CalculationRequest;
import ua.nulp.elHelper.service.dto.calculation.calculation.CalculationResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalculatorService {

    private final FormulaRepo formulaRepository;
    private final ProjectRepo projectRepository;
    private final CalculationRepo calculationRepository;

    private final StandardizationService standardizationService;
    private final SymbolicSolverService symbolicSolver;


    public Map<String, Object> calculateTest(CalculationRequest request) {
        Formula formula = formulaRepository.findById(request.getFormulaId())
                .orElseThrow(() -> new RuntimeException("Formula not found"));

        Map<String, Double> normalizedInputs = normalizeInputs(formula, request.getInputs(), request.getInputUnits());

        Map<String, Double> results = runAutoSolver(formula, normalizedInputs);

        Map<String, Double> stdResults = standardizationService.standardizeResults(results);

        return Map.of(
                "results", results,
                "standardizedResults", stdResults
        );
    }

    public CalculationResponse getById(Long id) {
        var calc = calculationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Calculation not found"));
        
        return mapToDto(calc);
    }

    @Transactional
    public CalculationResponse calculateAndSave(CalculationRequest request, String userEmail) {
        var project = projectRepository.findByIdAndUserEmail(request.getProjectId(), userEmail)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!project.isActive()) {
            throw new RuntimeException("Project is archived. Create a new version to edit.");
        }

        var formula = formulaRepository.findById(request.getFormulaId())
                .orElseThrow(() -> new RuntimeException("Formula not found"));

        Map<String, Double> normalizedInputs = normalizeInputs(formula, request.getInputs(), request.getInputUnits());
        Map<String, Double> results = runAutoSolver(formula, normalizedInputs);
        Map<String, Double> stdResults = standardizationService.standardizeResults(results);

        String calcName = (request.getName() != null && !request.getName().isBlank())
                ? request.getName()
                : formula.getNames().getOrDefault("uk", "Calculation");

        var calculation = Calculation.builder()
                .project(project)
                .formula(formula)
                .name(calcName)
                .inputs(request.getInputs())
                .inputUnits(request.getInputUnits())
                .results(results)
                .standardizedResults(stdResults)
                .build();

        return mapToDto(calculationRepository.save(calculation));
    }

    @Transactional
    public CalculationResponse updateCalculation(Long id, CalculationRequest request, String userEmail) {
        var calc = calculationRepository.findByIdAndProject_User_Email(id, userEmail)
                .orElseThrow(() -> new RuntimeException("Calculation not found"));

        if (!calc.getProject().isActive()) {
            throw new RuntimeException("Cannot update archived project");
        }

        calc.setInputs(request.getInputs());
        calc.setInputUnits(request.getInputUnits());
        if (request.getName() != null && !request.getName().isBlank()) {
            calc.setName(request.getName());
        }

        var formula = calc.getFormula();
        Map<String, Double> normalizedInputs = normalizeInputs(formula, request.getInputs(), request.getInputUnits());
        Map<String, Double> results = runAutoSolver(formula, normalizedInputs);
        Map<String, Double> stdResults = standardizationService.standardizeResults(results);

        calc.setResults(results);
        calc.setStandardizedResults(stdResults);

        return mapToDto(calculationRepository.save(calc));
    }

    public List<CalculationResponse> getProjectCalculations(Long projectId, String userEmail) {
        projectRepository.findByIdAndUserEmail(projectId, userEmail)
                .orElseThrow(() -> new RuntimeException("Access denied"));

        return calculationRepository.findAllByProjectId(projectId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteCalculation(Long id, String userEmail) {
        var calc = calculationRepository.findByIdAndProject_User_Email(id, userEmail)
                .orElseThrow(() -> new RuntimeException("Calculation not found"));

        if (!calc.getProject().isActive()) {
            throw new RuntimeException("Cannot delete from archived project");
        }
        calculationRepository.delete(calc);
    }

    private Map<String, Double> runAutoSolver(Formula formula, Map<String, Double> inputs) {
        // Контекст зберігає всі відомі на даний момент змінні (вхідні + обчислені)
        Map<String, Double> context = new HashMap<>(inputs);

        if (formula.getScripts() == null || formula.getScripts().isEmpty()) {
            return context;
        }

        // Список усіх скриптів (рівнянь) формули
        List<Formula.FormulaScript> scripts = formula.getScripts();

        boolean progress; // Прапорець: чи вдалося нам щось знайти в цьому проході?
        int maxPasses = 10; // Запобіжник від нескінченного циклу

        do {
            progress = false;

            for (Formula.FormulaScript script : scripts) {
                String equation = script.getExpression();

                // Нормалізація рівняння (якщо старий формат без "=")
                if (!equation.contains("=")) {
                    equation = "#" + script.getTarget() + " = " + equation;
                }

                // 1. Знаходимо всі змінні, які використовуються в ЦЬОМУ конкретному рівнянні
                // Наприклад, для "P = I * U" це [P, I, U]
                List<String> scriptVars = extractVariables(equation);

                // 2. Перевіряємо, скількох змінних нам не вистачає саме для цього рівняння
                String missingVar = null;
                int missingCount = 0;

                for (String var : scriptVars) {
                    if (!context.containsKey(var)) {
                        missingVar = var;
                        missingCount++;
                    }
                }

                // 3. Якщо не вистачає рівно однієї змінної -> ми можемо її знайти!
                if (missingCount == 1) {
                    try {
                        Double solvedValue = symbolicSolver.solve(equation, context, missingVar);

                        // Якщо Symja повернула результат, додаємо його в контекст
                        if (solvedValue != null && !Double.isNaN(solvedValue) && !Double.isInfinite(solvedValue)) {
                            context.put(missingVar, solvedValue);
                            progress = true; // Ми дізналися щось нове, треба пройтись по скриптах ще раз
                        }
                    } catch (Exception e) {
                        // Ігноруємо помилку, можливо дані для цього рівняння ще не готові
                    }
                }
            }
            maxPasses--;
        } while (progress && maxPasses > 0);

        return context;
    }

    private List<String> extractVariables(String equation) {
        List<String> vars = new ArrayList<>();
        Pattern pattern = Pattern.compile("#([a-zA-Z0-9_]+)");
        Matcher matcher = pattern.matcher(equation);

        while (matcher.find()) {
            vars.add(matcher.group(1));
        }
        return vars;
    }

    private Map<String, Double> normalizeInputs(Formula formula, Map<String, Double> inputs, Map<String, String> inputUnits) {
        Map<String, Double> normalized = new HashMap<>();
        if (inputs == null) return normalized;

        inputs.forEach((key, val) -> {
            double multiplier = 1.0;

            var paramOpt = formula.getParameters().stream()
                    .filter(p -> p.getVar().equals(key))
                    .findFirst();

            if (paramOpt.isPresent() && inputUnits != null && inputUnits.containsKey(key)) {
                String unitName = inputUnits.get(key);
                if (paramOpt.get().getUnits() != null) {
                    multiplier = paramOpt.get().getUnits().stream()
                            .filter(u -> u.getName().equals(unitName))
                            .findFirst()
                            .map(Formula.UnitDefinition::getMult)
                            .orElse(1.0);
                }
            }
            normalized.put(key, val * multiplier);
        });
        return normalized;
    }

    private CalculationResponse mapToDto(Calculation calc) {
        return CalculationResponse.builder()
                .id(calc.getId())
                .name(calc.getName())
                .formulaId(calc.getFormula().getId())
                .formulaName(calc.getFormula().getNames())
                .categoryId(calc.getFormula().getCategory().getId())
                .categoryName(calc.getFormula().getCategory().getNames())
                .inputs(calc.getInputs())
                .inputUnits(calc.getInputUnits())
                .results(calc.getResults())
                .standardizedResults(calc.getStandardizedResults())
                .createdAt(calc.getCreatedAt())
                .build();
    }
}
