package org.ThreeDotsSierpinski;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit тесты для ChiSquareUniformityTest.
 *
 * Покрывает:
 * - testWithDetails: pass для равномерных данных
 * - testWithDetails: fail для смещённых данных
 * - testWithDetails: fail для константы
 * - Валидация входных данных (null, < 10 элементов)
 * - Разные уровни alpha (0.01, 0.05, 0.1)
 * - getTestName()
 * - Интерфейс RandomnessTest
 * - Статистика в TestResult
 */
@DisplayName("ChiSquareUniformityTest — тест хи-квадрат")
@Tag("fast")
class ChiSquareUniformityTestTest {

    private ChiSquareUniformityTest chiTest;

    @BeforeEach
    void setUp() {
        chiTest = new ChiSquareUniformityTest();
    }

    // ========================================================================
    // Статистические тесты
    // ========================================================================

    @Nested
    @DisplayName("testWithDetails() — статистические результаты")
    class StatisticalTests {

        @Test
        @DisplayName("Равномерное распределение → passed")
        void testUniformDistributionPasses() {
            // Равномерно распределённые значения 0..65535
            List<Long> uniform = LongStream.iterate(0, n -> n + 64)
                    .limit(1024)
                    .boxed()
                    .collect(Collectors.toList());

            TestResult result = chiTest.testWithDetails(uniform, 0.05);
            assertTrue(result.passed(), "Равномерное распределение должно пройти, statistic: " + result.statistic());
        }

        @Test
        @DisplayName("java.util.Random → passed")
        void testRandomDistributionPasses() {
            Random rng = new Random(42);
            List<Long> sample = rng.ints(1000, 0, 65536)
                    .mapToLong(i -> i)
                    .boxed()
                    .collect(Collectors.toList());

            TestResult result = chiTest.testWithDetails(sample, 0.05);
            assertTrue(result.passed(), "Random должен пройти, statistic: " + result.statistic());
        }

        @Test
        @DisplayName("Все значения в одном bin → failed")
        void testConcentratedDistributionFails() {
            // Все числа в узком диапазоне 0–100 из 65536
            List<Long> concentrated = new ArrayList<>();
            for (int i = 0; i < 500; i++) {
                concentrated.add((long) (i % 100));
            }

            TestResult result = chiTest.testWithDetails(concentrated, 0.05);
            assertFalse(result.passed(), "Сконцентрированные данные не должны пройти");
        }

        @Test
        @DisplayName("Константная последовательность → failed")
        void testConstantSequenceFails() {
            List<Long> constant = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                constant.add(32768L);
            }

            TestResult result = chiTest.testWithDetails(constant, 0.05);
            assertFalse(result.passed(), "Константа не должна пройти");
        }

