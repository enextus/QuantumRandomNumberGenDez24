package org.ThreeDotsSierpinski;

import java.util.List;
import java.util.logging.Logger;

public class NISTRandomnessTest {
    private static final Logger LOGGER = LoggerConfig.getLogger();

    /**
     * Performs the Frequency (Monobit) Test on the given bit sequence.
     *
     * @param bits List of bits (0s and 1s) to be tested.
     * @return True if the test passes, False otherwise.
     */
    public boolean frequencyTest(List<Integer> bits) {
        int n = bits.size();
        long sum = 0;
        for (int bit : bits) {
            if (bit == 1) {
                sum += 1;
            } else if (bit == 0) {
                sum -= 1;
            } else {
                LOGGER.warning("Invalid bit value encountered: " + bit);
                return false;
            }
        }

        double s_obs = Math.abs(sum) / Math.sqrt(n);
        double p_value = erfComplement(s_obs / Math.sqrt(2));

        LOGGER.info("Frequency Test: S_obs = " + s_obs + ", p-value = " + p_value);
        return p_value >= 0.01; // Typically, p-value >= 0.01 indicates pass
    }

    /**
     * Performs the Runs Test on the given bit sequence.
     *
     * @param bits List of bits (0s and 1s) to be tested.
     * @return True if the test passes, False otherwise.
     */
    public boolean runsTest(List<Integer> bits) {
        int n = bits.size();
        double pi = 0.0;
        for (int bit : bits) {
            if (bit == 1) {
                pi += 1.0;
            } else if (bit == 0) {
                pi += 0.0;
            } else {
                LOGGER.warning("Invalid bit value encountered: " + bit);
                return false;
            }
        }
        pi /= n;

        if (Math.abs(pi - 0.5) > (2 / Math.sqrt(n))) {
            LOGGER.warning("Runs Test: pi = " + pi + " is too far from 0.5");
            return false;
        }

        int Vn = 1;
        for (int i = 1; i < n; i++) {
            if (!bits.get(i).equals(bits.get(i - 1))) {
                Vn++;
            }
        }

        double numerator = Math.abs(Vn - (2 * n * pi * (1 - pi)));
        double denominator = 2 * Math.sqrt(2 * n) * pi * (1 - pi);
        double p_value = erfComplement(numerator / denominator);

        LOGGER.info("Runs Test: Vn = " + Vn + ", p-value = " + p_value);
        return p_value >= 0.01; // Typically, p-value >= 0.01 indicates pass
    }

    /**
     * Computes the complementary error function.
     *
     * @param x The value to compute the complementary error function for.
     * @return The complementary error function value.
     */
    private double erfComplement(double x) {
        return 1 - erf(x);
    }

    /**
     * Approximation of the error function (erf).
     *
     * @param x The value to compute the error function for.
     * @return The error function value.
     */
    private double erf(double x) {
        // Abramowitz and Stegun formula 7.1.26 approximation
        // https://en.wikipedia.org/wiki/Error_function#Numerical_approximations
        double t = 1.0 / (1.0 + 0.5 * Math.abs(x));

        // Coefficients
        double tau = t * Math.exp(-x * x - 1.26551223 +
                1.00002368 * t +
                0.37409196 * Math.pow(t, 2) +
                0.09678418 * Math.pow(t, 3) -
                0.18628806 * Math.pow(t, 4) +
                0.27886807 * Math.pow(t, 5) -
                1.13520398 * Math.pow(t, 6) +
                1.48851587 * Math.pow(t, 7) -
                0.82215223 * Math.pow(t, 8) +
                0.17087277 * Math.pow(t, 9));

        if (x >= 0) {
            return 1 - tau;
        } else {
            return tau - 1;
        }
    }
}
