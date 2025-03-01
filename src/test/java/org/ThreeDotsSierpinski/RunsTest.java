package org.ThreeDotsSierpinski;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RunsTest {

    private final RNProvider rnProvider = new RNProvider(); // Инициализируем RNProvider

    @Test
    public void testRuns() {
        int sampleSize = 1000; // Количество случайных чисел

        // Генерируем последовательность случайных чисел с помощью RNProvider
        double[] randomNumbers = new double[sampleSize];
        for (int i = 0; i < sampleSize; i++) {
            randomNumbers[i] = rnProvider.getNextRandomNumber() / 255.0; // Нормализуем числа в диапазон [0, 1]
        }

        // Вычисляем количество серий
        int runs = 1; // Минимум одна серия
        for (int i = 1; i < sampleSize; i++) {
            if ((randomNumbers[i] > 0.5 && randomNumbers[i - 1] <= 0.5) ||
                    (randomNumbers[i] <= 0.5 && randomNumbers[i - 1] > 0.5)) {
                runs++;
            }
        }

        // Вычисляем ожидаемое количество серий и стандартное отклонение
        double expectedRuns = (2.0 * sampleSize - 1.0) / 3.0;
        double standardDeviation = Math.sqrt((16.0 * sampleSize - 29.0) / 90.0);

        // Проверяем, находится ли количество серий в допустимом интервале
        double z = Math.abs(runs - expectedRuns) / standardDeviation;
        double criticalValue = 1.96; // Уровень значимости 0.05

        System.out.println("Actual runs: " + runs);
        System.out.println("Expected runs: " + expectedRuns);
        System.out.println("Standard deviation: " + standardDeviation);
        System.out.println("Z-value: " + z);

        assertTrue(z < criticalValue, "Runs Test failed: Z = " + z + ", Runs = " + runs);
    }
}
