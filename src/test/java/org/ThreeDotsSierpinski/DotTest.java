package org.ThreeDotsSierpinski;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.*;
import java.time.LocalDateTime;
public class DotTest {
    @Test
    void testDot() {
        Point point = new Point(5, 7);
        LocalDateTime now = LocalDateTime.now();

        Dot dot = new Dot(point, now);

        assertEquals(point, dot.point);
        assertEquals(now, dot.creationDate);
    }

}
