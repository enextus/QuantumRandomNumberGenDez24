package org.ThreeDotsSierpinski;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.util.Date;

class DotTest {

    @Test
    void testDotConstructor() {
        // Arrange
        Point point = new Point(5, 10);
        Date creationDate = new Date();

        // Act
        Dot dot = new Dot(point, creationDate);

        // Assert
        Assertions.assertEquals(point, dot.point);
        Assertions.assertEquals(creationDate, dot.creationDate);
    }

    @Test
    void testDotPoint() {
        // Arrange
        Point point = new Point(5, 10);
        Date creationDate = new Date();
        Dot dot = new Dot(point, creationDate);

        // Act
        Point newPoint = new Point(15, 20);
        dot.point = newPoint;

        // Assert
        Assertions.assertEquals(newPoint, dot.point);
    }

    @Test
    void testDotCreationDate() {
        // Arrange
        Point point = new Point(5, 10);
        Date creationDate = new Date();
        Dot dot = new Dot(point, creationDate);

        // Act
        Date newCreationDate = new Date();
        dot.creationDate = newCreationDate;

        // Assert
        Assertions.assertEquals(newCreationDate, dot.creationDate);
    }

}
