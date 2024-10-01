package org.ThreeDotsSierpinski;

import java.awt.*;

/**
 * @param point Координаты точки
 */
public record Dot(Point point) {
    // Конструктор, принимающий начальные координаты точки
    public Dot(Point point) {
        this.point = new Point(point); // Создаем копию для иммутабельности
    }

    // Метод для получения копии точки (чтобы избежать изменения оригинала)
    @Override
    public Point point() {
        return new Point(point); // Возвращаем копию точки для безопасности
    }

}
