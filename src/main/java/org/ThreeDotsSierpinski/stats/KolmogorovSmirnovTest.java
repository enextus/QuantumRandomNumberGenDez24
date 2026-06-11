package org.ThreeDotsSierpinski.stats;

import java.util.List;
import java.util.Objects;

/**
 * Реализация теста Колмогорова-Смирнова для проверки равномерности распределения.
 */
public record KolmogorovSmirnovTest(long minRange, long maxRange) implements RandomnessTest {

    public KolmogorovSmirnovTest {
        if (minRange >= maxRange) {
            throw new IllegalArgumentException("minRange должен быть меньше maxRange");
        }
    }

    public KolmogorovSmirnovTest() {
        this(0L, 65535L);
    }

    @Override
    public TestResult testWithDetails(List<Long> numbers, double alpha) {
        Objects.requireNonNull(numbers, "Список чисел не может быть null");
        if (numbers.isEmpty()) {
            throw new IllegalArgumentException("Список чисел не может быть пустым");
        }
        if (numbers.size() < 10) {
            throw new IllegalArgumentException("Для теста требуется минимум 10 чисел, получено: " + numbers.size());
        }
        if (alpha <= 0 || alpha >= 1) {
            throw new IllegalArgumentException("alpha должен быть в диапазоне (0, 1)");
        }

        long[] sample = numbers.stream().mapToLong(Long::longValue).sorted().toArray();
        int n = sample.length;

        double maxDeviation = 0.0;
        for (int i = 0; i < n; i++) {
            double empiricalCDF = (double) (i + 1) / n;
            double theoreticalCDF = (double) (sample[i] - minRange) / (maxRange - minRange);
            theoreticalCDF = Math.clamp(theoreticalCDF, 0.0, 1.0);
            maxDeviation = Math.max(maxDeviation, Math.abs(empiricalCDF - theoreticalCDF));
        }

        double criticalValue = Math.sqrt(-0.5 * Math.log(alpha / 2)) / Math.sqrt(n);

        var quality = maxDeviation < criticalValue * 0.6 ? TestResult.Quality.STRONG
                : maxDeviation <= criticalValue ? TestResult.Quality.MARGINAL
                  : TestResult.Quality.FAIL;

        String stat = String.format("D=%.4f (crit=%.4f)", maxDeviation, criticalValue);
        return new TestResult(getTestName(), quality != TestResult.Quality.FAIL, stat, quality);
    }

    @Override
    public String getTestName() {
        return "Тест Колмогорова-Смирнова";
    }

}
