package org.ThreeDotsSierpinski.mode.montecarlo;

import org.ThreeDotsSierpinski.app.DotController;
import org.ThreeDotsSierpinski.mode.RngStepBudget;
import org.ThreeDotsSierpinski.mode.VisualizationMode;
import org.ThreeDotsSierpinski.rng.RNProvider;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.OptionalInt;

/**
 * Buddhabrot fractal via Monte Carlo orbit accumulation.
 *
 * <p>Random complex parameters c are sampled in the Mandelbrot window.
 * For points that escape, the whole orbit z_{n+1} = z_n^2 + c is projected
 * back into the histogram buffer. The density map gradually reveals the
 * characteristic "ghost" shape of the Buddhabrot.</p>
 */
public class BuddhabrotMode implements VisualizationMode {

    private record ModeMetadata(String id, String name, String description, String icon) { }

    private static final ModeMetadata METADATA = new ModeMetadata(
            "buddhabrot",
            "Buddhabrot",
            "Monte Carlo orbits reveal a Buddhabrot density cloud.\n"
                    + "Escaping Mandelbrot trajectories accumulate into a white-blue-gold nebula.",
            "☸"
    );

    private static final double RANDOM_MAX = 65_535.0;
    private static final int RANDOM_MIDPOINT = 32_767;
    private static final double MIN_REAL = -2.10;
    private static final double MAX_REAL = 1.10;
    private static final double MIN_IMAG = -1.50;
    private static final double MAX_IMAG = 1.50;
    private static final double PLANE_WIDTH = MAX_REAL - MIN_REAL;
    private static final double PLANE_HEIGHT = MAX_IMAG - MIN_IMAG;
    private static final double ESCAPE_RADIUS_SQUARED = 4.0;

    private static final int DEFAULT_MAX_ITERATIONS = 240;
    private static final int MIN_ORBIT_LENGTH_TO_DRAW = 12;
    private static final int SAMPLES_PER_STEP = 140;
    private static final int QUANTUM_SAMPLES_PER_STEP = 16;

    private static final int OUTER_PADDING = 18;
    private static final int HEADER_HEIGHT = 72;
    private static final int FOOTER_HEIGHT = 72;
    private static final int PANEL_RADIUS = 20;
    private static final int CONTROL_HEIGHT = 28;
    private static final int RESET_BUTTON_WIDTH = 86;
    private static final int ITERATIONS_COMBO_WIDTH = 96;

    private static final String RESET_TEXT = "Reset";
    private static final String RESET_TOOLTIP = "Restart Buddhabrot accumulation";
    private static final String ITERATIONS_LABEL_TEXT = "Iterations";
    private static final String ITERATIONS_TOOLTIP = "Higher values reveal more structure but consume more CPU";
    private static final String HELP_BUTTON_TEXT = "(?)";
    private static final String HELP_CLOSE_TEXT = "×";

    private static final Integer[] ITERATION_PRESETS = {160, 240, 320};

    private static final int HELP_BUTTON_WIDTH = 38;
    private static final int HELP_BUTTON_HEIGHT = 28;
    private static final int HELP_BUTTON_TITLE_GAP = 12;
    private static final int HELP_OVERLAY_MAX_WIDTH = 920;
    private static final int HELP_OVERLAY_MAX_HEIGHT = 620;
    private static final int HELP_OVERLAY_MARGIN = 54;
    private static final int HELP_OVERLAY_PADDING = 24;
    private static final int HELP_OVERLAY_LINE_HEIGHT = 20;
    private static final int HELP_OVERLAY_SECTION_GAP = 12;
    private static final int HELP_CLOSE_BUTTON_SIZE = 28;

