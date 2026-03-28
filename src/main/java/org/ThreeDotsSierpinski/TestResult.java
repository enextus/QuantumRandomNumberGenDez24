package org.ThreeDotsSierpinski;

/**
 * Результат выполнения одного теста случайности.
 *
 * @param testName название теста
 * @param passed   true если тест пройден
 */
public record TestResult(String testName, boolean passed) {

    @Override
    public String toString() {
        return (passed ? "\u2713" : "\u2717") + " " + testName;
    }
}
