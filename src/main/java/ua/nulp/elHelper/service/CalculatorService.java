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
        Map<String, Double> context = new HashMap<>(inputs);

        if (formula.getScripts() == null || formula.getScripts().isEmpty()) {
            return context;
        }

        List<Formula.FormulaScript> scripts = formula.getScripts();
        boolean progress;
        int maxPasses = 10;

        do {
            progress = false;

            for (Formula.FormulaScript script : scripts) {
                // КРОК 1: Очищаємо "сире" рівняння від #
                // Було: "#U = #I * #R" -> Стало: "U = I * R"
                String rawEquation = script.getExpression().replace("#", "");
                String targetVar = script.getTarget().replace("#", "");

                // Нормалізація: якщо немає "=", додаємо "U ="
                if (!rawEquation.contains("=")) {
                    rawEquation = targetVar + " = " + rawEquation;
                }

                // КРОК 2: Знаходимо змінні у вже ЧИСТОМУ рівнянні
                // поверне ["U", "I", "R"]
                List<String> cleanVars = extractVariables(rawEquation);

                // Перевіряємо, чого не вистачає
                String missingVarClean = null;
                int missingCount = 0;

                for (String var : cleanVars) {
                    // Перевіряємо в контексті ключі з # (бо в базі вони з #) і без
                    if (!context.containsKey("#" + var) && !context.containsKey(var)) {
                        missingVarClean = var;
                        missingCount++;
                    }
                }

                if (missingCount == 1) {
                    try {
                        // КРОК 3: Санітизація (додаємо префікс var_)
                        // Це врятує нас від I (струм vs уявна одиниця) та E (енергія vs число Ейлера)
                        String safeEquation = rawEquation;
                        Map<String, Double> safeContext = new HashMap<>();
                        String safeMissingVar = "var_" + missingVarClean;

                        // Сортуємо змінні за довжиною (спочатку довгі), щоб P_out не замінилося як P + _out
                        cleanVars.sort((s1, s2) -> s2.length() - s1.length());

                        for (String var : cleanVars) {
                            String safeName = "var_" + var;

                            // Замінюємо: "U = I * R" -> "var_U = var_I * var_R"
                            // Тут \b працює, бо var вже без #
                            safeEquation = safeEquation.replaceAll("\\b" + var + "\\b", safeName);

                            // Шукаємо значення (спочатку з #, потім без)
                            Double val = context.get("#" + var);
                            if (val == null) val = context.get(var);

                            if (val != null) {
                                safeContext.put(safeName, val);
                            }
                        }

                        // КРОК 4: Вирішуємо безпечне рівняння
                        Double solvedValue = symbolicSolver.solve(safeEquation, safeContext, safeMissingVar);

                        if (solvedValue != null && !Double.isNaN(solvedValue) && !Double.isInfinite(solvedValue)) {
                            // Зберігаємо результат. Додаємо # назад, щоб зберегти стиль бази даних
                            context.put("#" + missingVarClean, solvedValue);
                            progress = true;
                        }
                    } catch (Exception e) {
                        // Мовчки ігноруємо, як у старому коді
                    }
                }
            }
            maxPasses--;
        } while (progress && maxPasses > 0);

        return context;
    }

