package org.ThreeDotsSierpinski;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

class DotControllerTest {
    DotController dotController;

    @Test
    void testInitialDotCounter() {
        assertEquals(0, dotController.getDotCounter(), "Initial dot counter must be 0");
    }

    @Test
    void testMoveDot() {
        dotController.moveDot();
        assertEquals(1, dotController.getDotCounter(), "Dot counter must be incremented after moving the dot");
    }

}
