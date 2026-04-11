package org.ThreeDotsSierpinski;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit тесты для FrequencyBitTest (NIST Frequency/Monobit Test).
 *
 * Покрывает:
 * - Сбалансированные биты → passed
 * - Все нули (0x0000) → failed
 * - Все единицы (0xFFFF) → failed
 * - Смесь 0 и 0xFFFF → passed (баланс)
 * - Валидация (null, < 10)
 * - getTestName()
 * - Формат statistic (p=...)
 * - default test() = testWithDetails().passed()
 */
@DisplayName("FrequencyBitTest — частотный тест (Monobit)")
@Tag("fast")
class FrequencyBitTestTest {

    private FrequencyBitTest freqTest;

    @BeforeEach
    void setUp() {
        freqTest = new FrequencyBitTest();
    }

    @Nested
    @DisplayName("testWithDetails() — статистические результаты")
    class StatisticalTests {

        @Test
        @DisplayName("Случайные uint16 → passed")
        void testRandomDataPasses() {
            Random rng = new Random(42);
            List<Long> sample = rng.ints(500, 0, 65536)
                    .mapToLong(i -> i)
                    .boxed()
                    .collect(Collectors.toList());

            TestResult result = freqTest.testWithDetails(sample, 0.05);
            assertTrue(result.passed(), "Случайные данные должны пройти, stat: " + result.statistic());
        }

        @Test
        @DisplayName("Все 0x0000 (все биты = 0) → failed")
        void testAllZerosFails() {
            List<Long> zeros = new ArrayList<>();
            for (int i = 0; i < 100; i++) zeros.add(0L);

            TestResult result = freqTest.testWithDetails(zeros, 0.05);
            assertFalse(result.passed(), "Все нули → все биты = 0, тест не пройден");
        }

        @Test
        @DisplayName("Все 0xFFFF (все биты = 1) → failed")
        void testAllOnesFails() {
            List<Long> ones = new ArrayList<>();
            for (int i = 0; i < 100; i++) ones.add(65535L);

            TestResult result = freqTest.testWithDetails(ones, 0.05);
            assertFalse(result.passed(), "Все 0xFFFF → все биты = 1, тест не пройден");
        }

        @Test
        @DisplayName("50/50 mix: 0x0000 и 0xFFFF → passed (баланс)")
        void testBalancedMixPasses() {
            List<Long> balanced = new ArrayList<>();
            for (int i = 0; i < 250; i++) {
                balanced.add(0L);
                balanced.add(65535L);
            }

            TestResult result = freqTest.testWithDetails(balanced, 0.05);
            assertTrue(result.passed(), "50/50 баланс нулей и единиц, stat: " + result.statistic());
        }

        @Test
        @DisplayName("Сильно смещённые данные (90% 0xFFFF) → failed")
        void testBiasedDataFails() {
            List<Long> biased = new ArrayList<>();
            for (int i = 0; i < 900; i++) biased.add(65535L);
            for (int i = 0; i < 100; i++) biased.add(0L);

            TestResult result = freqTest.testWithDetails(biased, 0.05);
            assertFalse(result.passed(), "90% единиц не должны пройти");
        }

        @Test
        @DisplayName("0x5555 (0101...0101) — идеальный баланс → passed")
        void testAlternatingBitsPasses() {
            List<Long> alternating = new ArrayList<>();
            for (int i = 0; i < 200; i++) alternating.add(0x5555L); // 0101010101010101

            TestResult result = freqTest.testWithDetails(alternating, 0.05);
            assertTrue(result.passed(), "0x5555 имеет ровно 8 единиц и 8 нулей, stat: " + result.statistic());
        }
    }

    @Nested
    @DisplayName("Валидация входных данных")
    class ValidationTests {

        @Test
        @DisplayName("null → IllegalArgumentException")
        void testNullThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> freqTest.testWithDetails(null, 0.05));
        }

        @Test
        @DisplayName("Менее 10 элементов → IllegalArgumentException")
        void testTooFewThrows() {
            List<Long> small = List.of(1L, 2L, 3L);
            assertThrows(IllegalArgumentException.class,
                    () -> freqTest.testWithDetails(small, 0.05));
        }

        @Test
        @DisplayName("Ровно 10 элементов → не бросает")
        void testExactly10Ok() {
            List<Long> ten = new ArrayList<>();
            for (int i = 0; i < 10; i++) ten.add((long) (i * 6553));
            assertDoesNotThrow(() -> freqTest.testWithDetails(ten, 0.05));
        }
    }

    @Nested
    @DisplayName("Формат результата")
    class ResultFormatTests {

        @Test
        @DisplayName("statistic содержит p=")
        void testStatisticContainsP() {
            List<Long> sample = new ArrayList<>();
            Random rng = new Random(7);
            for (int i = 0; i < 100; i++) sample.add((long) rng.nextInt(65536));

            TestResult result = freqTest.testWithDetails(sample, 0.05);
            assertTrue(result.statistic().startsWith("p="),
                    "statistic должен начинаться с p=, получено: " + result.statistic());
        }

        @Test
        @DisplayName("testName содержит 'Frequency' или 'Частотный'")
        void testName() {
            String name = freqTest.getTestName();
            assertTrue(name.contains("Frequency") || name.contains("Частотный"),
                    "Имя: " + name);
        }
    }

    @Nested
    @DisplayName("Совместимость с интерфейсом")
    class InterfaceTests {

        @Test
        @DisplayName("Реализует RandomnessTest")
        void testImplements() {
            assertInstanceOf(RandomnessTest.class, freqTest);
        }

        @Test
        @DisplayName("default test() = testWithDetails().passed()")
        void testDefaultMethod() {
            Random rng = new Random(42);
            List<Long> sample = rng.ints(200, 0, 65536)
                    .mapToLong(i -> i).boxed().collect(Collectors.toList());

            assertEquals(
                    freqTest.test(sample, 0.05),
                    freqTest.testWithDetails(sample, 0.05).passed()
            );
        }
    }
}
