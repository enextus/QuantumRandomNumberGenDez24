package org.ThreeDotsSierpinski;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit тесты для класса RandomNumberProcessor.
 */
@DisplayName("RandomNumberProcessor - обработка случайных чисел")
@Tag("fast")
class RandomNumberProcessorTest {

    private RandomNumberProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new RandomNumberProcessor();
    }

    @Nested
    @DisplayName("processHexToNumbers()")
    class ProcessHexToNumbersTests {

        @Test
        @DisplayName("Корректно преобразует HEX строку в числа")
        void testProcessHexToNumbers() {
            // "0000" -> 0, "FFFF" -> 65535
            String hexData = "0000FFFF";
            List<Integer> numbers = processor.processHexToNumbers(hexData);

            assertEquals(2, numbers.size(), "Должно быть 2 числа");
            assertEquals(0, numbers.get(0), "Первое число должно быть 0");
            assertEquals(65535, numbers.get(1), "Второе число должно быть 65535");
        }

        @Test
        @DisplayName("Преобразует смешанные HEX значения")
        void testProcessMixedHexValues() {
            // "00FF" -> 255, "FF00" -> 65280, "8000" -> 32768
            String hexData = "00FFFF008000";
            List<Integer> numbers = processor.processHexToNumbers(hexData);

            assertEquals(3, numbers.size());
            assertEquals(255, numbers.get(0));    // 00FF
            assertEquals(65280, numbers.get(1));  // FF00
            assertEquals(32768, numbers.get(2));  // 8000
        }

        @Test
        @DisplayName("Обрабатывает нижний регистр")
        void testProcessLowerCaseHex() {
            String hexData = "abcd";
            List<Integer> numbers = processor.processHexToNumbers(hexData);

            assertEquals(1, numbers.size());
            assertEquals(0xABCD, numbers.get(0));
        }

        @Test
        @DisplayName("Выбрасывает исключение для нечётной длины")
        void testThrowsForOddLengthHex() {
            assertThrows(IllegalArgumentException.class, () -> {
                processor.processHexToNumbers("ABC");
            }, "Должно выбросить исключение для нечётной длины");
        }

        @Test
        @DisplayName("Выбрасывает исключение для некорректных символов")
        void testThrowsForInvalidCharacters() {
            assertThrows(IllegalArgumentException.class, () -> {
                processor.processHexToNumbers("GHIJ");
            }, "Должно выбросить исключение для некорректных символов");
        }

        @Test
        @DisplayName("Возвращает пустой список для пустой строки")
        void testEmptyHexString() {
            List<Integer> numbers = processor.processHexToNumbers("");
            assertTrue(numbers.isEmpty(), "Должен вернуть пустой список");
        }
    }

    @Nested
    @DisplayName("generateNumberInRange()")
    class GenerateNumberInRangeTests {

        @Test
        @DisplayName("Минимальное входное значение даёт минимум диапазона")
        void testMinInputGivesMinOutput() {
            long result = processor.generateNumberInRange(0, 0, 100);
            assertEquals(0, result, "0 должно дать минимум диапазона");
        }

        @Test
        @DisplayName("Максимальное входное значение даёт максимум диапазона")
        void testMaxInputGivesMaxOutput() {
            // 65535/65535 = 1.0, 0 + 1.0 * 100 = 100
            long result = processor.generateNumberInRange(65535, 0, 100);
            assertEquals(100, result, "65535 должно дать максимум диапазона");
        }

        @Test
        @DisplayName("Среднее значение даёт середину диапазона")
        void testMiddleInputGivesMiddleOutput() {
            // 32768/65535 ≈ 0.5, 0 + 0.5 * 100 = 50
            long result = processor.generateNumberInRange(32768, 0, 100);
            assertTrue(result >= 49 && result <= 51,
                    "Среднее значение должно быть около середины диапазона");
        }

        @ParameterizedTest
        @DisplayName("Все значения в пределах диапазона")
        @ValueSource(ints = {0, 1, 100, 1000, 10000, 32768, 50000, 65534, 65535})
        void testAllValuesInRange(int input) {
            long result = processor.generateNumberInRange(input, -1000, 1000);
            assertTrue(result >= -1000 && result <= 1000,
                    "Результат " + result + " должен быть в диапазоне [-1000, 1000]");
        }

        @Test
        @DisplayName("Работает с отрицательным минимумом")
        void testNegativeMin() {
            long result = processor.generateNumberInRange(0, -100, 100);
            assertEquals(-100, result);
        }

        @ParameterizedTest
        @DisplayName("Работает с различными диапазонами")
        @CsvSource({
                "0, 0, 10, 0",
                "65535, 0, 10, 10",
                "0, -50, 50, -50",
                "65535, -50, 50, 50"
        })
        void testVariousRanges(int input, long min, long max, long expected) {
            long result = processor.generateNumberInRange(input, min, max);
            assertEquals(expected, result);
        }
    }

    @Nested
    @DisplayName("generateNumberInRange() с явным sourceMax")
    class GenerateNumberInRangeWithSourceMaxTests {

        @Test
        @DisplayName("Работает с uint8 (0-255)")
        void testWithUint8() {
            // 255/255 = 1.0, диапазон 100
            long result = processor.generateNumberInRange(255, 0, 100, 255);
            assertEquals(100, result);

            // 128/255 ≈ 0.5
            result = processor.generateNumberInRange(128, 0, 100, 255);
            assertTrue(result >= 49 && result <= 51);
        }

        @Test
        @DisplayName("Работает с uint16 (0-65535)")
        void testWithUint16() {
            long result = processor.generateNumberInRange(65535, 0, 100, 65535);
            assertEquals(100, result);
        }

        @Test
        @DisplayName("Работает с произвольным sourceMax")
        void testWithCustomSourceMax() {
            // 500/1000 = 0.5, диапазон 0-200
            long result = processor.generateNumberInRange(500, 0, 200, 1000);
            assertEquals(100, result);
        }
    }

    @Nested
    @DisplayName("Распределение чисел")
    class DistributionTests {

        @Test
        @DisplayName("Равномерное распределение для последовательных входных значений")
        void testUniformDistribution() {
            int[] buckets = new int[10];

            // Проходим ВСЕ входные значения от 0 до 65535
            for (int i = 0; i <= 65535; i++) {
                long result = processor.generateNumberInRange(i, 0, 9);
                buckets[(int) result]++;
            }

            // Каждый bucket должен получить ~6554 значений (65536 / 10)
            int expected = 65536 / 10;
            for (int i = 0; i < 10; i++) {
                assertTrue(buckets[i] >= expected - 1 && buckets[i] <= expected + 1,
                        "Bucket " + i + " имеет " + buckets[i] + " элементов (ожидается ~" + expected + ")");
            }
        }

        @Test
        @DisplayName("Граничные значения получают равную вероятность (без bias)")
        void testBoundaryUniformity() {
            int[] buckets = new int[3];

            // Маппим все 65536 входных значений на 3 выходных [0, 1, 2]
            for (int i = 0; i <= 65535; i++) {
                long result = processor.generateNumberInRange(i, 0, 2);
                buckets[(int) result]++;
            }

            // С Math.round() было бы: [16384, 32768, 16384] — bias x2 на середине
            // С Math.floor() должно быть: ~[21845, 21845, 21846] — равномерно
            int expected = 65536 / 3; // 21845
            for (int i = 0; i < 3; i++) {
                assertTrue(buckets[i] >= expected && buckets[i] <= expected + 1,
                        "Значение " + i + " получило " + buckets[i] +
                                " входных (ожидается ~" + expected + "). Bias на границах!");
            }
        }

        @Test
        @DisplayName("Маппинг на uint8 sourceMax — равномерность границ")
        void testBoundaryUniformityUint8() {
            int[] buckets = new int[3];

            for (int i = 0; i <= 255; i++) {
                long result = processor.generateNumberInRange(i, 0, 2, 255);
                buckets[(int) result]++;
            }

            // 256 / 3 ≈ 85, каждый bucket: 85 или 86
            for (int i = 0; i < 3; i++) {
                assertTrue(buckets[i] >= 85 && buckets[i] <= 86,
                        "Значение " + i + " получило " + buckets[i] + " входных (ожидается 85-86)");
            }
        }
    }

}
