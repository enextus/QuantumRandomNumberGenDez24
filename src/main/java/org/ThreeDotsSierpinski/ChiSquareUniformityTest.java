package org.ThreeDotsSierpinski;

import java.util.List;

/**
 * Тест хи-квадрат для проверки равномерности распределения.
 *
 * Разбивает диапазон [0, 65535] на 16 равных интервалов и проверяет,
 * что количество попаданий в каждый интервал близко к ожидаемому.
 * Большое значение хи-квадрат указывает на неравномерность.
 */
public class ChiSquareUniformityTest implements RandomnessTest {

    private static final int NUM_BINS = 16;
    private static final long MIN_RANGE = 0;
    private static final long MAX_RANGE = 65535;

    /**
     * Критические значения хи-квадрат для 15 степеней свободы (NUM_BINS - 1).
     * alpha=0.01 → 30.578, alpha=0.05 → 24.996, alpha=0.10 → 22.307
     */
    private static double getCriticalValue(double alpha) {
        if (alpha <= 0.01) return 30.578;
        if (alpha <= 0.05) return 24.996;
        return 22.307;
    }

    @Override
    public boolean test(List<Long> numbers, double alpha) {
        if (numbers == null || numbers.size() < 10) {
            throw new IllegalArgumentException("Требуется минимум 10 чисел");
        }

        int[] bins = new int[NUM_BINS];
        long binSize = (MAX_RANGE - MIN_RANGE + 1) / NUM_BINS;

        for (long number : numbers) {
            int binIndex = (int) Math.min((number - MIN_RANGE) / binSize, NUM_BINS - 1);
            bins[binIndex]++;
        }

        double expectedCount = (double) numbers.size() / NUM_BINS;
        double chiSquare = 0.0;

        for (int count : bins) {
            chiSquare += Math.pow(count - expectedCount, 2) / expectedCount;
        }

        return chiSquare < getCriticalValue(alpha);
    }

    @Override
    public String getTestName() {
        return "Хи-квадрат (Chi-Square)";
    }
}
