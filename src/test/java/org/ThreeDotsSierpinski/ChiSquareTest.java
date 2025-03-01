package org.ThreeDotsSierpinski;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ChiSquareTest {

    @Test
    public void testChiSquare() {
        int sampleSize = 10000; // Количество случайных чисел
        int numberOfBins = 10;  // Количество интервалов
        Random random = new Random();

        // Создаем бины для распределения
        Map<Integer, Integer> bins = new HashMap<>();
        for (int i = 0; i < numberOfBins; i++) {
            bins.put(i, 0);
        }

        // Генерируем случайные числа и распределяем их по бинам
        for (int i = 0; i < sampleSize; i++) {
            int value = random.nextInt(numberOfBins);
            bins.put(value, bins.get(value) + 1);
        }

        // Ожидаемое количество чисел в каждом бине
        double expectedCount = (double) sampleSize / numberOfBins;

        // Вычисляем статистику хи-квадрат
        double chiSquare = 0.0;
        for (int count : bins.values()) {
            chiSquare += Math.pow(count - expectedCount, 2) / expectedCount;
        }

        // Критическое значение для уровня значимости 0.05 и 9 степеней свободы
        double criticalValue = 16.92;

        // Проверяем гипотезу
        assertTrue(chiSquare < criticalValue, "Chi-Square test failed: " + chiSquare);
    }
}