    private static final Color BACKGROUND = new Color(1, 5, 12);
    private static final Color PANEL_BACKGROUND = new Color(5, 12, 24);
    private static final Color PANEL_BORDER = new Color(60, 88, 122, 180);
    private static final Color TITLE_COLOR = new Color(240, 246, 255);
    private static final Color TEXT_PRIMARY = new Color(219, 231, 248);
    private static final Color TEXT_MUTED = new Color(132, 160, 198);
    private static final Color TEXT_ACCENT = new Color(255, 210, 112);
    private static final Color TEXT_DIM = new Color(97, 118, 146);
    private static final Color HELP_BUTTON_BACKGROUND = new Color(255, 210, 112, 35);
    private static final Color HELP_BUTTON_BORDER = new Color(255, 210, 112, 180);
    private static final Color HELP_OVERLAY_SCRIM = new Color(0, 0, 0, 178);
    private static final Color HELP_OVERLAY_BACKGROUND = new Color(5, 12, 24, 244);
    private static final Color HELP_OVERLAY_BORDER = new Color(90, 135, 190, 220);

    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 28);
    private static final Font SUBTITLE_FONT = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font VALUE_FONT = new Font("SansSerif", Font.BOLD, 13);
    private static final Font HELP_TITLE_FONT = new Font("SansSerif", Font.BOLD, 22);
    private static final Font HELP_SECTION_FONT = new Font("SansSerif", Font.BOLD, 14);
    private static final Font HELP_TEXT_FONT = new Font("SansSerif", Font.PLAIN, 13);
    private static final Font HELP_MONO_FONT = new Font("Monospaced", Font.PLAIN, 12);
    private static final Font SMALL_MONO_FONT = new Font("Monospaced", Font.PLAIN, 12);

    private int width;
    private int height;

    private int pointCount;
    private int randomNumbersUsed;
    private int sampleCount;
    private int acceptedOrbitCount;
    private int skippedInteriorCount;
    private int maxIterations = DEFAULT_MAX_ITERATIONS;
    private long histogramHits;
    private int maxHistogramCount;

    private long xLowerHalfCount;
    private long xUpperHalfCount;
    private long yLowerHalfCount;
    private long yUpperHalfCount;
    private final long[] quadrantCounts = new long[4];
    private boolean hasPreviousRandomValue;
    private double previousRandomValue;
    private long serialPairCount;
    private double serialSumPrevious;
    private double serialSumCurrent;
    private double serialSumPreviousSquared;
    private double serialSumCurrentSquared;
    private double serialSumProduct;

    private Rectangle plotBounds = new Rectangle();
    private Rectangle headerBounds = new Rectangle();
    private Rectangle footerBounds = new Rectangle();
    private Rectangle helpButtonBounds = new Rectangle();
    private Rectangle helpOverlayBounds = new Rectangle();
    private Rectangle helpCloseButtonBounds = new Rectangle();

    private int[] histogram;
    private int[] plotPixels;
    private double[] orbitReal;
    private double[] orbitImag;
    private BufferedImage chromeLayer;
    private DotController controller;
    private RNProvider.Mode lastObservedMode = RNProvider.Mode.PSEUDO;
    private boolean helpOverlayVisible = false;

    @Override
    public String getId() { return METADATA.id(); }

    @Override
    public String getName() { return METADATA.name(); }

    @Override
    public String getDescription() { return METADATA.description(); }

    @Override
    public String getIcon() { return METADATA.icon(); }

    @Override
    public boolean usesDarkBackground() { return true; }

    @Override
    public boolean usesRecolorAnimation() { return false; }

    @Override
    public boolean usesInfoTextOverlay() { return false; }

    @Override
    public boolean usesPointCounterOverlay() { return false; }

    @Override
    public boolean usesRngModeIndicatorOverlay() { return false; }

    @Override
    public List<JComponent> createModeControls(DotController controller) {
        this.controller = controller;

        return List.of(createResetButton(), new JLabel(ITERATIONS_LABEL_TEXT), createIterationsComboBox());
    }

    private JButton createResetButton() {
        JButton button = new JButton(RESET_TEXT);
        button.setPreferredSize(new Dimension(RESET_BUTTON_WIDTH, CONTROL_HEIGHT));
        button.setToolTipText(RESET_TOOLTIP);
        button.addActionListener(ignored -> restart());
        return button;
    }

    private JComboBox<Integer> createIterationsComboBox() {
        JComboBox<Integer> comboBox = new JComboBox<>();
        for (Integer preset : ITERATION_PRESETS) {
            comboBox.addItem(preset);
        }
        comboBox.setSelectedItem(maxIterations);
        comboBox.setPreferredSize(new Dimension(ITERATIONS_COMBO_WIDTH, CONTROL_HEIGHT));
        comboBox.setToolTipText(ITERATIONS_TOOLTIP);
        comboBox.addActionListener(ignored -> applyIterationPreset(comboBox.getSelectedItem()));
        return comboBox;
    }

    private void applyIterationPreset(Object selectedItem) {
        if (selectedItem instanceof Integer selectedIterations && selectedIterations != maxIterations) {
            maxIterations = selectedIterations;
            restart();
        }
    }

    private void restart() {
        resetState();
        layoutDashboard();
        refreshController();
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
        lastObservedMode = provider.getMode();

        int processedSamples = 0;

        int samplesThisStep = RngStepBudget.forProvider(provider, SAMPLES_PER_STEP, QUANTUM_SAMPLES_PER_STEP);

        for (int i = 0; i < samplesThisStep; i++) {
            OptionalInt rawReal = provider.getNextRandomNumber();
            if (rawReal.isEmpty()) {
                break;
            }
            randomNumbersUsed++;
            recordSerialRandomValue(rawReal.getAsInt());

            OptionalInt rawImag = provider.getNextRandomNumber();
            if (rawImag.isEmpty()) {
                break;
            }
            randomNumbersUsed++;
            recordSerialRandomValue(rawImag.getAsInt());

            processedSamples++;
            sampleCount++;
            recordSpatialSampleDiagnostics(rawReal.getAsInt(), rawImag.getAsInt());

            double cReal = MIN_REAL + normalize(rawReal.getAsInt()) * PLANE_WIDTH;
            double cImag = MIN_IMAG + normalize(rawImag.getAsInt()) * PLANE_HEIGHT;

            if (isInsideMainCardioidOrPeriodTwoBulb(cReal, cImag)) {
                skippedInteriorCount++;
                continue;
            }

            int orbitLength = traceOrbit(cReal, cImag);
            if (orbitLength >= MIN_ORBIT_LENGTH_TO_DRAW) {
                acceptedOrbitCount++;
                accumulateOrbit(orbitLength);
            }
        }

        if (processedSamples > 0) {
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

    @Override
    public void handleMouseClicked(Point point, Component parent) {
        if (point == null) {
            return;
        }

        if (helpButtonBounds.contains(point)) {
            helpOverlayVisible = !helpOverlayVisible;
            refreshView(parent);
            return;
        }

        if (helpOverlayVisible && closeHelpOverlayRequested(point)) {
            helpOverlayVisible = false;
            refreshView(parent);
        }
    }

    private boolean closeHelpOverlayRequested(Point point) {
        return helpCloseButtonBounds.contains(point) || !helpOverlayBounds.contains(point);
    }

    private void recordSpatialSampleDiagnostics(int rawReal, int rawImag) {
        boolean upperX = rawReal > RANDOM_MIDPOINT;
        boolean upperY = rawImag > RANDOM_MIDPOINT;

        if (upperX) {
            xUpperHalfCount++;
        } else {
            xLowerHalfCount++;
        }

        if (upperY) {
            yUpperHalfCount++;
        } else {
            yLowerHalfCount++;
        }

        int quadrantIndex = quadrantIndex(upperX, upperY);
        quadrantCounts[quadrantIndex]++;
    }

    private int quadrantIndex(boolean upperX, boolean upperY) {
        if (!upperX && !upperY) {
            return 0;
        }
        if (upperX && !upperY) {
            return 1;
        }
        return upperX ? 2 : 3;
    }

    private void recordSerialRandomValue(int rawValue) {
        double normalizedValue = normalize(rawValue);
        if (hasPreviousRandomValue) {
            serialPairCount++;
            serialSumPrevious += previousRandomValue;
            serialSumCurrent += normalizedValue;
            serialSumPreviousSquared += previousRandomValue * previousRandomValue;
            serialSumCurrentSquared += normalizedValue * normalizedValue;
            serialSumProduct += previousRandomValue * normalizedValue;
        }

        previousRandomValue = normalizedValue;
        hasPreviousRandomValue = true;
    }

    private void resetDiagnostics() {
        xLowerHalfCount = 0L;
        xUpperHalfCount = 0L;
        yLowerHalfCount = 0L;
        yUpperHalfCount = 0L;
        for (int i = 0; i < quadrantCounts.length; i++) {
            quadrantCounts[i] = 0L;
        }
        hasPreviousRandomValue = false;
        previousRandomValue = 0.0;
        serialPairCount = 0L;
        serialSumPrevious = 0.0;
        serialSumCurrent = 0.0;
        serialSumPreviousSquared = 0.0;
        serialSumCurrentSquared = 0.0;
        serialSumProduct = 0.0;
    }

    private void ensureInitialized(BufferedImage canvas) {
        if (canvas == null) {
            return;
        }

        if (histogram == null || canvas.getWidth() != width || canvas.getHeight() != height) {
            initialize(canvas, canvas.getWidth(), canvas.getHeight());
        }
    }

    private void resetState() {
        pointCount = 0;
        randomNumbersUsed = 0;
        sampleCount = 0;
        acceptedOrbitCount = 0;
        skippedInteriorCount = 0;
        histogramHits = 0L;
        maxHistogramCount = 0;
        resetDiagnostics();
        orbitReal = new double[maxIterations];
        orbitImag = new double[maxIterations];
        if (plotBounds.width > 0 && plotBounds.height > 0) {
            histogram = new int[plotBounds.width * plotBounds.height];
            plotPixels = new int[plotBounds.width * plotBounds.height];
        } else {
            histogram = null;
            plotPixels = null;
        }
    }

    private void layoutDashboard() {
        int safeWidth = Math.max(1, width);
        int safeHeight = Math.max(1, height);

        headerBounds = new Rectangle(
                OUTER_PADDING,
                OUTER_PADDING,
                Math.max(1, safeWidth - OUTER_PADDING * 2),
                HEADER_HEIGHT
        );

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

        layoutHelpOverlayBounds(safeWidth, safeHeight);

        histogram = new int[Math.max(1, plotBounds.width * plotBounds.height)];
        plotPixels = new int[Math.max(1, plotBounds.width * plotBounds.height)];
        chromeLayer = new BufferedImage(safeWidth, safeHeight, BufferedImage.TYPE_INT_ARGB);
        renderChromeLayer();
    }

    private void layoutHelpOverlayBounds(int safeWidth, int safeHeight) {
        int overlayWidth = Math.min(HELP_OVERLAY_MAX_WIDTH, Math.max(1, safeWidth - HELP_OVERLAY_MARGIN * 2));
        int overlayHeight = Math.min(HELP_OVERLAY_MAX_HEIGHT, Math.max(1, safeHeight - HELP_OVERLAY_MARGIN * 2));
        int overlayX = Math.max(OUTER_PADDING, (safeWidth - overlayWidth) / 2);
        int overlayY = Math.max(OUTER_PADDING, (safeHeight - overlayHeight) / 2);

        helpOverlayBounds = new Rectangle(overlayX, overlayY, overlayWidth, overlayHeight);
        helpCloseButtonBounds = new Rectangle(
                overlayX + overlayWidth - HELP_OVERLAY_PADDING - HELP_CLOSE_BUTTON_SIZE,
                overlayY + HELP_OVERLAY_PADDING,
                HELP_CLOSE_BUTTON_SIZE,
                HELP_CLOSE_BUTTON_SIZE
        );
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
            int titleX = headerBounds.x + 18;
            int titleBaselineY = headerBounds.y + 30;
            graphics.drawString(METADATA.name(), titleX, titleBaselineY);
            layoutHelpButton(graphics, titleX, titleBaselineY);
            drawHelpButton(graphics);

            graphics.setColor(TEXT_MUTED);
            graphics.setFont(SUBTITLE_FONT);
            graphics.drawString(
                    "Escaping Mandelbrot orbits accumulate into a density nebula — Monte Carlo orbit histogram",
                    headerBounds.x + 18,
                    headerBounds.y + 52
            );
        } finally {
            graphics.dispose();
        }
    }

    private void drawPanel(Graphics2D graphics, Rectangle bounds) {
        graphics.setColor(PANEL_BACKGROUND);
        graphics.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, PANEL_RADIUS, PANEL_RADIUS);
        graphics.setColor(PANEL_BORDER);
        graphics.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, PANEL_RADIUS, PANEL_RADIUS);
    }

    private void layoutHelpButton(Graphics2D graphics, int titleX, int titleBaselineY) {
        int titleWidth = graphics.getFontMetrics(TITLE_FONT).stringWidth(METADATA.name());
        int buttonX = titleX + titleWidth + HELP_BUTTON_TITLE_GAP;
        int buttonY = titleBaselineY - HELP_BUTTON_HEIGHT + 4;
        helpButtonBounds = new Rectangle(buttonX, buttonY, HELP_BUTTON_WIDTH, HELP_BUTTON_HEIGHT);
    }

    private void drawHelpButton(Graphics2D graphics) {
        graphics.setColor(HELP_BUTTON_BACKGROUND);
        graphics.fillRoundRect(
                helpButtonBounds.x,
                helpButtonBounds.y,
                helpButtonBounds.width,
                helpButtonBounds.height,
                HELP_BUTTON_HEIGHT,
                HELP_BUTTON_HEIGHT
        );
        graphics.setColor(HELP_BUTTON_BORDER);
        graphics.drawRoundRect(
                helpButtonBounds.x,
                helpButtonBounds.y,
                helpButtonBounds.width,
                helpButtonBounds.height,
                HELP_BUTTON_HEIGHT,
                HELP_BUTTON_HEIGHT
        );

        graphics.setColor(TEXT_ACCENT);
        graphics.setFont(VALUE_FONT);
        FontMetrics metrics = graphics.getFontMetrics();
        int textX = helpButtonBounds.x + (helpButtonBounds.width - metrics.stringWidth(HELP_BUTTON_TEXT)) / 2;
        int textY = helpButtonBounds.y + (helpButtonBounds.height - metrics.getHeight()) / 2 + metrics.getAscent();
        graphics.drawString(HELP_BUTTON_TEXT, textX, textY);
    }

    private int traceOrbit(double cReal, double cImag) {
        double zReal = 0.0;
        double zImag = 0.0;

        for (int iteration = 0; iteration < maxIterations; iteration++) {
            double zRealSquared = zReal * zReal;
            double zImagSquared = zImag * zImag;
            if (zRealSquared + zImagSquared > ESCAPE_RADIUS_SQUARED) {
                return iteration;
            }

            double nextImag = 2.0 * zReal * zImag + cImag;
            double nextReal = zRealSquared - zImagSquared + cReal;

            zReal = nextReal;
            zImag = nextImag;
            orbitReal[iteration] = zReal;
            orbitImag[iteration] = zImag;
        }

        return -1;
    }

    private void accumulateOrbit(int orbitLength) {
        int plotWidth = plotBounds.width;
        int plotHeight = plotBounds.height;
        if (plotWidth <= 0 || plotHeight <= 0 || histogram == null) {
            return;
        }

        for (int index = 0; index < orbitLength; index++) {
            int plotX = (int) ((orbitReal[index] - MIN_REAL) / PLANE_WIDTH * plotWidth);
            int plotY = (int) ((MAX_IMAG - orbitImag[index]) / PLANE_HEIGHT * plotHeight);

            if (plotX < 0 || plotX >= plotWidth || plotY < 0 || plotY >= plotHeight) {
                continue;
            }

            int histogramIndex = plotY * plotWidth + plotX;
            int nextCount = histogram[histogramIndex] + 1;
            histogram[histogramIndex] = nextCount;
            histogramHits++;
            pointCount++;
            if (nextCount > maxHistogramCount) {
                maxHistogramCount = nextCount;
            }
        }
    }

    private void render(BufferedImage canvas) {
        if (canvas == null) {
            return;
        }

        Graphics2D graphics = canvas.createGraphics();
        try {
            graphics.drawImage(chromeLayer, 0, 0, null);
            paintDensityMap();
            graphics.drawImage(createPlotImage(), plotBounds.x, plotBounds.y, null);
            drawOverlayText(graphics);
            if (helpOverlayVisible) {
                drawHelpOverlay(graphics);
            }
        } finally {
            graphics.dispose();
        }
    }

    private BufferedImage createPlotImage() {
        BufferedImage image = new BufferedImage(
                Math.max(1, plotBounds.width),
                Math.max(1, plotBounds.height),
                BufferedImage.TYPE_INT_ARGB
        );
        image.setRGB(0, 0, plotBounds.width, plotBounds.height, plotPixels, 0, plotBounds.width);
        return image;
    }

    private void paintDensityMap() {
        if (plotPixels == null || histogram == null) {
            return;
        }

        int totalPixels = Math.min(plotPixels.length, histogram.length);
        if (maxHistogramCount <= 0) {
            for (int i = 0; i < totalPixels; i++) {
                plotPixels[i] = BACKGROUND.getRGB();
            }
            return;
        }

        double logMax = Math.log1p(maxHistogramCount);
        for (int i = 0; i < totalPixels; i++) {
            int count = histogram[i];
            if (count <= 0) {
                plotPixels[i] = BACKGROUND.getRGB();
                continue;
            }

            double normalized = Math.log1p(count) / logMax;
            normalized = Math.pow(normalized, 0.88);
            plotPixels[i] = densityColor(normalized).getRGB();
        }
    }

    private Color densityColor(double normalized) {
        if (normalized <= 0.0) {
            return BACKGROUND;
        }
        if (normalized < 0.35) {
            return interpolate(
                    new Color(6, 12, 26),
                    new Color(45, 92, 210),
                    normalized / 0.35
            );
        }
        if (normalized < 0.72) {
            return interpolate(
                    new Color(45, 92, 210),
                    new Color(184, 238, 255),
                    (normalized - 0.35) / 0.37
            );
        }
        if (normalized < 0.92) {
            return interpolate(
                    new Color(184, 238, 255),
                    new Color(255, 211, 105),
                    (normalized - 0.72) / 0.20
            );
        }
        return interpolate(
                new Color(255, 211, 105),
                new Color(255, 253, 248),
                (normalized - 0.92) / 0.08
        );
    }

    private Color interpolate(Color first, Color second, double t) {
        double clamped = Math.max(0.0, Math.min(1.0, t));
        return new Color(
                blendChannel(first.getRed(), second.getRed(), clamped),
                blendChannel(first.getGreen(), second.getGreen(), clamped),
                blendChannel(first.getBlue(), second.getBlue(), clamped)
        );
    }

    private int blendChannel(int first, int second, double t) {
        return (int) Math.round(first + (second - first) * t);
    }

    private void drawOverlayText(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        graphics.setColor(TEXT_PRIMARY);
        graphics.setFont(VALUE_FONT);
        graphics.drawString("Buddhabrot density map", plotBounds.x + 14, plotBounds.y + 20);

        graphics.setColor(TEXT_DIM);
        graphics.setFont(LABEL_FONT);
        graphics.drawString("complex plane: Re[-2.10, 1.10]  Im[-1.50, 1.50]", plotBounds.x + 14, plotBounds.y + 38);

        graphics.setFont(SMALL_MONO_FONT);
        graphics.setColor(TEXT_PRIMARY);
        String leftMetrics = String.format(
                "samples=%d  accepted=%d  skipped=%d  hits=%d  peak=%d",
                sampleCount,
                acceptedOrbitCount,
                skippedInteriorCount,
                histogramHits,
                maxHistogramCount
        );
        graphics.drawString(leftMetrics, footerBounds.x + 14, footerBounds.y + 22);

        graphics.setColor(TEXT_MUTED);
        String rightMetrics = String.format(
                "rng=%s  iterations=%d  numbers=%d  palette=white-blue-gold",
                lastObservedMode,
                maxIterations,
                randomNumbersUsed
        );
        graphics.drawString(rightMetrics, footerBounds.x + 14, footerBounds.y + 38);

        graphics.setColor(TEXT_ACCENT);
        graphics.setFont(SMALL_MONO_FONT);
        graphics.drawString(createDiagnosticText(), footerBounds.x + 14, footerBounds.y + 56);

        graphics.setColor(TEXT_ACCENT);
        graphics.setFont(LABEL_FONT);
        String narrative = "white-hot ridges = dense orbit traffic, blue haze = low-density escaped trajectories";
        int narrativeWidth = graphics.getFontMetrics().stringWidth(narrative);
        int narrativeX = footerBounds.x + footerBounds.width - narrativeWidth - 14;
        graphics.drawString(narrative, Math.max(footerBounds.x + 14, narrativeX), footerBounds.y + 22);
    }

    private String createDiagnosticText() {
        return String.format(
                "diag x=%s y=%s quad=%s serial=%s acc=%s sym=%s",
                formatSignedPercent(balancePercent(xUpperHalfCount, xLowerHalfCount)),
                formatSignedPercent(balancePercent(yUpperHalfCount, yLowerHalfCount)),
                formatPercentPoints(maxQuadrantDeviationPercentPoints()),
                formatCorrelation(serialCorrelation()),
                formatPercent(ratioPercent(acceptedOrbitCount, sampleCount)),
                formatPercent(symmetryScorePercent())
        );
    }

    private double balancePercent(long upperCount, long lowerCount) {
        long total = upperCount + lowerCount;
        if (total == 0L) {
            return Double.NaN;
        }
        return (upperCount - lowerCount) * 100.0 / total;
    }

    private double maxQuadrantDeviationPercentPoints() {
        if (sampleCount == 0) {
            return Double.NaN;
        }

        double maxDeviation = 0.0;
        for (long quadrantCount : quadrantCounts) {
            double percentage = quadrantCount * 100.0 / sampleCount;
            maxDeviation = Math.max(maxDeviation, Math.abs(percentage - 25.0));
        }
        return maxDeviation;
    }

    private double serialCorrelation() {
        if (serialPairCount < 2) {
            return Double.NaN;
        }

        double n = serialPairCount;
        double numerator = n * serialSumProduct - serialSumPrevious * serialSumCurrent;
        double previousVariance = n * serialSumPreviousSquared - serialSumPrevious * serialSumPrevious;
        double currentVariance = n * serialSumCurrentSquared - serialSumCurrent * serialSumCurrent;
        double denominator = Math.sqrt(previousVariance * currentVariance);
        if (denominator <= 0.0) {
            return Double.NaN;
        }

        return numerator / denominator;
    }

    private double ratioPercent(long numerator, long denominator) {
        if (denominator == 0L) {
            return Double.NaN;
        }
        return numerator * 100.0 / denominator;
    }

    private double symmetryScorePercent() {
        if (histogram == null || plotBounds.width <= 0 || plotBounds.height <= 1 || histogramHits == 0L) {
            return Double.NaN;
        }

        long differenceSum = 0L;
        long totalSum = 0L;
        int plotWidth = plotBounds.width;
        int halfHeight = plotBounds.height / 2;

        for (int y = 0; y < halfHeight; y++) {
            int mirrorY = plotBounds.height - 1 - y;
            int rowOffset = y * plotWidth;
            int mirrorRowOffset = mirrorY * plotWidth;
            for (int x = 0; x < plotWidth; x++) {
                int upper = histogram[rowOffset + x];
                int lower = histogram[mirrorRowOffset + x];
                differenceSum += Math.abs(upper - lower);
                totalSum += upper + lower;
            }
        }

        if (totalSum == 0L) {
            return Double.NaN;
        }

        return Math.max(0.0, 100.0 * (1.0 - differenceSum / (double) totalSum));
    }

    private String formatSignedPercent(double value) {
        if (Double.isNaN(value)) {
            return "n/a";
        }
        return String.format("%+.2f%%", value);
    }

    private String formatPercent(double value) {
        if (Double.isNaN(value)) {
            return "n/a";
        }
        return String.format("%.2f%%", value);
    }

    private String formatPercentPoints(double value) {
        if (Double.isNaN(value)) {
            return "n/a";
        }
        return String.format("±%.2fpp", value);
    }

    private String formatCorrelation(double value) {
        if (Double.isNaN(value)) {
            return "n/a";
        }
        return String.format("%+.4f", value);
    }

    private void drawHelpOverlay(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        graphics.setColor(HELP_OVERLAY_SCRIM);
        graphics.fillRect(0, 0, width, height);

        graphics.setColor(HELP_OVERLAY_BACKGROUND);
        graphics.fillRoundRect(
                helpOverlayBounds.x,
                helpOverlayBounds.y,
                helpOverlayBounds.width,
                helpOverlayBounds.height,
                PANEL_RADIUS,
                PANEL_RADIUS
        );
        graphics.setColor(HELP_OVERLAY_BORDER);
        graphics.drawRoundRect(
                helpOverlayBounds.x,
                helpOverlayBounds.y,
                helpOverlayBounds.width,
                helpOverlayBounds.height,
                PANEL_RADIUS,
                PANEL_RADIUS
        );

        drawHelpCloseButton(graphics);

        int textX = helpOverlayBounds.x + HELP_OVERLAY_PADDING;
        int textY = helpOverlayBounds.y + HELP_OVERLAY_PADDING + 8;
        int textWidth = helpOverlayBounds.width - HELP_OVERLAY_PADDING * 2 - HELP_CLOSE_BUTTON_SIZE - 10;

        graphics.setColor(TITLE_COLOR);
        graphics.setFont(HELP_TITLE_FONT);
        textY += graphics.getFontMetrics().getAscent();
        graphics.drawString("Что показывает Buddhabrot", textX, textY);
        textY += HELP_OVERLAY_SECTION_GAP + 8;

        textY = drawHelpSection(graphics, "Идея",
                "Обычный Mandelbrot красит саму точку c. Buddhabrot делает иначе: случайная точка c запускает орбиту z(n+1)=z(n)^2+c. Если орбита убегает, в histogram попадает весь её путь. Поэтому картинка — это карта плотности escaping trajectories, а не обычная escape-time карта.",
                textX, textY, textWidth);

        textY = drawHelpSection(graphics, "Как читать map",
                "Чёрный фон означает почти нулевые посещения. Синий туман — слабая плотность. Белые и золотые ridges — области, через которые проходит много орбит. Это похоже на long-exposure photo движения частиц в комплексной динамической системе.",
                textX, textY, textWidth);

        textY = drawHelpSection(graphics, "Связь с RNG",
                "RNG выбирает координаты Re(c) и Im(c). Если random stream хорошо покрывает плоскость, density-map проявляется гладко и симметрично. Сильный bias мог бы дать полосы, дырки, повторяющиеся диагонали или аномальные сгущения. Но это visual sanity check, не строгий statistical proof.",
                textX, textY, textWidth);

        textY = drawHelpSection(graphics, "Diagnostic strip",
                "x/y-balance должны быть близки к 0%. quadrant balance показывает максимальное отклонение квадранта от идеальных 25%. serial correlation должна быть около 0. accepted ratio показывает долю useful escaping orbits. symmetry score должен расти к 100% при хорошем покрытии и долгом накоплении.",
                textX, textY, textWidth);

        drawHelpCodeLine(graphics, "random numbers → c = x + iy → orbit → histogram → diagnostics", textX, textY);
    }

    private void drawHelpCloseButton(Graphics2D graphics) {
        graphics.setColor(new Color(255, 255, 255, 24));
        graphics.fillRoundRect(
                helpCloseButtonBounds.x,
                helpCloseButtonBounds.y,
                helpCloseButtonBounds.width,
                helpCloseButtonBounds.height,
                HELP_CLOSE_BUTTON_SIZE,
                HELP_CLOSE_BUTTON_SIZE
        );
        graphics.setColor(TEXT_ACCENT);
        graphics.setFont(HELP_TITLE_FONT);
        FontMetrics metrics = graphics.getFontMetrics();
        int textX = helpCloseButtonBounds.x + (helpCloseButtonBounds.width - metrics.stringWidth(HELP_CLOSE_TEXT)) / 2;
        int textY = helpCloseButtonBounds.y + (helpCloseButtonBounds.height - metrics.getHeight()) / 2 + metrics.getAscent();
        graphics.drawString(HELP_CLOSE_TEXT, textX, textY);
    }

    private int drawHelpSection(Graphics2D graphics, String title, String body, int x, int y, int maxWidth) {
        graphics.setColor(TEXT_ACCENT);
        graphics.setFont(HELP_SECTION_FONT);
        y += graphics.getFontMetrics().getAscent();
        graphics.drawString(title, x, y);
        y += 8;

        graphics.setColor(TEXT_PRIMARY);
        graphics.setFont(HELP_TEXT_FONT);
        y = drawWrappedText(graphics, body, x, y, maxWidth, HELP_OVERLAY_LINE_HEIGHT);
        return y + HELP_OVERLAY_SECTION_GAP;
    }

    private int drawWrappedText(Graphics2D graphics, String text, int x, int y, int maxWidth, int lineHeight) {
        FontMetrics metrics = graphics.getFontMetrics();
        StringBuilder line = new StringBuilder();

        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (metrics.stringWidth(candidate) > maxWidth && !line.isEmpty()) {
                y += lineHeight;
                graphics.drawString(line.toString(), x, y);
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }

        if (!line.isEmpty()) {
            y += lineHeight;
            graphics.drawString(line.toString(), x, y);
        }

        return y;
    }

    private void drawHelpCodeLine(Graphics2D graphics, String text, int x, int y) {
        graphics.setColor(TEXT_MUTED);
        graphics.setFont(HELP_MONO_FONT);
        graphics.drawString(text, x, y + HELP_OVERLAY_LINE_HEIGHT);
    }

    private void refreshView(Component parent) {
        if (controller != null) {
            controller.refreshVisualization();
            return;
        }
        if (parent != null) {
            parent.repaint();
        }
    }

    private void refreshController() {
        refreshView(null);
    }

    private double normalize(int value) {
        return Math.max(0.0, Math.min(1.0, value / RANDOM_MAX));
    }

    private boolean isInsideMainCardioidOrPeriodTwoBulb(double real, double imaginary) {
        double xMinusQuarter = real - 0.25;
        double ySquared = imaginary * imaginary;
        double q = xMinusQuarter * xMinusQuarter + ySquared;
        if (q * (q + xMinusQuarter) <= 0.25 * ySquared) {
            return true;
        }

        double xPlusOne = real + 1.0;
        return xPlusOne * xPlusOne + ySquared <= 0.0625;
    }
}
