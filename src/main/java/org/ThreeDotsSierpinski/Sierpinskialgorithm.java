package org.ThreeDotsSierpinski;

import java.awt.Point;

/**
 * Алгоритм построения фрактала Серпинского методом "Chaos Game".
 *
 * Правила:
 * 1. Начинаем с произвольной точки внутри треугольника
 * 2. По случайному числу выбираем одну из трёх вершин
 * 3. Перемещаемся на половину расстояния к выбранной вершине
 * 4. Повторяем шаги 2-3
 *
 * Этот класс содержит только математику — никаких зависимостей от Swing/UI.
 */
public class SierpinskiAlgorithm {

    private final Point vertexA; // Верхняя вершина
    private final Point vertexB; // Нижняя левая
    private final Point vertexC; // Нижняя правая

    private final long minRandomValue;
    private final long maxRandomValue;
    private final long rangePart;

    /**
     * Создаёт алгоритм с заданными параметрами треугольника и диапазоном случайных чисел.
     *
     * @param width           ширина области рисования
     * @param height          высота области рисования
     * @param minRandomValue  минимальное значение случайного числа (включительно)
     * @param maxRandomValue  максимальное значение случайного числа (включительно)
     */
    public SierpinskiAlgorithm(int width, int height, long minRandomValue, long maxRandomValue) {
        this.vertexA = new Point(width / 2, 0);
        this.vertexB = new Point(0, height);
        this.vertexC = new Point(width, height);
        this.minRandomValue = minRandomValue;
        this.maxRandomValue = maxRandomValue;
        this.rangePart = (maxRandomValue - minRandomValue) / 3;
    }

    /**
     * Создаёт алгоритм с параметрами из конфигурации.
     */
    public SierpinskiAlgorithm() {
        this(
                Config.getInt("panel.size.width"),
                Config.getInt("panel.size.height"),
                Config.getLong("random.min.value"),
                Config.getLong("random.max.value")
        );
    }

    /**
     * Вычисляет новую позицию точки по алгоритму Chaos Game.
     *
     * Случайное число делится на три равных диапазона, каждый из которых
     * соответствует одной из вершин треугольника. Новая точка — середина
     * отрезка между текущей точкой и выбранной вершиной.
     *
     * @param currentPoint текущая позиция точки
     * @param randomValue  случайное число для выбора вершины
     * @return новая позиция точки (середина отрезка к выбранной вершине)
     */
    public Point calculateNewDotPosition(Point currentPoint, long randomValue) {
        int x = currentPoint.x;
        int y = currentPoint.y;

        if (randomValue <= minRandomValue + rangePart) {
            x = (x + vertexA.x) / 2;
            y = (y + vertexA.y) / 2;
        } else if (randomValue <= minRandomValue + 2 * rangePart) {
            x = (x + vertexB.x) / 2;
            y = (y + vertexB.y) / 2;
        } else {
            x = (x + vertexC.x) / 2;
            y = (y + vertexC.y) / 2;
        }

        return new Point(x, y);
    }

    /**
     * Возвращает начальную точку (центр области рисования).
     */
    public Point getStartingPoint() {
        return new Point(vertexA.x, (vertexB.y + vertexA.y) / 2);
    }

    public Point getVertexA() { return new Point(vertexA); }
    public Point getVertexB() { return new Point(vertexB); }
    public Point getVertexC() { return new Point(vertexC); }
}