package org.ThreeDotsSierpinski.mode.physics;

import org.ThreeDotsSierpinski.app.DotController;
import org.ThreeDotsSierpinski.mode.VisualizationMode;
import org.ThreeDotsSierpinski.rng.RNProvider;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.OptionalInt;

/**
 * Chirikov standard map phase-space visualization.
 *
 * <p>QRNG/PSEUDO values seed many initial conditions in the torus (x, p). The
 * deterministic standard map then evolves each orbit. For intermediate K the
 * plot reveals KAM islands embedded in a chaotic sea.</p>
 */
public class ChirikovStandardMapMode implements VisualizationMode {

    private static final String ID = "chirikov-standard-map";
    private static final String NAME = "Chirikov Standard Map";
    private static final String DESCRIPTION =
            "Hamiltonian chaos on a torus: islands of order inside a chaotic sea.\n"
                    + "QRNG seeds phase-space orbits; K controls the transition to chaos.";
    private static final String ICON = "⊙";

    private static final double RANDOM_MAX = 65_535.0;
    private static final double TWO_PI = Math.PI * 2.0;
    private static final int ORBITS_PER_STEP = 18;
    private static final int ITERATIONS_PER_ORBIT = 80;

    private static final int OUTER_PADDING = 18;
    private static final int HEADER_HEIGHT = 76;
    private static final int FOOTER_HEIGHT = 76;
    private static final int OCCUPANCY_GRID_SIZE = 64;
    private static final int OCCUPANCY_CELL_COUNT = OCCUPANCY_GRID_SIZE * OCCUPANCY_GRID_SIZE;
    private static final double BALANCE_PERCENT_SCALE = 100.0;
    private static final double IDEAL_QUADRANT_PERCENT = 25.0;
    private static final int PANEL_RADIUS = 20;
    private static final int CONTROL_HEIGHT = 28;
    private static final int RESET_BUTTON_WIDTH = 82;
    private static final int K_COMBO_WIDTH = 94;

    private static final String RESET_TEXT = "Reset";
    private static final String RESET_TOOLTIP = "Restart Chirikov phase-space accumulation";
    private static final String K_LABEL_TEXT = "K";
    private static final String K_TOOLTIP = "Nonlinearity parameter: low K keeps islands, high K creates a chaotic sea";
    private static final Double[] K_PRESETS = {0.8, 1.2, 2.5, 4.0, 6.0};

