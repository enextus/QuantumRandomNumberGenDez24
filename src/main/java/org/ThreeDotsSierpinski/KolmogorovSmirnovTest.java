package org.ThreeDotsSierpinski;

import java.util.Arrays;

public class KolmogorovSmirnovTest {

    public static boolean test(int[] sample, double alpha) {
        // Сортируем выборку по возрастанию
        Arrays.sort(sample);

        // Вычисляем эмпирическую функцию распределения и находим максимальное отклонение
        double maxDeviation = 0.0;
        int n = sample.length;
        for (int i = 0; i < n; i++) {
            double empiricalCDF = (double) (i + 1) / n;
            double theoreticalCDF = calculateTheoreticalCDF(sample[i]);

            double deviation = Math.abs(empiricalCDF - theoreticalCDF);
            maxDeviation = Math.max(maxDeviation, deviation);
        }

        // Вычисляем критическое значение
        double criticalValue = Math.sqrt(-0.5 * Math.log(alpha / 2)) / Math.sqrt(n);

        // Проверяем условие
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
