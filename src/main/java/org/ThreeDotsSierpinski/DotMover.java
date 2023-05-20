package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class DotMover extends JPanel {
    private static final int SIZE = 1100; // размер плоскости
    public static final int HEIGHT1 = 10;
    public static final int WIDTH1 = 10;
    public static final int DELAY_TIME = 1000;
    public static final int WIDTH2 = 10;
    public static final int HEIGHT2 = 10;
    Point dot;
    private List<Dot> dots; // список отметин
    private DiceRoller dice;
    private int dotCounter; // счетчик количества отметин

    public int getDotCounter() {
        return dotCounter;
    }

    public DotMover(DiceRoller diceRoller) {
        setPreferredSize(new Dimension(SIZE, SIZE));
        dot = new Point(SIZE / 2, SIZE / 2); // начальная точка в центре плоскости
        dots = new ArrayList<>(); // инициализация списка отметин
        dice = new DiceRoller();
        dotCounter = 0; // инициализация счетчика
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setColor(Color.BLACK);
        g2d.fillOval(dot.x, dot.y, WIDTH1, HEIGHT1);

        for (Dot dot : dots) { // перерисовка всех отметин
            long diffInMillies = new Date().getTime() - dot.creationDate.getTime();
            long diffInSeconds = diffInMillies / DELAY_TIME;
            float alpha = 1f - Math.min(0.9f, diffInSeconds / 30f);
            alpha = Math.max(alpha, 0.3f);

            g2d.setColor(new Color(0, 0, 0, alpha));
            g2d.fillOval(dot.point.x, dot.point.y, WIDTH2, HEIGHT2);
        }
    }

    public void moveDot() {

        int roll = dice.rollDice();

//        -2,147,483,648 до -715,827,882 (Integer.MIN_VALUE до Integer.MIN_VALUE / 3)
//        -715,827,881 до 715,827,881 (Integer.MIN_VALUE / 3 + 1 до Integer.MAX_VALUE / 3)
//        715,827,882 до 2,147,483,647 (Integer.MAX_VALUE / 3 * 2 до Integer.MAX_VALUE)
//
//        1,431,655,767
//        1,431,655,763
//        1,431,655,766
//
//        1,431,655,767 + 1,431,655,763 + 1,431,655,766 = 4,294,967,296

        if (roll <= Integer.MIN_VALUE / 3 || roll > Integer.MAX_VALUE / 3 * 2) {
            dot.x = dot.x / 2;
            dot.y = dot.y / 2;
        } else if (roll <= Integer.MAX_VALUE / 3) {
            dot.x = SIZE / 2 + dot.x / 2;
            dot.y = dot.y / 2;
        } else {
            dot.x = dot.x / 2;
            dot.y = SIZE / 2 + dot.y / 2;
        }
        dots.add(new Dot(new Point(dot.x, dot.y), new Date())); // добавление новой отметины в список
        dotCounter++; // увеличение значения счетчика
        repaint();
    }


}