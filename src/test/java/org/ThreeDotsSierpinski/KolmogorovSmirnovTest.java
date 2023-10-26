package org.ThreeDotsSierpinski;

import java.util.Arrays;

public class KolmogorovSmirnovTest {

    public static boolean test(int[] sample, double alpha) {
        // Сортируем выборку по возрастанию
        Arrays.sort(sample);

        // Вычисляем эмпирическую функцию распределения и находим максимальное отклонение
        double maxDeviation = 0.0;
        for (int i = 0; i < sample.length; i++) {
            double empiricalCDF = (double) (i + 1) / sample.length;
            double theoreticalCDF = calculateTheoreticalCDF(sample[i]);

            double deviation = Math.abs(empiricalCDF - theoreticalCDF);
            maxDeviation = Math.max(maxDeviation, deviation);
        }

        // Вычисляем критическое значение
        double criticalValue = Math.sqrt(-0.5 * Math.log(alpha / 2) / sample.length);

        // Проверяем условие
        return maxDeviation <= criticalValue;
    }

    private static double calculateTheoreticalCDF(double value) {
        // Здесь должна быть реализация теоретической функции распределения.
        // Например, для равномерного распределения в пределах [0,1]:
        return value;
    }

}
