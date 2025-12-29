package org.ThreeDotsSierpinski;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.security.SecureRandom;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Набор статистических тестов для проверки качества случайных чисел.
 * 
 * Эти тесты проверяют статистические свойства генератора случайных чисел
 * с использованием SecureRandom как эталона (без реального API).
 * 
 * Для тестирования с реальным API используйте интеграционные тесты.
 */
@DisplayName("Статистические тесты случайности")
class StatisticalRandomnessTest {

    // Размер выборки для тестов
    private static final int SAMPLE_SIZE = 10000;
    
    // Диапазон uint16
    private static final int MIN_VALUE = 0;
    private static final int MAX_VALUE = 65535;
    
    // Сгенерированная выборка
    private static List<Integer> randomSample;
    
    @BeforeAll
    static void generateSample() {
        // Используем SecureRandom для генерации тестовых данных
        SecureRandom random = new SecureRandom();
        randomSample = new ArrayList<>(SAMPLE_SIZE);
        
        for (int i = 0; i < SAMPLE_SIZE; i++) {
            randomSample.add(random.nextInt(MAX_VALUE + 1));
        }
    }

    @Nested
    @DisplayName("Частотный тест (Frequency/Monobit)")
    class FrequencyTest {

        @Test
        @DisplayName("Биты равномерно распределены (0 и 1)")
        void testBitFrequency() {
            int totalBits = 0;
            int oneBits = 0;
            
            for (int number : randomSample) {
                for (int bit = 0; bit < 16; bit++) {
                    totalBits++;
                    if ((number & (1 << bit)) != 0) {
                        oneBits++;
                    }
                }
            }
            
            double proportion = (double) oneBits / totalBits;
            
            // Ожидаем пропорцию близкую к 0.5 (допуск ±3%)
            assertTrue(proportion > 0.47 && proportion < 0.53,
                "Пропорция единиц (" + proportion + ") должна быть близка к 0.5");
        }

        @Test
        @DisplayName("Числа равномерно распределены по диапазону")
        void testNumberFrequency() {
            int numberOfBins = 16;
            int[] bins = new int[numberOfBins];
            int binSize = (MAX_VALUE + 1) / numberOfBins;
            
            for (int number : randomSample) {
                int binIndex = Math.min(number / binSize, numberOfBins - 1);
                bins[binIndex]++;
            }
            
            double expectedCount = (double) SAMPLE_SIZE / numberOfBins;
            
            // Хи-квадрат тест
            double chiSquare = 0.0;
            for (int count : bins) {
                chiSquare += Math.pow(count - expectedCount, 2) / expectedCount;
            }
            
            // Критическое значение для 15 степеней свободы и alpha=0.05 = 24.996
            double criticalValue = 25.0;
            
            assertTrue(chiSquare < criticalValue,
                "Chi-Square (" + chiSquare + ") должен быть меньше " + criticalValue);
        }
    }

    @Nested
    @DisplayName("Тест на серии (Runs Test)")
    class RunsTest {

        @Test
        @DisplayName("Количество серий в допустимых пределах")
        void testRunsCount() {
            // Подсчитываем серии (последовательности выше/ниже медианы)
            double median = calculateMedian(randomSample);
            
            int runs = 1;
            boolean currentAboveMedian = randomSample.get(0) > median;
            
            for (int i = 1; i < randomSample.size(); i++) {
                boolean aboveMedian = randomSample.get(i) > median;
                if (aboveMedian != currentAboveMedian) {
                    runs++;
                    currentAboveMedian = aboveMedian;
                }
            }
            
            // Подсчёт n1 и n2
            int n1 = 0, n2 = 0;
            for (int number : randomSample) {
                if (number > median) n1++;
                else n2++;
            }
            
            // Ожидаемое количество серий
            double expectedRuns = 1.0 + (2.0 * n1 * n2) / (n1 + n2);
            double variance = (2.0 * n1 * n2 * (2.0 * n1 * n2 - n1 - n2)) 
                            / ((n1 + n2) * (n1 + n2) * (n1 + n2 - 1));
            double stdDev = Math.sqrt(variance);
            
            // Z-статистика
            double z = Math.abs(runs - expectedRuns) / stdDev;
            
            // Для alpha=0.05, критическое значение = 1.96
            assertTrue(z < 1.96,
                "Z-статистика (" + z + ") должна быть меньше 1.96");
        }
        
