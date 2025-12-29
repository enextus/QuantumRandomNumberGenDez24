package org.ThreeDotsSierpinski;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit тесты для record Dot.
 */
@DisplayName("Dot - точка на панели")
class DotTest {

    @Test
    @DisplayName("Создание Dot с Point")
    void testDotCreation() {
        Point point = new Point(100, 200);
        Dot dot = new Dot(point);
        
        assertNotNull(dot);
        assertEquals(point, dot.point());
        assertEquals(100, dot.point().x);
        assertEquals(200, dot.point().y);
    }

    @Test
    @DisplayName("Два Dot с одинаковыми координатами равны")
    void testDotEquality() {
        Dot dot1 = new Dot(new Point(50, 75));
        Dot dot2 = new Dot(new Point(50, 75));
        
        assertEquals(dot1, dot2);
        assertEquals(dot1.hashCode(), dot2.hashCode());
    }

    @Test
    @DisplayName("Два Dot с разными координатами не равны")
    void testDotInequality() {
        Dot dot1 = new Dot(new Point(50, 75));
        Dot dot2 = new Dot(new Point(50, 76));
        
        assertNotEquals(dot1, dot2);
    }

    @Test
    @DisplayName("toString() содержит координаты")
    void testDotToString() {
        Dot dot = new Dot(new Point(123, 456));
        String str = dot.toString();
        
        assertTrue(str.contains("123") || str.contains("456"),
            "toString() должен содержать координаты");
    }

    @Test
    @DisplayName("Работа с граничными координатами")
    void testBoundaryCoordinates() {
        // Минимальные координаты
        Dot dotMin = new Dot(new Point(0, 0));
        assertEquals(0, dotMin.point().x);
        assertEquals(0, dotMin.point().y);
        
        // Максимальные координаты (типичный размер панели)
        Dot dotMax = new Dot(new Point(600, 600));
        assertEquals(600, dotMax.point().x);
        assertEquals(600, dotMax.point().y);
    }

    @Test
    @DisplayName("Создание с отрицательными координатами")
    void testNegativeCoordinates() {
        // Point допускает отрицательные координаты
        Dot dot = new Dot(new Point(-10, -20));
        assertEquals(-10, dot.point().x);
        assertEquals(-20, dot.point().y);
    }
}
