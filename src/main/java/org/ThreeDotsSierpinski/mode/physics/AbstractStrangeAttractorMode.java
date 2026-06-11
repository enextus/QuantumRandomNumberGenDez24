package org.ThreeDotsSierpinski.mode.physics;

import org.ThreeDotsSierpinski.mode.VisualizationMode;
import org.ThreeDotsSierpinski.rng.RNProvider;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.OptionalInt;

/**
 * Shared 3D renderer and RK4 integrator for continuous strange-attractor modes.
 * <p>
 * The attractor itself is deterministic; QRNG/PSEUDO numbers add very small
 * parameter jitter per animation frame so the external random stream remains
 * part of the visual dynamics without destroying the attractor shape.
 */
abstract class AbstractStrangeAttractorMode implements VisualizationMode {

    private static final double RANDOM_MAX = 65_535.0;
    private static final double TWO_PI = Math.PI * 2.0;
    private static final int MIN_CANVAS_SIZE = 1;
    private static final int TITLE_X = 16;
    private static final int TITLE_Y = 22;
    private static final int SUBTITLE_Y = 40;
    private static final int DASHBOARD_X = 16;
    private static final int DASHBOARD_Y = 62;
    private static final int DASHBOARD_LINE_HEIGHT = 16;
    private static final int GRID_DIVISIONS = 10;
    private static final int SAFE_BORDER = 46;
    private static final int DEFAULT_FADE_ALPHA = 10;
    private static final int RESET_RANDOM_STEP = 997;
    private static final int RESET_RANDOM_OFFSET = 37;

    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 14);
    private static final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, 11);
    private static final Font MONO_FONT = new Font("Monospaced", Font.PLAIN, 11);

    private static final Color BACKGROUND = new Color(3, 7, 14);
    private static final Color GRID = new Color(16, 32, 52);
    private static final Color GRID_BRIGHT = new Color(38, 72, 102);
    private static final Color TEXT = new Color(190, 220, 245);
    private static final Color MUTED_TEXT = new Color(118, 154, 190);
    private static final Color DASHBOARD_BG = new Color(5, 10, 20, 140);

    protected int width;
    protected int height;
    protected int pointCount;
    protected int randomNumbersUsed;
    protected int frame;

    private double x;
    private double y;
    private double z;
    private double spin;
    private double tilt;

    private static double distanceSquared(double oldX, double oldY, double oldZ, double newX, double newY, double newZ) {
        double dx = newX - oldX;
        double dy = newY - oldY;
        double dz = newZ - oldZ;
        return dx * dx + dy * dy + dz * dz;
    }

    private static String format(double value) {
        return String.format(java.util.Locale.US, "%.3f", value);
    }

    private static String formatParameters(double[] parameters) {
        StringBuilder builder = new StringBuilder();
        int limit = Math.min(3, parameters.length);
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(String.format(java.util.Locale.US, "%.2f", parameters[i]));
        }
        return builder.toString();
    }

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
        this.spin = initialSpin();
        this.tilt = initialTilt();

        double[] start = initialState();
        this.x = start[0];
        this.y = start[1];
        this.z = start[2];

        clearCanvas(canvas);
        drawBaseOverlay(canvas);
    }

    @Override
    public List<Point> step(RNProvider provider, BufferedImage canvas, int dotSize) {
        if (provider == null || canvas == null) {
            return List.of();
        }

        this.width = canvas.getWidth();
        this.height = canvas.getHeight();

        int randomValue = nextRandomOrFrame(provider);
        double randomSigned = signedUnit(randomValue);
        double[] parameters = jitteredParameters(randomSigned);

        fadeCanvas(canvas, fadeAlpha());

        Graphics2D g2d = canvas.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int previousX = projectX(x, y, z);
            int previousY = projectY(x, y, z);
            Color lineColor = colorForFrame(randomSigned);
            g2d.setColor(lineColor);

            for (int i = 0; i < stepsPerFrame(); i++) {
                double oldX = x;
                double oldY = y;
                double oldZ = z;

                rk4(parameters);

                if (!isFiniteAndBounded(x, y, z)) {
                    resetState(randomValue + i * RESET_RANDOM_STEP);
                    previousX = projectX(x, y, z);
                    previousY = projectY(x, y, z);
                    continue;
                }

                int currentX = projectX(x, y, z);
                int currentY = projectY(x, y, z);

                if (isLineDrawable(previousX, previousY, currentX, currentY)) {
                    g2d.drawLine(previousX, previousY, currentX, currentY);
                    pointCount++;
                }

                previousX = currentX;
                previousY = currentY;

                if ((i & 63) == 0 && distanceSquared(oldX, oldY, oldZ, x, y, z) < 1.0e-18) {
                    resetState(randomValue + i + RESET_RANDOM_OFFSET);
                }
            }

            drawBaseOverlay(canvas);
            drawDashboard(g2d, parameters);
        } finally {
            g2d.dispose();
        }

        spin += spinSpeed();
        tilt += tiltSpeed();
        frame++;
        return List.of();
    }

    @Override
    public void redraw(BufferedImage canvas, int width, int height, int dotSize) {
        if (canvas == null) {
            return;
        }

        this.width = Math.max(MIN_CANVAS_SIZE, width);
        this.height = Math.max(MIN_CANVAS_SIZE, height);
        clearCanvas(canvas);
        drawBaseOverlay(canvas);
    }

    protected abstract double[] baseParameters();

    protected abstract double[] jitterAmplitudes();

    protected abstract double[] initialState();

    protected abstract void derivative(double x, double y, double z, double[] parameters, double[] out);

    protected abstract String formulaLabel();

    protected int stepsPerFrame() {
        return 1_100;
    }

    protected double dt() {
        return 0.006;
    }

    protected double scale() {
        return 70.0;
    }

    protected double maxAbsCoordinate() {
        return 120.0;
    }

    protected double initialSpin() {
        return -0.55;
    }

    protected double initialTilt() {
        return 0.42;
    }

    protected double spinSpeed() {
        return 0.006;
    }

    protected double tiltSpeed() {
        return 0.0015;
    }

    protected int fadeAlpha() {
        return DEFAULT_FADE_ALPHA;
    }

    protected Color colorForFrame(double randomSigned) {
        float hue = (float) ((frame * 0.003 + 0.55 + randomSigned * 0.04) % 1.0);
        if (hue < 0.0f) {
            hue += 1.0f;
        }
        return Color.getHSBColor(hue, 0.58f, 1.0f);
    }

    protected String subtitle() {
        return "QRNG-jittered continuous strange attractor";
    }

    private void rk4(double[] parameters) {
        double h = dt();
        double[] k1 = new double[3];
        double[] k2 = new double[3];
        double[] k3 = new double[3];
        double[] k4 = new double[3];

        derivative(x, y, z, parameters, k1);
        derivative(x + h * k1[0] / 2.0, y + h * k1[1] / 2.0, z + h * k1[2] / 2.0, parameters, k2);
        derivative(x + h * k2[0] / 2.0, y + h * k2[1] / 2.0, z + h * k2[2] / 2.0, parameters, k3);
        derivative(x + h * k3[0], y + h * k3[1], z + h * k3[2], parameters, k4);

        x += h * (k1[0] + 2.0 * k2[0] + 2.0 * k3[0] + k4[0]) / 6.0;
        y += h * (k1[1] + 2.0 * k2[1] + 2.0 * k3[1] + k4[1]) / 6.0;
        z += h * (k1[2] + 2.0 * k2[2] + 2.0 * k3[2] + k4[2]) / 6.0;
    }

    private double[] jitteredParameters(double randomSigned) {
        double[] base = baseParameters();
        double[] jitter = jitterAmplitudes();
        double[] result = new double[base.length];

        for (int i = 0; i < base.length; i++) {
            double amplitude = i < jitter.length ? jitter[i] : 0.0;
            result[i] = base[i] + randomSigned * amplitude;
        }

        return result;
    }

    private int nextRandomOrFrame(RNProvider provider) {
        OptionalInt value = provider.getNextRandomNumber();
        if (value.isPresent()) {
            randomNumbersUsed++;
            return value.getAsInt();
        }
        return Math.floorMod(frame * 1103 + 97, 65_536);
    }

    private double unit(int randomValue) {
        return Math.clamp(randomValue / RANDOM_MAX, 0.0, 1.0);
    }

    private double signedUnit(int randomValue) {
        return unit(randomValue) * 2.0 - 1.0;
    }

    private void resetState(int seed) {
        double[] start = initialState();
        double angle = TWO_PI * unit(Math.floorMod(seed, 65_536));
        double radius = 0.05 + 0.15 * unit(Math.floorMod(seed * 31 + 7, 65_536));

        x = start[0] + Math.cos(angle) * radius;
        y = start[1] + Math.sin(angle) * radius;
        z = start[2] + signedUnit(Math.floorMod(seed * 17 + 11, 65_536)) * radius;
    }

    private boolean isFiniteAndBounded(double currentX, double currentY, double currentZ) {
        double max = maxAbsCoordinate();
        return Double.isFinite(currentX)
                && Double.isFinite(currentY)
                && Double.isFinite(currentZ)
                && Math.abs(currentX) <= max
                && Math.abs(currentY) <= max
                && Math.abs(currentZ) <= max;
    }

    private boolean isLineDrawable(int x1, int y1, int x2, int y2) {
        Rectangle expandedCanvas = new Rectangle(-width, -height, width * 3, height * 3);
        return expandedCanvas.contains(x1, y1) || expandedCanvas.contains(x2, y2);
    }

    private int projectX(double sourceX, double sourceY, double sourceZ) {
        double[] projected = rotate(sourceX, sourceY, sourceZ);
        return (int) Math.round(width / 2.0 + projected[0] * scale());
    }

    private int projectY(double sourceX, double sourceY, double sourceZ) {
        double[] projected = rotate(sourceX, sourceY, sourceZ);
        return (int) Math.round(height / 2.0 - projected[1] * scale());
    }

    private double[] rotate(double sourceX, double sourceY, double sourceZ) {
        double cosSpin = Math.cos(spin);
        double sinSpin = Math.sin(spin);
        double cosTilt = Math.cos(tilt);
        double sinTilt = Math.sin(tilt);

        double x1 = sourceX * cosSpin + sourceZ * sinSpin;
        double z1 = -sourceX * sinSpin + sourceZ * cosSpin;
        double y1 = sourceY * cosTilt - z1 * sinTilt;

        return new double[]{x1, y1};
    }

    private void clearCanvas(BufferedImage canvas) {
        Graphics2D g2d = canvas.createGraphics();
        try {
            g2d.setColor(BACKGROUND);
            g2d.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        } finally {
            g2d.dispose();
        }
    }

    private void fadeCanvas(BufferedImage canvas, int alpha) {
        Graphics2D g2d = canvas.createGraphics();
        try {
            int safeAlpha = Math.clamp(alpha, 0, 255);
            g2d.setColor(new Color(BACKGROUND.getRed(), BACKGROUND.getGreen(), BACKGROUND.getBlue(), safeAlpha));
            g2d.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        } finally {
            g2d.dispose();
        }
    }

    private void drawBaseOverlay(BufferedImage canvas) {
        Graphics2D g2d = canvas.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            drawGrid(g2d, canvas.getWidth(), canvas.getHeight());
            drawTitle(g2d);
        } finally {
            g2d.dispose();
        }
    }

    private void drawGrid(Graphics2D g2d, int canvasWidth, int canvasHeight) {
        g2d.setColor(GRID);
        int stepX = Math.max(48, canvasWidth / GRID_DIVISIONS);
        int stepY = Math.max(48, canvasHeight / GRID_DIVISIONS);

        for (int gx = stepX; gx < canvasWidth; gx += stepX) {
            g2d.drawLine(gx, SAFE_BORDER / 2, gx, canvasHeight - SAFE_BORDER / 2);
        }
        for (int gy = stepY; gy < canvasHeight; gy += stepY) {
            g2d.drawLine(SAFE_BORDER / 2, gy, canvasWidth - SAFE_BORDER / 2, gy);
        }

        g2d.setColor(GRID_BRIGHT);
        g2d.drawRect(SAFE_BORDER / 2, SAFE_BORDER / 2,
                Math.max(0, canvasWidth - SAFE_BORDER),
                Math.max(0, canvasHeight - SAFE_BORDER));
    }

    private void drawTitle(Graphics2D g2d) {
        g2d.setFont(TITLE_FONT);
        g2d.setColor(TEXT);
        g2d.drawString(getName(), TITLE_X, TITLE_Y);

        g2d.setFont(LABEL_FONT);
        g2d.setColor(MUTED_TEXT);
        g2d.drawString(subtitle(), TITLE_X, SUBTITLE_Y);
        g2d.drawString(formulaLabel(), TITLE_X, height - 18);
    }

    private void drawDashboard(Graphics2D g2d, double[] parameters) {
        int dashboardWidth = 245;
        int dashboardHeight = 80;
        g2d.setColor(DASHBOARD_BG);
        g2d.fillRoundRect(DASHBOARD_X - 8, DASHBOARD_Y - 13, dashboardWidth, dashboardHeight, 12, 12);

        g2d.setFont(MONO_FONT);
        g2d.setColor(TEXT);
        g2d.drawString("points: " + pointCount, DASHBOARD_X, DASHBOARD_Y);
        g2d.drawString("random: " + randomNumbersUsed, DASHBOARD_X, DASHBOARD_Y + DASHBOARD_LINE_HEIGHT);
        g2d.drawString("spin: " + format(spin), DASHBOARD_X, DASHBOARD_Y + DASHBOARD_LINE_HEIGHT * 2);
        g2d.drawString("params: " + formatParameters(parameters), DASHBOARD_X, DASHBOARD_Y + DASHBOARD_LINE_HEIGHT * 3);
    }
}
