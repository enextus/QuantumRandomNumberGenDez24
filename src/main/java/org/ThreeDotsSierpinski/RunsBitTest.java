package org.ThreeDotsSierpinski;

import java.util.List;

/**
 * NIST Runs Test.
 *
 * Проверяет количество серий (переходов 0→1, 1→0) в битовой последовательности.
 */
public class RunsBitTest implements RandomnessTest {

    @Override
    public TestResult testWithDetails(List<Long> numbers, double alpha) {
        if (numbers == null || numbers.size() < 10) {
            throw new IllegalArgumentException("Требуется минимум 10 чисел");
        }

        int totalBits = numbers.size() * 16;
        int[] bits = new int[totalBits];
        int idx = 0;
        for (long number : numbers) {
            for (int bit = 0; bit < 16; bit++) {
                bits[idx++] = (int) ((number >> bit) & 1);
            }
        }

        int n = bits.length;

        double pi = 0;
        for (int b : bits) {
            pi += b;
        }
        pi /= n;

        if (Math.abs(pi - 0.5) > 2.0 / Math.sqrt(n)) {
            return new TestResult(getTestName(), false, "pi=" + String.format("%.4f", pi) + " (pre-test fail)");
        }

        int runs = 1;
        for (int i = 1; i < n; i++) {
            if (bits[i] != bits[i - 1]) {
                runs++;
            }
        }

        double numerator = Math.abs(runs - 2.0 * n * pi * (1 - pi));
        double denominator = 2.0 * Math.sqrt(2.0 * n) * pi * (1 - pi);

        if (denominator == 0) {
            return new TestResult(getTestName(), false, "div/0");
        }

        double pValue = erfc(numerator / denominator);
        boolean passed = pValue >= alpha;

        String stat = String.format("p=%.4f", pValue);
        return new TestResult(getTestName(), passed, stat);
    }

    @Override
    public String getTestName() {
        return "Серии (Runs)";
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
