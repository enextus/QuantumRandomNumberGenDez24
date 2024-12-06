package org.ThreeDotsSierpinski;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class KolmogorovSmirnovTestRunnerTest {

    @Test
    void testKolmogorovSmirnov() {
        // Генерация выборки случайных чисел
        int sampleSize = 1000;
        int[] sample = new int[sampleSize];
        Random random = new Random();
        for (int i = 0; i < sampleSize; i++) {
            sample[i] = random.nextInt(200000000) - 99999999; // Случайные числа в диапазоне [-99999999, 100000000]
        }

        // Уровень значимости
        double alpha = 0.05;

        // Использование класса KolmogorovSmirnovTest
        boolean result = KolmogorovSmirnovTest.test(sample, alpha);

        // Вывод результата в консоль
        if (result) {
            System.out.println("Выборка соответствует теоретическому распределению на уровне значимости " + alpha);
            System.out.println("Резюме: ОК. Случайные числа соответствуют ожидаемому распределению.");
        } else {
            System.out.println("Выборка не соответствует теоретическому распределению на уровне значимости " + alpha);
            System.out.println("Резюме: Ошибка. Случайные числа некачественные.");
        }

        // Проверка через assert
        assertTrue(result, "Выборка не соответствует ожидаемому распределению.");
    }
}
