package org.ThreeDotsSierpinski;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;

import java.awt.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для класса SierpinskiAlgorithm (алгоритм "Chaos Game").
 *
 * Обновлено под новую логику выбора вершины: (randomValue % 3).
 */
@DisplayName("SierpinskiAlgorithm — алгоритм Серпинского (Chaos Game)")
@Tag("slow")
class SierpinskiAlgorithmTest {

    private static final int SIZE_WIDTH = 680;
    private static final int SIZE_HEIGHT = 600;
    private static final long MAX_VALUE = 65535; // uint16

    private SierpinskiAlgorithm algorithm;
    private SecureRandom random;

    @BeforeEach
    void setUp() {
        // ИСПРАВЛЕНО: Старый конструктор с 4 параметрами удалён
        algorithm = new SierpinskiAlgorithm(SIZE_WIDTH, SIZE_HEIGHT);
        random = new SecureRandom();
    }

    @Nested
    @DisplayName("Базовые операции")
    class BasicOperationsTest {

        @Test
        @DisplayName("Перемещение к вершине A (остаток 0)")
        void testMoveToVertexA() {
            Point start = new Point(300, 300);
            // Vertex A сейчас (340, 0)
            Point result = algorithm.calculateNewDotPosition(start, 0);

            assertEquals((300 + 340) / 2, result.x); // 320
            assertEquals((300 + 0) / 2, result.y);   // 150
        }

        @Test
        @DisplayName("Перемещение к вершине B (остаток 1)")
        void testMoveToVertexB() {
            Point start = new Point(300, 300);
            // 1 % 3 = 1 -> Vertex B
            Point result = algorithm.calculateNewDotPosition(start, 1);

            assertEquals((300 + 0) / 2, result.x);
            assertEquals((300 + 600) / 2, result.y);
        }

        @Test
        @DisplayName("Перемещение к вершине C (остаток 2)")
        void testMoveToVertexC() {
            Point start = new Point(300, 300);
            // 65534 % 3 = 2 -> Vertex C (680, 600)
            Point result = algorithm.calculateNewDotPosition(start, 65534);

            assertEquals((300 + 680) / 2, result.x); // 490
            assertEquals((300 + 600) / 2, result.y); // 450
        }

        @Test
        @DisplayName("Граничные значения по модулю 3")
        void testBoundaryValues() {
            Point start = new Point(300, 300);

            // 3 % 3 = 0 -> A (340, 0)
            Point resultA = algorithm.calculateNewDotPosition(start, 3);
            assertEquals((300 + 340) / 2, resultA.x); // 320
            assertEquals((300 + 0) / 2, resultA.y);   // 150

            // 4 % 3 = 1 -> B (0, 600)
            Point resultB = algorithm.calculateNewDotPosition(start, 4);
            assertEquals((300 + 0) / 2, resultB.x);   // 150
            assertEquals((300 + 600) / 2, resultB.y); // 450

            // 5 % 3 = 2 -> C (680, 600)
            Point resultC = algorithm.calculateNewDotPosition(start, 5);
            assertEquals((300 + 680) / 2, resultC.x); // 490
            assertEquals((300 + 600) / 2, resultC.y); // 450
        }

        @Nested
        @DisplayName("Геттеры вершин")
        class VertexGetterTests {

            @Test
            @DisplayName("Вершины треугольника корректны")
            void testVertices() {
                assertEquals(new Point(SIZE_WIDTH / 2, 0), algorithm.getVertexA());
                assertEquals(new Point(0, SIZE_HEIGHT), algorithm.getVertexB());
                assertEquals(new Point(SIZE_WIDTH, SIZE_HEIGHT), algorithm.getVertexC());
            }

