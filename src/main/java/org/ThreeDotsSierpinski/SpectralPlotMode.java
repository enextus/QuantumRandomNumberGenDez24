package org.ThreeDotsSierpinski;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * Режим визуализации: spectral plot для потока случайных чисел.
 * <p>
 * Последовательные тройки значений интерпретируются как точки
 * (x_n, x_{n+1}, x_{n+2}) в нормализованном 3D-пространстве.
 * На экран выводится 2D-проекция этого облака. Для плохих генераторов
 * могут проявляться полосы, решётки или плоскости; хороший поток должен
 * давать более равномерное облако.
 */
public class SpectralPlotMode implements VisualizationMode {

    private static final String ID = "spectral-plot";
    private static final String NAME = "Spectral RNG Plot";
    private static final String DESCRIPTION =
            "Тройки чисел (xₙ, xₙ₊₁, xₙ₊₂) становятся 3D-точками.\n"
                    + "Плохой генератор выдаёт плоскости/полосы, хороший — равномерное облако.";
    private static final String ICON = "✦";

    private static final int RANDOM_RANGE = 65_536;
    private static final float RANDOM_MAX = 65_535.0f;

    private static final int TRIPLES_PER_STEP = 320;
    private static final int VALUES_PER_TRIPLE = 3;

    private static final int MIN_CANVAS_SIZE = 1;
    private static final int CANVAS_ORIGIN_X = 0;
    private static final int CANVAS_ORIGIN_Y = 0;

    private static final int PLOT_MARGIN = 42;
    private static final int GRID_LINES = 8;
    private static final int GRID_ALPHA = 55;
    private static final int AXIS_ALPHA = 135;
    private static final int POINT_ALPHA = 145;

    private static final float DEPTH_SHIFT_X_FACTOR = 0.30f;
    private static final float DEPTH_SHIFT_Y_FACTOR = 0.26f;
    private static final float FRONT_SCALE = 0.78f;

    private static final int MIN_POINT_SIZE = 1;
    private static final int MAX_POINT_SIZE = 4;

    private static final int LABEL_X = 16;
    private static final int LABEL_Y = 22;
    private static final int LABEL_FONT_SIZE = 12;

