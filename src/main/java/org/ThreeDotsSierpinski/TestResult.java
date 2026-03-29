package org.ThreeDotsSierpinski;

/**
 * Результат выполнения одного теста случайности.
 *
 * @param testName  название теста
 * @param passed    true если тест пройден
 * @param statistic строковое представление ключевой метрики (например "p=0.847")
 */
public record TestResult(String testName, boolean passed, String statistic) {

    @Override
    public String toString() {
        String mark = passed ? "\u2713" : "\u2717";
        return mark + "  " + statistic + "    " + testName;
    }
}