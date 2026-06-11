package org.ThreeDotsSierpinski.mode.physics;

import org.ThreeDotsSierpinski.mode.VisualizationMode;
import org.ThreeDotsSierpinski.rng.RNProvider;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * Visualization mode: bifurcation diagram for the logistic map.
 * Formula:
 * x(n+1) = r * x(n) * (1 - x(n))
 * The horizontal axis is the control parameter r.
 * The vertical axis is the long-term value of x.
 * As r grows, the system moves from a stable fixed point to period doubling
 * and then into chaos, with small islands of order inside the chaotic region.
 */
public class BifurcationDiagramMode implements VisualizationMode {

    private static final String ID = "bifurcation-diagram";
    private static final String NAME = "Bifurcation Diagram";
    private static final String ICON = "Y";

    private static final String DESCRIPTION = "Логистическое отображение показывает путь от порядка к хаосу:\n"
            + "стабильность → раздвоения → каскад бифуркаций → хаотическая область.";

    private static final double R_MIN = 2.5;
    private static final double R_MAX = 4.0;

    private static final int WARMUP_ITERATIONS = 140;
    private static final int PLOT_ITERATIONS = 52;
    private static final int COLUMNS_PER_STEP = 3;

    private static final double DEFAULT_INITIAL_X = 0.5;
    private static final double RANDOM_X_JITTER = 0.18;
    private static final int UINT16_MAX = 65_535;

    private static final int AXIS_LEFT = 48;
    private static final int AXIS_RIGHT = 18;
    private static final int AXIS_TOP = 34;
    private static final int AXIS_BOTTOM = 42;

    private static final int MIN_CANVAS_SIZE = 1;
    private static final int MIN_PLOT_SIZE = 10;

    private static final Color BACKGROUND = new Color(4, 8, 16);
    private static final Color BORDER = new Color(52, 82, 120);
    private static final Color GRID = new Color(20, 38, 62);
    private static final Color AXIS = new Color(120, 150, 190);
    private static final Color LABEL = new Color(180, 205, 235);
    private static final Color POINT_COLD = new Color(70, 150, 255);
    private static final Color POINT_HOT = new Color(120, 255, 220);
    private static final Color POINT_WHITE = new Color(235, 255, 255);

