package org.ThreeDotsSierpinski;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * Режим визуализации: треугольник Серпинского (Chaos Game).
 * Классический алгоритм:
 * 1. Начинаем с центра треугольника
 * 2. Случайное число определяет одну из трёх вершин
 * 3. Перемещаемся на половину расстояния к вершине
 * 4. Ставим точку
 * Из чистого хаоса рождается фрактальная структура.
 */
public class SierpinskiMode implements VisualizationMode {

    private static final int DOTS_PER_STEP = Config.getInt("dots.per.update");

    private SierpinskiAlgorithm algorithm;
    private Point currentPoint;
    private int pointCount = 0;
    private int randomNumbersUsed = 0;

    @Override
    public String getId() { return "Sierpinski"; }

    @Override
    public String getName() { return "Sierpinski Triangle"; }

    @Override
    public String getDescription() {
        return "Фрактал из хаоса: случайные числа определяют вершину,\n"
             + "точка прыгает на полпути — и возникает треугольник Серпинского.";
    }

    @Override
    public String getIcon() { return "△"; }

    @Override
    public void initialize(BufferedImage canvas, int width, int height) {
        if (canvas == null) throw new IllegalArgumentException("Canvas cannot be null");
        algorithm = new SierpinskiAlgorithm(width, height);
        currentPoint = new Point(width / 2, height / 2);
        pointCount = 0;
        randomNumbersUsed = 0;
    }

    @Override
    public List<Point> step(RNProvider provider, BufferedImage canvas, int dotSize) {
        var newPoints = new ArrayList<Point>();
        var g2d = canvas.createGraphics();
        g2d.setColor(Color.RED);

        for (int i = 0; i < DOTS_PER_STEP; i++) {
            OptionalInt randomOpt = provider.getNextRandomNumber();
            if (randomOpt.isEmpty()) {
                break; // Буфер пуст, прерываем batch, вернем то, что успели нарисовать
            }

            long randomValue = randomOpt.getAsInt();
            randomNumbersUsed++;

            currentPoint = algorithm.calculateNewDotPosition(currentPoint, randomValue);
            g2d.fillRect(currentPoint.x, currentPoint.y, dotSize, dotSize);
            newPoints.add(new Point(currentPoint));
            pointCount++;
        }

        g2d.dispose();
        return newPoints;
    }

    @Override
    public int getPointCount() { return pointCount; }

    @Override
    public int getRandomNumbersUsed() { return randomNumbersUsed; }
}