        private double calculateMedian(List<Integer> numbers) {
            List<Integer> sorted = new ArrayList<>(numbers);
            Collections.sort(sorted);
            int size = sorted.size();
            if (size % 2 == 0) {
                return (sorted.get(size/2 - 1) + sorted.get(size/2)) / 2.0;
            } else {
                return sorted.get(size/2);
            }
        }
    }

    @Nested
    @DisplayName("Тест хи-квадрат (Chi-Square)")
    class ChiSquareTest {

        @Test
        @DisplayName("Распределение по 10 интервалам равномерно")
        void testChiSquare10Bins() {
            int numberOfBins = 10;
            int[] bins = new int[numberOfBins];
            int binSize = (MAX_VALUE + 1) / numberOfBins;
            
            for (int number : randomSample) {
                int binIndex = Math.min(number / binSize, numberOfBins - 1);
                bins[binIndex]++;
            }
            
            double expectedCount = (double) SAMPLE_SIZE / numberOfBins;
            double chiSquare = 0.0;
            
            for (int count : bins) {
                chiSquare += Math.pow(count - expectedCount, 2) / expectedCount;
            }
            
            // Критическое значение для 9 степеней свободы и alpha=0.05 = 16.919
            assertTrue(chiSquare < 16.92,
                "Chi-Square (" + chiSquare + ") должен быть меньше 16.92");
        }

        @Test
        @DisplayName("Распределение последних цифр равномерно")
        void testLastDigitDistribution() {
            int[] digitCounts = new int[10];
            
            for (int number : randomSample) {
                digitCounts[number % 10]++;
            }
            
            double expectedCount = (double) SAMPLE_SIZE / 10;
            double chiSquare = 0.0;
            
            for (int count : digitCounts) {
                chiSquare += Math.pow(count - expectedCount, 2) / expectedCount;
            }
            
            // Критическое значение для 9 степеней свободы
            assertTrue(chiSquare < 16.92,
                "Chi-Square для последних цифр (" + chiSquare + ") должен быть меньше 16.92");
        }
    }

    @Nested
    @DisplayName("Тест на автокорреляцию")
    class AutocorrelationTest {

        @Test
        @DisplayName("Нет значимой автокорреляции с лагом 1")
        void testAutocorrelationLag1() {
            double correlation = calculateAutocorrelation(randomSample, 1);
            
            // Для случайных чисел автокорреляция должна быть близка к 0
            assertTrue(Math.abs(correlation) < 0.05,
                "Автокорреляция с лагом 1 (" + correlation + ") должна быть близка к 0");
        }

        @Test
        @DisplayName("Нет значимой автокорреляции с лагом 5")
        void testAutocorrelationLag5() {
            double correlation = calculateAutocorrelation(randomSample, 5);
            
            assertTrue(Math.abs(correlation) < 0.05,
                "Автокорреляция с лагом 5 (" + correlation + ") должна быть близка к 0");
        }

        @Test
        @DisplayName("Нет значимой автокорреляции с лагом 10")
        void testAutocorrelationLag10() {
            double correlation = calculateAutocorrelation(randomSample, 10);
            
            assertTrue(Math.abs(correlation) < 0.05,
                "Автокорреляция с лагом 10 (" + correlation + ") должна быть близка к 0");
        }
        
        private double calculateAutocorrelation(List<Integer> data, int lag) {
            int n = data.size();
            
            // Среднее
            double mean = data.stream().mapToInt(Integer::intValue).average().orElse(0);
            
            // Ковариация и дисперсия
            double covariance = 0.0;
            double variance = 0.0;
            
            for (int i = 0; i < n - lag; i++) {
                covariance += (data.get(i) - mean) * (data.get(i + lag) - mean);
            }
            
            for (int i = 0; i < n; i++) {
                variance += Math.pow(data.get(i) - mean, 2);
            }
            
            covariance /= (n - lag);
            variance /= n;
            
            return covariance / variance;
        }
    }

