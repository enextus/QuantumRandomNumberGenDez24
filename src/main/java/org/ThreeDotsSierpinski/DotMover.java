package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class DotMover extends JPanel {
    private static final int SIZE = 1050; // размер плоскости
    public static final int HEIGHT1 = 10;
    public static final int WIDTH1 = 10;
    public static final int DELAY_TIME = 1000;
    public static final int WIDTH2 = 10;
    public static final int HEIGHT2 = 10;
    public static final String GET_DOT_COUNTER = "getDotCounter(): ";
    Point dot;
    private final List<Dot> dots; // список отметин
    private final DiceRoller dice;
    private int dotCounter; // счетчик количества отметин

    public int getDotCounter() {
        return dotCounter;
    }

    public DotMover(DiceRoller diceRoller) {
        setPreferredSize(new Dimension(SIZE, SIZE));
        dot = new Point(SIZE / 2, SIZE / 2);
        dots = new ArrayList<>();
        dice = new DiceRoller();
        dotCounter = 0;

        // Установка цвета фона на пастельный синий
        setBackground(new Color(176, 224, 230));
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Устанавливаем цвет фона пастельным синим
        g.setColor(new Color(0, 0, 0));
        g.fillRect(0, 0, this.getWidth(), this.getHeight());

        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setColor(Color.BLACK);
        g2d.fillOval(dot.x, dot.y, WIDTH1, HEIGHT1);

        for (Dot dot : dots) {
            long diffInMillies = new Date().getTime() - dot.creationDate.getTime();
            long diffInSeconds = diffInMillies / DELAY_TIME;
            float alpha = 1f - Math.min(0.7f, diffInSeconds / 30f);
            alpha = Math.max(alpha, 0.3f);

            Color c = new Color(0, 0, 0, (int) (alpha * 255));

//            color change from black to red 30%: 1.0
            if (alpha <= 0.3f) {
                c = new Color(0.0f, 0.0f, 0.0f, alpha);
            }

            g2d.setColor(c);
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

        System.out.println(GET_DOT_COUNTER + getDotCounter());
        repaint();
    }

}