    private static final Color BACKGROUND = new Color(2, 6, 12);
    private static final Color PANEL_BACKGROUND = new Color(7, 14, 28);
    private static final Color PANEL_BORDER = new Color(70, 96, 135, 180);
    private static final Color TITLE_COLOR = new Color(238, 246, 255);
    private static final Color TEXT_PRIMARY = new Color(220, 232, 248);
    private static final Color TEXT_MUTED = new Color(130, 156, 190);
    private static final Color TEXT_ACCENT = new Color(255, 210, 105);

    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 27);
    private static final Font SUBTITLE_FONT = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font VALUE_FONT = new Font("Monospaced", Font.PLAIN, 12);

    private int width;
    private int height;
    private int pointCount;
    private int randomNumbersUsed;
    private int orbitCount;
    private int xSeedHighCount;
    private int pSeedHighCount;
    private final int[] quadrantCounts = new int[4];
    private final int[] occupancyCells = new int[OCCUPANCY_CELL_COUNT];
    private int occupiedCellCount;
    private long occupancyHitCount;
    private double seedSumX;
    private double seedSumP;
    private double seedSumXSquared;
    private double seedSumPSquared;
    private double seedSumXP;
    private double kValue = 2.5;

    private Rectangle headerBounds = new Rectangle();
    private Rectangle plotBounds = new Rectangle();
    private Rectangle footerBounds = new Rectangle();
    private BufferedImage chromeLayer;
    private BufferedImage orbitLayer;
    private DotController controller;
    private RNProvider.Mode lastObservedMode = RNProvider.Mode.PSEUDO;

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
    public boolean usesDarkBackground() {
        return true;
    }

    @Override
    public boolean usesRecolorAnimation() {
        return false;
    }

    @Override
    public boolean usesInfoTextOverlay() {
        return false;
    }

    @Override
    public boolean usesPointCounterOverlay() {
        return false;
    }

    @Override
    public boolean usesRngModeIndicatorOverlay() {
        return false;
    }

    @Override
    public List<JComponent> createModeControls(DotController controller) {
        this.controller = controller;

        JButton resetButton = new JButton(RESET_TEXT);
        resetButton.setPreferredSize(new Dimension(RESET_BUTTON_WIDTH, CONTROL_HEIGHT));
        resetButton.setToolTipText(RESET_TOOLTIP);
        resetButton.addActionListener(event -> restart());

        JLabel kLabel = new JLabel(K_LABEL_TEXT);
        JComboBox<Double> kComboBox = new JComboBox<>(K_PRESETS);
        kComboBox.setSelectedItem(kValue);
        kComboBox.setPreferredSize(new Dimension(K_COMBO_WIDTH, CONTROL_HEIGHT));
        kComboBox.setToolTipText(K_TOOLTIP);
        kComboBox.addActionListener(event -> {
            Object selected = kComboBox.getSelectedItem();
            if (selected instanceof Double newK && Double.compare(newK, kValue) != 0) {
                kValue = newK;
                restart();
            }
        });

        return List.of(resetButton, kLabel, kComboBox);
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
        if (canvas == null || orbitLayer == null) {
            return List.of();
        }

        lastObservedMode = provider.getMode();
        int acceptedOrbits = 0;

        Graphics2D graphics = orbitLayer.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            graphics.setComposite(AlphaComposite.SrcOver);

            for (int orbit = 0; orbit < ORBITS_PER_STEP; orbit++) {
                OptionalInt rawX = provider.getNextRandomNumber();
                if (rawX.isEmpty()) {
                    break;
                }
                randomNumbersUsed++;

                OptionalInt rawP = provider.getNextRandomNumber();
                if (rawP.isEmpty()) {
                    break;
                }
                randomNumbersUsed++;

                acceptedOrbits++;
                orbitCount++;

                double normalizedX = normalize(rawX.getAsInt());
                double normalizedP = normalize(rawP.getAsInt());
                updateSeedDiagnostics(normalizedX, normalizedP);
                drawOrbit(graphics, normalizedX * TWO_PI, normalizedP * TWO_PI);
            }
        } finally {
            graphics.dispose();
        }

        if (acceptedOrbits > 0) {
            render(canvas);
        }

        return List.of();
    }

    @Override
    public void redraw(BufferedImage canvas, int width, int height, int dotSize) {
        if (canvas == null) {
            return;
        }

        if (canvas.getWidth() != this.width || canvas.getHeight() != this.height) {
            this.width = Math.max(1, canvas.getWidth());
            this.height = Math.max(1, canvas.getHeight());
            layoutDashboard();
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

    private void drawOrbit(Graphics2D graphics, double x, double p) {
        for (int iteration = 0; iteration < ITERATIONS_PER_ORBIT; iteration++) {
            p = wrapToTwoPi(p + kValue * Math.sin(x));
            x = wrapToTwoPi(x + p);

            int plotX = (int) Math.floor(x / TWO_PI * Math.max(1, plotBounds.width));
            int plotY = plotBounds.height - 1 - (int) Math.floor(p / TWO_PI * Math.max(1, plotBounds.height));

            if (plotX >= 0 && plotX < plotBounds.width && plotY >= 0 && plotY < plotBounds.height) {
                float hue = (float) (0.55 + 0.25 * iteration / (double) ITERATIONS_PER_ORBIT);
                graphics.setColor(new Color(Color.HSBtoRGB(hue, 0.72f, 1.0f) & 0x66FFFFFF, true));
                graphics.fillRect(plotX, plotY, 1, 1);
                updateOccupancyDiagnostics(x, p);
                pointCount++;
            }
        }
    }

    private void restart() {
        resetState();
        layoutDashboard();
        if (controller != null) {
            controller.refreshVisualization();
        }
    }

    private void resetState() {
        pointCount = 0;
        randomNumbersUsed = 0;
        orbitCount = 0;
        xSeedHighCount = 0;
        pSeedHighCount = 0;
        occupiedCellCount = 0;
        occupancyHitCount = 0L;
        seedSumX = 0.0;
        seedSumP = 0.0;
        seedSumXSquared = 0.0;
        seedSumPSquared = 0.0;
        seedSumXP = 0.0;
        clearIntArray(quadrantCounts);
        clearIntArray(occupancyCells);
    }

    private void clearIntArray(int[] values) {
        for (int i = 0; i < values.length; i++) {
            values[i] = 0;
        }
    }

    private void ensureInitialized(BufferedImage canvas) {
        if (canvas == null) {
            return;
        }

        if (orbitLayer == null || canvas.getWidth() != width || canvas.getHeight() != height) {
            initialize(canvas, canvas.getWidth(), canvas.getHeight());
        }
    }

    private void layoutDashboard() {
        int safeWidth = Math.max(1, width);
        int safeHeight = Math.max(1, height);

        headerBounds = new Rectangle(OUTER_PADDING, OUTER_PADDING, Math.max(1, safeWidth - OUTER_PADDING * 2), HEADER_HEIGHT);
        footerBounds = new Rectangle(
                OUTER_PADDING,
                Math.max(OUTER_PADDING, safeHeight - FOOTER_HEIGHT - OUTER_PADDING),
                Math.max(1, safeWidth - OUTER_PADDING * 2),
                FOOTER_HEIGHT
        );
        int plotY = headerBounds.y + headerBounds.height + 10;
        int plotBottom = footerBounds.y - 10;
        plotBounds = new Rectangle(
                OUTER_PADDING,
                plotY,
                Math.max(1, safeWidth - OUTER_PADDING * 2),
                Math.max(1, plotBottom - plotY)
        );

        chromeLayer = new BufferedImage(safeWidth, safeHeight, BufferedImage.TYPE_INT_ARGB);
        orbitLayer = new BufferedImage(Math.max(1, plotBounds.width), Math.max(1, plotBounds.height), BufferedImage.TYPE_INT_ARGB);
        renderChromeLayer();
    }

    private void renderChromeLayer() {
        if (chromeLayer == null) {
            return;
        }

        Graphics2D graphics = chromeLayer.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(BACKGROUND);
            graphics.fillRect(0, 0, chromeLayer.getWidth(), chromeLayer.getHeight());
            drawPanel(graphics, headerBounds);
            drawPanel(graphics, plotBounds);
            drawPanel(graphics, footerBounds);

            graphics.setColor(TITLE_COLOR);
            graphics.setFont(TITLE_FONT);
            graphics.drawString(NAME, headerBounds.x + 18, headerBounds.y + 32);
            graphics.setColor(TEXT_MUTED);
            graphics.setFont(SUBTITLE_FONT);
            graphics.drawString("Standard map: p' = p + K sin(x), x' = x + p'  — QRNG seeds initial phase points", headerBounds.x + 18, headerBounds.y + 55);
        } finally {
            graphics.dispose();
        }
    }

    private void render(BufferedImage canvas) {
        if (canvas == null) {
            return;
        }

        Graphics2D graphics = canvas.createGraphics();
        try {
            graphics.drawImage(chromeLayer, 0, 0, null);
            graphics.drawImage(orbitLayer, plotBounds.x, plotBounds.y, null);
            drawGridAndLabels(graphics);
            drawMetrics(graphics);
        } finally {
            graphics.dispose();
        }
    }

    private void drawGridAndLabels(Graphics2D graphics) {
        graphics.setColor(new Color(90, 125, 170, 52));
        for (int i = 1; i < 4; i++) {
            int x = plotBounds.x + i * plotBounds.width / 4;
            int y = plotBounds.y + i * plotBounds.height / 4;
            graphics.drawLine(x, plotBounds.y, x, plotBounds.y + plotBounds.height);
            graphics.drawLine(plotBounds.x, y, plotBounds.x + plotBounds.width, y);
        }

        graphics.setFont(LABEL_FONT);
        graphics.setColor(TEXT_PRIMARY);
        graphics.drawString("phase space torus: x horizontally, p vertically", plotBounds.x + 14, plotBounds.y + 21);
    }

    private void drawMetrics(Graphics2D graphics) {
        graphics.setFont(VALUE_FONT);
        graphics.setColor(TEXT_PRIMARY);
        graphics.drawString(
                String.format("K=%.2f  orbits=%d  phase-points=%d  numbers=%d  rng=%s", kValue, orbitCount, pointCount, randomNumbersUsed, lastObservedMode),
                footerBounds.x + 14,
                footerBounds.y + 20
        );

        graphics.setColor(TEXT_MUTED);
        graphics.drawString(
                String.format(
                        "diag x=%+.2f%%  p=%+.2f%%  quad=±%.2fpp  corrXP=%+.4f",
                        calculateXSeedBalance(),
                        calculatePSeedBalance(),
                        calculateQuadrantBalance(),
                        calculateSeedCorrelationXp()
                ),
                footerBounds.x + 14,
                footerBounds.y + 38
        );

        graphics.setColor(TEXT_ACCENT);
        graphics.drawString(
                String.format(
                        "cells=%d/%d  cover=%.2f%%  entropy=%.2f%%    low K: islands   high K: chaotic sea",
                        occupiedCellCount,
                        OCCUPANCY_CELL_COUNT,
                        calculateCoveragePercent(),
                        calculateNormalizedEntropyPercent()
                ),
                footerBounds.x + 14,
                footerBounds.y + 56
        );
    }

    private void updateSeedDiagnostics(double normalizedX, double normalizedP) {
        if (normalizedX >= 0.5) {
            xSeedHighCount++;
        }
        if (normalizedP >= 0.5) {
            pSeedHighCount++;
        }

        int quadrantIndex = (normalizedX >= 0.5 ? 1 : 0) + (normalizedP >= 0.5 ? 2 : 0);
        quadrantCounts[quadrantIndex]++;

        seedSumX += normalizedX;
        seedSumP += normalizedP;
        seedSumXSquared += normalizedX * normalizedX;
        seedSumPSquared += normalizedP * normalizedP;
        seedSumXP += normalizedX * normalizedP;
    }

    private void updateOccupancyDiagnostics(double x, double p) {
        int cellX = Math.min(OCCUPANCY_GRID_SIZE - 1, (int) Math.floor(x / TWO_PI * OCCUPANCY_GRID_SIZE));
        int cellP = Math.min(OCCUPANCY_GRID_SIZE - 1, (int) Math.floor(p / TWO_PI * OCCUPANCY_GRID_SIZE));
        int cellIndex = cellP * OCCUPANCY_GRID_SIZE + cellX;

        if (occupancyCells[cellIndex] == 0) {
            occupiedCellCount++;
        }
        occupancyCells[cellIndex]++;
        occupancyHitCount++;
    }

    private double calculateXSeedBalance() {
        return calculateHalfRangeBalance(xSeedHighCount);
    }

    private double calculatePSeedBalance() {
        return calculateHalfRangeBalance(pSeedHighCount);
    }

    private double calculateHalfRangeBalance(int highCount) {
        if (orbitCount == 0) {
            return 0.0;
        }
        int lowCount = orbitCount - highCount;
        return (highCount - lowCount) * BALANCE_PERCENT_SCALE / orbitCount;
    }

    private double calculateQuadrantBalance() {
        if (orbitCount == 0) {
            return 0.0;
        }

        double maxDeviation = 0.0;
        for (int quadrantCount : quadrantCounts) {
            double quadrantPercent = quadrantCount * BALANCE_PERCENT_SCALE / orbitCount;
            maxDeviation = Math.max(maxDeviation, Math.abs(quadrantPercent - IDEAL_QUADRANT_PERCENT));
        }
        return maxDeviation;
    }

    private double calculateSeedCorrelationXp() {
        if (orbitCount < 2) {
            return 0.0;
        }

        double covariance = orbitCount * seedSumXP - seedSumX * seedSumP;
        double varianceX = orbitCount * seedSumXSquared - seedSumX * seedSumX;
        double varianceP = orbitCount * seedSumPSquared - seedSumP * seedSumP;
        double denominator = Math.sqrt(varianceX * varianceP);

        if (denominator == 0.0) {
            return 0.0;
        }
        return covariance / denominator;
    }

    private double calculateCoveragePercent() {
        return occupiedCellCount * BALANCE_PERCENT_SCALE / OCCUPANCY_CELL_COUNT;
    }

    private double calculateNormalizedEntropyPercent() {
        if (occupancyHitCount == 0L) {
            return 0.0;
        }

        double entropy = 0.0;
        for (int count : occupancyCells) {
            if (count == 0) {
                continue;
            }
            double probability = count / (double) occupancyHitCount;
            entropy -= probability * Math.log(probability);
        }

        return entropy / Math.log(OCCUPANCY_CELL_COUNT) * BALANCE_PERCENT_SCALE;
    }

    private void drawPanel(Graphics2D graphics, Rectangle bounds) {
        graphics.setColor(PANEL_BACKGROUND);
        graphics.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, PANEL_RADIUS, PANEL_RADIUS);
        graphics.setColor(PANEL_BORDER);
        graphics.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, PANEL_RADIUS, PANEL_RADIUS);
    }

    private double normalize(int value) {
        return Math.clamp(value / RANDOM_MAX, 0.0, 1.0);
    }

    private double wrapToTwoPi(double value) {
        double wrapped = value % TWO_PI;
        return wrapped < 0.0 ? wrapped + TWO_PI : wrapped;
    }
}
