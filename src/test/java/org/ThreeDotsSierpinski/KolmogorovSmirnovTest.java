package org.ThreeDotsSierpinski;

import java.util.Arrays;

public class KolmogorovSmirnovTest {

    public static boolean test(int[] sample, double alpha) {
        // Сортируем выборку по возрастанию
        Arrays.sort(sample);

        // Вычисляем эмпирическую функцию распределения
        double[] empiricalCDF = new double[sample.length];
        for (int i = 0; i < sample.length; i++) {
            empiricalCDF[i] = (double) (i + 1) / sample.length;
        }

        // Вычисляем теоретическую функцию распределения (равномерное распределение)
        double[] theoreticalCDF = new double[sample.length];
        for (int i = 0; i < sample.length; i++) {
            theoreticalCDF[i] = (double) (i + 1) / sample.length;
        }

        // Вычисляем максимальное отклонение между эмпирической и теоретической функциями распределения
        double maxDeviation = 0.0;
        for (int i = 0; i < sample.length; i++) {
            double deviation = Math.abs(empiricalCDF[i] - theoreticalCDF[i]);
            if (deviation > maxDeviation) {
                maxDeviation = deviation;
            }
        }

        // Вычисляем критическое значение
        double criticalValue = Math.sqrt(-0.5 * Math.log(alpha / 2) / sample.length);

        // Проверяем условие
        return maxDeviation <= criticalValue;
    }

}
