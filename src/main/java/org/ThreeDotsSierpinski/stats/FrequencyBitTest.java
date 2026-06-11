package org.ThreeDotsSierpinski.stats;

import org.ThreeDotsSierpinski.app.*;
import org.ThreeDotsSierpinski.config.*;
import org.ThreeDotsSierpinski.math.*;
import org.ThreeDotsSierpinski.mode.*;
import org.ThreeDotsSierpinski.model.*;
import org.ThreeDotsSierpinski.rng.*;
import org.ThreeDotsSierpinski.stats.*;

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
        double pValue = MathUtils.erfc(sObs / Math.sqrt(2));

        var quality = pValue >= 2 * alpha ? TestResult.Quality.STRONG
                    : pValue >= alpha     ? TestResult.Quality.MARGINAL
                    :                       TestResult.Quality.FAIL;

        String stat = String.format("p=%.4f", pValue);
        return new TestResult(getTestName(), quality != TestResult.Quality.FAIL, stat, quality);
    }

    @Override
    public String getTestName() {
        return "Частотный (Frequency)";
    }

}