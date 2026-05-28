package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.OptionalInt;

/**
 * Режим визуализации: Monte Carlo Mandelbrot area estimation.
 *
 * Случайные точки бросаются в прямоугольник комплексной плоскости:
 * x ∈ [-2.0, 1.0], y ∈ [-1.5, 1.5].
 *
 * Для каждой точки проверяется, принадлежит ли она множеству Мандельброта:
 * z₀ = 0
 * zₙ₊₁ = zₙ² + c
 *
 * Если точка не "убегает" за MAX_ITERATIONS, она считается bounded/inside.
 * Доля bounded-точек даёт Monte Carlo оценку площади множества Мандельброта.
 */
public class MonteCarloMandelbrotAreaMode implements VisualizationMode {

    private static final String ID = "monte-carlo-mandelbrot-area";
    private static final String NAME = "Monte Carlo Mandelbrot Area";
    private static final String DESCRIPTION =
            "Случайные точки исследуют комплексную плоскость.\n"
                    + "Из облака постепенно проступает множество Мандельброта и оценка его площади.";
    private static final String ICON = "𝕄";

    private static final double RANDOM_MAX = 65_535.0;

    private static final double MIN_REAL = -2.0;
    private static final double MAX_REAL = 1.0;
    private static final double MIN_IMAG = -1.5;
    private static final double MAX_IMAG = 1.5;

    private static final double PLANE_WIDTH = MAX_REAL - MIN_REAL;
    private static final double PLANE_HEIGHT = MAX_IMAG - MIN_IMAG;
    private static final double PLANE_AREA = PLANE_WIDTH * PLANE_HEIGHT;

    private static final int MAX_ITERATIONS = 96;
    private static final double ESCAPE_RADIUS_SQUARED = 4.0;
    private static final int SAMPLES_PER_STEP = 220;

    private static final int HEADER_HEIGHT = 96;
    private static final int OUTER_PADDING = 18;
    private static final int PANEL_INSET = 16;
    private static final int PANEL_RADIUS = 22;
    private static final int STAT_CARD_HEIGHT = 74;
    private static final int STAT_CARD_GAP = 10;
    private static final int STAT_CARD_COUNT = 4;
    private static final int RESET_BUTTON_WIDTH = 82;
    private static final int CONTROL_HEIGHT = 28;

    private static final String RESET_TEXT = "Reset";
    private static final String RESET_TOOLTIP = "Restart Monte Carlo Mandelbrot area estimation";

    private static final Color BACKGROUND = new Color(2, 7, 14);
    private static final Color PANEL_BACKGROUND = new Color(8, 16, 30);
    private static final Color PANEL_BORDER = new Color(55, 90, 130, 180);
    private static final Color GRID_COLOR = new Color(90, 130, 170, 45);

    private static final Color TITLE_COLOR = new Color(238, 244, 252);
    private static final Color TEXT_PRIMARY = new Color(224, 235, 248);
    private static final Color TEXT_MUTED = new Color(140, 160, 190);
    private static final Color TEXT_DIM = new Color(90, 110, 136);

