package org.ThreeDotsSierpinski.mode.physics;

import org.ThreeDotsSierpinski.mode.VisualizationMode;
import org.ThreeDotsSierpinski.rng.RNProvider;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.OptionalInt;

/**
 * Shared drawing helpers for Lissajous-family visualization modes.
 */
abstract class AbstractLissajousMode implements VisualizationMode {

    protected static final double TWO_PI = Math.PI * 2.0;
    protected static final double RANDOM_MAX = 65_535.0;
    protected static final int MIN_CANVAS_SIZE = 1;
    protected static final Color BACKGROUND = new Color(3, 7, 14);
    protected static final Color BACKGROUND_SOFT = new Color(5, 10, 20, 58);
    protected static final Color GRID = new Color(18, 35, 58);
    protected static final Color GRID_BRIGHT = new Color(35, 65, 96);
    protected static final Color TEXT = new Color(188, 216, 245);
    protected static final Color MUTED_TEXT = new Color(120, 154, 190);
    protected static final Color CYAN = new Color(90, 235, 255);
    protected static final Color BLUE = new Color(70, 145, 255);
    protected static final Color MAGENTA = new Color(255, 105, 210);
    protected static final Color AMBER = new Color(255, 190, 80);
    protected static final Color GREEN = new Color(125, 255, 170);
    protected static final Color WHITE_GLOW = new Color(235, 255, 255);
    private static final int TITLE_X = 16;
    private static final int TITLE_Y = 22;
    private static final int SUBTITLE_Y = 40;
    private static final int AXIS_LABEL_OFFSET = 18;
    private static final int SAFE_BORDER = 48;
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 14);
    private static final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, 11);
    protected int width;
    protected int height;
    protected int pointCount;
    protected int randomNumbersUsed;
    protected int frame;

    @Override
    public boolean usesDarkBackground() {
        return true;
    }

    @Override
    public boolean usesRecolorAnimation() {
        return false;
    }

    @Override
    public boolean usesRandomNumbersStackOverlay() {
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

    @Override
    public void initialize(BufferedImage canvas, int width, int height) {
        if (canvas == null) {
            throw new IllegalArgumentException("Canvas cannot be null");
        }

        this.width = Math.max(MIN_CANVAS_SIZE, width);
        this.height = Math.max(MIN_CANVAS_SIZE, height);
        this.pointCount = 0;
        this.randomNumbersUsed = 0;
        this.frame = 0;

        clearCanvas(canvas);
        drawGrid(canvas, getName(), subtitle());
    }

    @Override
    public List<Point> step(RNProvider provider, BufferedImage canvas, int dotSize) {
        if (provider == null || canvas == null) {
            return List.of();
        }

        this.width = canvas.getWidth();
        this.height = canvas.getHeight();
        return stepLissajous(provider, canvas, Math.max(1, dotSize));
    }

    @Override
    public void redraw(BufferedImage canvas, int width, int height, int dotSize) {
        if (canvas == null) {
            return;
        }

        this.width = Math.max(MIN_CANVAS_SIZE, width);
        this.height = Math.max(MIN_CANVAS_SIZE, height);
        clearCanvas(canvas);
        drawGrid(canvas, getName(), subtitle());
    }

    protected abstract List<Point> stepLissajous(RNProvider provider, BufferedImage canvas, int dotSize);

    protected String subtitle() {
        return "QRNG-driven Lissajous visualization";
    }

    protected OptionalInt nextRandom(RNProvider provider) {
        OptionalInt value = provider.getNextRandomNumber();
        if (value.isPresent()) {
            randomNumbersUsed++;
        }
        return value;
    }

    protected double unit(int randomValue) {
        return Math.clamp(randomValue / RANDOM_MAX, 0.0, 1.0);
    }

    protected double signedUnit(int randomValue) {
        return unit(randomValue) * 2.0 - 1.0;
    }

    protected int mapX(double normalizedX) {
        double half = Math.max(1.0, (width - SAFE_BORDER * 2) / 2.0);
        return (int) Math.round(width / 2.0 + normalizedX * half);
    }

    protected int mapY(double normalizedY) {
        double half = Math.max(1.0, (height - SAFE_BORDER * 2) / 2.0);
        return (int) Math.round(height / 2.0 - normalizedY * half);
    }

    protected int mapXInRect(Rectangle rect, double normalizedX) {
        return (int) Math.round(rect.x + rect.width / 2.0 + normalizedX * Math.max(1.0, rect.width * 0.45));
    }

    protected int mapYInRect(Rectangle rect, double normalizedY) {
        return (int) Math.round(rect.y + rect.height / 2.0 - normalizedY * Math.max(1.0, rect.height * 0.45));
    }

    protected void clearCanvas(BufferedImage canvas) {
        Graphics2D g2d = canvas.createGraphics();
        try {
            g2d.setColor(BACKGROUND);
            g2d.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        } finally {
            g2d.dispose();
        }
    }

    protected void fadeCanvas(BufferedImage canvas, int alpha) {
        Graphics2D g2d = canvas.createGraphics();
        try {
            g2d.setColor(new Color(BACKGROUND.getRed(), BACKGROUND.getGreen(), BACKGROUND.getBlue(), Math.clamp(alpha, 0, 255)));
            g2d.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        } finally {
            g2d.dispose();
        }
    }

    protected void drawGrid(BufferedImage canvas, String title, String subtitle) {
        Graphics2D g2d = canvas.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(GRID);

            int stepX = Math.max(40, canvas.getWidth() / 12);
            int stepY = Math.max(40, canvas.getHeight() / 10);
            for (int x = stepX; x < canvas.getWidth(); x += stepX) {
                g2d.drawLine(x, 0, x, canvas.getHeight());
            }
            for (int y = stepY; y < canvas.getHeight(); y += stepY) {
                g2d.drawLine(0, y, canvas.getWidth(), y);
            }

            g2d.setColor(GRID_BRIGHT);
            g2d.drawLine(canvas.getWidth() / 2, SAFE_BORDER / 2, canvas.getWidth() / 2, canvas.getHeight() - SAFE_BORDER / 2);
            g2d.drawLine(SAFE_BORDER / 2, canvas.getHeight() / 2, canvas.getWidth() - SAFE_BORDER / 2, canvas.getHeight() / 2);

            g2d.setFont(TITLE_FONT);
            g2d.setColor(TEXT);
            g2d.drawString(title, TITLE_X, TITLE_Y);

            g2d.setFont(LABEL_FONT);
            g2d.setColor(MUTED_TEXT);
            g2d.drawString(subtitle, TITLE_X, SUBTITLE_Y);
            g2d.drawString("x = sin(a·t + δ)", TITLE_X, canvas.getHeight() - AXIS_LABEL_OFFSET);
            g2d.drawString("y = sin(b·t)", canvas.getWidth() - 120, canvas.getHeight() - AXIS_LABEL_OFFSET);
        } finally {
            g2d.dispose();
        }
    }

    protected void drawPolyline(Graphics2D g2d, int[] xs, int[] ys, int count, Color color) {
        if (count < 2) {
            return;
        }

        g2d.setColor(color);
        for (int i = 1; i < count; i++) {
            g2d.drawLine(xs[i - 1], ys[i - 1], xs[i], ys[i]);
        }
        pointCount += count;
    }

    protected void drawDot(Graphics2D g2d, int x, int y, int size, Color color) {
        g2d.setColor(color);
        int half = Math.max(0, size / 2);
        g2d.fillRect(x - half, y - half, Math.max(1, size), Math.max(1, size));
        pointCount++;
    }
}
