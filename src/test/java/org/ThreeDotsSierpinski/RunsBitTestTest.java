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
 * Unit тесты для RunsBitTest (NIST Runs Test).
 *
 * Покрывает:
 * - Случайные данные → passed
 * - Константные данные → failed (pre-test fail: pi далеко от 0.5)
 * - Чередующиеся биты → результат зависит от числа runs
 * - Валидация (null, < 10)
 * - pre-test fail → statistic содержит "pre-test"
 * - getTestName()
 * - Формат statistic (p=...)
 */
@DisplayName("RunsBitTest — тест на серии (Runs)")
@Tag("fast")
class RunsBitTestTest {

    private RunsBitTest runsTest;

    @BeforeEach
    void setUp() {
        runsTest = new RunsBitTest();
    }

    @Nested
    @DisplayName("testWithDetails() — статистические результаты")
    class StatisticalTests {

        @Test
        @DisplayName("Случайные uint16 → passed")
        void testRandomDataPasses() {
            Random rng = new Random(42);
            List<Long> sample = rng.ints(500, 0, 65536)
                    .mapToLong(i -> i).boxed().collect(Collectors.toList());

            TestResult result = runsTest.testWithDetails(sample, 0.05);
            assertTrue(result.passed(), "Случайные данные должны пройти, stat: " + result.statistic());
        }

        @Test
        @DisplayName("Все 0x0000 → failed (pre-test: pi=0)")
        void testAllZerosFails() {
            List<Long> zeros = new ArrayList<>();
            for (int i = 0; i < 100; i++) zeros.add(0L);

            TestResult result = runsTest.testWithDetails(zeros, 0.05);
            assertFalse(result.passed());
            assertTrue(result.statistic().contains("pre-test") || result.statistic().contains("pi"),
                    "Должен содержать информацию о pre-test fail, получено: " + result.statistic());
        }

        @Test
        @DisplayName("Все 0xFFFF → failed (pre-test: pi=1)")
        void testAllOnesFails() {
            List<Long> ones = new ArrayList<>();
            for (int i = 0; i < 100; i++) ones.add(65535L);

            TestResult result = runsTest.testWithDetails(ones, 0.05);
            assertFalse(result.passed());
        }

        @Test
        @DisplayName("50/50 mix 0x0000 и 0xFFFF — баланс pi, но мало runs → может fail")
        void testExtremeBitBlocks() {
            // 50 нулей подряд + 50 единиц подряд: pi≈0.5 но Vn очень мало
            List<Long> blocks = new ArrayList<>();
            for (int i = 0; i < 50; i++) blocks.add(0L);
            for (int i = 0; i < 50; i++) blocks.add(65535L);

            TestResult result = runsTest.testWithDetails(blocks, 0.05);
            // Не утверждаем pass/fail — зависит от структуры, но не должен крашиться
            assertNotNull(result.statistic());
        }

        @Test
        @DisplayName("0x5555 повторяется — высокая частота переходов → может pass")
        void testAlternatingPattern() {
            List<Long> alt = new ArrayList<>();
            for (int i = 0; i < 200; i++) alt.add(0x5555L);

            TestResult result = runsTest.testWithDetails(alt, 0.05);
            // 0x5555 = 0101...0101, внутри каждого числа много переходов
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Одно значение 0 повторяется → pre-test fail (pi=0)")
        void testSingleValueZero() {
            List<Long> data = new ArrayList<>();
            for (int i = 0; i < 50; i++) data.add(0L);

            TestResult result = runsTest.testWithDetails(data, 0.05);
            assertFalse(result.passed());
        }

        @Test
        @DisplayName("Значения дают pi ≈ 0.5 → проходит pre-test")
        void testBalancedPiPassesPreTest() {
            // 0xAAAA = 1010...1010 — ровно 8 единиц из 16 бит
            List<Long> balanced = new ArrayList<>();
            for (int i = 0; i < 200; i++) balanced.add(0xAAAAL);

            TestResult result = runsTest.testWithDetails(balanced, 0.05);
            // pi ≈ 0.5, pre-test пройден. Runs test может pass или fail.
            assertFalse(result.statistic().contains("pre-test"),
                    "pi≈0.5, pre-test не должен fail, stat: " + result.statistic());
        }
    }

    @Nested
    @DisplayName("Валидация входных данных")
    class ValidationTests {

        @Test
        @DisplayName("null → IllegalArgumentException")
        void testNullThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> runsTest.testWithDetails(null, 0.05));
        }

        @Test
        @DisplayName("Менее 10 элементов → IllegalArgumentException")
        void testTooFewThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> runsTest.testWithDetails(List.of(1L, 2L), 0.05));
        }

        @Test
        @DisplayName("Ровно 10 элементов → не бросает")
        void testExactly10Ok() {
            List<Long> ten = new ArrayList<>();
            Random rng = new Random(1);
            for (int i = 0; i < 10; i++) ten.add((long) rng.nextInt(65536));
            assertDoesNotThrow(() -> runsTest.testWithDetails(ten, 0.05));
        }
    }

    @Nested
    @DisplayName("Формат результата и интерфейс")
    class FormatAndInterfaceTests {

        @Test
        @DisplayName("statistic содержит p= или pre-test")
        void testStatisticFormat() {
            Random rng = new Random(42);
            List<Long> sample = rng.ints(100, 0, 65536)
                    .mapToLong(i -> i).boxed().collect(Collectors.toList());

            TestResult result = runsTest.testWithDetails(sample, 0.05);
            assertTrue(result.statistic().contains("p=") || result.statistic().contains("pre-test"),
                    "statistic: " + result.statistic());
        }

        @Test
        @DisplayName("getTestName() содержит 'Runs' или 'Серии'")
        void testName() {
            String name = runsTest.getTestName();
            assertTrue(name.contains("Runs") || name.contains("Серии"),
                    "Имя: " + name);
        }

        @Test
        @DisplayName("Реализует RandomnessTest")
        void testImplements() {
            assertInstanceOf(RandomnessTest.class, runsTest);
        }

        @Test
        @DisplayName("default test() = testWithDetails().passed()")
        void testDefaultMethod() {
            Random rng = new Random(42);
            List<Long> sample = rng.ints(200, 0, 65536)
                    .mapToLong(i -> i).boxed().collect(Collectors.toList());

            assertEquals(
                    runsTest.test(sample, 0.05),
                    runsTest.testWithDetails(sample, 0.05).passed()
            );
        }
    }
}
