package org.ThreeDotsSierpinski;

import java.awt.*;

public final class Dot {
    private final Point point; // Координаты точки

    // Конструктор, принимающий начальные координаты точки
    public Dot(Point point) {
        this.point = new Point(point); // Создаем копию для иммутабельности
    }

    // Метод для получения копии точки (чтобы избежать изменения оригинала)
    public Point getPoint() {
        return new Point(point); // Возвращаем копию точки для безопасности
    }

}
