package org.ThreeDotsSierpinski;

import java.util.List;

/**
 * NIST Frequency (Monobit) Test.
 *
 * Проверяет баланс нулей и единиц в битовом представлении чисел.
 */
public class FrequencyBitTest implements RandomnessTest {

    @Override
    public TestResult testWithDetails(List<Long> numbers, double alpha) {
        if (numbers == null || numbers.size() < 10) {
            throw new IllegalArgumentException("Требуется минимум 10 чисел");
        }

        long sum = 0;
        int totalBits = 0;

        for (long number : numbers) {
            for (int bit = 0; bit < 16; bit++) {
                sum += ((number >> bit) & 1) == 1 ? 1 : -1;
                totalBits++;
            }
        }

        double sObs = Math.abs(sum) / Math.sqrt(totalBits);
        double pValue = erfc(sObs / Math.sqrt(2));
        boolean passed = pValue >= alpha;

        String stat = String.format("p=%.4f", pValue);
        return new TestResult(getTestName(), passed, stat);
    }

    @Override
    public String getTestName() {
        return "Частотный (Frequency)";
    }

    private double erfc(double x) { return 1.0 - erf(x); }

    private double erf(double x) {
        double t = 1.0 / (1.0 + 0.5 * Math.abs(x));
        double tau = t * Math.exp(-x * x - 1.26551223
                + 1.00002368 * t
                + 0.37409196 * t * t
                + 0.09678418 * t * t * t
                - 0.18628806 * t * t * t * t
                + 0.27886807 * t * t * t * t * t
                - 1.13520398 * t * t * t * t * t * t
                + 1.48851587 * t * t * t * t * t * t * t
                - 0.82215223 * t * t * t * t * t * t * t * t
                + 0.17087277 * t * t * t * t * t * t * t * t * t);
        return x >= 0 ? 1 - tau : tau - 1;
    }

}
