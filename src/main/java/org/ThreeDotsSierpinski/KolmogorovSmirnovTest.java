package org.ThreeDotsSierpinski;

import java.util.Arrays;
import java.util.Random;

public class KolmogorovSmirnovTest {

    public static void main(String[] args) {
        // Generate a sample of random numbers
        int sampleSize = 1000;
        int[] sample = new int[sampleSize];
        Random random = new Random();
        for (int i = 0; i < sampleSize; i++) {
            sample[i] = random.nextInt(200000000) - 99999999; // Random numbers in the range [-99999999, 100000000]
        }

        // Significance level
        double alpha = 0.05;

        // Test the sample for compliance with the theoretical distribution
        boolean result = test(sample, alpha);

        // Output the result to the console
        if (result) {
            System.out.println("The sample conforms to the theoretical distribution at the significance level of " + alpha);
            System.out.println("Summary: Yes, everything is fine. The random numbers conform to the expected distribution.");
        } else {
            System.out.println("The sample does not conform to the theoretical distribution at the significance level of " + alpha);
            System.out.println("Summary: No, the random numbers are of poor quality. They do not conform to the expected distribution.");
        }
    }

    public static boolean test(int[] sample, double alpha) {
        // Sort the sample in ascending order
        Arrays.sort(sample);

        // Calculate the empirical distribution function and find the maximum deviation
        double maxDeviation = 0.0;
        int n = sample.length;
        for (int i = 0; i < n; i++) {
            double empiricalCDF = (double) (i + 1) / n;
            double theoreticalCDF = calculateTheoreticalCDF(sample[i]);

            double deviation = Math.abs(empiricalCDF - theoreticalCDF);
            maxDeviation = Math.max(maxDeviation, deviation);
        }

        // Calculate the critical value
        double criticalValue = Math.sqrt(-0.5 * Math.log(alpha / 2)) / Math.sqrt(n);

        // Check the condition
        return maxDeviation <= criticalValue;
    }

    private static double calculateTheoreticalCDF(double value) {
        double a = -99999999;
        double b = 100000000;
        if (value < a) {
            return 0.0;
        } else if (value > b) {
            return 1.0;
        } else {
            return (value - a) / (b - a);
        }
    }
}
