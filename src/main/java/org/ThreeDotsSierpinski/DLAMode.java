package org.ThreeDotsSierpinski;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Режим визуализации: Diffusion-Limited Aggregation (DLA) / Brownian Tree.
 * <p>
 * Алгоритм:
 * 1. Семя (seed) в центре экрана
 * 2. Частица запускается со случайной позиции на границе
 * 3. Случайное блуждание: каждый шаг — одно случайное число → направление
 * 4. Если частица касается кластера — прилипает
 * 5. Если уходит за границу — новая частица
 * <p>
 * Результат: структуры, похожие на кораллы, молнии, корни, минеральные дендриты.
 * Фрактальная размерность ≈ 1.71.
 */
public class DLAMode implements VisualizationMode {

    private boolean[][] grid;
    private int width;
    private int height;
    private int pointCount = 0;
    private int randomNumbersUsed = 0;

    // Текущая блуждающая частица
    private int walkerX;
    private int walkerY;
    private boolean hasWalker = false;

    /**
     * Максимум шагов блуждания за один тик анимации
     */
    private static final int MAX_WALK_STEPS_PER_TICK = 500;

    /**
     * Расстояние от кластера, с которого запускать частицу
     */
    private int spawnRadius;

    /**
     * 4 направления: dx, dy
     */
    private static final int[][] DIRS = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};

    @Override
    public String getId() {
        return "dla";
    }

    @Override
    public String getName() {
        return "DLA / Brownian Tree";
    }

    @Override
    public String getDescription() {
        return "Частицы блуждают случайно и прилипают к кластеру.\n"
                + "Возникают структуры кораллов, молний и кристаллов.";
    }

    @Override
    public String getIcon() {
        return "⚡";
    }

    @Override
    public void initialize(BufferedImage canvas, int width, int height) {
        if (canvas == null) throw new IllegalArgumentException("Canvas cannot be null");
        this.width = width;
        this.height = height;
        this.grid = new boolean[width][height];
        this.pointCount = 0;
        this.randomNumbersUsed = 0;
        this.hasWalker = false;
        this.spawnRadius = 30; // Маленький стартовый радиус для мгновенного первого прилипания

        // Семя в центре
        int cx = width / 2;
        int cy = height / 2;
        grid[cx][cy] = true;
        pointCount = 1;

        // Рисуем семя
        var g2d = canvas.createGraphics();
        g2d.setColor(Color.BLACK);
        g2d.fillRect(cx, cy, 2, 2);
        g2d.dispose();
    }

    @Override
    public List<Point> step(RNProvider provider, BufferedImage canvas, int dotSize) {
        var newPoints = new ArrayList<Point>();
        var g2d = canvas.createGraphics();

        int particlesThisTick = 0;
        int stepsLeft = MAX_WALK_STEPS_PER_TICK;

        while (stepsLeft > 0) {
            // Запускаем новую частицу если нужно
            if (!hasWalker) {
                spawnWalker(provider);
            }

            // Один шаг блуждания
            int dirIndex = provider.getNextRandomNumber() % 4;
            randomNumbersUsed++;
            stepsLeft--;

            walkerX += DIRS[dirIndex][0];
            walkerY += DIRS[dirIndex][1];

            // Вышла за границу → перезапуск
            if (walkerX < 1 || walkerX >= width - 1 || walkerY < 1 || walkerY >= height - 1) {
                hasWalker = false;
                continue;
            }

            // Слишком далеко от кластера → перезапуск
            int cx = width / 2, cy = height / 2;
            double distFromCenter = Math.sqrt((walkerX - cx) * (walkerX - cx) + (walkerY - cy) * (walkerY - cy));
            if (distFromCenter > spawnRadius * 2.5) {
                hasWalker = false;
                continue;
            }

            // Проверка соседства с кластером
            if (touchesCluster(walkerX, walkerY)) {
                // Прилипает!
                grid[walkerX][walkerY] = true;
                pointCount++;
                hasWalker = false;

                // Рисуем — красным (будет перекрашена в чёрный)
                g2d.setColor(Color.RED);
                g2d.fillRect(walkerX, walkerY, dotSize, dotSize);
                newPoints.add(new Point(walkerX, walkerY));

                // Расширяем радиус спавна по мере роста кластера
                double distFromSeed = Math.sqrt((walkerX - cx) * (walkerX - cx) + (walkerY - cy) * (walkerY - cy));
                if (distFromSeed + 20 > spawnRadius) {
                    spawnRadius = (int) distFromSeed + 30;
                    spawnRadius = Math.min(spawnRadius, Math.min(width, height) / 2 - 10);
                }

                particlesThisTick++;
                if (particlesThisTick >= 3) break; // Макс 3 прилипания за тик (для плавной анимации)
            }
        }

        g2d.dispose();
        return newPoints;
    }

    /**
     * Запускает новую частицу со случайной позиции на окружности спавна.
     */
    private void spawnWalker(RNProvider provider) {
        int angle = provider.getNextRandomNumber() % 360;
        randomNumbersUsed++;

        double rad = Math.toRadians(angle);
        int cx = width / 2, cy = height / 2;
        walkerX = cx + (int) (spawnRadius * Math.cos(rad));
        walkerY = cy + (int) (spawnRadius * Math.sin(rad));

        // Clamp to bounds
        walkerX = Math.clamp(walkerX, 1, width - 2);
        walkerY = Math.clamp(walkerY, 1, height - 2);

        hasWalker = true;
    }

    /**
     * Проверяет, касается ли позиция (x, y) кластера (8 соседей).
     */
    private boolean touchesCluster(int x, int y) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                int nx = x + dx, ny = y + dy;
                if (nx >= 0 && nx < width && ny >= 0 && ny < height && grid[nx][ny]) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public int getPointCount() {
        return pointCount;
    }

    @Override
    public int getRandomNumbersUsed() {
        return randomNumbersUsed;
    }
}
