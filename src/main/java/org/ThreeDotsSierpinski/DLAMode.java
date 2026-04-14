package org.ThreeDotsSierpinski;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Режим визуализации: Diffusion-Limited Aggregation (DLA) / Brownian Tree.
 * <p>
 * Алгоритм:
 * 1. Семя (seed) в центре экрана
 * 2. 5 параллельных частиц запускаются со случайной позиции
 * 3. Случайное блуждание: каждый шаг — одно случайное число → направление
 * 4. Если частица касается кластера — прилипает
 * 5. Если уходит за границу или блуждает слишком долго — новая частица
 * <p>
 * Результат: структуры, похожие на кораллы, молнии, корни.
 * Фрактальная размерность ≈ 1.71.
 */
public class DLAMode implements VisualizationMode {

    private boolean[][] grid;
    private int width;
    private int height;
    private int pointCount = 0;
    private int randomNumbersUsed = 0;

    /** 4 направления: dx, dy */
    private static final int[][] DIRS = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};

    // ========================================================================
    // ПАРАЛЛЕЛЬНЫЕ БЛУЖДАЮЩИЕ ЧАСТИЦЫ (Ускорение в 5 раз)
    // ========================================================================

    private static final int PARALLEL_WALKERS = 50;              // Было 5
    private static final int MAX_TOTAL_STEPS_PER_TICK = 15000;  // Было 1500
    private static final int KILL_RADIUS_STEPS = 1500;       // Было 2000 (помогает, т.к. кластер становится плотнее)
    private static final int MAX_STICKS_PER_TICK = 20;         // Было 5 (чтобы не моргало красным)

    private int spawnRadius;

    // Массивы состояний для параллельных частиц
    private final int[] walkerX = new int[PARALLEL_WALKERS];
    private final int[] walkerY = new int[PARALLEL_WALKERS];
    private final int[] walkerSteps = new int[PARALLEL_WALKERS];

    @Override
    public String getId() { return "dla"; }

    @Override
    public String getName() { return "DLA / Brownian Tree"; }

    @Override
    public String getDescription() {
        return "Частицы блуждают случайно и прилипают к кластеру.\n"
                + "Возникают структуры кораллов, молний и кристаллов.";
    }

    @Override
    public String getIcon() { return "⚡"; }

    @Override
    public void initialize(BufferedImage canvas, int width, int height) {
        if (canvas == null) throw new IllegalArgumentException("Canvas cannot be null");
        this.width = width;
        this.height = height;
        this.grid = new boolean[width][height];
        this.pointCount = 0;
        this.randomNumbersUsed = 0;
        this.spawnRadius = 30; // Маленький старт для быстрого первого прилипания

        // Сброс состояний блуждающих частиц
        Arrays.fill(walkerSteps, 0);

        // Семя в центре
        int cx = width / 2;
        int cy = height / 2;
        grid[cx][cy] = true;
        pointCount = 1;

        var g2d = canvas.createGraphics();
        g2d.setColor(Color.BLACK);
        g2d.fillRect(cx, cy, 2, 2);
        g2d.dispose();
    }

    @Override
    public List<Point> step(RNProvider provider, BufferedImage canvas, int dotSize) {
        var newPoints = new ArrayList<Point>();
        var g2d = canvas.createGraphics();
        g2d.setColor(Color.RED);

        int sticksThisTick = 0;
        int totalStepsLeft = MAX_TOTAL_STEPS_PER_TICK;

        while (totalStepsLeft > 0) {
            for (int i = 0; i < PARALLEL_WALKERS && totalStepsLeft > 0; i++) {

                // Если частица не активна (ещё не "спавнилась" или была убита) — запускаем
                if (walkerSteps[i] <= 0) {
                    spawnWalker(provider, i);
                }

                // Шаг блуждания
                int dirIndex = provider.getNextRandomNumber() % 4;
                randomNumbersUsed++;
                totalStepsLeft--;
                walkerSteps[i]++;

                walkerX[i] += DIRS[dirIndex][0];
                walkerY[i] += DIRS[dirIndex][1];

                // Вышла за границу
                if (walkerX[i] < 1 || walkerX[i] >= width - 1 ||
                        walkerY[i] < 1 || walkerY[i] >= height - 1) {
                    walkerSteps[i] = 0; // Пометить как неактивную (перезапустится)
                    continue;
                }

                // Убийство: застряла во фьерде (сэкономит миллионы пустых шагов)
                if (walkerSteps[i] > KILL_RADIUS_STEPS) {
                    walkerSteps[i] = 0; // Убить
                    continue;
                }

                // Проверка соседства с кластером
                if (touchesCluster(walkerX[i], walkerY[i])) {
                    // Прилипает!
                    grid[walkerX[i]][walkerY[i]] = true;
                    pointCount++;

                    // Расширяем радиус спавна по мере роста
                    updateSpawnRadius(walkerX[i], walkerY[i]);

                    // Рисуем
                    g2d.fillRect(walkerX[i], walkerY[i], dotSize, dotSize);
                    newPoints.add(new Point(walkerX[i], walkerY[i]));

                    sticksThisTick++;
                    walkerSteps[i] = 0; // Сбросить частицу

                    if (sticksThisTick >= MAX_STICKS_PER_TICK) break;
                }
            }
        }

        g2d.dispose();
        return newPoints;
    }

    /**
     * Запускает конкретную частицу со случайной позиции на окружности спавна.
     */
    private void spawnWalker(RNProvider provider, int index) {
        int angle = provider.getNextRandomNumber() % 360;
        randomNumbersUsed++;

        double rad = Math.toRadians(angle);
        int cx = width / 2, cy = height / 2;
        walkerX[index] = cx + (int) (spawnRadius * Math.cos(rad));
        walkerY[index] = cy + (int) (spawnRadius * Math.sin(rad));

        // Clamp to bounds
        walkerX[index] = Math.clamp(walkerX[index], 1, width - 2);
        walkerY[index] = Math.clamp(walkerY[index], 1, height - 2);

        walkerSteps[index] = 1; // Активировать
    }

    /**
     * Увеличивает радиус спавна, если новая точка находится близко к его границе.
     */
    private void updateSpawnRadius(int x, int y) {
        int cx = width / 2, cy = height / 2;
        long distSq = (long)(x - cx) * (x - cx) + (long)(y - cy) * (y - cy);
        long spawnLimitSq = (long)(spawnRadius - 20) * (spawnRadius - 20);

        if (distSq > spawnLimitSq) {
            int newRadius = (int) Math.sqrt(distSq) + 30;
            spawnRadius = Math.min(newRadius, Math.min(width, height) / 2 - 10);
        }
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
    public int getPointCount() { return pointCount; }

    @Override
    public int getRandomNumbersUsed() { return randomNumbersUsed; }
}