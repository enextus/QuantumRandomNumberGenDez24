package org.ThreeDotsSierpinski;

import java.util.List;

/**
 * Набор тестов случайности.
 *
 * Запускает все зарегистрированные тесты на одной выборке чисел
 * и возвращает список результатов. Добавить новый тест —
 * создать класс, реализующий RandomnessTest, и добавить в список.
 */
public class RandomnessTestSuite {

    private final List<RandomnessTest> tests;

    public RandomnessTestSuite() {
        tests = List.of(
                new KolmogorovSmirnovTest(),
                new FrequencyBitTest(),
                new ChiSquareUniformityTest(),
                new RunsBitTest()
        );
    }

    /**
     * Запускает все тесты на выборке чисел.
     *
     * @param numbers выборка случайных чисел
     * @param alpha   уровень значимости (0.01, 0.05 или 0.1)
     * @return список результатов по каждому тесту
     */
    public List<TestResult> runAll(List<Long> numbers, double alpha) {
        return tests.stream()
                .map(test -> {
                    try {
                        boolean passed = test.test(numbers, alpha);
                        return new TestResult(test.getTestName(), passed);
                    } catch (Exception e) {
                        return new TestResult(test.getTestName() + " (ошибка: " + e.getMessage() + ")", false);
                    }
                })
                .toList();
    }

    /**
     * Форматирует результаты для отображения в UI.
     */
    public static String formatResults(List<TestResult> results) {
        StringBuilder sb = new StringBuilder();
        int passed = 0;
        int total = results.size();

        for (TestResult result : results) {
            sb.append(result).append("\n");
            if (result.passed()) passed++;
        }

        sb.append("\nИтого: ").append(passed).append("/").append(total).append(" тестов пройдено");
        return sb.toString();
    }
}
