package org.ThreeDotsSerpinski;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
class DotMover extends JPanel {
    private static final int SIZE = 1200; // размер плоскости
    public static final int HEIGHT1 = 10;
    public static final int WIDTH1 = 10;
    public static final int INT = 1000;
    public static final int WIDTH2 = 10;
    public static final int HEIGHT2 = 10;
    Point dot;
    private List<Dot> dots; // список отметин
    private DiceRoller dice;

    public DotMover() {
        setPreferredSize(new Dimension(SIZE, SIZE));
        dot = new Point(SIZE / 2, SIZE / 2); // начальная точка в центре плоскости
        dots = new ArrayList<>(); // инициализация списка отметин
        dice = new DiceRoller();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setColor(Color.BLACK);
        g2d.fillOval(dot.x, dot.y, WIDTH1, HEIGHT1);

        for (Dot dot : dots) { // перерисовка всех отметин
            long diffInMillies = new Date().getTime() - dot.creationDate.getTime();
            long diffInSeconds = diffInMillies / INT;
            float alpha = 1f - Math.min(0.7f, diffInSeconds / 60f);
            alpha = Math.max(alpha, 0.3f);

            g2d.setColor(new Color(0, 0, 0, alpha));
            g2d.fillOval(dot.point.x, dot.point.y, WIDTH2, HEIGHT2);
        }
    }

    public void moveDot() {
        int roll = dice.rollDice();
        switch (roll) {
            case 1:
            case 2:
                dot.x = dot.x / 2;
                dot.y = dot.y / 2;
                break;
            case 3:
            case 4:
                dot.x = SIZE / 2 + dot.x / 2;
                dot.y = dot.y / 2;
                break;
            case 5:
            case 6:
                dot.x = dot.x / 2;
                dot.y = SIZE / 2 + dot.y / 2;
                break;
        }
        dots.add(new Dot(new Point(dot.x, dot.y), new Date())); // добавление новой отметины в список
        repaint();
    }

}
