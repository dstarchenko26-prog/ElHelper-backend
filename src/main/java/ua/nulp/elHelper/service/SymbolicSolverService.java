package ua.nulp.elHelper.service;

import org.matheclipse.core.eval.ExprEvaluator;
import org.matheclipse.core.interfaces.IExpr;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SymbolicSolverService {

    private final ExprEvaluator evaluator = new ExprEvaluator();

    /**
     * Вирішує рівняння відносно заданої змінної.
     *
     * @param equation  Рівняння, наприклад "#I = #U / #R"
     * @param inputs    Відомі значення, наприклад {"U": 12.0, "I": 2.0}
     * @param targetVar Змінна, яку треба знайти, наприклад "R"
     * @return Знайдене значення (Double) або null, якщо розв'язку немає
     */
    public Double solve(String equation, Map<String, Double> inputs, String targetVar) {
        try {
            // 1. Очистка синтаксису:
            // - Видаляємо SpEL-маркери "#"
            // - Замінюємо "=" на "==" (синтаксис рівності в Symja/Mathematica)
            String preparedEq = equation.replace("#", "").replace("=", "==");

            // 2. Підстановка відомих значень у стрічку рівняння
            // Ми замінюємо змінні на їх числові значення ДО передачі в солвер.
            // Це спрощує задачу і уникає конфліктів імен системних змінних (напр. I, E, Pi).
            for (Map.Entry<String, Double> entry : inputs.entrySet()) {
                String varName = entry.getKey();
                String value = String.valueOf(entry.getValue());

                // Використовуємо регулярний вираз з межею слова (\b), щоб
                // заміна "R" не замінила букву R у змінній "R1" або "Rate".
                preparedEq = preparedEq.replaceAll("\\b" + varName + "\\b", value);
            }

            // На цьому етапі preparedEq може виглядати як "2.0 == 12.0 / R"

            // 3. Формуємо команду для Symja:
            // Solve(рівняння, змінна) - знайти корені
            // N(...) - перетворити результат у число (float), а не дріб (12/5)
            String command = "N(Solve(" + preparedEq + ", " + targetVar + "))";

            // 4. Виконання
            // evaluator не є thread-safe за замовчуванням у складних сценаріях,
            // але для простих рівнянь і локального використання evaluator.eval() працює стабільно.
            // При високому навантаженні варто використовувати ThreadLocal<ExprEvaluator>.
            IExpr result;
            synchronized (evaluator) {
                result = evaluator.eval(command);
            }

            // 5. Парсинг результату
            // Symja повертає результат у форматі списку правил заміни: {{R->6.0}}
            return extractPositiveRoot(result.toString());

        } catch (Exception e) {
            System.err.println("Symja error solving equation: " + equation + ". Error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Витягує число з відповіді Symja.
     * Відповідь приходить у форматі: {{Var->Value}} або {{Var->Value1}, {Var->Value2}}
     */
    private Double extractPositiveRoot(String symjaResponse) {
        // Якщо розв'язків немає (порожній список "{}")
        if (symjaResponse.equals("{}")) return null;

        // Регулярка шукає числа після стрілочки "->".
        // Враховує від'ємні числа та десяткові дроби.
        Pattern pattern = Pattern.compile("->\\s*(-?\\d+(\\.\\d+)?)");
        Matcher matcher = pattern.matcher(symjaResponse);

        Double bestResult = null;

        while (matcher.find()) {
            try {
                double val = Double.parseDouble(matcher.group(1));

                // ЕВРИСТИКА: Для інженерних задач (опір, напруга, ємність)
                // ми зазвичай шукаємо додатне дійсне число.
                if (val >= 0) {
                    return val;
                }

                // Якщо поки не знайшли додатного, запам'ятаємо хоча б від'ємне
                if (bestResult == null) {
                    bestResult = val;
                }
            } catch (NumberFormatException e) {
                // ігноруємо помилки парсингу
            }
        }
        return bestResult;
    }
}
