package org.ThreeDotsSierpinski;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.awt.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для алгоритма построения треугольника Серпинского.
 * 
 * Алгоритм "Chaos Game":
 * 1. Начинаем с произвольной точки внутри треугольника
 * 2. Выбираем случайную вершину
 * 3. Перемещаемся на половину расстояния к выбранной вершине
 * 4. Повторяем шаги 2-3
 */
@DisplayName("Алгоритм Серпинского (Chaos Game)")
class SierpinskiAlgorithmTest {

    // Размеры панели из конфигурации
    private static final int SIZE_WIDTH = 600;
    private static final int SIZE_HEIGHT = 600;
    
    // Диапазон случайных чисел uint16
    private static final long MIN_VALUE = 0;
    private static final long MAX_VALUE = 65535;
    
    // Вершины треугольника
    private Point vertexA; // Верхняя
    private Point vertexB; // Нижняя левая
    private Point vertexC; // Нижняя правая
    
    private SecureRandom random;

    @BeforeEach
    void setUp() {
        vertexA = new Point(SIZE_WIDTH / 2, 0);
        vertexB = new Point(0, SIZE_HEIGHT);
        vertexC = new Point(SIZE_WIDTH, SIZE_HEIGHT);
        random = new SecureRandom();
    }

    /**
     * Вычисляет новую позицию точки по алгоритму Chaos Game.
     * Это копия логики из DotController.calculateNewDotPosition()
     */
    private Point calculateNewDotPosition(Point currentPoint, long randomValue) {
        long rangePart = (MAX_VALUE - MIN_VALUE) / 3;
        int x = currentPoint.x;
        int y = currentPoint.y;
        
        if (randomValue <= MIN_VALUE + rangePart) {
            // К вершине A
            x = (x + vertexA.x) / 2;
            y = (y + vertexA.y) / 2;
        } else if (randomValue <= MIN_VALUE + 2 * rangePart) {
            // К вершине B
            x = (x + vertexB.x) / 2;
            y = (y + vertexB.y) / 2;
        } else {
            // К вершине C
            x = (x + vertexC.x) / 2;
            y = (y + vertexC.y) / 2;
        }
        
        return new Point(x, y);
    }

    @Nested
    @DisplayName("Базовые операции")
    class BasicOperationsTest {

        @Test
        @DisplayName("Перемещение к вершине A (малые значения)")
        void testMoveToVertexA() {
            Point start = new Point(300, 300);
            Point result = calculateNewDotPosition(start, 0);
            
            // Должны переместиться к верхней вершине
            assertEquals((300 + 300) / 2, result.x); // (300 + 300) / 2 = 300
            assertEquals((300 + 0) / 2, result.y);   // (300 + 0) / 2 = 150
        }

        @Test
        @DisplayName("Перемещение к вершине B (средние значения)")
        void testMoveToVertexB() {
            Point start = new Point(300, 300);
            long middleValue = MAX_VALUE / 2; // ~32767
            Point result = calculateNewDotPosition(start, middleValue);
            
            // Должны переместиться к нижней левой вершине
            assertEquals((300 + 0) / 2, result.x);   // (300 + 0) / 2 = 150
            assertEquals((300 + 600) / 2, result.y); // (300 + 600) / 2 = 450
        }

        @Test
        @DisplayName("Перемещение к вершине C (большие значения)")
        void testMoveToVertexC() {
            Point start = new Point(300, 300);
            Point result = calculateNewDotPosition(start, MAX_VALUE);
            
            // Должны переместиться к нижней правой вершине
            assertEquals((300 + 600) / 2, result.x); // (300 + 600) / 2 = 450
            assertEquals((300 + 600) / 2, result.y); // (300 + 600) / 2 = 450
        }
    }

    @Nested
    @DisplayName("Свойства алгоритма")
    class AlgorithmPropertiesTest {

        @Test
        @DisplayName("Точки остаются внутри треугольника")
        void testPointsStayInsideTriangle() {
            Point current = new Point(SIZE_WIDTH / 2, SIZE_HEIGHT / 2);
            
            for (int i = 0; i < 1000; i++) {
                long randomValue = random.nextInt((int) MAX_VALUE + 1);
                current = calculateNewDotPosition(current, randomValue);
                
                // Проверяем границы
                assertTrue(current.x >= 0 && current.x <= SIZE_WIDTH,
                    "X координата " + current.x + " вне границ");
                assertTrue(current.y >= 0 && current.y <= SIZE_HEIGHT,
                    "Y координата " + current.y + " вне границ");
            }
        }