    private static final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, 11);
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 14);

    private int width;
    private int height;
    private int plotX;
    private int plotY;
    private int plotWidth;
    private int plotHeight;

    private int currentColumn;
    private int pointCount;
    private int randomNumbersUsed;

    private static double initialXFromRandom(int randomValue) {
        double normalized = Math.clamp(randomValue / (double) UINT16_MAX, 0.0, 1.0);
        double centered = normalized - 0.5;
        return Math.clamp(DEFAULT_INITIAL_X + centered * RANDOM_X_JITTER, 0.001, 0.999);
    }

    private static double logistic(double r, double x) {
        return r * x * (1.0 - x);
    }

    private static Color colorForIteration(int iteration) {
        if (iteration > PLOT_ITERATIONS * 2 / 3) {
            return POINT_WHITE;
        }
        if (iteration > PLOT_ITERATIONS / 3) {
            return POINT_HOT;
        }
        return POINT_COLD;
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
    public void initialize(BufferedImage canvas, int width, int height) {
        if (canvas == null) {
            throw new IllegalArgumentException("Canvas cannot be null");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Canvas size must be positive");
        }

        this.width = width;
        this.height = height;
        this.plotX = Math.min(AXIS_LEFT, Math.max(0, width - MIN_PLOT_SIZE));
        this.plotY = Math.min(AXIS_TOP, Math.max(0, height - MIN_PLOT_SIZE));
        this.plotWidth = Math.max(MIN_PLOT_SIZE, width - AXIS_LEFT - AXIS_RIGHT);
        this.plotHeight = Math.max(MIN_PLOT_SIZE, height - AXIS_TOP - AXIS_BOTTOM);
        this.currentColumn = 0;
        this.pointCount = 0;
        this.randomNumbersUsed = 0;

        clearAndDrawBase(canvas);
    }

    @Override
    public List<Point> step(RNProvider provider, BufferedImage canvas, int dotSize) {
        if (provider == null) {
            throw new IllegalArgumentException("RNProvider cannot be null");
        }
        if (canvas == null) {
            throw new IllegalArgumentException("Canvas cannot be null");
        }

        ensureInitialized(canvas);

        var newPoints = new ArrayList<Point>(COLUMNS_PER_STEP * PLOT_ITERATIONS);
        Graphics2D g2d = canvas.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

            for (int i = 0; i < COLUMNS_PER_STEP; i++) {
                if (currentColumn >= plotWidth) {
                    currentColumn = 0;
                    clearAndDrawBase(canvas);
                }

                OptionalInt randomOpt = provider.getNextRandomNumber();
                if (randomOpt.isEmpty()) {
                    break;
                }

                int randomValue = randomOpt.getAsInt();
                randomNumbersUsed++;

                double r = columnToR(currentColumn);
                double x = initialXFromRandom(randomValue);

                for (int warmup = 0; warmup < WARMUP_ITERATIONS; warmup++) {
                    x = logistic(r, x);
                }

                for (int iteration = 0; iteration < PLOT_ITERATIONS; iteration++) {
                    x = logistic(r, x);
                    Point p = mapToPixel(currentColumn, x);

                    if (isInsidePlot(p)) {
                        g2d.setColor(colorForIteration(iteration));
                        g2d.fillRect(p.x, p.y, Math.max(1, dotSize), Math.max(1, dotSize));
                        newPoints.add(p);
                        pointCount++;
                    }
                }

                currentColumn++;
            }
        } finally {
            g2d.dispose();
        }

        return newPoints;
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
    public boolean usesDarkBackground() {
        return true;
    }

    @Override
    public boolean usesRecolorAnimation() {
        return false;
    }

    /**
     * Compatibility with current visualization infrastructure variants.
     * Some older baselines have this method abstract in VisualizationMode.
     */
    public boolean usesDefaultOverlay() {
        return true;
    }

    @Override
    public void redraw(BufferedImage canvas, int width, int height, int dotSize) {
        if (canvas == null) {
            return;
        }
        initialize(canvas, Math.max(MIN_CANVAS_SIZE, width), Math.max(MIN_CANVAS_SIZE, height));
    }

    private void ensureInitialized(BufferedImage canvas) {
        if (width <= 0 || height <= 0 || plotWidth <= 0 || plotHeight <= 0) {
            initialize(canvas, canvas.getWidth(), canvas.getHeight());
        }
    }

    private void clearAndDrawBase(BufferedImage canvas) {
        Graphics2D g2d = canvas.createGraphics();
        try {
            g2d.setColor(BACKGROUND);
            g2d.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

            drawGrid(g2d);
            drawAxes(g2d);
            drawLabels(g2d);
        } finally {
            g2d.dispose();
        }
    }

    private void drawGrid(Graphics2D g2d) {
        g2d.setColor(GRID);
        for (int i = 1; i < 6; i++) {
            int x = plotX + i * plotWidth / 6;
            g2d.drawLine(x, plotY, x, plotY + plotHeight);
        }
        for (int i = 1; i < 5; i++) {
            int y = plotY + i * plotHeight / 5;
            g2d.drawLine(plotX, y, plotX + plotWidth, y);
        }
    }

    private void drawAxes(Graphics2D g2d) {
        g2d.setColor(BORDER);
        g2d.drawRect(plotX, plotY, plotWidth, plotHeight);

        g2d.setColor(AXIS);
        g2d.drawLine(plotX, plotY + plotHeight, plotX + plotWidth, plotY + plotHeight);
        g2d.drawLine(plotX, plotY, plotX, plotY + plotHeight);
    }

    private void drawLabels(Graphics2D g2d) {
        g2d.setFont(TITLE_FONT);
        g2d.setColor(LABEL);
        g2d.drawString("Logistic map bifurcation:  xₙ₊₁ = r · xₙ · (1 − xₙ)", plotX, 20);

        g2d.setFont(LABEL_FONT);
        g2d.drawString("r = 2.5", plotX, plotY + plotHeight + 22);
        g2d.drawString("r = 4.0", plotX + plotWidth - 42, plotY + plotHeight + 22);
        g2d.drawString("x", 14, plotY + 12);
        g2d.drawString("1.0", 20, plotY + 4);
        g2d.drawString("0.0", 20, plotY + plotHeight + 4);
    }

    private double columnToR(int column) {
        if (plotWidth <= 1) {
            return R_MIN;
        }
        double t = column / (double) (plotWidth - 1);
        return R_MIN + t * (R_MAX - R_MIN);
    }

    private Point mapToPixel(int column, double xValue) {
        int px = plotX + column;
        int py = plotY + plotHeight - (int) Math.round(xValue * plotHeight);
        return new Point(px, py);
    }

    private boolean isInsidePlot(Point point) {
        return point.x >= plotX
                && point.x <= plotX + plotWidth
                && point.y >= plotY
                && point.y <= plotY + plotHeight;
    }
}