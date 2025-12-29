package org.ThreeDotsSierpinski;

import java.util.List;
import java.util.Objects;

/**
 * Реализация теста Колмогорова-Смирнова для проверки равномерности распределения.
 *
 * ИСПРАВЛЕНО:
 * - Убраны static поля (теперь потокобезопасно)
 * - Реализован метод getTestName()
 */
public class KolmogorovSmirnovTest implements RandomnessTest {

    // ИСПРАВЛЕНО: Убран static модификатор - теперь поля принадлежат экземпляру
    private final long minRange;
    private final long maxRange;

    /**
     * Конструктор с настраиваемым диапазоном равномерного распределения.
     *
     * @param minRange Минимальное значение диапазона
     * @param maxRange Максимальное значение диапазона
     * @throws IllegalArgumentException если minRange >= maxRange
     */
    public KolmogorovSmirnovTest(long minRange, long maxRange) {
        if (minRange >= maxRange) {
            throw new IllegalArgumentException("minRange должен быть меньше maxRange");
        }
        this.minRange = minRange;
        this.maxRange = maxRange;
    }

    /**
     * Конструктор с диапазоном по умолчанию для uint16 (0, 65535).
     */
    public KolmogorovSmirnovTest() {
        this(0L, 65535L);
    }

    /**
     * Проверяет, соответствует ли выборка равномерному распределению.
     *
     * @param numbers Список чисел для тестирования
     * @param alpha Уровень значимости (обычно 0.01, 0.05 или 0.1)
     * @return true, если выборка проходит тест, false в противном случае
     */
    @Override
    public boolean test(List<Long> numbers, double alpha) {
        // Проверка входных данных
        Objects.requireNonNull(numbers, "Список чисел не может быть null");
        if (numbers.isEmpty()) {
            throw new IllegalArgumentException("Список чисел не может быть пустым");
        }
        if (alpha <= 0 || alpha >= 1) {
            throw new IllegalArgumentException("alpha должен быть в диапазоне (0, 1)");
        }

        // Преобразование в массив и сортировка
        long[] sample = numbers.stream().mapToLong(Long::longValue).sorted().toArray();
        int n = sample.length;

        // Вычисление максимального отклонения (статистика Колмогорова-Смирнова)
        double maxDeviation = 0.0;
        for (int i = 0; i < n; i++) {
            // Эмпирическая функция распределения
            double empiricalCDF = (double) (i + 1) / n;

            // Теоретическая функция распределения (равномерное распределение)
            double theoreticalCDF = (double) (sample[i] - minRange) / (maxRange - minRange);

            // Ограничиваем CDF диапазоном [0, 1]
            theoreticalCDF = Math.max(0.0, Math.min(1.0, theoreticalCDF));

            maxDeviation = Math.max(maxDeviation, Math.abs(empiricalCDF - theoreticalCDF));
        }

        // Критическое значение для двустороннего теста (асимптотическое приближение)
        double criticalValue = Math.sqrt(-0.5 * Math.log(alpha / 2)) / Math.sqrt(n);

        return maxDeviation <= criticalValue;
    }

    /**
     * Возвращает имя теста.
     * ИСПРАВЛЕНО: Теперь возвращает осмысленное имя вместо пустой строки.
     *
     * @return Название теста
     */
    @Override
    public String getTestName() {
        return "Тест Колмогорова-Смирнова";
    }

    /**
     * Возвращает минимальное значение диапазона.
     */
    public long getMinRange() {
        return minRange;
    }

    /**
     * Возвращает максимальное значение диапазона.
     */
    public long getMaxRange() {
        return maxRange;
    }
}