        @Test
        @DisplayName("Точки сходятся к фрактальной структуре")
        void testConvergenceToFractal() {
            Point current = new Point(SIZE_WIDTH / 2, SIZE_HEIGHT / 2);
            Set<String> visitedRegions = new HashSet<>();
            
            // Делим пространство на регионы 50x50 пикселей
            int regionSize = 50;
            
            for (int i = 0; i < 10000; i++) {
                long randomValue = random.nextInt((int) MAX_VALUE + 1);
                current = calculateNewDotPosition(current, randomValue);
                
                int regionX = current.x / regionSize;
                int regionY = current.y / regionSize;
                visitedRegions.add(regionX + "," + regionY);
            }
            
            // Фрактал Серпинского не заполняет всё пространство
            // Максимум регионов: (600/50) * (600/50) = 144
            // Фрактал должен занимать значительно меньше
            int totalRegions = (SIZE_WIDTH / regionSize) * (SIZE_HEIGHT / regionSize);
            
            assertTrue(visitedRegions.size() < totalRegions * 0.7,
                "Фрактал не должен заполнять всё пространство. " +
                "Заполнено " + visitedRegions.size() + " из " + totalRegions);
        }

        @RepeatedTest(5)
        @DisplayName("Распределение по вершинам примерно равномерное")
        void testUniformVertexDistribution() {
            int countA = 0, countB = 0, countC = 0;
            long rangePart = (MAX_VALUE - MIN_VALUE) / 3;
            
            for (int i = 0; i < 10000; i++) {
                long randomValue = random.nextInt((int) MAX_VALUE + 1);
                
                if (randomValue <= MIN_VALUE + rangePart) {
                    countA++;
                } else if (randomValue <= MIN_VALUE + 2 * rangePart) {
                    countB++;
                } else {
                    countC++;
                }
            }
            
            // Каждая вершина должна быть выбрана примерно в 33% случаев
            double expectedCount = 10000 / 3.0;
            double tolerance = expectedCount * 0.1; // 10% допуск
            
            assertTrue(Math.abs(countA - expectedCount) < tolerance,
                "Вершина A выбрана " + countA + " раз (ожидается ~" + (int)expectedCount + ")");
            assertTrue(Math.abs(countB - expectedCount) < tolerance,
                "Вершина B выбрана " + countB + " раз (ожидается ~" + (int)expectedCount + ")");
            assertTrue(Math.abs(countC - expectedCount) < tolerance,
                "Вершина C выбрана " + countC + " раз (ожидается ~" + (int)expectedCount + ")");
        }
    }

    @Nested
    @DisplayName("Центральная пустая область")
    class CentralVoidTest {

        @Test
        @DisplayName("Центр треугольника остаётся пустым")
        void testCentralVoidRemains() {
            Point current = new Point(SIZE_WIDTH / 2, SIZE_HEIGHT / 2);
            
            // Центр треугольника примерно в (300, 400) для треугольника с вершинами
            // A(300,0), B(0,600), C(600,600)
            // Центр масс: ((300+0+600)/3, (0+600+600)/3) = (300, 400)
            int centerX = 300;
            int centerY = 400;
            int voidRadius = 30; // Радиус пустой области
            
            List<Point> pointsNearCenter = new ArrayList<>();
            
            for (int i = 0; i < 50000; i++) {
                long randomValue = random.nextInt((int) MAX_VALUE + 1);
                current = calculateNewDotPosition(current, randomValue);
                
                // Пропускаем первые 100 итераций (начальная сходимость)
                if (i > 100) {
                    double distToCenter = Math.sqrt(
                        Math.pow(current.x - centerX, 2) + 
                        Math.pow(current.y - centerY, 2)
                    );
                    
                    if (distToCenter < voidRadius) {
                        pointsNearCenter.add(new Point(current));
                    }
                }
            }
            
            // Центральная область должна содержать мало точек
            assertTrue(pointsNearCenter.size() < 100,
                "Центральная область содержит " + pointsNearCenter.size() + 
                " точек (должно быть мало)");
        }
    }

    @Nested
    @DisplayName("Воспроизводимость")
    class ReproducibilityTest {

        @Test
        @DisplayName("Одинаковые входные данные дают одинаковый результат")
        void testDeterminism() {
            Point start = new Point(250, 350);
            long[] sequence = {1000, 30000, 60000, 15000, 45000};
            
            // Первый проход
            Point current1 = new Point(start);
            for (long value : sequence) {
                current1 = calculateNewDotPosition(current1, value);
            }
            
            // Второй проход
            Point current2 = new Point(start);
            for (long value : sequence) {
                current2 = calculateNewDotPosition(current2, value);
            }
            
            assertEquals(current1, current2, 
                "Одинаковые входные данные должны давать одинаковый результат");
        }
    }
}
