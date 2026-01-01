package ua.nulp.elHelper.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ua.nulp.elHelper.entity.calculation.Calculation;
import ua.nulp.elHelper.entity.calculation.Formula;
import ua.nulp.elHelper.entity.calculation.Project;
import ua.nulp.elHelper.repository.ProjectRepo;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BomService {

    private final ProjectRepo projectRepository;

    // BOM для коректного відображення кирилиці в Excel
    private static final byte[] BOM_UTF8 = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    public byte[] generateCsvBom(Long projectId, String userEmail) {
        Project project = projectRepository.findByIdAndUserEmail(projectId, userEmail)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        StringBuilder csv = new StringBuilder();

        // Заголовки CSV
        csv.append("Designator;Calculation Name;Value;Standard (E24);Unit;Description\n");

        for (Calculation calc : project.getCalculations()) {
            Map<String, Double> rawResults = calc.getResults();
            Map<String, Double> stdResults = calc.getStandardizedResults();
            List<Formula.FormulaParam> params = calc.getFormula().getParameters();

            if (stdResults != null) {
                // Сортуємо по ключу (R1, R2...)
                stdResults.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(entry -> {
                            String varName = entry.getKey();
                            Double stdValue = entry.getValue();

                            // Фільтруємо лише компоненти (R, C, L)
                            if (isComponent(varName)) {
                                Double rawValue = rawResults.getOrDefault(varName, 0.0);
                                String baseUnit = findBaseUnit(params, varName);

                                // Формуємо основний опис (напр. "4.7 kOhm")
                                String mainDesc = formatEngineering(stdValue, baseUnit);

                                // --- НОВА ЛОГІКА: Додаємо дод. параметри (Потужність, Напруга) ---
                                String extraSpecs = getAdditionalSpecs(varName, rawResults);

                                String fullDescription = mainDesc;
                                if (!extraSpecs.isEmpty()) {
                                    fullDescription += ", " + extraSpecs;
                                }

                                csv.append(varName).append(";")                               // Designator
                                        .append(escapeCsv(calc.getName())).append(";")             // Calc Name
                                        .append(formatDecimal(rawValue)).append(";")               // Value Raw
                                        .append(formatDecimal(stdValue)).append(";")               // Value Std
                                        .append(baseUnit).append(";")                              // Unit
                                        .append(escapeCsv(fullDescription))                        // Description
                                        .append("\n");
                            }
                        });
            }
        }

        byte[] csvBytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[BOM_UTF8.length + csvBytes.length];
        System.arraycopy(BOM_UTF8, 0, result, 0, BOM_UTF8.length);
        System.arraycopy(csvBytes, 0, result, BOM_UTF8.length, csvBytes.length);

        return result;
    }

    private boolean isComponent(String varName) {
        return varName != null && varName.matches("^[RCL].*");
    }

    /**
     * Шукає додаткові параметри компонента (Потужність для R, Напруга для C).
     * Логіка пошуку:
     * 1. Шукаємо "P_varName" (напр. P_R1).
     * 2. Якщо компонент просто "R" (без цифри), шукаємо просто "P".
     */
    private String getAdditionalSpecs(String componentName, Map<String, Double> results) {
        StringBuilder specs = new StringBuilder();

        // --- Для резисторів шукаємо Потужність (Power) ---
        if (componentName.startsWith("R")) {
            // Варіант 1: P_R1
            String pVar1 = "P_" + componentName;
            // Варіант 2: Просто P (якщо компонент називається просто R)
            String pVar2 = "P";

            Double powerVal = null;
            if (results.containsKey(pVar1)) {
                powerVal = results.get(pVar1);
            } else if (componentName.equals("R") && results.containsKey(pVar2)) {
                powerVal = results.get(pVar2);
            }

            if (powerVal != null) {
                specs.append(formatEngineering(powerVal, "W")); // Форматуємо як Вати (W)
            }
        }

        // --- Для конденсаторів шукаємо Напругу (Voltage) ---
        if (componentName.startsWith("C")) {
            // Варіант 1: U_C1 або V_C1
            String uVar1 = "U_" + componentName;
            String uVar2 = "V_" + componentName;
            String uVar3 = "U"; // Для простого випадку

            Double voltVal = null;
            if (results.containsKey(uVar1)) voltVal = results.get(uVar1);
            else if (results.containsKey(uVar2)) voltVal = results.get(uVar2);
            else if (componentName.equals("C") && results.containsKey(uVar3)) voltVal = results.get(uVar3);

            if (voltVal != null) {
                specs.append(formatDecimal(voltVal)).append("V");
            }
        }

        return specs.toString();
    }

    private String findBaseUnit(List<Formula.FormulaParam> params, String varName) {
        if (params == null) return "";
        return params.stream()
                .filter(p -> p.getVar().equals(varName))
                .findFirst()
                .map(p -> {
                    if (p.getUnits() == null || p.getUnits().isEmpty()) return "";
                    return p.getUnits().stream()
                            .filter(u -> Math.abs(u.getMult() - 1.0) < 0.0001)
                            .findFirst()
                            .map(Formula.UnitDefinition::getName)
                            .orElse(p.getUnits().get(0).getName());
                })
                .orElse("");
    }

    private String formatDecimal(Double value) {
        if (value == null) return "";
        return String.format(Locale.US, "%.4f", value);
    }

    private String escapeCsv(String data) {
        if (data == null) return "";
        String escapedData = data.replaceAll("\\R", " ");
        if (data.contains(";") || data.contains("\"")) {
            data = data.replace("\"", "\"\"");
            escapedData = "\"" + data + "\"";
        }
        return escapedData;
    }

    /**
     * Форматує число в інженерний вигляд (4700 -> 4.7 k).
     */
    private String formatEngineering(Double value, String unit) {
        if (value == null) return "";
        if (value == 0) return "0 " + unit;

        // Для потужності нам важливо бачити, наприклад, 0.25 W, а не 250 mW (хоча і так, і так правильно)
        // Але стандартний алгоритм спрацює добре.

        String[] prefixes = {"p", "n", "u", "m", "", "k", "M", "G"};
        int baseIndex = 4; // Індекс для пустой приставки (10^0)

        double log10 = Math.log10(Math.abs(value));
        int exponent = (int) Math.floor(log10 / 3.0) * 3;

        int prefixIndex = baseIndex + (exponent / 3);

        if (prefixIndex < 0) prefixIndex = 0;
        if (prefixIndex >= prefixes.length) prefixIndex = prefixes.length - 1;

        double scaledValue = value / Math.pow(10, (prefixIndex - baseIndex) * 3);

        // Видаляємо зайві нулі (наприклад 10.00 -> 10)
        String formattedNum = String.format(Locale.US, "%.2f", scaledValue)
                .replaceAll("\\.?0+$", "");

        return formattedNum + " " + prefixes[prefixIndex] + unit;
    }
}