package org.ThreeDotsSierpinski;

import java.awt.*;

/**
 * The Dot record represents a point with coordinates on a 2D plane.
 *
 * @param point Coordinates of the point.
 */
public record Dot(Point point) {
    /**
     * Constructor that accepts initial coordinates for the point.
     * It creates a copy of the provided point to ensure immutability.
     *
     * @param point The initial point.
     */
    public Dot(Point point) {
        this.point = new Point(point); // Create a copy to ensure immutability.
    }

    /**
     * Returns a copy of the point to prevent modification of the original point.
     *
     * @return A new Point object, which is a copy of the original point.
     */
    @Override
    public Point point() {
        return new Point(point); // Return a copy of the point for safety.
    }

    /**
     * Overrides the toString method to provide a string representation of the point.
     * The format is "x,y", where x is the X-coordinate and y is the Y-coordinate.
     *
     * @return A string in the format "x,y".
     */
    @Override
    public String toString() {
        return point.x + "," + point.y; // Return a comma-separated string of the coordinates.
    }

}