    @Nested
    @DisplayName("Тест на монотонность")
    class MonotonicityTest {

        @Test
        @DisplayName("Нет длинных монотонных последовательностей")
        void testNoLongMonotonicSequences() {
            int maxIncreasingRun = 0;
            int maxDecreasingRun = 0;
            int currentIncreasing = 1;
            int currentDecreasing = 1;
            
            for (int i = 1; i < randomSample.size(); i++) {
                if (randomSample.get(i) > randomSample.get(i - 1)) {
                    currentIncreasing++;
                    maxIncreasingRun = Math.max(maxIncreasingRun, currentIncreasing);
                    currentDecreasing = 1;
                } else if (randomSample.get(i) < randomSample.get(i - 1)) {
                    currentDecreasing++;
                    maxDecreasingRun = Math.max(maxDecreasingRun, currentDecreasing);
                    currentIncreasing = 1;
                } else {
                    currentIncreasing = 1;
                    currentDecreasing = 1;
                }
            }
            
            // Для 10000 случайных чисел, вероятность серии > 20 очень мала
            assertTrue(maxIncreasingRun < 20,
                "Максимальная возрастающая серия (" + maxIncreasingRun + ") слишком длинная");
            assertTrue(maxDecreasingRun < 20,
                "Максимальная убывающая серия (" + maxDecreasingRun + ") слишком длинная");
        }
    }

    @Nested
    @DisplayName("Тест покрытия диапазона")
    class RangeCoverageTest {

        @Test
        @DisplayName("Минимум и максимум близки к границам диапазона")
        void testMinMaxCoverage() {
            int min = Collections.min(randomSample);
            int max = Collections.max(randomSample);
            
            // Для 10000 чисел в диапазоне 0-65535 минимум и максимум 
            // должны быть близки к границам
            assertTrue(min < 1000, "Минимум (" + min + ") должен быть близок к 0");
            assertTrue(max > 64535, "Максимум (" + max + ") должен быть близок к 65535");
        }

        @Test
        @DisplayName("Все квартили покрыты")
        void testQuartileCoverage() {
            int q1Count = 0, q2Count = 0, q3Count = 0, q4Count = 0;
            int quartileSize = (MAX_VALUE + 1) / 4;
            
            for (int number : randomSample) {
                if (number < quartileSize) q1Count++;
                else if (number < 2 * quartileSize) q2Count++;
                else if (number < 3 * quartileSize) q3Count++;
                else q4Count++;
            }
            
            int expectedPerQuartile = SAMPLE_SIZE / 4;
            double tolerance = expectedPerQuartile * 0.1; // 10% допуск
            
            assertTrue(Math.abs(q1Count - expectedPerQuartile) < tolerance,
                "Q1 count (" + q1Count + ") должен быть около " + expectedPerQuartile);
            assertTrue(Math.abs(q2Count - expectedPerQuartile) < tolerance,
                "Q2 count (" + q2Count + ") должен быть около " + expectedPerQuartile);
            assertTrue(Math.abs(q3Count - expectedPerQuartile) < tolerance,
                "Q3 count (" + q3Count + ") должен быть около " + expectedPerQuartile);
            assertTrue(Math.abs(q4Count - expectedPerQuartile) < tolerance,
                "Q4 count (" + q4Count + ") должен быть около " + expectedPerQuartile);
        }
    }

    @Nested
    @DisplayName("Тест Колмогорова-Смирнова (интеграция)")
    class KSIntegrationTest {

        @Test
        @DisplayName("SecureRandom проходит тест K-S")
        void testSecureRandomPassesKS() {
            KolmogorovSmirnovTest ksTest = new KolmogorovSmirnovTest(MIN_VALUE, MAX_VALUE);
            
            List<Long> longSample = randomSample.stream()
                    .map(Integer::longValue)
                    .toList();
            
            boolean result = ksTest.test(longSample, 0.05);
            assertTrue(result, "SecureRandom должен пройти тест K-S");
        }
    }
}
