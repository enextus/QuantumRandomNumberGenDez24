package org.ThreeDotsSierpinski.stats;

import org.ThreeDotsSierpinski.app.*;
import org.ThreeDotsSierpinski.config.*;
import org.ThreeDotsSierpinski.math.*;
import org.ThreeDotsSierpinski.mode.*;
import org.ThreeDotsSierpinski.mode.chaos.*;
import org.ThreeDotsSierpinski.mode.montecarlo.*;
import org.ThreeDotsSierpinski.mode.physics.*;
import org.ThreeDotsSierpinski.mode.stochastic.*;
import org.ThreeDotsSierpinski.model.*;
import org.ThreeDotsSierpinski.rng.*;
import org.ThreeDotsSierpinski.stats.*;

import org.jetbrains.annotations.NotNull;

/**
 * Результат выполнения одного теста случайности.
 *
 * @param testName  название теста
 * @param passed    true если тест пройден
 * @param statistic строковое представление ключевой метрики (например "p=0.847")
 * @param quality   уровень качества: STRONG / MARGINAL / FAIL
 */
public record TestResult(String testName, boolean passed, String statistic, Quality quality) {

    /**
     * Уровень качества результата теста.
     * STRONG — уверенно пройден, большой запас до порога
     * MARGINAL — пройден, но близко к порогу (требует внимания)
     * FAIL — не пройден
     */
    public enum Quality {
        STRONG, MARGINAL, FAIL
    }

    /**
     * Обратно-совместимый конструктор: quality вычисляется из passed.
     * Используется в RandomnessTestSuite при перехвате exception.
     */
    public TestResult(String testName, boolean passed, String statistic) {
        this(testName, passed, statistic, passed ? Quality.STRONG : Quality.FAIL);
    }

    @Override
    public @NotNull String toString() {
        String mark = switch (quality) {
            case STRONG -> "✓";   // ✓
            case MARGINAL -> "○"; // ○
            case FAIL -> "✗";     // ✗
        };
        return mark + "  " + statistic + "    " + testName;
    }
}