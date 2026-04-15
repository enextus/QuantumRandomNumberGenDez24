package org.ThreeDotsSierpinski;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;

/**
 * Режим визуализации: Diffusion-Limited Aggregation (DLA) / Brownian Tree.
 * Частицы блуждают случайно и прилипают к растущему кластеру.
 * Визуализация «Using spheres of decreasing radius» (Paul Bourke).
 * Цвет: HSB-градиент от центра (красный/тёплый) к краям (голубой/холодный).
 * Размер: убывает с расстоянием от центра.
 * Фон: чёрный.
 *
 * @see <a href="https://paulbourke.net/fractals/dla/">Paul Bourke: DLA</a>
 */
public class DLAMode implements VisualizationMode {

    private boolean[][] grid;
    private int width;
    private int height;
    private int pointCount = 0;
    private int randomNumbersUsed = 0;
    private int baseDotSize = 5;

    // Центр и метрики кластера
    private int centerX;
    private int centerY;
    private double maxDist = 1.0; // Максимальное расстояние от центра до края кластера

    // Spawn/kill параметры
    private int spawnRadius = 30;

    // Параллельные блуждающие частицы
    private static final int PARALLEL_WALKERS = 50;
    private static final int MAX_STEPS_PER_TICK = 15_000;   // Общий бюджет шагов за тик
    private static final int MAX_STICKS_PER_TICK = 20;      // Макс прилипаний за тик
    private static final int WALKER_LIFESPAN = 2000;        // Макс шагов одного walker-а

    private final int[] walkerX = new int[PARALLEL_WALKERS];
    private final int[] walkerY = new int[PARALLEL_WALKERS];
    private final int[] walkerAge = new int[PARALLEL_WALKERS];
    private final boolean[] walkerAlive = new boolean[PARALLEL_WALKERS];

