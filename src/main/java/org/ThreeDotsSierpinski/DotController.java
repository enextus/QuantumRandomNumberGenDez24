package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class DotController extends JPanel {
    private static final int SIZE = 925;
    private static final int DOTWIDTH = 7;
    private static final int DOTHEIGHT = 7;
    private final List<Dot> dots;
    private final RandomNumberProvider randomNumberProvider;
    private int dotCounter;
    private String errorMessage;

    public DotController() {
        setPreferredSize(new Dimension(SIZE, SIZE));
        setBackground(new Color(176, 224, 230));
        dots = new ArrayList<>();
        randomNumberProvider = new RandomNumberProvider();
        dotCounter = 0;
        errorMessage = null;
    }

    public void moveDot() {
        try {
            int randomValue = randomNumberProvider.getNextRandomNumber();
            Point newPoint = calculateNewDotPosition(randomValue);
            Dot newDot = new Dot(newPoint);
            dots.add(newDot);
            dotCounter++;
            repaint();
        } catch (Exception e) {
            errorMessage = "Error: Cannot connect to Random Number Provider.";
            repaint();
        }
    }

    private Point calculateNewDotPosition(int randomValue) {
        int x = SIZE / 2;
        int y = SIZE / 2;

        if (randomValue <= Integer.MIN_VALUE / 3) {
            x = x / 2;
            y = y / 2;
        } else if (randomValue <= Integer.MAX_VALUE / 3) {
            x = SIZE / 2 + x / 2;
            y = y / 2;
        } else if (randomValue <= Integer.MAX_VALUE / 3 * 2) {
            x = x / 2;
            y = SIZE / 2 + y / 2;
        }

        return new Point(x, y);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (errorMessage != null) {
            g.setColor(Color.RED);
            g.drawString(errorMessage, 10, 20);
        } else {
            for (Dot dot : dots) {
                g.setColor(Color.BLACK);
                g.fillOval(dot.getPoint().x, dot.getPoint().y, DOTWIDTH, DOTHEIGHT);
            }
        }
    }

    public int getDotCounter() {
        return dotCounter;
    }
}
