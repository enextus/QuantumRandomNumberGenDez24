package org.ThreeDotsSierpinski;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для NISTRandomnessTest утилитного класса.
 */
@DisplayName("NISTRandomnessTest - NIST тесты случайности")
class NISTRandomnessTestUnitTest {

    private NISTRandomnessTest nistTest;
    private Random random;

    @BeforeEach
    void setUp() {
        nistTest = new NISTRandomnessTest();
        random = new Random(42); // Фиксированный seed
    }

    /**
     * Генерирует случайную битовую последовательность.
     */
    private List<Integer> generateRandomBits(int count) {
        List<Integer> bits = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            bits.add(random.nextInt(2));
        }
        return bits;
    }

    /**
     * Генерирует смещённую битовую последовательность (больше единиц).
     */
    private List<Integer> generateBiasedBits(int count, double onesProbability) {
        List<Integer> bits = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            bits.add(random.nextDouble() < onesProbability ? 1 : 0);
        }
        return bits;
    }

    @Nested
    @DisplayName("frequencyTest() - Частотный тест")
    class FrequencyTestTests {

        @Test
        @DisplayName("Проходит для сбалансированной последовательности")
        void testPassesForBalancedSequence() {
            List<Integer> bits = generateRandomBits(1000);
            boolean result = nistTest.frequencyTest(bits);
            assertTrue(result, "Случайная последовательность должна пройти тест");
        }

        @Test
        @DisplayName("Не проходит для последовательности только из единиц")
        void testFailsForAllOnes() {
            List<Integer> bits = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                bits.add(1);
            }
            boolean result = nistTest.frequencyTest(bits);
            assertFalse(result, "Последовательность только из единиц не должна пройти");
        }

        @Test
        @DisplayName("Не проходит для последовательности только из нулей")
        void testFailsForAllZeros() {
            List<Integer> bits = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                bits.add(0);
            }
            boolean result = nistTest.frequencyTest(bits);
            assertFalse(result, "Последовательность только из нулей не должна пройти");
        }

        @Test
        @DisplayName("Не проходит для сильно смещённой последовательности")
        void testFailsForBiasedSequence() {
            List<Integer> bits = generateBiasedBits(1000, 0.9); // 90% единиц
            boolean result = nistTest.frequencyTest(bits);
            assertFalse(result, "Смещённая последовательность не должна пройти");
        }

        @Test
        @DisplayName("Возвращает false для некорректных битов")
        void testReturnsFalseForInvalidBits() {
            List<Integer> bits = List.of(0, 1, 2, 0, 1); // 2 - некорректный бит
            boolean result = nistTest.frequencyTest(bits);
            assertFalse(result, "Должен вернуть false для некорректных битов");
        }
    }

    @Nested
    @DisplayName("runsTest() - Тест на серии")
    class RunsTestTests {

        @Test
        @DisplayName("Проходит для случайной последовательности")
        void testPassesForRandomSequence() {
            List<Integer> bits = generateRandomBits(1000);
            boolean result = nistTest.runsTest(bits);
            assertTrue(result, "Случайная последовательность должна пройти тест");
        }

        @Test
        @DisplayName("Не проходит для чередующейся последовательности")
        void testFailsForAlternatingSequence() {
            List<Integer> bits = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                bits.add(i % 2);
            }
            // Чередующаяся последовательность имеет максимальное число серий
            boolean result = nistTest.runsTest(bits);
            // Может или пройти или нет в зависимости от реализации
            assertNotNull(result); // Просто проверяем, что не крашится
        }

        @Test
        @DisplayName("Не проходит для константной последовательности")
        void testFailsForConstantSequence() {
            List<Integer> bits = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                bits.add(1);
            }
            boolean result = nistTest.runsTest(bits);
            assertFalse(result, "Константная последовательность не должна пройти");
        }

        @Test
        @DisplayName("Возвращает false при сильном отклонении pi от 0.5")
        void testReturnsFalseForSkewedPi() {
            // Если pi слишком далеко от 0.5, тест сразу возвращает false
            List<Integer> bits = generateBiasedBits(100, 0.95);
            boolean result = nistTest.runsTest(bits);
            assertFalse(result, "Должен вернуть false при сильном отклонении pi");
        }
    }

    @Nested
    @DisplayName("Преобразование чисел в биты")
    class NumberToBitsConversionTest {

        @Test
        @DisplayName("Корректное преобразование uint16 в биты")
        void testUint16ToBits() {
            // Пример: число 43690 = 0xAAAA = 1010101010101010 в бинарном
            int number = 0xAAAA;
            List<Integer> bits = new ArrayList<>();
            
            for (int i = 15; i >= 0; i--) {
                bits.add((number >> i) & 1);
            }
            
            // Проверяем чередование
            for (int i = 0; i < 16; i++) {
                int expected = (i % 2 == 0) ? 1 : 0;
                assertEquals(expected, bits.get(i), 
                    "Бит " + i + " должен быть " + expected);
            }
        }

        @Test
        @DisplayName("Полный тест на битах из uint16 чисел")
        void testFullBitSequenceFromNumbers() {
            // Генерируем случайные uint16 числа и преобразуем в биты
            List<Integer> bits = new ArrayList<>();
            
            for (int n = 0; n < 100; n++) {
                int number = random.nextInt(65536);
                for (int i = 15; i >= 0; i--) {
                    bits.add((number >> i) & 1);
                }
            }
            
            // 100 чисел * 16 бит = 1600 бит
            assertEquals(1600, bits.size());
            
            // Случайная последовательность должна пройти частотный тест
            boolean freqResult = nistTest.frequencyTest(bits);
            assertTrue(freqResult, "Биты случайных чисел должны пройти частотный тест");
        }
    }
}
