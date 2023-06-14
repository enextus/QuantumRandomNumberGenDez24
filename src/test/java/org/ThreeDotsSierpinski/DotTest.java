package org.ThreeDotsSierpinski;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.*;
import java.util.Date;

public class DotTest {

    @Test
    public void testDotCreation() {
        // arrange
        Point expectedPoint = new Point(1, 2);
        Date expectedDate = new Date();

        // act
        Dot dot = new Dot(expectedPoint, expectedDate);

        // assert
        assertThat(dot.point.x).isEqualTo(expectedPoint.x);
        assertThat(dot.point.y).isEqualTo(expectedPoint.y);
        assertThat(dot.creationDate).isEqualTo(expectedDate);
    }
}
