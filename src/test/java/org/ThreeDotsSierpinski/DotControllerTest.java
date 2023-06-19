package org.ThreeDotsSierpinski;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

class DotControllerTest {

    DotController dotController;

    @BeforeEach
    void setUp() {
        dotController = new DotController();
    }

    @Test
    void testInitialDotCounter() {
        assertEquals(0, dotController.getDotCounter(), "Initial dot counter must be 0");
    }

    @Test
    void testMoveDot() {
        dotController.moveDot();
        assertEquals(1, dotController.getDotCounter(), "Dot counter must be incremented after moving the dot");
    }

    @Test
    void testDotPositionAfterMoving() {
        Point initialDot = new Point(dotController.dot.x, dotController.dot.y);

        dotController.moveDot();
        assertNotEquals(initialDot, dotController.dot, "Dot position must change after moving");
    }
}
