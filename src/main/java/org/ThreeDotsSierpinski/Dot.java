package org.ThreeDotsSierpinski;

import java.awt.*;
import java.time.LocalDateTime;

public class Dot {
    protected Point point;
    protected LocalDateTime creationDate;

    public Dot(Point point, LocalDateTime creationDate) {
        this.point = new Point(point); // Создаем копию точки для защиты от изменений
        this.creationDate = LocalDateTime.from(creationDate); // Создаем копию времени для защиты от изменений
    }

    public Point getPoint() {
        return new Point(point); // Возвращаем копию для защиты от изменений
    }

    public LocalDateTime getCreationDate() {
        return LocalDateTime.from(creationDate); // Возвращаем копию для защиты от изменений
    }

}