//    private Map<String, Double> runAutoSolver(Formula formula, Map<String, Double> inputs) {
//        // Контекст зберігає всі відомі на даний момент змінні (вхідні + обчислені)
//        Map<String, Double> context = new HashMap<>(inputs);
//
//        if (formula.getScripts() == null || formula.getScripts().isEmpty()) {
//            return context;
//        }
//
//        // Список усіх скриптів (рівнянь) формули
//        List<Formula.FormulaScript> scripts = formula.getScripts();
//
//        boolean progress; // Прапорець: чи вдалося нам щось знайти в цьому проході?
//        int maxPasses = 10; // Запобіжник від нескінченного циклу
//
//        do {
//            progress = false;
//
//            for (Formula.FormulaScript script : scripts) {
//                String equation = script.getExpression();
//
//                // Нормалізація рівняння (якщо старий формат без "=")
//                if (!equation.contains("=")) {
//                    equation = "#" + script.getTarget() + " = " + equation;
//                }
//
//                // 1. Знаходимо всі змінні, які використовуються в ЦЬОМУ конкретному рівнянні
//                // Наприклад, для "P = I * U" це [P, I, U]
//                List<String> scriptVars = extractVariables(equation);
//
//                // 2. Перевіряємо, скількох змінних нам не вистачає саме для цього рівняння
//                String missingVar = null;
//                int missingCount = 0;
//
//                for (String var : scriptVars) {
//                    if (!context.containsKey(var)) {
//                        missingVar = var;
//                        missingCount++;
//                    }
//                }
//
//                // 3. Якщо не вистачає рівно однієї змінної -> ми можемо її знайти!
//                if (missingCount == 1) {
//                    try {
//
//                        Double solvedValue = symbolicSolver.solve(equation, context, missingVar);
//
////                        // Створюємо "безпечне" рівняння та контекст для солвера,
////                        // щоб уникнути конфліктів з зарезервованими іменами (I, E, Pi, Im)
////                        String safeEquation = equation;
////                        Map<String, Double> safeContext = new HashMap<>();
////                        String safeMissingVar = "safe_" + missingVar;
////
////                        // Проходимо по всіх змінних цього рівняння і підміняємо їх
////                        for (String var : scriptVars) {
////                            String safeName = "safe_" + var;
////
////                            // Замінюємо назву змінної в рівнянні (використовуємо \b для меж слова)
////                            // Це перетворить "U = I * R" на "safe_U = safe_I * safe_R"
////                            safeEquation = safeEquation.replaceAll("\\b" + var + "\\b", safeName);
////
////                            // Якщо змінна відома (є в context), додаємо її значення в safeContext
////                            if (context.containsKey(var)) {
////                                safeContext.put(safeName, context.get(var));
////                            }
////                        }
////
////                        // Викликаємо солвер з БЕЗПЕЧНИМИ даними
////                        Double solvedValue = symbolicSolver.solve(safeEquation, safeContext, safeMissingVar);
//
//
//                        // Якщо Symja повернула результат, додаємо його в контекст
//                        if (solvedValue != null && !Double.isNaN(solvedValue) && !Double.isInfinite(solvedValue)) {
//                            context.put(missingVar, solvedValue);
//                            progress = true; // Ми дізналися щось нове, треба пройтись по скриптах ще раз
//                        }
//                    } catch (Exception e) {
//                        // Ігноруємо помилку, можливо дані для цього рівняння ще не готові
//                    }
//                }
//            }
//            maxPasses--;
//        } while (progress && maxPasses > 0);
//
//        return context;
//    }

//    private Map<String, Double> runAutoSolver(Formula formula, Map<String, Double> inputs) {
//        Map<String, Double> context = new HashMap<>(inputs);
//
//        if (formula.getScripts() == null || formula.getScripts().isEmpty()) {
//            return context;
//        }
//
//        List<Formula.FormulaScript> scripts = formula.getScripts();
//        boolean progress;
//        int maxPasses = 10;
//
//        do {
//            progress = false;
//
//            for (Formula.FormulaScript script : scripts) {
//                String equation = script.getExpression();
//
//                // Нормалізація
//                if (!equation.contains("=")) {
//                    equation = "#" + script.getTarget() + " = " + equation;
//                }
//
//                List<String> scriptVars = extractVariables(equation);
//
//                String missingVar = null;
//                int missingCount = 0;
//
//                for (String var : scriptVars) {
//                    // Важливо: переконайся, що var тут чистий (без #), якщо в context ключі без #
//                    if (!context.containsKey(var)) {
//                        missingVar = var;
//                        missingCount++;
//                    }
//                }
//
//                if (missingCount == 1) {
//                    try {
//                        // 1. 🔥 ФІКС: Видаляємо всі решітки з самого рівняння, щоб отримати чисту математику
//                        // Було: "#U = #I * #R" -> Стало: "U = I * R"
//                        String safeEquation = equation.replace("#", "");
//
//                        Map<String, Double> safeContext = new HashMap<>();
//
//                        // Очищаємо і шукану змінну від можливих решіток
//                        String cleanMissingVar = missingVar.replace("#", "");
//                        String safeMissingVar = "safe_" + cleanMissingVar;
//
//                        for (String var : scriptVars) {
//                            String cleanVarName = var.replace("#", "");
//                            String safeName = "safe_" + cleanVarName;
//
//                            // 2. Замінюємо чисту змінну на безпечну
//                            // "U = I * R" -> "safe_U = safe_I * safe_R"
//                            safeEquation = safeEquation.replaceAll("\\b" + cleanVarName + "\\b", safeName);
//
//                            // 3. Заповнюємо контекст
//                            if (context.containsKey(var)) {
//                                safeContext.put(safeName, context.get(var));
//                            }
//                        }
//
//                        // System.out.println("Solving: " + safeEquation + " for " + safeMissingVar); // Для дебагу
//
//                        Double solvedValue = symbolicSolver.solve(safeEquation, safeContext, safeMissingVar);
//
//                        if (solvedValue != null && !Double.isNaN(solvedValue) && !Double.isInfinite(solvedValue)) {
//                            context.put(missingVar, solvedValue);
//                            progress = true;
//                        }
//                    } catch (Exception e) {
//                        // 🔥 ФІКС: Виведи помилку в консоль, щоб бачити, що пішло не так
//                        System.err.println("Solver Error for equation [" + equation + "]: " + e.getMessage());
//                        e.printStackTrace();
//                    }
//                }
//            }
//            maxPasses--;
//        } while (progress && maxPasses > 0);
//
//        return context;
//    }

