package org.ThreeDotsSierpinski;

import java.awt.*;

/**
 * The Dot record represents a point with coordinates on a plane.
 *
 * @param point The coordinates of the point
 */
public record Dot(Point point) {
    /**
     * Constructor that takes the initial coordinates of the point.
     * Creates a copy of the passed point to ensure immutability.
     *
     * @param point The initial point
     */
    public Dot(Point point) {
        this.point = new Point(point); // Creates a copy to ensure immutability
    }

    /**
     * Returns a copy of the point to prevent modification of the original.
     *
     * @return A copy of the point
     */
    @Override
    public Point point() {
        return new Point(point); // Returns a copy of the point for safety
    }

    /**
     * Overrides the toString method to provide a comma-separated representation.
     *
     * @return A string in the format "x,y"
     */
    @Override
    public String toString() {
        return point.x + "," + point.y;
    }
}