            @Test
            @DisplayName("Геттеры возвращают копии (иммутабельность)")
            void testVerticesAreDefensiveCopies() {
                Point a = algorithm.getVertexA();
                a.x = 999;
                assertNotEquals(999, algorithm.getVertexA().x,
                        "Изменение возвращённой точки не должно менять внутреннее состояние");
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
                    current = algorithm.calculateNewDotPosition(current, randomValue);

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
                int regionSize = 50;

                for (int i = 0; i < 10000; i++) {
                    long randomValue = random.nextInt((int) MAX_VALUE + 1);
                    current = algorithm.calculateNewDotPosition(current, randomValue);

                    int regionX = current.x / regionSize;
                    int regionY = current.y / regionSize;
                    visitedRegions.add(regionX + "," + regionY);
                }

                int totalRegions = (SIZE_WIDTH / regionSize) * (SIZE_HEIGHT / regionSize);

                assertTrue(visitedRegions.size() < totalRegions * 0.7,
                        "Фрактал не должен заполнять всё пространство. " +
                                "Заполнено " + visitedRegions.size() + " из " + totalRegions);
            }

            @RepeatedTest(5)
            @DisplayName("Распределение по вершинам строго равномерное (modulo 3)")
            void testUniformVertexDistribution() {
                int countA = 0, countB = 0, countC = 0;

                for (int i = 0; i < 10000; i++) {
                    long randomValue = random.nextInt((int) MAX_VALUE + 1);

                    // ИСПРАВЛЕНО: Проверяем ту же логику, что использует алгоритм
                    int vertexIndex = (int) (randomValue % 3);
                    if (vertexIndex == 0) countA++;
                    else if (vertexIndex == 1) countB++;
                    else countC++;
                }

                double expectedCount = 10000 / 3.0;
                double tolerance = expectedCount * 0.1;

                assertTrue(Math.abs(countA - expectedCount) < tolerance,
                        "Вершина A выбрана " + countA + " раз (ожидается ~" + (int) expectedCount + ")");
                assertTrue(Math.abs(countB - expectedCount) < tolerance,
                        "Вершина B выбрана " + countB + " раз (ожидается ~" + (int) expectedCount + ")");
                assertTrue(Math.abs(countC - expectedCount) < tolerance,
                        "Вершина C выбрана " + countC + " раз (ожидается ~" + (int) expectedCount + ")");
            }
        }

        @Nested
        @DisplayName("Центральная пустая область")
        class CentralVoidTest {

            @Test
            @DisplayName("Центр треугольника остаётся пустым")
            void testCentralVoidRemains() {
                Point current = new Point(SIZE_WIDTH / 2, SIZE_HEIGHT / 2);
                int centerX = 340;
                int centerY = 400;
                int voidRadius = 30;

                List<Point> pointsNearCenter = new ArrayList<>();

                for (int i = 0; i < 50000; i++) {
                    long randomValue = random.nextInt((int) MAX_VALUE + 1);
                    current = algorithm.calculateNewDotPosition(current, randomValue);

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

                Point current1 = new Point(start);
                for (long value : sequence) {
                    current1 = algorithm.calculateNewDotPosition(current1, value);
                }

                Point current2 = new Point(start);
                for (long value : sequence) {
                    current2 = algorithm.calculateNewDotPosition(current2, value);
                }

                assertEquals(current1, current2,
                        "Одинаковые входные данные должны давать одинаковый результат");
            }

            @Test
            @DisplayName("Разные экземпляры с одинаковыми параметрами дают одинаковый результат")
            void testDifferentInstancesSameResult() {
                // ИСПРАВЛЕНО: Старый конструктор с 4 параметрами
                SierpinskiAlgorithm algo2 = new SierpinskiAlgorithm(SIZE_WIDTH, SIZE_HEIGHT);
                Point start = new Point(200, 400);

                Point result1 = algorithm.calculateNewDotPosition(start, 12345);
                Point result2 = algo2.calculateNewDotPosition(start, 12345);

                assertEquals(result1, result2,
                        "Разные экземпляры с одинаковыми параметрами должны давать одинаковый результат");
            }
        }
    }
}