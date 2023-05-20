package org.ThreeDotsSerpinski;


import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

class Dot {
    public Point point;
    public Date creationDate;

    public Dot(Point point, Date creationDate) {
        this.point = point;
        this.creationDate = creationDate;
    }
}
