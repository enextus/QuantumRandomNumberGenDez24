package org.ThreeDotsSierpinski;

import java.util.List;
import java.util.Objects;

/**
 * Реализация теста Колмогорова-Смирнова для проверки равномерности распределения.
 */
public class KolmogorovSmirnovTest implements RandomnessTest {
    private static long minRange = 0;
    private static long maxRange = 0;

    /**
     * Конструктор с настраиваемым диапазоном равномерного распределения.
     *
     * @param minRange Минимальное значение диапазона
     * @param maxRange Максимальное значение диапазона
     */
    public KolmogorovSmirnovTest(long minRange, long maxRange) {
        if (minRange >= maxRange) {
            throw new IllegalArgumentException("minRange должен быть меньше maxRange");
        }
        this.minRange = minRange;
        this.maxRange = maxRange;
    }

    /**
     * Конструктор с диапазоном по умолчанию (-99999999, 100000000).
     */
    public KolmogorovSmirnovTest() {
        this(-99999999L, 100000000L);
    }

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

        // Вычисление максимального отклонения
        double maxDeviation = 0.0;
        for (int i = 0; i < n; i++) {
            double empiricalCDF = (double) (i + 1) / n;
            double theoreticalCDF = (sample[i] - minRange) / (double) (maxRange - minRange);
            maxDeviation = Math.max(maxDeviation, Math.abs(empiricalCDF - theoreticalCDF));
        }

        // Критическое значение для двустороннего теста
        double criticalValue = Math.sqrt(-0.5 * Math.log(alpha / 2)) / Math.sqrt(n);
        return maxDeviation <= criticalValue;
    }

    @Override
    public String getTestName() {
        return "";
    }

}