        @Test
        @DisplayName("Только нижняя четверть диапазона → failed")
        void testLowerQuarterFails() {
            Random rng = new Random(123);
            List<Long> biased = rng.ints(500, 0, 16384)
                    .mapToLong(i -> i)
                    .boxed()
                    .collect(Collectors.toList());

            TestResult result = chiTest.testWithDetails(biased, 0.05);
            assertFalse(result.passed(), "Данные только из нижней четверти не должны пройти");
        }
    }

    // ========================================================================
    // Уровни alpha
    // ========================================================================

    @Nested
    @DisplayName("Разные уровни alpha")
    class AlphaTests {

        @Test
        @DisplayName("Меньший alpha (строже к отклонению H0) → выше critical → больше выборок pass")
        void testSmallerAlphaPassesMoreOrEqual() {
            Random rng = new Random(99);
            int passedAt01 = 0, passedAt05 = 0, passedAt10 = 0;

            for (int trial = 0; trial < 30; trial++) {
                List<Long> sample = rng.ints(200, 0, 65536)
                        .mapToLong(i -> i)
                        .boxed()
                        .collect(Collectors.toList());

                if (chiTest.testWithDetails(sample, 0.01).passed()) passedAt01++;
                if (chiTest.testWithDetails(sample, 0.05).passed()) passedAt05++;
                if (chiTest.testWithDetails(sample, 0.10).passed()) passedAt10++;
            }

            // alpha=0.01 → criticalValue=30.578 (самый высокий) → проще пройти
            // alpha=0.10 → criticalValue=22.307 (самый низкий)  → сложнее пройти
            assertTrue(passedAt01 >= passedAt05,
                    "alpha=0.01 (crit=30.6) должен пропускать ≥ чем alpha=0.05 (crit=25.0). " +
                            "Было: " + passedAt01 + " vs " + passedAt05);
            assertTrue(passedAt05 >= passedAt10,
                    "alpha=0.05 (crit=25.0) должен пропускать ≥ чем alpha=0.10 (crit=22.3). " +
                            "Было: " + passedAt05 + " vs " + passedAt10);
        }
    }

    // ========================================================================
    // Валидация входных данных
    // ========================================================================

    @Nested
    @DisplayName("Валидация входных данных")
    class ValidationTests {

        @Test
        @DisplayName("null → IllegalArgumentException")
        void testNullThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> chiTest.testWithDetails(null, 0.05));
        }

        @Test
        @DisplayName("Менее 10 элементов → IllegalArgumentException")
        void testTooFewElementsThrows() {
            List<Long> small = List.of(1L, 2L, 3L, 4L, 5L);
            assertThrows(IllegalArgumentException.class,
                    () -> chiTest.testWithDetails(small, 0.05));
        }

        @Test
        @DisplayName("Ровно 10 элементов → не бросает")
        void testExactly10ElementsOk() {
            List<Long> ten = LongStream.range(0, 10).boxed().collect(Collectors.toList());
            assertDoesNotThrow(() -> chiTest.testWithDetails(ten, 0.05));
        }
    }

    // ========================================================================
    // TestResult содержимое
    // ========================================================================

    @Nested
    @DisplayName("Формат TestResult")
    class ResultFormatTests {

        @Test
        @DisplayName("statistic содержит χ² и critical value")
        void testStatisticFormat() {
            List<Long> sample = LongStream.range(0, 100)
                    .map(i -> i * 655)
                    .boxed()
                    .collect(Collectors.toList());

            TestResult result = chiTest.testWithDetails(sample, 0.05);

            assertTrue(result.statistic().contains("χ²") || result.statistic().contains("χ"),
                    "statistic должен содержать символ хи, получено: " + result.statistic());
            assertTrue(result.statistic().contains("crit"),
                    "statistic должен содержать критическое значение, получено: " + result.statistic());
        }
    }

    // ========================================================================
    // Интерфейс RandomnessTest
    // ========================================================================

    @Nested
    @DisplayName("Интерфейс RandomnessTest")
    class InterfaceTests {

        @Test
        @DisplayName("Реализует RandomnessTest")
        void testImplementsInterface() {
            assertInstanceOf(RandomnessTest.class, chiTest);
        }

        @Test
        @DisplayName("getTestName() непустой и осмысленный")
        void testGetTestName() {
            String name = chiTest.getTestName();
            assertNotNull(name);
            assertFalse(name.isEmpty());
            assertTrue(name.toLowerCase().contains("хи") || name.toLowerCase().contains("chi"),
                    "Имя должно содержать 'хи' или 'chi', получено: " + name);
        }

        @Test
        @DisplayName("default test() совпадает с testWithDetails().passed()")
        void testDefaultMethodConsistency() {
            List<Long> sample = LongStream.range(0, 200)
                    .map(i -> i * 327)
                    .boxed()
                    .collect(Collectors.toList());

            boolean defaultResult = chiTest.test(sample, 0.05);
            boolean detailedResult = chiTest.testWithDetails(sample, 0.05).passed();

            assertEquals(defaultResult, detailedResult,
                    "test() и testWithDetails().passed() должны совпадать");
        }
    }
}
