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

import java.util.List;

/**
 * Интерфейс для тестов случайности числовых выборок.
 */
public interface RandomnessTest {

    /**
     * Запускает тест и возвращает детальный результат с метрикой.
     *
     * @param numbers выборка чисел
     * @param alpha   уровень значимости (0.01, 0.05, 0.1)
     * @return результат с названием теста, статусом и значением метрики
     */
    TestResult testWithDetails(List<Long> numbers, double alpha);

    /**
     * Возвращает имя теста.
     */
    String getTestName();

    /**
     * Упрощённый метод: возвращает только pass/fail.
     * Сохранён для обратной совместимости с существующими тестами.
     */
    default boolean test(List<Long> numbers, double alpha) {
        return testWithDetails(numbers, alpha).passed();
    }

}