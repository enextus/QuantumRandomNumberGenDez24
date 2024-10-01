package org.ThreeDotsSierpinski;

import java.awt.*;

/**
 * Запись (record) представляет собой точку с координатами на плоскости.
 *
 * The Dot record represents a point with coordinates on a plane.
 *
 * @param point Координаты точки
 *              The coordinates of the point
 */
public record Dot(Point point) {

    /**
     * Конструктор, принимающий начальные координаты точки.
     * Создает копию переданной точки для обеспечения иммутабельности.
     *
     * Constructor that takes the initial coordinates of the point.
     * Creates a copy of the passed point to ensure immutability.
     *
     * @param point Начальная точка
     *              The initial point
     */
    public Dot(Point point) {
        this.point = new Point(point); // Создаем копию для иммутабельности
        // Creates a copy to ensure immutability
    }

    /**
     * Метод для получения копии точки.
     * Возвращает новую копию точки, чтобы избежать изменения оригинала.
     *
     * Method to obtain a copy of the point.
     * Returns a new copy of the point to prevent modification of the original.
     *
     * @return Копия точки
     *         A copy of the point
     */
    @Override
    public Point point() {
        return new Point(point); // Возвращаем копию точки для безопасности
        // Returns a copy of the point for safety
    }
}