    private static final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, LABEL_FONT_SIZE);

    private int width;
    private int height;
    private int pointCount = 0;
    private int randomNumbersUsed = 0;

    @Override
    public boolean usesLeftPointCounterOverlay() {
        return true;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public String getIcon() {
        return ICON;
    }

    @Override
    public boolean usesRecolorAnimation() {
        return false;
    }

    @Override
    public boolean usesDarkBackground() {
        return true;
    }

    @Override
    public void initialize(BufferedImage canvas, int width, int height) {
        if (canvas == null) {
            throw new IllegalArgumentException("Canvas cannot be null");
        }

        this.width = Math.max(MIN_CANVAS_SIZE, width);
        this.height = Math.max(MIN_CANVAS_SIZE, height);
        this.pointCount = 0;
        this.randomNumbersUsed = 0;

        drawBackground(canvas);
    }

    @Override
    public List<Point> step(RNProvider provider, BufferedImage canvas, int dotSize) {
        var newPoints = new ArrayList<Point>(TRIPLES_PER_STEP);
        Graphics2D g2d = canvas.createGraphics();

        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setComposite(AlphaComposite.SrcOver.derive(0.92f));

            for (int i = 0; i < TRIPLES_PER_STEP; i++) {
                OptionalInt xValue = provider.getNextRandomNumber();
                if (xValue.isEmpty()) {
                    break;
                }
                randomNumbersUsed++;

                OptionalInt yValue = provider.getNextRandomNumber();
                if (yValue.isEmpty()) {
                    break;
                }
                randomNumbersUsed++;

                OptionalInt zValue = provider.getNextRandomNumber();
                if (zValue.isEmpty()) {
                    break;
                }
                randomNumbersUsed++;

                Point projected = plotTriple(g2d, xValue.getAsInt(), yValue.getAsInt(), zValue.getAsInt(), dotSize);
                newPoints.add(projected);
                pointCount++;
            }
        } finally {
            g2d.dispose();
        }

        return newPoints;
    }

    private Point plotTriple(Graphics2D g2d, int rawX, int rawY, int rawZ, int dotSize) {
        float x = normalize(rawX);
        float y = normalize(rawY);
        float z = normalize(rawZ);

        int plotWidth = Math.max(MIN_CANVAS_SIZE, width - PLOT_MARGIN * 2);
        int plotHeight = Math.max(MIN_CANVAS_SIZE, height - PLOT_MARGIN * 2);

        float depthShiftX = (z - 0.5f) * plotWidth * DEPTH_SHIFT_X_FACTOR;
        float depthShiftY = (z - 0.5f) * plotHeight * DEPTH_SHIFT_Y_FACTOR;

        int screenX = Math.round(PLOT_MARGIN + x * plotWidth * FRONT_SCALE + depthShiftX);
        int screenY = Math.round(PLOT_MARGIN + (1.0f - y) * plotHeight * FRONT_SCALE - depthShiftY);

        int pointSize = Math.max(
                MIN_POINT_SIZE,
                Math.min(MAX_POINT_SIZE, Math.max(1, dotSize) + Math.round(z * 2.0f))
        );

        g2d.setColor(colorForDepth(z));
        g2d.fillOval(
                screenX - pointSize / 2,
                screenY - pointSize / 2,
                pointSize,
                pointSize
        );

        return new Point(screenX, screenY);
    }

    private static float normalize(int value) {
        return Math.floorMod(value, RANDOM_RANGE) / RANDOM_MAX;
    }

    private static Color colorForDepth(float z) {
        float hue = 0.66f - 0.56f * z;
        float saturation = 0.85f;
        float brightness = 0.55f + 0.45f * z;

        Color base = Color.getHSBColor(hue, saturation, brightness);
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), POINT_ALPHA);
    }

    private void drawBackground(BufferedImage canvas) {
        Graphics2D g2d = canvas.createGraphics();

        try {
            g2d.setColor(Color.BLACK);
            g2d.fillRect(CANVAS_ORIGIN_X, CANVAS_ORIGIN_Y, width, height);

            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setFont(LABEL_FONT);

            drawGrid(g2d);
            drawLabel(g2d);
        } finally {
            g2d.dispose();
        }
    }

    private void drawGrid(Graphics2D g2d) {
        int plotWidth = Math.max(MIN_CANVAS_SIZE, width - PLOT_MARGIN * 2);
        int plotHeight = Math.max(MIN_CANVAS_SIZE, height - PLOT_MARGIN * 2);
        int right = PLOT_MARGIN + plotWidth;
        int bottom = PLOT_MARGIN + plotHeight;

        g2d.setColor(new Color(80, 90, 120, GRID_ALPHA));
        for (int i = 0; i <= GRID_LINES; i++) {
            int x = PLOT_MARGIN + Math.round((float) i / GRID_LINES * plotWidth);
            int y = PLOT_MARGIN + Math.round((float) i / GRID_LINES * plotHeight);
            g2d.drawLine(x, PLOT_MARGIN, x, bottom);
            g2d.drawLine(PLOT_MARGIN, y, right, y);
        }

        g2d.setColor(new Color(180, 200, 255, AXIS_ALPHA));
        g2d.drawRect(PLOT_MARGIN, PLOT_MARGIN, plotWidth, plotHeight);
        g2d.drawString("xₙ", right - 18, bottom + 18);
        g2d.drawString("xₙ₊₁", PLOT_MARGIN - 30, PLOT_MARGIN + 12);
        g2d.drawString("depth = xₙ₊₂", right - 88, PLOT_MARGIN - 10);
    }

    private void drawLabel(Graphics2D g2d) {
        g2d.setColor(new Color(190, 210, 255, 180));
        g2d.drawString("Spectral plot: (xₙ, xₙ₊₁, xₙ₊₂)", LABEL_X, LABEL_Y);
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
