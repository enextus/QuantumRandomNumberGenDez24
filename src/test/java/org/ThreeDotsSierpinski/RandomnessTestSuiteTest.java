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
 * Unit тесты для RandomnessTestSuite.
 *
 * Покрывает:
 * - runAll: все 4 теста запускаются
 * - runAll: случайные данные → большинство pass
 * - runAll: константные данные → все fail
 * - runAll: exception в тесте → TestResult с passed=false и "error"
 * - formatResults: формат отчёта
 * - formatResults: содержит итого
 */
@DisplayName("RandomnessTestSuite — набор тестов случайности")
@Tag("fast")
class RandomnessTestSuiteTest {

    private RandomnessTestSuite suite;

    @BeforeEach
    void setUp() {
        suite = new RandomnessTestSuite();
    }

    @Nested
    @DisplayName("runAll()")
    class RunAllTests {

        @Test
        @DisplayName("Запускает все 4 зарегистрированных теста")
        void testRunsAllFourTests() {
            Random rng = new Random(42);
            List<Long> sample = rng.ints(500, 0, 65536)
                    .mapToLong(i -> i).boxed().collect(Collectors.toList());

            List<TestResult> results = suite.runAll(sample, 0.05);

            assertEquals(4, results.size(), "Должно быть 4 результата (K-S, Frequency, Chi-Square, Runs)");
        }

        @Test
        @DisplayName("Каждый результат имеет уникальное testName")
        void testUniqueTestNames() {
            Random rng = new Random(42);
            List<Long> sample = rng.ints(500, 0, 65536)
                    .mapToLong(i -> i).boxed().collect(Collectors.toList());

            List<TestResult> results = suite.runAll(sample, 0.05);
            long uniqueNames = results.stream()
                    .map(TestResult::testName)
                    .distinct()
                    .count();

            assertEquals(4, uniqueNames, "Все 4 теста должны иметь разные имена");
        }

        @Test
        @DisplayName("Случайные данные → большинство тестов pass")
        void testRandomDataMostlyPasses() {
            Random rng = new Random(42);
            List<Long> sample = rng.ints(1000, 0, 65536)
                    .mapToLong(i -> i).boxed().collect(Collectors.toList());

            List<TestResult> results = suite.runAll(sample, 0.05);
            long passed = results.stream().filter(TestResult::passed).count();

            assertTrue(passed >= 3,
                    "Для 1000 случайных чисел минимум 3 из 4 тестов должны пройти, прошло: " + passed);
        }

        @Test
        @DisplayName("Константные данные → все тесты fail")
        void testConstantDataAllFail() {
            List<Long> constant = new ArrayList<>();
            for (int i = 0; i < 500; i++) constant.add(32768L);

            List<TestResult> results = suite.runAll(constant, 0.05);
            long passed = results.stream().filter(TestResult::passed).count();

            assertEquals(0, passed,
                    "Константные данные: ни один тест не должен пройти, прошло: " + passed);
        }

        @Test
        @DisplayName("Каждый результат содержит непустой statistic")
        void testAllResultsHaveStatistic() {
            Random rng = new Random(42);
            List<Long> sample = rng.ints(200, 0, 65536)
                    .mapToLong(i -> i).boxed().collect(Collectors.toList());

            List<TestResult> results = suite.runAll(sample, 0.05);

            for (TestResult result : results) {
                assertNotNull(result.statistic(),
                        "statistic не должен быть null для " + result.testName());
                assertFalse(result.statistic().isEmpty(),
                        "statistic не должен быть пустым для " + result.testName());
            }
        }

        @Test
        @DisplayName("Содержит все ожидаемые тесты по имени")
        void testContainsExpectedTests() {
            Random rng = new Random(42);
            List<Long> sample = rng.ints(200, 0, 65536)
                    .mapToLong(i -> i).boxed().collect(Collectors.toList());

            List<TestResult> results = suite.runAll(sample, 0.05);
            String allNames = results.stream()
                    .map(TestResult::testName)
                    .collect(Collectors.joining(", "));

            // Проверяем наличие каждого теста (по частичному совпадению имени)
            assertTrue(allNames.contains("Колмогоров") || allNames.contains("K-S"),
                    "Должен содержать KS тест, имена: " + allNames);
            assertTrue(allNames.contains("Частотный") || allNames.contains("Frequency"),
                    "Должен содержать Frequency тест, имена: " + allNames);
            assertTrue(allNames.contains("Хи") || allNames.contains("Chi"),
                    "Должен содержать Chi-Square тест, имена: " + allNames);
            assertTrue(allNames.contains("Серии") || allNames.contains("Runs"),
                    "Должен содержать Runs тест, имена: " + allNames);
        }
    }

    @Nested
    @DisplayName("Обработка ошибок в тестах")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Слишком мало данных → TestResult с error (не крашит suite)")
        void testTooFewDataHandledGracefully() {
            List<Long> tiny = List.of(1L, 2L, 3L);

            // Отдельные тесты бросают IllegalArgumentException для <10 элементов
            // suite должен поймать и вернуть error result
            List<TestResult> results = suite.runAll(tiny, 0.05);

            assertEquals(4, results.size(), "Все 4 результата должны вернуться");
            for (TestResult result : results) {
                assertFalse(result.passed(), "С 3 элементами ни один тест не должен пройти");
                assertTrue(result.statistic().contains("error"),
                        "statistic должен содержать 'error', получено: " + result.statistic()
                                + " для " + result.testName());
            }
        }
    }

    @Nested
    @DisplayName("formatResults()")
    class FormatResultsTests {

        @Test
        @DisplayName("Содержит итого с количеством пройденных")
        void testFormatContainsSummary() {
            List<TestResult> results = List.of(
                    new TestResult("Test A", true, "p=0.9"),
                    new TestResult("Test B", false, "p=0.001"),
                    new TestResult("Test C", true, "p=0.5")
            );

            String report = RandomnessTestSuite.formatResults(results);

            assertTrue(report.contains("2/3"), "Должен содержать '2/3' пройденных");
            assertTrue(report.contains("Итого") || report.contains("итого"),
                    "Должен содержать слово 'Итого'");
        }

        @Test
        @DisplayName("Содержит имена всех тестов")
        void testFormatContainsAllTestNames() {
            List<TestResult> results = List.of(
                    new TestResult("Alpha Test", true, "stat1"),
                    new TestResult("Beta Test", false, "stat2")
            );

            String report = RandomnessTestSuite.formatResults(results);

            assertTrue(report.contains("Alpha Test"));
            assertTrue(report.contains("Beta Test"));
        }

        @Test
        @DisplayName("Содержит ✓ и ✗")
        void testFormatContainsMarks() {
            List<TestResult> results = List.of(
                    new TestResult("P", true, "s"),
                    new TestResult("F", false, "s")
            );

            String report = RandomnessTestSuite.formatResults(results);

            assertTrue(report.contains("\u2713"), "Должен содержать ✓");
            assertTrue(report.contains("\u2717"), "Должен содержать ✗");
        }

        @Test
        @DisplayName("Пустой список → '0/0'")
        void testFormatEmptyList() {
            String report = RandomnessTestSuite.formatResults(List.of());

            assertTrue(report.contains("0/0"), "Пустой список: '0/0'");
        }
    }
}
