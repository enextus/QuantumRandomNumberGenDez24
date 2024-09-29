package org.ThreeDotsSierpinski;

import java.awt.*;

public class Dot {
    protected Point point; // Координаты точки
    protected long creationNanoTime; // Время создания точки

    // Конструктор, принимающий начальные координаты точки
    public Dot(Point point) {
        this.point = point;
        this.creationNanoTime = System.nanoTime(); // Запоминаем время создания в наносекундах
    }

    // Метод для получения копии точки (чтобы избежать изменения оригинала)
    public Point getPoint() {
        return new Point(point); // Возвращаем копию точки для безопасности
    }

    // Метод для получения времени создания точки
    public long getCreationNanoTime() {
        return creationNanoTime;
    }
}
