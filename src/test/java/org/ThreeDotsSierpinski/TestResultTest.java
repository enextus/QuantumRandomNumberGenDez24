package org.ThreeDotsSierpinski;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit тесты для record TestResult.
 *
 * Покрывает:
 * - Создание и доступ к полям
 * - toString(): ✓ для passed, ✗ для failed
 * - equals/hashCode (record-based)
 */
@DisplayName("TestResult — результат теста случайности")
@Tag("fast")
class TestResultTest {

    @Nested
    @DisplayName("Создание и доступ к полям")
    class FieldAccessTests {

        @Test
        @DisplayName("Поля доступны через accessor-методы")
        void testFieldAccess() {
            TestResult result = new TestResult("K-S Test", true, "D=0.012");

            assertEquals("K-S Test", result.testName());
            assertTrue(result.passed());
            assertEquals("D=0.012", result.statistic());
        }

        @Test
        @DisplayName("Работает с пустыми строками")
        void testEmptyStrings() {
            TestResult result = new TestResult("", false, "");

            assertEquals("", result.testName());
            assertFalse(result.passed());
            assertEquals("", result.statistic());
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringTests {

        @Test
        @DisplayName("Passed → содержит ✓ (U+2713)")
        void testPassedContainsCheckMark() {
            TestResult result = new TestResult("Frequency", true, "p=0.847");
            String str = result.toString();

            assertTrue(str.contains("\u2713"), "Должен содержать ✓ для passed");
            assertTrue(str.contains("p=0.847"), "Должен содержать statistic");
            assertTrue(str.contains("Frequency"), "Должен содержать testName");
        }

        @Test
        @DisplayName("Failed → содержит ✗ (U+2717)")
        void testFailedContainsCrossMark() {
            TestResult result = new TestResult("Chi-Square", false, "χ²=30.5");
            String str = result.toString();

            assertTrue(str.contains("\u2717"), "Должен содержать ✗ для failed");
            assertTrue(str.contains("χ²=30.5"), "Должен содержать statistic");
            assertTrue(str.contains("Chi-Square"), "Должен содержать testName");
        }

        @Test
        @DisplayName("Не содержит ✓ для failed и ✗ для passed")
        void testNoWrongMarks() {
            TestResult passed = new TestResult("T", true, "s");
            TestResult failed = new TestResult("T", false, "s");

            assertFalse(passed.toString().contains("\u2717"), "Passed не должен содержать ✗");
            assertFalse(failed.toString().contains("\u2713"), "Failed не должен содержать ✓");
        }
    }

    @Nested
    @DisplayName("equals() и hashCode()")
    class EqualityTests {

        @Test
        @DisplayName("Одинаковые поля → равны")
        void testEqualResults() {
            TestResult a = new TestResult("Test", true, "p=0.5");
            TestResult b = new TestResult("Test", true, "p=0.5");

            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("Разный passed → не равны")
        void testDifferentPassed() {
            TestResult a = new TestResult("Test", true, "p=0.5");
            TestResult b = new TestResult("Test", false, "p=0.5");

            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("Разный statistic → не равны")
        void testDifferentStatistic() {
            TestResult a = new TestResult("Test", true, "p=0.5");
            TestResult b = new TestResult("Test", true, "p=0.9");

            assertNotEquals(a, b);
        }
    }
}