    private static final Color INSIDE_COLOR = new Color(45, 255, 205, 210);
    private static final Color BORDER_COLOR = new Color(255, 225, 96, 210);
    private static final Color FAST_ESCAPE_COLOR = new Color(70, 90, 135, 120);

    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 29);
    private static final Font SUBTITLE_FONT = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font PANEL_TITLE_FONT = new Font("SansSerif", Font.BOLD, 18);
    private static final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font VALUE_FONT = new Font("SansSerif", Font.BOLD, 18);
    private static final Font SMALL_FONT = new Font("SansSerif", Font.PLAIN, 11);

    private int width;
    private int height;

    private int pointCount;
    private int randomNumbersUsed;
    private int insideCount;
    private int escapedCount;

    private BufferedImage sampleLayer;
    private Rectangle plotBounds = new Rectangle();
    private Rectangle panelBounds = new Rectangle();
    private final Rectangle[] statCards = new Rectangle[STAT_CARD_COUNT];

    private DotController controller;

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
    public List<JComponent> createModeControls(DotController controller) {
        this.controller = controller;

        JButton resetButton = new JButton(RESET_TEXT);
        resetButton.setPreferredSize(new Dimension(RESET_BUTTON_WIDTH, CONTROL_HEIGHT));
        resetButton.setToolTipText(RESET_TOOLTIP);
        resetButton.addActionListener(ignored -> {
            resetState();
            layoutDashboard();

            if (this.controller != null) {
                this.controller.refreshVisualization();
            }
        });

        return List.of(resetButton);
    }

    @Override
    public void initialize(BufferedImage canvas, int width, int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);

        resetState();
        layoutDashboard();
        render(canvas);
    }

    @Override
    public List<Point> step(RNProvider provider, BufferedImage canvas, int dotSize) {
        ensureInitialized(canvas);

        int drawSize = Math.max(1, dotSize);
        int drawnThisStep = 0;

        Graphics2D sampleGraphics = sampleLayer.createGraphics();
        try {
            sampleGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            sampleGraphics.setComposite(AlphaComposite.SrcOver);

            for (int i = 0; i < SAMPLES_PER_STEP; i++) {
                OptionalInt rawX = provider.getNextRandomNumber();
                if (rawX.isEmpty()) {
                    break;
                }
                randomNumbersUsed++;

                OptionalInt rawY = provider.getNextRandomNumber();
                if (rawY.isEmpty()) {
                    break;
                }
                randomNumbersUsed++;

                double real = MIN_REAL + normalize(rawX.getAsInt()) * PLANE_WIDTH;
                double imaginary = MIN_IMAG + normalize(rawY.getAsInt()) * PLANE_HEIGHT;

                int escapeIteration = escapeIterations(real, imaginary);
                boolean inside = escapeIteration >= MAX_ITERATIONS;

                pointCount++;
                drawnThisStep++;

                if (inside) {
                    insideCount++;
                } else {
                    escapedCount++;
                }

                drawSamplePoint(sampleGraphics, real, imaginary, escapeIteration, inside, drawSize);
            }
        } finally {
            sampleGraphics.dispose();
        }

        if (drawnThisStep > 0) {
            render(canvas);
        }

        return List.of();
    }

    @Override
    public void redraw(BufferedImage canvas, int width, int height, int dotSize) {
        if (canvas == null) {
            return;
        }

        if (this.width != width || this.height != height || sampleLayer == null) {
            initialize(canvas, width, height);
            return;
        }

        render(canvas);
    }

    @Override
    public int getPointCount() {
        return pointCount;
    }

    @Override
    public int getRandomNumbersUsed() {
        return randomNumbersUsed;
    }

    private void ensureInitialized(BufferedImage canvas) {
        if (sampleLayer == null || width != canvas.getWidth() || height != canvas.getHeight()) {
            initialize(canvas, canvas.getWidth(), canvas.getHeight());
        }
    }

    private void resetState() {
        pointCount = 0;
        randomNumbersUsed = 0;
        insideCount = 0;
        escapedCount = 0;

        sampleLayer = new BufferedImage(
                Math.max(1, width),
                Math.max(1, height),
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g = sampleLayer.createGraphics();
        try {
            g.setComposite(AlphaComposite.Clear);
            g.fillRect(0, 0, sampleLayer.getWidth(), sampleLayer.getHeight());
            g.setComposite(AlphaComposite.SrcOver);
        } finally {
            g.dispose();
        }
    }

    private void layoutDashboard() {
        int cardsY = height - OUTER_PADDING - STAT_CARD_HEIGHT;
        int mainTop = HEADER_HEIGHT + OUTER_PADDING;
        int mainHeight = Math.max(120, cardsY - mainTop - OUTER_PADDING);

        panelBounds = new Rectangle(
                OUTER_PADDING,
                mainTop,
                width - OUTER_PADDING * 2,
                mainHeight
        );

        int plotSize = Math.max(
                80,
                Math.min(
                        panelBounds.width - PANEL_INSET * 2,
                        panelBounds.height - PANEL_INSET * 2 - 34
                )
        );

        int plotX = panelBounds.x + (panelBounds.width - plotSize) / 2;
        int plotY = panelBounds.y + PANEL_INSET + 34;

        plotBounds = new Rectangle(plotX, plotY, plotSize, plotSize);

        int availableCardsWidth = width - OUTER_PADDING * 2 - STAT_CARD_GAP * (STAT_CARD_COUNT - 1);
        int cardWidth = Math.max(120, availableCardsWidth / STAT_CARD_COUNT);

        for (int i = 0; i < STAT_CARD_COUNT; i++) {
            int x = OUTER_PADDING + i * (cardWidth + STAT_CARD_GAP);
            statCards[i] = new Rectangle(x, cardsY, cardWidth, STAT_CARD_HEIGHT);
        }
    }

    private void render(BufferedImage canvas) {
        Graphics2D g = canvas.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g.setColor(BACKGROUND);
            g.fillRect(0, 0, width, height);

            drawHeader(g);
            drawPanel(g);
            drawStats(g);
        } finally {
            g.dispose();
        }
    }

    private void drawHeader(Graphics2D g) {
        g.setFont(TITLE_FONT);
        g.setColor(TITLE_COLOR);
        g.drawString("MONTE CARLO MANDELBROT AREA", OUTER_PADDING, 44);

        g.setFont(SUBTITLE_FONT);
        g.setColor(TEXT_MUTED);
        g.drawString(
                "Random complex points reveal the Mandelbrot silhouette and estimate its area.",
                OUTER_PADDING,
                68
        );

        g.setFont(SMALL_FONT);
        g.setColor(TEXT_DIM);
        g.drawString(
                "window: Re ∈ [-2.0, 1.0], Im ∈ [-1.5, 1.5], max iterations = " + MAX_ITERATIONS,
                OUTER_PADDING,
                86
        );
    }

    private void drawPanel(Graphics2D g) {
        drawPanelBox(g, panelBounds);

        g.setFont(PANEL_TITLE_FONT);
        g.setColor(TEXT_PRIMARY);
        g.drawString("RANDOM SAMPLE SPACE OVER COMPLEX PLANE", panelBounds.x + PANEL_INSET, panelBounds.y + 24);

        drawGrid(g);
        g.drawImage(sampleLayer, 0, 0, null);

        g.setColor(PANEL_BORDER);
        g.drawRect(plotBounds.x, plotBounds.y, plotBounds.width, plotBounds.height);

        g.setFont(SMALL_FONT);
        g.setColor(TEXT_MUTED);
        g.drawString("Re", plotBounds.x + plotBounds.width + 8, plotBounds.y + plotBounds.height + 4);
        g.drawString("Im", plotBounds.x - 16, plotBounds.y - 4);

        g.drawString("-2.0", plotBounds.x - 10, plotBounds.y + plotBounds.height + 17);
        g.drawString("1.0", plotBounds.x + plotBounds.width - 10, plotBounds.y + plotBounds.height + 17);
        g.drawString("1.5", plotBounds.x - 24, plotBounds.y + 4);
        g.drawString("-1.5", plotBounds.x - 28, plotBounds.y + plotBounds.height + 4);

        drawLegend(g);
    }

    private void drawGrid(Graphics2D g) {
        g.setColor(GRID_COLOR);
        for (int i = 0; i <= 6; i++) {
            int x = plotBounds.x + Math.round((float) i / 6 * plotBounds.width);
            int y = plotBounds.y + Math.round((float) i / 6 * plotBounds.height);
            g.drawLine(x, plotBounds.y, x, plotBounds.y + plotBounds.height);
            g.drawLine(plotBounds.x, y, plotBounds.x + plotBounds.width, y);
        }
    }

    private void drawLegend(Graphics2D g) {
        int x = panelBounds.x + panelBounds.width - 260;
        int y = panelBounds.y + 20;

        g.setFont(LABEL_FONT);

        g.setColor(INSIDE_COLOR);
        g.fillOval(x, y - 8, 8, 8);
        g.drawString("bounded / inside", x + 14, y);

        g.setColor(BORDER_COLOR);
        g.fillOval(x + 128, y - 8, 8, 8);
        g.drawString("slow escape", x + 142, y);
    }

    private void drawStats(Graphics2D g) {
        drawStatCard(g, statCards[0], "TOTAL SAMPLES", Integer.toString(pointCount), "random complex points");
        drawStatCard(g, statCards[1], "BOUNDED", Integer.toString(insideCount), formatPercent(insideRatio()));
        drawStatCard(g, statCards[2], "ESCAPED", Integer.toString(escapedCount), formatPercent(escapedRatio()));
        drawStatCard(g, statCards[3], "AREA ESTIMATE", formatArea(areaEstimate()), "area ≈ 9 × bounded / total");
    }

    private void drawStatCard(Graphics2D g, Rectangle bounds, String label, String value, String smallText) {
        if (bounds == null) {
            return;
        }

        drawPanelBox(g, bounds);

        g.setFont(LABEL_FONT);
        g.setColor(TEXT_MUTED);
        g.drawString(label, bounds.x + 14, bounds.y + 22);

        g.setFont(VALUE_FONT);
        g.setColor(TITLE_COLOR);
        g.drawString(value, bounds.x + 14, bounds.y + 47);

        g.setFont(SMALL_FONT);
        g.setColor(TEXT_DIM);
        g.drawString(smallText, bounds.x + 14, bounds.y + 64);
    }

    private void drawPanelBox(Graphics2D g, Rectangle bounds) {
        drawPanelBox(g, bounds.x, bounds.y, bounds.width, bounds.height);
    }

    private void drawPanelBox(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(PANEL_BACKGROUND);
        g.fillRoundRect(x, y, w, h, PANEL_RADIUS, PANEL_RADIUS);

        g.setColor(PANEL_BORDER);
        g.drawRoundRect(x, y, w, h, PANEL_RADIUS, PANEL_RADIUS);
    }

    private void drawSamplePoint(
            Graphics2D g,
            double real,
            double imaginary,
            int escapeIteration,
            boolean inside,
            int drawSize
    ) {
        int px = plotBounds.x + clampInt(
                (int) Math.round((real - MIN_REAL) / PLANE_WIDTH * plotBounds.width),
                0,
                Math.max(0, plotBounds.width - 1)
        );

        int py = plotBounds.y + clampInt(
                (int) Math.round((MAX_IMAG - imaginary) / PLANE_HEIGHT * plotBounds.height),
                0,
                Math.max(0, plotBounds.height - 1)
        );

        if (inside) {
            g.setColor(INSIDE_COLOR);
        } else {
            g.setColor(colorForEscape(escapeIteration));
        }

        g.fillRect(px, py, drawSize, drawSize);
    }

    private static Color colorForEscape(int iteration) {
        if (iteration <= 4) {
            return FAST_ESCAPE_COLOR;
        }

        float hue = 0.62f - Math.min(0.48f, iteration / (float) MAX_ITERATIONS * 0.48f);
        float saturation = 0.85f;
        float brightness = 0.95f;
        return Color.getHSBColor(hue, saturation, brightness);
    }

    private static int escapeIterations(double cx, double cy) {
        double zx = 0.0;
        double zy = 0.0;

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            double zxNext = zx * zx - zy * zy + cx;
            double zyNext = 2.0 * zx * zy + cy;

            zx = zxNext;
            zy = zyNext;

            if (zx * zx + zy * zy > ESCAPE_RADIUS_SQUARED) {
                return iteration;
            }
        }

        return MAX_ITERATIONS;
    }

    private static double normalize(int value) {
        return Math.max(0.0, Math.min(1.0, value / RANDOM_MAX));
    }

    private double insideRatio() {
        return pointCount == 0 ? 0.0 : (double) insideCount / pointCount;
    }

    private double escapedRatio() {
        return pointCount == 0 ? 0.0 : (double) escapedCount / pointCount;
    }

    private double areaEstimate() {
        return pointCount == 0 ? 0.0 : PLANE_AREA * insideRatio();
    }

    private static String formatPercent(double value) {
        return String.format(java.util.Locale.US, "%.4f%%", value * 100.0);
    }

    private static String formatArea(double value) {
        if (value == 0.0) {
            return "—";
        }

        return String.format(java.util.Locale.US, "%.6f", value);
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
