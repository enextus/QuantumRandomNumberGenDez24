package org.ThreeDotsSierpinski;

import java.util.List;

/**
 * Набор тестов случайности.
 *
 * Запускает все зарегистрированные тесты и возвращает детальные результаты.
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

    public List<TestResult> runAll(List<Long> numbers, double alpha) {
        return tests.stream()
                .map(test -> {
                    try {
                        return test.testWithDetails(numbers, alpha);
                    } catch (Exception e) {
                        return new TestResult(test.getTestName(), false, "error: " + e.getMessage());
                    }
                })
                .toList();
    }

    public static String formatResults(List<TestResult> results) {
        StringBuilder sb = new StringBuilder();
        int passed = 0;

        for (TestResult result : results) {
            sb.append(result).append("\n");
            if (result.passed()) passed++;
        }

        sb.append("\nИтого: ").append(passed).append("/").append(results.size()).append(" тестов пройдено");
        return sb.toString();
    }

}
