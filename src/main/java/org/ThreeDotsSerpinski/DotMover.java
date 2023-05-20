package org.ThreeDotsSerpinski;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class DotMover extends JPanel {
    private static final int SIZE = 1200;
    public static final int HEIGHT1 = 10;
    public static final int WIDTH1 = 10;
    public static final int MILLIS_TO_SECONDS = 1000;
    public static final int WIDTH2 = 10;
    public static final int HEIGHT2 = 10;

    Point dot;
    private List<Dot> dots;
    private DiceRoller dice;

    private BufferedImage buffer;

    private int dotCounter = 0; // Счетчик точек

    public DotMover() {
        setPreferredSize(new Dimension(SIZE, SIZE));
        dot = new Point(SIZE / 2, SIZE / 2);
        dots = new ArrayList<>();
        dice = new DiceRoller();

        // create the buffer image
        buffer = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(buffer, 0, 0, null);
    }

    public void moveDot() {
        int roll = dice.rollDice();
        switch (roll) {
            case 1, 2 -> {
                dot.x = dot.x / 2;
                dot.y = dot.y / 2;
            }
            case 3, 4 -> {
                dot.x = SIZE / 2 + dot.x / 2;
                dot.y = dot.y / 2;
            }
            case 5, 6 -> {
                dot.x = dot.x / 2;
                dot.y = SIZE / 2 + dot.y / 2;
            }
        }

        dots.add(new Dot(new Point(dot.x, dot.y), new Date()));
        dotCounter++;

        Graphics2D g2d = buffer.createGraphics();

        g2d.setColor(Color.BLACK);
        g2d.fillOval(dot.x, dot.y, WIDTH1, HEIGHT1);

        for (Dot dot : dots) {
            long diffInMillies = new Date().getTime() - dot.creationDate.getTime();
            long diffInSeconds = diffInMillies / MILLIS_TO_SECONDS;
            float alpha = 1f - Math.min(0.7f, diffInSeconds / 60f);
            alpha = Math.max(alpha, 0.3f);

            g2d.setColor(new Color(0, 0, 0, alpha));
            g2d.fillOval(dot.point.x, dot.point.y, WIDTH2, HEIGHT2);
        }

        // Рисуем счетчик точек на буфере
        g2d.setColor(Color.GREEN);
        g2d.drawString("Dot Count: " + dotCounter, SIZE - 150, SIZE - 30);

        g2d.dispose();
        repaint();
    }

}
