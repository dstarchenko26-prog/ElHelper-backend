package ua.nulp.elHelper.service;

import org.matheclipse.core.eval.ExprEvaluator;
import org.matheclipse.core.interfaces.IExpr;
import org.springframework.stereotype.Service;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;

@Service
public class SymbolicSolverService {

    private final ExprEvaluator evaluator = new ExprEvaluator();

    public Double solve(String equation, Map<String, Double> inputs, String targetVar) {
        try {
            // 1. Підготовка рівняння
            // Замінюємо "=" на "==", бо Symja розуміє тільки подвійне дорівнює
            String commandEq = equation.replace("=", "==");

            // 2. Підставляємо відомі значення прямо в рядок
            // (Ми припускаємо, що equation ВЖЕ прийшов із безпечними іменами змінних типу var_I)
            for (Map.Entry<String, Double> entry : inputs.entrySet()) {
                String varName = entry.getKey();
                String val = String.valueOf(entry.getValue());

                // Використовуємо replace, а не regex, щоб було швидше і надійніше
                // Але перевіряємо межі слова, щоб var_R не замінило шматок var_R1
                // Оскільки ми контролюємо імена (вони починаються на var_), replaceAll з \b безпечний.
                commandEq = commandEq.replaceAll("\\b" + varName + "\\b", val);
            }

            // 3. Формуємо команду
            String command = "N(Solve(" + commandEq + ", " + targetVar + "))";

            // LOG для відладки (буде в консолі Render)
            System.out.println("Symja Command: " + command);

            // 4. Виконання
            IExpr result;
            synchronized (evaluator) {
                evaluator.eval("Clear(" + targetVar + ")"); // Забуваємо старі значення
                result = evaluator.eval(command);
            }

            System.out.println("Symja Result: " + result.toString());

            return extractPositiveRoot(result.toString());

        } catch (Exception e) {
            System.err.println("Solver Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private Double extractPositiveRoot(String symjaResponse) {
        if (symjaResponse == null || symjaResponse.equals("{}") || symjaResponse.equals("List()")) return null;

        // Шукаємо число після "->". Підтримує цілі, дробові, від'ємні, науковий формат (1.2*^5)
        Pattern pattern = Pattern.compile("->\\s*(-?\\d+(\\.\\d*)?([eE][+-]?\\d+)?)");
        Matcher matcher = pattern.matcher(symjaResponse);

        Double bestResult = null;

        while (matcher.find()) {
            try {
                double val = Double.parseDouble(matcher.group(1));
                if (val >= 0) return val; // Повертаємо перший додатний корінь
                if (bestResult == null) bestResult = val;
            } catch (Exception e) { /* ignore */ }
        }
        return bestResult;
    }
}