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


//
//import org.matheclipse.core.eval.ExprEvaluator;
//import org.matheclipse.core.interfaces.IExpr;
//import org.springframework.stereotype.Service;
//
//import java.util.Map;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//@Service
//public class SymbolicSolverService {
//
//    private final ExprEvaluator evaluator = new ExprEvaluator();
//
//    /**
//     * Вирішує рівняння безпечно, уникаючи конфліктів із зарезервованими іменами Symja (I, E, Pi).
//     */
//    public Double solve(String equation, Map<String, Double> inputs, String targetVar) {
//        try {
//            // 1. Прибираємо всі # з вхідних даних для чистоти
//            String cleanEq = equation.replace("#", "");
//            String cleanTarget = targetVar.replace("#", "");
//
//            // 2. САНІТИЗАЦІЯ:
//            // Додаємо префікс "v_" до всіх слів, які виглядають як змінні.
//            // Це перетворить "I = U / R" на "v_I = v_U / v_R".
//            // Тепер "v_I" для Symja - це просто змінна, а не уявна одиниця.
//            String safeEq = cleanEq.replaceAll("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\b", "v_$1");
//            String safeTarget = "v_" + cleanTarget;
//
//            // Замінюємо "=" на "==" для Symja
//            safeEq = safeEq.replace("=", "==");
//
//            // 3. Підставляємо відомі значення у "безпечне" рівняння
//            for (Map.Entry<String, Double> entry : inputs.entrySet()) {
//                // Очищаємо ключ від # (якщо є) і додаємо префікс v_
//                String originalKey = entry.getKey().replace("#", "");
//                String safeKey = "v_" + originalKey;
//
//                // Форматуємо число (String.valueOf може дати науковий формат 1.0E-5, Symja це розуміє)
//                String value = String.valueOf(entry.getValue());
//
//                // Замінюємо конкретну змінну на число
//                safeEq = safeEq.replaceAll("\\b" + safeKey + "\\b", value);
//            }
//
//            // На цьому етапі safeEq виглядає як: "v_I == 12.0 / 220.0" (якщо шукаємо I)
//            // Або "2.0 == 12.0 / v_R" (якщо шукаємо R)
//
//            // 4. Формуємо команду: N(Solve(рівняння, змінна))
//            String command = "N(Solve(" + safeEq + ", " + safeTarget + "))";
//
//            // 5. Виконання (synchronized, бо evaluator не thread-safe)
//            IExpr result;
//            synchronized (evaluator) {
//                // Очищаємо змінні перед розрахунком (Clear), про всяк випадок
//                evaluator.eval("Clear(" + safeTarget + ")");
//                result = evaluator.eval(command);
//            }
//
//            // 6. Парсинг результату
//            return extractPositiveRoot(result.toString());
//
//        } catch (Exception e) {
//            System.err.println("Symja error solving equation [" + equation + "]: " + e.getMessage());
//            return null;
//        }
//    }
//
//    private Double extractPositiveRoot(String symjaResponse) {
//        if (symjaResponse.equals("{}") || symjaResponse.equals("List()")) return null;
//
//        // Покращена регулярка:
//        // -> шукає стрілочку
//        // \s* можливі пробіли
//        // (-?\d+(\.\d*)?) шукає число (можливий мінус, ціла частина, необов'язкова крапка і дробна частина)
//        // Дозволяє числа виду: 6, 6.0, 0.5, -12.
//        Pattern pattern = Pattern.compile("->\\s*(-?\\d+(\\.\\d*)?)");
//        Matcher matcher = pattern.matcher(symjaResponse);
//
//        Double bestResult = null;
//
//        while (matcher.find()) {
//            try {
//                double val = Double.parseDouble(matcher.group(1));
//
//                // Пріоритет: додатні числа (фізичні величини зазвичай > 0)
//                if (val >= 0) {
//                    return val;
//                }
//                // Якщо знайшли тільки від'ємне - запам'ятаємо (раптом це температура чи напруга зміщення)
//                if (bestResult == null) {
//                    bestResult = val;
//                }
//            } catch (NumberFormatException e) {
//                // ігноруємо
//            }
//        }
//        return bestResult;
//    }
//}
//
////public class SymbolicSolverService {
////
////    private final ExprEvaluator evaluator = new ExprEvaluator();
////
////    /**
////     * Вирішує рівняння відносно заданої змінної.
////     *
////     * @param equation  Рівняння, наприклад "#I = #U / #R"
////     * @param inputs    Відомі значення, наприклад {"U": 12.0, "I": 2.0}
////     * @param targetVar Змінна, яку треба знайти, наприклад "R"
////     * @return Знайдене значення (Double) або null, якщо розв'язку немає
////     */
////    public Double solve(String equation, Map<String, Double> inputs, String targetVar) {
////        try {
////            // 1. Очистка синтаксису:
////            // - Видаляємо SpEL-маркери "#"
////            // - Замінюємо "=" на "==" (синтаксис рівності в Symja/Mathematica)
////            String preparedEq = equation.replace("#", "").replace("=", "==");
////
////            // 2. Підстановка відомих значень у стрічку рівняння
////            // Ми замінюємо змінні на їх числові значення ДО передачі в солвер.
////            // Це спрощує задачу і уникає конфліктів імен системних змінних (напр. I, E, Pi).
////            for (Map.Entry<String, Double> entry : inputs.entrySet()) {
////                String varName = entry.getKey();
////                String value = String.valueOf(entry.getValue());
////
////                // Використовуємо регулярний вираз з межею слова (\b), щоб
////                // заміна "R" не замінила букву R у змінній "R1" або "Rate".
////                preparedEq = preparedEq.replaceAll("\\b" + varName + "\\b", value);
////            }
////
////            // На цьому етапі preparedEq може виглядати як "2.0 == 12.0 / R"
////
////            // 3. Формуємо команду для Symja:
////            // Solve(рівняння, змінна) - знайти корені
////            // N(...) - перетворити результат у число (float), а не дріб (12/5)
////            String command = "N(Solve(" + preparedEq + ", " + targetVar + "))";
////
////            // 4. Виконання
////            // evaluator не є thread-safe за замовчуванням у складних сценаріях,
////            // але для простих рівнянь і локального використання evaluator.eval() працює стабільно.
////            // При високому навантаженні варто використовувати ThreadLocal<ExprEvaluator>.
////            IExpr result;
////            synchronized (evaluator) {
////                result = evaluator.eval(command);
////            }
////
////            // 5. Парсинг результату
////            // Symja повертає результат у форматі списку правил заміни: {{R->6.0}}
////            return extractPositiveRoot(result.toString());
////
////        } catch (Exception e) {
////            System.err.println("Symja error solving equation: " + equation + ". Error: " + e.getMessage());
////            return null;
////        }
////    }
////
////    /**
////     * Витягує число з відповіді Symja.
////     * Відповідь приходить у форматі: {{Var->Value}} або {{Var->Value1}, {Var->Value2}}
////     */
////    private Double extractPositiveRoot(String symjaResponse) {
////        // Якщо розв'язків немає (порожній список "{}")
////        if (symjaResponse.equals("{}")) return null;
////
////        // Регулярка шукає числа після стрілочки "->".
////        // Враховує від'ємні числа та десяткові дроби.
////        Pattern pattern = Pattern.compile("->\\s*(-?\\d+(\\.\\d+)?)");
////        Matcher matcher = pattern.matcher(symjaResponse);
////
////        Double bestResult = null;
////
////        while (matcher.find()) {
////            try {
////                double val = Double.parseDouble(matcher.group(1));
////
////                // ЕВРИСТИКА: Для інженерних задач (опір, напруга, ємність)
////                // ми зазвичай шукаємо додатне дійсне число.
////                if (val >= 0) {
////                    return val;
////                }
////
////                // Якщо поки не знайшли додатного, запам'ятаємо хоча б від'ємне
////                if (bestResult == null) {
////                    bestResult = val;
////                }
////            } catch (NumberFormatException e) {
////                // ігноруємо помилки парсингу
////            }
////        }
////        return bestResult;
////    }
////}
