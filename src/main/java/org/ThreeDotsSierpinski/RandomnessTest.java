package org.ThreeDotsSierpinski;

import java.util.List; /**
 * Интерфейс для тестов случайности числовых выборок.
 */
public interface RandomnessTest {
    /**
     * Проверяет, соответствует ли выборка заданному критерию случайности.
     *
     * @param numbers Список чисел для тестирования
     * @param alpha Уровень значимости (обычно 0.01, 0.05 или 0.1)
     * @return true, если выборка проходит тест, false в противном случае
     */
    boolean test(List<Long> numbers, double alpha);

    /**
     * Возвращает имя теста.
     *
     * @return Название теста
     */
    String getTestName();
}