//    private Map<String, Double> runAutoSolver(Formula formula, Map<String, Double> inputs) {
//        Map<String, Double> context = new HashMap<>(inputs);
//
//        if (formula.getScripts() == null || formula.getScripts().isEmpty()) {
//            return context;
//        }
//
//        List<Formula.FormulaScript> scripts = formula.getScripts();
//        boolean progress;
//        int maxPasses = 10;
//
//        do {
//            progress = false;
//
//            for (Formula.FormulaScript script : scripts) {
//                // 1. Очищаємо рівняння від усіх # на старті
//                // Було: "#U = #I * #R" -> Стало: "U = I * R"
//                String originalEquation = script.getExpression().replace("#", "");
//
//                // Нормалізація (додаємо цільову змінну, якщо її немає)
//                if (!originalEquation.contains("=")) {
//                    String target = script.getTarget().replace("#", "");
//                    originalEquation = target + " = " + originalEquation;
//                }
//
//                // Отримуємо змінні (припускаємо, що метод повертає список типу ["U", "I", "R"] або ["#U", ...])
//                List<String> scriptVars = extractVariables(script.getExpression());
//
//                // Створюємо список "чистих" назв змінних
//                List<String> cleanVars = new ArrayList<>();
//                for (String v : scriptVars) cleanVars.add(v.replace("#", ""));
//
//                // Сортуємо за довжиною (від довгих до коротких), щоб уникнути заміни частини слова
//                // Наприклад, щоб не замінити "I" всередині "I_max"
//                cleanVars.sort((s1, s2) -> s2.length() - s1.length());
//
//                // Рахуємо невідомі
//                String missingVar = null;
//                int missingCount = 0;
//
//                for (String var : cleanVars) {
//                    // Перевіряємо наявність у контексті (шукаємо і "var", і "#var")
//                    boolean exists = context.containsKey(var) || context.containsKey("#" + var);
//                    if (!exists) {
//                        missingVar = var;
//                        missingCount++;
//                    }
//                }
//
//                // Якщо не вистачає рівно однієї змінної
//                if (missingCount == 1) {
//                    try {
//                        // --- ЕТАП САНІТИЗАЦІЇ ---
//                        String safeEquation = originalEquation;
//                        Map<String, Double> safeContext = new HashMap<>();
//                        String safeMissingVar = "var_" + missingVar; // Префікс var_ безпечніший за safe_
//
//                        for (String var : cleanVars) {
//                            String safeName = "var_" + var;
//
//                            // Використовуємо Regex \b (межа слова), щоб замінити точно цю змінну
//                            safeEquation = safeEquation.replaceAll("\\b" + var + "\\b", safeName);
//
//                            // Дістаємо значення з контексту (пробуємо з # і без)
//                            Double val = context.get(var);
//                            if (val == null) val = context.get("#" + var);
//
//                            if (val != null) {
//                                safeContext.put(safeName, val);
//                            }
//                        }
//
//                        // --- ЛОГУВАННЯ (Дивись у консоль!) ---
//                        System.out.println("---- SOLVER DEBUG ----");
//                        System.out.println("Orig: " + originalEquation);
//                        System.out.println("Safe: " + safeEquation);
//                        System.out.println("Solve For: " + safeMissingVar);
//                        System.out.println("Context: " + safeContext);
//
//                        // Виклик солвера
//                        Double solvedValue = symbolicSolver.solve(safeEquation, safeContext, safeMissingVar);
//
//                        System.out.println("Result: " + solvedValue);
//                        System.out.println("----------------------");
//
//                        if (solvedValue != null && !Double.isNaN(solvedValue) && !Double.isInfinite(solvedValue)) {
//                            // Зберігаємо результат (пробуємо зберегти з #, якщо вхідні були з #)
//                            if (inputs.containsKey("#" + cleanVars.get(0))) { // Евристика
//                                context.put("#" + missingVar, solvedValue);
//                            } else {
//                                context.put(missingVar, solvedValue);
//                            }
//
//                            progress = true;
//                        }
//                    } catch (Exception e) {
//                        System.err.println("❌ SOLVER EXCEPTION: " + e.getMessage());
//                        e.printStackTrace();
//                    }
//                }
//            }
//            maxPasses--;
//        } while (progress && maxPasses > 0);
//
//        return context;
//    }


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
