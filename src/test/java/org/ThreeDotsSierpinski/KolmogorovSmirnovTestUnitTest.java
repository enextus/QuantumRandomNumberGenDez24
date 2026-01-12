package org.ThreeDotsSierpinski;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit тесты для класса KolmogorovSmirnovTest.
 * 
 * Примечание: Имя класса KolmogorovSmirnovTestUnitTest чтобы избежать
 * конфликта с основным классом KolmogorovSmirnovTest.
 */
@DisplayName("KolmogorovSmirnovTest - тест равномерности распределения")
class KolmogorovSmirnovTestUnitTest {

    private KolmogorovSmirnovTest ksTest;

    @BeforeEach
    void setUp() {
        // Используем диапазон uint16: 0-65535
        ksTest = new KolmogorovSmirnovTest(0, 65535);
    }

    @Nested
    @DisplayName("Конструкторы")
    class ConstructorTests {

        @Test
        @DisplayName("Конструктор по умолчанию использует uint16 диапазон")
        void testDefaultConstructor() {
            KolmogorovSmirnovTest test = new KolmogorovSmirnovTest();
            assertEquals(0, test.getMinRange());
            assertEquals(65535, test.getMaxRange());
        }

        @Test
        @DisplayName("Конструктор с параметрами устанавливает диапазон")
        void testParameterizedConstructor() {
            KolmogorovSmirnovTest test = new KolmogorovSmirnovTest(100, 200);
            assertEquals(100, test.getMinRange());
            assertEquals(200, test.getMaxRange());
        }

        @Test
        @DisplayName("Выбрасывает исключение если min >= max")
        void testThrowsForInvalidRange() {
            assertThrows(IllegalArgumentException.class, () -> {
                new KolmogorovSmirnovTest(100, 100);
            });
            
            assertThrows(IllegalArgumentException.class, () -> {
                new KolmogorovSmirnovTest(200, 100);
            });
        }
    }

    @Nested
    @DisplayName("getTestName()")
    class GetTestNameTests {

        @Test
        @DisplayName("Возвращает непустое имя теста")
        void testReturnsNonEmptyName() {
            String name = ksTest.getTestName();
            assertNotNull(name);
            assertFalse(name.isEmpty());
        }

        @Test
        @DisplayName("Возвращает осмысленное имя")
        void testReturnsMeaningfulName() {
            String name = ksTest.getTestName();
            assertTrue(name.toLowerCase().contains("колмогоров") || 
                       name.toLowerCase().contains("k-s") ||
                       name.toLowerCase().contains("smirnov"),
                "Имя должно содержать 'Колмогоров' или 'K-S'");
        }
    }

    @Nested
    @DisplayName("test() - валидация входных данных")
    class InputValidationTests {

        @Test
        @DisplayName("Выбрасывает исключение для null списка")
        void testThrowsForNullList() {
            assertThrows(NullPointerException.class, () -> {
                ksTest.test(null, 0.05);
            });
        }

        @Test
        @DisplayName("Выбрасывает исключение для пустого списка")
        void testThrowsForEmptyList() {
            assertThrows(IllegalArgumentException.class, () -> {
                ksTest.test(new ArrayList<>(), 0.05);
            });
        }

        @Test
        @DisplayName("Выбрасывает исключение для слишком маленькой выборки")
        void testThrowsForTooSmallSample() {
            List<Long> smallSample = List.of(1L, 2L, 3L);
            assertThrows(IllegalArgumentException.class, () -> {
                ksTest.test(smallSample, 0.05);
            });
        }

        @ParameterizedTest
        @DisplayName("Выбрасывает исключение для некорректного alpha")
        @ValueSource(doubles = {0.0, -0.1, 1.0, 1.5})
        void testThrowsForInvalidAlpha(double alpha) {
            List<Long> sample = LongStream.range(0, 100).boxed().collect(Collectors.toList());
            assertThrows(IllegalArgumentException.class, () -> {
                ksTest.test(sample, alpha);
            });
        }
    }

    @Nested
    @DisplayName("test() - статистические тесты")
    class StatisticalTests {

        @Test
        @DisplayName("Проходит для равномерно распределённых чисел")
        void testPassesForUniformDistribution() {
            // Создаём равномерно распределённые числа
            List<Long> uniformSample = LongStream.iterate(0, n -> n + 65)
                    .limit(1000)
                    .filter(n -> n <= 65535)
                    .boxed()
                    .collect(Collectors.toList());
            
            boolean result = ksTest.test(uniformSample, 0.05);
            assertTrue(result, "Равномерное распределение должно пройти тест");
        }

        @Test
        @DisplayName("Проходит для случайных чисел от java.util.Random")
        void testPassesForRandomNumbers() {
            Random random = new Random(42); // Фиксированный seed для воспроизводимости
            List<Long> randomSample = random.ints(1000, 0, 65536)
                    .mapToLong(i -> i)
                    .boxed()
                    .collect(Collectors.toList());
            
            boolean result = ksTest.test(randomSample, 0.05);
            assertTrue(result, "Случайные числа должны пройти тест");
        }

        @Test
        @DisplayName("Не проходит для сильно смещённого распределения")
        void testFailsForBiasedDistribution() {
            // Все числа в нижней четверти диапазона
            List<Long> biasedSample = LongStream.iterate(0, n -> n + 16)
                    .limit(1000)
                    .filter(n -> n < 16384) // Только первая четверть
                    .boxed()
                    .collect(Collectors.toList());
            
            // Добавляем больше элементов если нужно
            while (biasedSample.size() < 100) {
                biasedSample.add((long) (Math.random() * 16384));
            }
            
            boolean result = ksTest.test(biasedSample, 0.05);
            assertFalse(result, "Смещённое распределение не должно пройти тест");
        }

        @Test
        @DisplayName("Не проходит для константной последовательности")
        void testFailsForConstantSequence() {
            List<Long> constantSample = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                constantSample.add(32768L); // Все числа одинаковые
            }
            
            boolean result = ksTest.test(constantSample, 0.05);
            assertFalse(result, "Константная последовательность не должна пройти тест");
        }

        @Test
        @DisplayName("Более строгий alpha отклоняет больше выборок")
        void testStricterAlphaRejectsMore() {
            Random random = new Random(123);
            int passedAt01 = 0;
            int passedAt05 = 0;
            
            for (int trial = 0; trial < 20; trial++) {
                List<Long> sample = random.ints(100, 0, 65536)
                        .mapToLong(i -> i)
                        .boxed()
                        .collect(Collectors.toList());
                
                if (ksTest.test(sample, 0.01)) passedAt01++;
                if (ksTest.test(sample, 0.05)) passedAt05++;
            }
            
            assertTrue(passedAt01 <= passedAt05, 
                "alpha=0.01 должен пропускать не больше выборок чем alpha=0.05");
        }
    }

    @Nested
    @DisplayName("Интеграция с RandomnessTest интерфейсом")
    class InterfaceTests {

        @Test
        @DisplayName("Реализует интерфейс RandomnessTest")
        void testImplementsInterface() {
            assertTrue(ksTest instanceof RandomnessTest);
        }

        @Test
        @DisplayName("Может использоваться полиморфно")
        void testPolymorphicUsage() {
            RandomnessTest test = new KolmogorovSmirnovTest();
            
            List<Long> sample = LongStream.range(0, 100)
                    .map(i -> i * 655)
                    .boxed()
                    .collect(Collectors.toList());
            
            // Должен работать через интерфейс
            assertDoesNotThrow(() -> test.test(sample, 0.05));
            assertNotNull(test.getTestName());
        }
    }
}
