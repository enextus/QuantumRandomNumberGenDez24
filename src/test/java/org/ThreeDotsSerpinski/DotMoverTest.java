package org.ThreeDotsSerpinski;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Point;

public class DotMoverTest {
    private DotMover dotMover;

    @BeforeEach
    public void setUp() {
        this.dotMover = new DotMover();
    }

    @Test
    public void testMoveDot() {
        Point initialPoint = new Point(dotMover.dot);
        dotMover.moveDot();
        Point finalPoint = dotMover.dot;
        assertTrue(!finalPoint.equals(initialPoint), "The dot should have moved after calling moveDot()");
    }
}
