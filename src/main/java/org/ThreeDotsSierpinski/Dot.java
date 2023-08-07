package org.ThreeDotsSierpinski;

import java.awt.*;

public class Dot {
    protected Point point;
    protected long creationNanoTime;

    public Dot(Point point) {
        this.point = point;
        this.creationNanoTime = System.nanoTime();
    }

    public Point getPoint() {
        return new Point(point); // We return a copy to protect from modifications
    }

    public long getCreationNanoTime() {
        return creationNanoTime;
    }
}