    // 4 направления + 4 диагонали = 8 (для блуждания используем 4, для касания — 8)
    private static final int[][] WALK_DIRS = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};

    @Override
    public String getId() { return "dla"; }

    @Override
    public String getName() { return "DLA / Brownian Tree"; }

    @Override
    public String getDescription() {
        return "Частицы блуждают случайно и прилипают к кластеру.\n"
             + "Кораллы, молнии, кристаллы — из чистой случайности.";
    }

    @Override
    public String getIcon() { return "⚡"; }

    @Override
    public boolean usesRecolorAnimation() { return false; } // DLA сам управляет цветами

    @Override
    public boolean usesDarkBackground() { return true; } // Чёрный фон

    @Override
    public void initialize(BufferedImage canvas, int width, int height) {
        this.width = width;
        this.height = height;
        this.grid = new boolean[width][height];
        this.pointCount = 0;
        this.randomNumbersUsed = 0;
        this.centerX = width / 2;
        this.centerY = height / 2;
        this.maxDist = 1.0;
        this.spawnRadius = 30;

        // Чёрный фон
        var g2d = canvas.createGraphics();
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, width, height);

        // Seed в центре
        grid[centerX][centerY] = true;
        pointCount = 1;

        // Рисуем seed — крупный, красный
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int seedSize = getSizeForDepth(0.0);
        g2d.setColor(getColorForDepth(0.0));
        g2d.fillOval(centerX - seedSize / 2, centerY - seedSize / 2, seedSize, seedSize);
        g2d.dispose();

        // Все walkers неактивны
        Arrays.fill(walkerAlive, false);
    }

    @Override
    public List<Point> step(RNProvider provider, BufferedImage canvas, int dotSize) {
        this.baseDotSize = dotSize;
        var newPoints = new ArrayList<Point>();

        var g2d = canvas.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int sticksThisTick = 0;
        int stepsLeft = MAX_STEPS_PER_TICK;

        while (stepsLeft > 0 && sticksThisTick < MAX_STICKS_PER_TICK) {
            boolean bufferEmpty = false; // Флаг остановки

            for (int i = 0; i < PARALLEL_WALKERS && stepsLeft > 0; i++) {

                // Spawn если мёртв
                if (!walkerAlive[i]) {
                    if (!spawnWalker(provider, i)) { // Если spawnWalker вернул false
                        bufferEmpty = true;         // значит буфер пуст
                        break;
                    }
                }

                // Один шаг блуждания
                OptionalInt dirOpt = provider.getNextRandomNumber();
                if (dirOpt.isEmpty()) {
                    bufferEmpty = true; // Буфер пуст
                    break;
                }

                int dir = Math.abs(dirOpt.getAsInt()) % 4;
                randomNumbersUsed++;
                stepsLeft--;

                walkerX[i] += WALK_DIRS[dir][0];
                walkerY[i] += WALK_DIRS[dir][1];
                walkerAge[i]++;

                // Вышел за границу → убить
                if (walkerX[i] < 1 || walkerX[i] >= width - 1 ||
                        walkerY[i] < 1 || walkerY[i] >= height - 1) {
                    walkerAlive[i] = false;
                    continue;
                }

                // Слишком долго блуждает → убить
                if (walkerAge[i] > WALKER_LIFESPAN) {
                    walkerAlive[i] = false;
                    continue;
                }

                // Слишком далеко от кластера → убить
                double distFromCenter = distance(walkerX[i], walkerY[i], centerX, centerY);
                if (distFromCenter > spawnRadius * 2.5) {
                    walkerAlive[i] = false;
                    continue;
                }

                // Касается кластера → прилипает!
                if (touchesCluster(walkerX[i], walkerY[i])) {
                    grid[walkerX[i]][walkerY[i]] = true;
                    pointCount++;
                    walkerAlive[i] = false;

                    // Обновляем максимальное расстояние
                    double dist = distance(walkerX[i], walkerY[i], centerX, centerY);
                    if (dist > maxDist) {
                        maxDist = dist;
                    }

                    // Расширяем радиус спавна
                    if (dist + 20 > spawnRadius) {
                        spawnRadius = Math.min((int) dist + 30, Math.min(width, height) / 2 - 10);
                    }

                    // Рисуем — цвет и размер зависят от расстояния до центра
                    double t = dist / maxDist; // 0.0 = центр, 1.0 = край
                    int size = getSizeForDepth(t);
                    Color color = getColorForDepth(t);

                    g2d.setColor(color);
                    g2d.fillOval(walkerX[i] - size / 2, walkerY[i] - size / 2, size, size);

                    newPoints.add(new Point(walkerX[i], walkerY[i]));
                    sticksThisTick++;

                    if (sticksThisTick >= MAX_STICKS_PER_TICK) break;
                }
            }

            // Если в этом тике обнаружили пустой буфер - прерываем ВЕСЬ процесс до следующего вызова step()
            if (bufferEmpty) {
                break;
            }
        }

        g2d.dispose();
        return newPoints;
    }

    // ========================================================================
    // Визуализация: цвет и размер
    // ========================================================================

    /**
     * HSB-градиент: красный (центр) → оранжевый → жёлтый → зелёный → голубой (край).
     * Тёплые тона в ядре, холодные на периферии.
     *
     * @param t нормализованное расстояние [0.0 = центр, 1.0 = край]
     */
    private Color getColorForDepth(double t) {
        t = Math.clamp(t, 0.0, 1.0);

        // Hue: 0.0 (красный) → 0.15 (оранжевый) → 0.33 (зелёный) → 0.5 (голубой)
        float hue = (float) (t * 0.5);

        // Saturation: высокая везде, чуть ярче в центре
        float saturation = (float) (0.85 + 0.15 * (1.0 - t));

        // Brightness: яркий в центре, чуть приглушённый на краях
        float brightness = (float) (0.95 - 0.25 * t);

        return Color.getHSBColor(hue, saturation, brightness);
    }

    /**
     * Размер сферы убывает от центра к краям.
     * Центр: baseDotSize * 2.5, Край: max(2, baseDotSize * 0.5)
     *
     * @param t нормализованное расстояние [0.0 = центр, 1.0 = край]
     */
    private int getSizeForDepth(double t) {
        t = Math.clamp(t, 0.0, 1.0);
        double scale = 2.5 - 2.0 * t; // 2.5 → 0.5
        return Math.max(2, (int) (baseDotSize * scale));
    }

    // ========================================================================
    // Частицы
    // ========================================================================

    private boolean spawnWalker(RNProvider provider, int index) {
        OptionalInt angleOpt = provider.getNextRandomNumber();
        if (angleOpt.isEmpty()) {
            walkerAlive[index] = false;
            return false; // Сигнализируем наверх, что чисел нет
        }

        int angle = Math.abs(angleOpt.getAsInt()) % 360;
        randomNumbersUsed++;

        double rad = Math.toRadians(angle);
        walkerX[index] = centerX + (int) (spawnRadius * Math.cos(rad));
        walkerY[index] = centerY + (int) (spawnRadius * Math.sin(rad));

        // Clamp (ограничение координат)
        walkerX[index] = Math.clamp(walkerX[index], 1, width - 2);
        walkerY[index] = Math.clamp(walkerY[index], 1, height - 2);

        walkerAge[index] = 0;
        walkerAlive[index] = true;

        return true; // Успешный спавн
    }

    /**
     * Проверяет, касается ли (x, y) кластера (8 соседей).
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

    private static double distance(int x1, int y1, int x2, int y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public int getPointCount() { return pointCount; }

    @Override
    public int getRandomNumbersUsed() { return randomNumbersUsed; }
}
