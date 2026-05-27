package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * Режим визуализации: Monte Carlo estimation of π.
 * <p>
 * Случайные пары чисел интерпретируются как точки в unit square.
 * Точки внутри вписанной окружности подсвечиваются холодным неоном,
 * вне окружности — тёплым оранжевым. Справа строятся две живые панели:
 * сходимость оценки π и спад абсолютной ошибки.
 * <p>
 * Это не просто scatter plot, а небольшой dashboard, где одновременно видно:
 * - sample space,
 * - текущую оценку π,
 * - inside/outside counts,
 * - историю сходимости,
 * - историю ошибки.
 */
public class MonteCarloPiMode implements VisualizationMode {

    private static final String ID = "monte-carlo-pi";
    private static final String NAME = "Monte Carlo π Dashboard";
    private static final String DESCRIPTION =
            "Случайные точки постепенно проявляют круг и оценку числа π.\n"
                    + "Слева sample space, справа — convergence/error charts.";
    private static final String ICON = "π";

    private static final int RANDOM_RANGE = 65_536;
    private static final double RANDOM_MAX = 65_535.0;
    private static final int SAMPLES_PER_STEP = 180;
    private static final int VALUES_PER_SAMPLE = 2;

    private static final int PANEL_RADIUS = 22;
    private static final int OUTER_PADDING = 18;
    private static final int HEADER_HEIGHT = 96;
    private static final int SECTION_GAP = 14;
    private static final int PANEL_INSET = 16;
    private static final int SAMPLE_AXIS_EXTRA_BOTTOM = 22;
    private static final int SAMPLE_AXIS_EXTRA_LEFT = 18;
    private static final int RIGHT_PANEL_MIN_WIDTH = 280;
    private static final int CARD_HEIGHT = 86;
    private static final int CARD_GAP = 10;
    private static final int STATUS_BOX_WIDTH = 252;
    private static final int STATUS_BOX_HEIGHT = 68;

    private static final int AXIS_TICK_COUNT = 4;
    private static final int CHART_HORIZONTAL_GRID_LINES = 5;
    private static final int CHART_VERTICAL_GRID_LINES = 6;
    private static final int HISTORY_CAPACITY = 1_800;

    private static final String RESET_TEXT = "Reset";
    private static final String RESET_TOOLTIP = "Start the Monte Carlo π dashboard from zero";
    private static final int RESET_BUTTON_WIDTH = 82;
    private static final int CONTROL_HEIGHT = 28;

    private static final Color BACKGROUND = new Color(3, 9, 18);
    private static final Color PANEL_BACKGROUND = new Color(10, 18, 32);
    private static final Color PANEL_BACKGROUND_SOFT = new Color(8, 16, 27, 220);
    private static final Color PANEL_BORDER = new Color(52, 84, 122, 180);
    private static final Color PANEL_GLOW = new Color(40, 140, 200, 28);

    private static final Color TITLE_COLOR = new Color(242, 244, 250);
    private static final Color SUBTITLE_COLOR = new Color(150, 170, 196);
    private static final Color TEXT_PRIMARY = new Color(226, 233, 244);
    private static final Color TEXT_MUTED = new Color(126, 148, 176);
    private static final Color TEXT_DIM = new Color(94, 112, 138);

    private static final Color INSIDE_COLOR = new Color(16, 255, 198, 190);
    private static final Color OUTSIDE_COLOR = new Color(255, 116, 54, 175);
    private static final Color CIRCLE_BORDER = new Color(75, 226, 255, 215);
    private static final Color GRID_COLOR = new Color(100, 135, 170, 55);
    private static final Color SAMPLE_BORDER = new Color(86, 108, 138, 155);

    private static final Color CONVERGENCE_LINE = new Color(65, 235, 255);
    private static final Color TRUE_VALUE_LINE = new Color(110, 225, 255, 120);
    private static final Color ERROR_LINE = new Color(255, 210, 72);
    private static final Color CARD_LABEL_COLOR = new Color(160, 179, 205);
    private static final Color CARD_VALUE_CYAN = new Color(70, 235, 255);
    private static final Color CARD_VALUE_GREEN = new Color(35, 255, 185);
    private static final Color CARD_VALUE_ORANGE = new Color(255, 145, 84);
    private static final Color CARD_VALUE_YELLOW = new Color(255, 222, 110);

    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 30);
    private static final Font SUBTITLE_FONT = new Font("SansSerif", Font.PLAIN, 15);
    private static final Font PANEL_TITLE_FONT = new Font("SansSerif", Font.BOLD, 19);
    private static final Font PANEL_META_FONT = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font CHART_VALUE_FONT = new Font("SansSerif", Font.PLAIN, 11);
    private static final Font CARD_LABEL_FONT = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font CARD_VALUE_FONT = new Font("SansSerif", Font.BOLD, 18);
    private static final Font CARD_SMALL_FONT = new Font("SansSerif", Font.PLAIN, 11);
    private static final Font STATUS_TITLE_FONT = new Font("SansSerif", Font.BOLD, 13);
    private static final Font STATUS_VALUE_FONT = new Font("SansSerif", Font.BOLD, 18);
    private static final Font STATUS_SMALL_FONT = new Font("SansSerif", Font.PLAIN, 11);

    private int width;
    private int height;

    private int pointCount;
    private int randomNumbersUsed;
    private int insideCount;
    private int outsideCount;

    private BufferedImage sampleLayer;
    private Rectangle samplePanelBounds = new Rectangle();
    private Rectangle samplePlotBounds = new Rectangle();
    private Rectangle convergenceChartBounds = new Rectangle();
    private Rectangle errorChartBounds = new Rectangle();
    private final List<Rectangle> metricCardBounds = new ArrayList<>();

    private final List<Double> estimateHistory = new ArrayList<>();
    private final List<Double> errorHistory = new ArrayList<>();
    private final List<Integer> sampleHistory = new ArrayList<>();

    private DotController controller;
    private RNProvider.Mode displayedProviderMode = RNProvider.Mode.PSEUDO;
    private String displayedFallbackReason;

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
        syncProviderStateFromController();

        JButton resetButton = new JButton(RESET_TEXT);
        resetButton.setPreferredSize(new Dimension(RESET_BUTTON_WIDTH, CONTROL_HEIGHT));
        resetButton.setToolTipText(RESET_TOOLTIP);
        resetButton.addActionListener(event -> {
            resetState();
            if (controller != null) {
                controller.refreshVisualization();
            }
        });
        return List.of(resetButton);
    }

    @Override
    public void initialize(BufferedImage canvas, int width, int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        syncProviderStateFromController();
        resetState();
        layoutDashboard();
        render(canvas);
    }

    @Override
    public List<Point> step(RNProvider provider, BufferedImage canvas, int dotSize) {
        ensureInitialized(canvas);
        syncProviderState(provider);

        int drawnThisStep = 0;
        int drawSize = Math.max(1, dotSize);

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

                double x = normalize(rawX.getAsInt());
                double y = normalize(rawY.getAsInt());
                boolean insideCircle = isInsideCircle(x, y);

                pointCount++;
                drawnThisStep++;
                if (insideCircle) {
                    insideCount++;
                } else {
                    outsideCount++;
                }

                drawSamplePoint(sampleGraphics, x, y, drawSize, insideCircle);
            }
        } finally {
            sampleGraphics.dispose();
        }

        if (drawnThisStep > 0) {
            recordHistorySnapshot();
        }

        render(canvas);
        return List.of();
    }

    @Override
    public void redraw(BufferedImage canvas, int width, int height, int dotSize) {
        if (canvas == null) {
            return;
        }

        syncProviderStateFromController();

        if (this.width != width || this.height != height || sampleLayer == null) {
            initialize(canvas, width, height);
            return;
        }

        render(canvas);
    }

    private void ensureInitialized(BufferedImage canvas) {
        if (sampleLayer == null || this.width != canvas.getWidth() || this.height != canvas.getHeight()) {
            initialize(canvas, canvas.getWidth(), canvas.getHeight());
        }
    }

    private void syncProviderState(RNProvider provider) {
        if (provider == null) {
            return;
        }
        displayedProviderMode = provider.getMode();
        displayedFallbackReason = provider.getFallbackReason();
    }

    private void syncProviderStateFromController() {
        if (controller == null) {
            return;
        }
        syncProviderState(controller.getRandomNumberProvider());
    }

    private void resetState() {
        this.pointCount = 0;
        this.randomNumbersUsed = 0;
        this.insideCount = 0;
        this.outsideCount = 0;
        this.estimateHistory.clear();
        this.errorHistory.clear();
        this.sampleHistory.clear();
        this.metricCardBounds.clear();

        this.sampleLayer = new BufferedImage(
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
        int cardHeight = Math.min(CARD_HEIGHT, Math.max(56, height / 6));
        int cardsY = height - OUTER_PADDING - cardHeight;
        int mainTop = Math.min(height - 40, HEADER_HEIGHT + OUTER_PADDING);
        int availableMainHeight = Math.max(90, cardsY - mainTop - SECTION_GAP);
        int mainHeight = availableMainHeight;
        int leftWidth = Math.max(120, Math.min(width - OUTER_PADDING * 2, (int) Math.round(width * 0.47)));
        int rightX = OUTER_PADDING + leftWidth + SECTION_GAP;
        int rightWidth = Math.max(80, width - rightX - OUTER_PADDING);

        samplePanelBounds = new Rectangle(OUTER_PADDING, mainTop, leftWidth, mainHeight);

        int plotAvailableWidth = samplePanelBounds.width - PANEL_INSET * 2 - SAMPLE_AXIS_EXTRA_LEFT;
        int plotAvailableHeight = samplePanelBounds.height - PANEL_INSET * 2 - 38 - SAMPLE_AXIS_EXTRA_BOTTOM;
        int plotSize = Math.max(40, Math.min(plotAvailableWidth, plotAvailableHeight));
        int plotX = samplePanelBounds.x + PANEL_INSET + SAMPLE_AXIS_EXTRA_LEFT;
        int plotY = samplePanelBounds.y + 46;
        samplePlotBounds = new Rectangle(plotX, plotY, plotSize, plotSize);

        int chartHeight = (mainHeight - SECTION_GAP) / 2;
        convergenceChartBounds = new Rectangle(rightX, mainTop, rightWidth, chartHeight);
        errorChartBounds = new Rectangle(rightX, mainTop + chartHeight + SECTION_GAP, rightWidth, mainHeight - chartHeight - SECTION_GAP);

        layoutMetricCards();
    }

    private void layoutMetricCards() {
        metricCardBounds.clear();

        int cardCount = 5;
        int availableWidth = width - OUTER_PADDING * 2 - CARD_GAP * (cardCount - 1);
        int cardWidth = Math.max(120, availableWidth / cardCount);
        int totalWidth = cardWidth * cardCount + CARD_GAP * (cardCount - 1);
        int startX = OUTER_PADDING + Math.max(0, (width - OUTER_PADDING * 2 - totalWidth) / 2);
        int cardHeight = Math.min(CARD_HEIGHT, Math.max(56, height / 6));
        int y = height - OUTER_PADDING - cardHeight;

        for (int i = 0; i < cardCount; i++) {
            int x = startX + i * (cardWidth + CARD_GAP);
            metricCardBounds.add(new Rectangle(x, y, cardWidth, cardHeight));
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
            drawSamplePanel(g);
            drawConvergencePanel(g);
            drawErrorPanel(g);
            drawMetricCards(g);
        } finally {
            g.dispose();
        }
    }

    private void drawHeader(Graphics2D g) {
        g.setFont(TITLE_FONT);
        g.setColor(TITLE_COLOR);
        g.drawString("MONTE CARLO ESTIMATION OF π", OUTER_PADDING, 44);

        g.setFont(SUBTITLE_FONT);
        g.setColor(SUBTITLE_COLOR);
        g.drawString("True random numbers reveal a circle, an estimate and a converging law.", OUTER_PADDING, 68);

        int statusX = width - OUTER_PADDING - STATUS_BOX_WIDTH;
        int statusY = 16;
        drawPanelBox(g, statusX, statusY, STATUS_BOX_WIDTH, STATUS_BOX_HEIGHT, PANEL_BACKGROUND_SOFT);

        g.setFont(STATUS_TITLE_FONT);
        g.setColor(TEXT_MUTED);
        g.drawString("RANDOMNESS SOURCE", statusX + 14, statusY + 20);

        boolean quantum = displayedProviderMode == RNProvider.Mode.QUANTUM;
        String sourceLabel = quantum ? "TRUE RANDOM / QRNG" : "PSEUDO / LOCAL PRNG";
        Color sourceColor = quantum ? CARD_VALUE_GREEN : CARD_VALUE_ORANGE;
        String detailLabel = providerDetailLabel(quantum);

        g.setFont(STATUS_VALUE_FONT);
        g.setColor(sourceColor);
        g.drawString(sourceLabel, statusX + 14, statusY + 43);

        g.setFont(STATUS_SMALL_FONT);
        g.setColor(TEXT_DIM);
        g.drawString(detailLabel, statusX + 14, statusY + 59);
    }

    private String providerDetailLabel(boolean quantum) {
        if (quantum) {
            return "π ≈ 4 × inside / total";
        }
        if (displayedFallbackReason == null || displayedFallbackReason.isBlank()) {
            return "Local generator: L128X256MixRandom";
        }
        String normalized = displayedFallbackReason.trim();
        if (normalized.length() > 34) {
            normalized = normalized.substring(0, 31) + "...";
        }
        return normalized;
    }

    private void drawSamplePanel(Graphics2D g) {
        drawPanelBox(g, samplePanelBounds.x, samplePanelBounds.y, samplePanelBounds.width, samplePanelBounds.height, PANEL_BACKGROUND);

        g.setFont(PANEL_TITLE_FONT);
        g.setColor(TEXT_PRIMARY);
        g.drawString("MONTE CARLO SAMPLE SPACE", samplePanelBounds.x + PANEL_INSET, samplePanelBounds.y + 24);

        g.setFont(PANEL_META_FONT);
        g.setColor(CARD_VALUE_GREEN);
        g.fillOval(samplePanelBounds.x + samplePanelBounds.width - 180, samplePanelBounds.y + 14, 8, 8);
        g.drawString("inside circle", samplePanelBounds.x + samplePanelBounds.width - 166, samplePanelBounds.y + 22);
        g.setColor(CARD_VALUE_ORANGE);
        g.fillOval(samplePanelBounds.x + samplePanelBounds.width - 84, samplePanelBounds.y + 14, 8, 8);
        g.drawString("outside", samplePanelBounds.x + samplePanelBounds.width - 70, samplePanelBounds.y + 22);

        drawSampleAxes(g);
        g.drawImage(sampleLayer, 0, 0, null);

        g.setColor(CIRCLE_BORDER);
        g.setStroke(new BasicStroke(2f));
        g.drawOval(samplePlotBounds.x, samplePlotBounds.y, samplePlotBounds.width, samplePlotBounds.height);
        g.setStroke(new BasicStroke(1f));
    }

    private void drawSampleAxes(Graphics2D g) {
        g.setColor(GRID_COLOR);
        for (int i = 0; i <= AXIS_TICK_COUNT; i++) {
            int x = samplePlotBounds.x + Math.round((float) i / AXIS_TICK_COUNT * samplePlotBounds.width);
            int y = samplePlotBounds.y + Math.round((float) i / AXIS_TICK_COUNT * samplePlotBounds.height);
            g.drawLine(x, samplePlotBounds.y, x, samplePlotBounds.y + samplePlotBounds.height);
            g.drawLine(samplePlotBounds.x, y, samplePlotBounds.x + samplePlotBounds.width, y);
        }

        g.setColor(SAMPLE_BORDER);
        g.drawRect(samplePlotBounds.x, samplePlotBounds.y, samplePlotBounds.width, samplePlotBounds.height);

        g.setFont(CHART_VALUE_FONT);
        g.setColor(TEXT_MUTED);
        g.drawString("y", samplePlotBounds.x - 14, samplePlotBounds.y - 4);
        g.drawString("x", samplePlotBounds.x + samplePlotBounds.width + 8, samplePlotBounds.y + samplePlotBounds.height + 16);

        drawAxisValue(g, "0", samplePlotBounds.x - 14, samplePlotBounds.y + samplePlotBounds.height + 4);
        drawAxisValue(g, "0.5", samplePlotBounds.x + samplePlotBounds.width / 2 - 10, samplePlotBounds.y + samplePlotBounds.height + 16);
        drawAxisValue(g, "1", samplePlotBounds.x + samplePlotBounds.width - 4, samplePlotBounds.y + samplePlotBounds.height + 16);

        drawAxisValue(g, "1", samplePlotBounds.x - 14, samplePlotBounds.y + 4);
        drawAxisValue(g, "0.5", samplePlotBounds.x - 24, samplePlotBounds.y + samplePlotBounds.height / 2 + 4);
        drawAxisValue(g, "0", samplePlotBounds.x - 14, samplePlotBounds.y + samplePlotBounds.height + 4);
    }

    private void drawConvergencePanel(Graphics2D g) {
        drawPanelBox(g,
                convergenceChartBounds.x,
                convergenceChartBounds.y,
                convergenceChartBounds.width,
                convergenceChartBounds.height,
                PANEL_BACKGROUND);

        g.setFont(PANEL_TITLE_FONT);
        g.setColor(TEXT_PRIMARY);
        g.drawString("CONVERGENCE OF π ESTIMATE", convergenceChartBounds.x + PANEL_INSET, convergenceChartBounds.y + 24);

        double currentEstimate = currentEstimate();
        String valueText = pointCount == 0
                ? "π ≈ —"
                : String.format(java.util.Locale.US, "π ≈ %.8f", currentEstimate);

        g.setFont(PANEL_META_FONT);
        g.setColor(TEXT_MUTED);
        g.drawString(valueText, convergenceChartBounds.x + convergenceChartBounds.width - 120, convergenceChartBounds.y + 23);

        Rectangle plot = innerChartArea(convergenceChartBounds);
        drawChartGrid(g, plot);

        double[] estimateScale = estimateScale();
        drawTrueValueLine(g, plot, normalizeRange(Math.PI, estimateScale[0], estimateScale[1]), "π true");

        if (estimateHistory.size() >= 2) {
            g.setColor(CONVERGENCE_LINE);
            g.setStroke(new BasicStroke(2f));
            drawEstimatePolyline(g, plot, estimateScale[0], estimateScale[1]);
            g.setStroke(new BasicStroke(1f));
        }

        g.setFont(CHART_VALUE_FONT);
        g.setColor(TEXT_DIM);
        g.drawString("samples", plot.x + plot.width - 40, plot.y + plot.height + 18);
        g.drawString(pointCount > 0 ? Integer.toString(pointCount) : "0", plot.x + plot.width - 22, plot.y + plot.height + 32);
    }

    private void drawErrorPanel(Graphics2D g) {
        drawPanelBox(g,
                errorChartBounds.x,
                errorChartBounds.y,
                errorChartBounds.width,
                errorChartBounds.height,
                PANEL_BACKGROUND);

        g.setFont(PANEL_TITLE_FONT);
        g.setColor(TEXT_PRIMARY);
        g.drawString("ABSOLUTE ERROR  |πestimate − πtrue|", errorChartBounds.x + PANEL_INSET, errorChartBounds.y + 24);

        g.setFont(PANEL_META_FONT);
        g.setColor(TEXT_MUTED);
        g.drawString(pointCount == 0 ? "waiting for samples" : formatScientific(currentError()),
                errorChartBounds.x + errorChartBounds.width - 96,
                errorChartBounds.y + 23);

        Rectangle plot = innerChartArea(errorChartBounds);
        drawChartGrid(g, plot);
        drawErrorReferenceLabels(g, plot);

        if (errorHistory.size() >= 2) {
            g.setColor(ERROR_LINE);
            g.setStroke(new BasicStroke(2f));
            drawErrorPolyline(g, plot);
            g.setStroke(new BasicStroke(1f));
        }
    }

    private void drawMetricCards(Graphics2D g) {
        if (metricCardBounds.size() < 5) {
            return;
        }

        double estimate = currentEstimate();
        double absoluteError = currentError();
        double relativeError = pointCount == 0 ? 0.0 : absoluteError / Math.PI;
        double insideRatio = pointCount == 0 ? 0.0 : (double) insideCount / pointCount;

        drawMetricCard(g, metricCardBounds.get(0), "TOTAL SAMPLES", formatWithGrouping(pointCount),
                pointCount == 0 ? "waiting" : "inside + outside", CARD_VALUE_CYAN);
        drawMetricCard(g, metricCardBounds.get(1), "POINTS INSIDE", formatWithGrouping(insideCount),
                pointCount == 0 ? "0.0000%" : percent(insideRatio), CARD_VALUE_GREEN);
        drawMetricCard(g, metricCardBounds.get(2), "POINTS OUTSIDE", formatWithGrouping(outsideCount),
                pointCount == 0 ? "0.0000%" : percent(1.0 - insideRatio), CARD_VALUE_ORANGE);
        drawMetricCard(g, metricCardBounds.get(3), "ESTIMATE OF π", pointCount == 0 ? "—" : String.format(java.util.Locale.US, "%.8f", estimate),
                "4 × (inside / N)", CARD_VALUE_CYAN);
        drawMetricCard(g, metricCardBounds.get(4), "ABS / REL ERROR", pointCount == 0 ? "—" : formatScientific(absoluteError),
                pointCount == 0 ? "relative —" : "rel " + formatScientific(relativeError), CARD_VALUE_YELLOW);
    }

    private void drawMetricCard(Graphics2D g, Rectangle bounds, String label, String value, String meta, Color valueColor) {
        drawPanelBox(g, bounds.x, bounds.y, bounds.width, bounds.height, PANEL_BACKGROUND_SOFT);

        g.setFont(CARD_LABEL_FONT);
        g.setColor(CARD_LABEL_COLOR);
        g.drawString(label, bounds.x + 14, bounds.y + 22);

        g.setFont(CARD_VALUE_FONT);
        g.setColor(valueColor);
        g.drawString(value, bounds.x + 14, bounds.y + 48);

        g.setFont(CARD_SMALL_FONT);
        g.setColor(TEXT_DIM);
        g.drawString(meta, bounds.x + 14, bounds.y + 67);
    }

    private void drawPanelBox(Graphics2D g, int x, int y, int panelWidth, int panelHeight, Color fill) {
        RoundRectangle2D.Float box = new RoundRectangle2D.Float(x, y, panelWidth, panelHeight, PANEL_RADIUS, PANEL_RADIUS);

        g.setColor(PANEL_GLOW);
        g.fillRoundRect(x + 4, y + 6, panelWidth - 8, panelHeight - 8, PANEL_RADIUS, PANEL_RADIUS);

        g.setColor(fill);
        g.fill(box);
        g.setColor(PANEL_BORDER);
        g.draw(box);
    }

    private void drawChartGrid(Graphics2D g, Rectangle plot) {
        g.setColor(GRID_COLOR);
        for (int i = 0; i <= CHART_HORIZONTAL_GRID_LINES; i++) {
            int y = plot.y + Math.round((float) i / CHART_HORIZONTAL_GRID_LINES * plot.height);
            g.drawLine(plot.x, y, plot.x + plot.width, y);
        }
        for (int i = 0; i <= CHART_VERTICAL_GRID_LINES; i++) {
            int x = plot.x + Math.round((float) i / CHART_VERTICAL_GRID_LINES * plot.width);
            g.drawLine(x, plot.y, x, plot.y + plot.height);
        }

        g.setColor(SAMPLE_BORDER);
        g.drawRect(plot.x, plot.y, plot.width, plot.height);
    }

    private void drawTrueValueLine(Graphics2D g, Rectangle plot, double relativeY, String label) {
        Stroke oldStroke = g.getStroke();
        g.setColor(TRUE_VALUE_LINE);
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{6f, 6f}, 0f));
        int y = plot.y + (int) Math.round(relativeY * plot.height);
        g.drawLine(plot.x, y, plot.x + plot.width, y);
        g.setStroke(oldStroke);

        g.setFont(CHART_VALUE_FONT);
        g.drawString(label, plot.x + plot.width - 34, y - 6);
    }

    private void drawEstimatePolyline(Graphics2D g, Rectangle plot, double minEstimate, double maxEstimate) {
        for (int i = 1; i < estimateHistory.size(); i++) {
            int x1 = interpolateX(plot, i - 1, estimateHistory.size());
            int x2 = interpolateX(plot, i, estimateHistory.size());
            int y1 = plot.y + plot.height - (int) Math.round(normalizeRange(estimateHistory.get(i - 1), minEstimate, maxEstimate) * plot.height);
            int y2 = plot.y + plot.height - (int) Math.round(normalizeRange(estimateHistory.get(i), minEstimate, maxEstimate) * plot.height);
            g.drawLine(x1, y1, x2, y2);
        }
    }


    private double[] estimateScale() {
        double minEstimate = Math.PI - 0.45;
        double maxEstimate = Math.PI + 0.45;
        for (double value : estimateHistory) {
            minEstimate = Math.min(minEstimate, value);
            maxEstimate = Math.max(maxEstimate, value);
        }
        double padding = Math.max(0.06, (maxEstimate - minEstimate) * 0.08);
        return new double[]{minEstimate - padding, maxEstimate + padding};
    }

    private void drawErrorPolyline(Graphics2D g, Rectangle plot) {
        double minLog = -8.0;
        double maxLog = 0.0;

        for (double error : errorHistory) {
            if (error > 0.0) {
                minLog = Math.min(minLog, Math.log10(error));
                maxLog = Math.max(maxLog, Math.log10(error));
            }
        }

        minLog = Math.min(minLog - 0.2, -1.0);
        maxLog = Math.max(maxLog + 0.2, -0.2);

        for (int i = 1; i < errorHistory.size(); i++) {
            double e1 = Math.max(1.0e-12, errorHistory.get(i - 1));
            double e2 = Math.max(1.0e-12, errorHistory.get(i));
            double n1 = normalizeRange(Math.log10(e1), minLog, maxLog);
            double n2 = normalizeRange(Math.log10(e2), minLog, maxLog);

            int x1 = interpolateX(plot, i - 1, errorHistory.size());
            int x2 = interpolateX(plot, i, errorHistory.size());
            int y1 = plot.y + plot.height - (int) Math.round(n1 * plot.height);
            int y2 = plot.y + plot.height - (int) Math.round(n2 * plot.height);
            g.drawLine(x1, y1, x2, y2);
        }
    }

    private void drawErrorReferenceLabels(Graphics2D g, Rectangle plot) {
        g.setFont(CHART_VALUE_FONT);
        g.setColor(TEXT_DIM);
        g.drawString("10⁰", plot.x - 22, plot.y + 10);
        g.drawString("10⁻²", plot.x - 28, plot.y + plot.height / 3 + 4);
        g.drawString("10⁻⁴", plot.x - 28, plot.y + (2 * plot.height) / 3 + 4);
        g.drawString("10⁻⁶", plot.x - 28, plot.y + plot.height - 2);
        g.drawString("error", plot.x + plot.width - 30, plot.y + plot.height + 18);
    }

    private Rectangle innerChartArea(Rectangle panelBounds) {
        int left = panelBounds.x + 48;
        int top = panelBounds.y + 42;
        int right = panelBounds.x + panelBounds.width - 16;
        int bottom = panelBounds.y + panelBounds.height - 32;
        return new Rectangle(left, top, Math.max(80, right - left), Math.max(60, bottom - top));
    }

    private void drawSamplePoint(Graphics2D g, double x, double y, int drawSize, boolean insideCircle) {
        if (samplePlotBounds.width <= 0 || samplePlotBounds.height <= 0) {
            return;
        }

        int px = samplePlotBounds.x + (int) Math.round(x * (samplePlotBounds.width - 1));
        int py = samplePlotBounds.y + samplePlotBounds.height - 1 - (int) Math.round(y * (samplePlotBounds.height - 1));
        int size = Math.max(1, drawSize);
        int half = size / 2;

        g.setColor(insideCircle ? INSIDE_COLOR : OUTSIDE_COLOR);
        if (size <= 2) {
            g.fillRect(px - half, py - half, size, size);
        } else {
            g.fillOval(px - half, py - half, size, size);
        }
    }

    private void recordHistorySnapshot() {
        double estimate = currentEstimate();
        double error = Math.abs(estimate - Math.PI);

        appendBounded(sampleHistory, pointCount);
        appendBounded(estimateHistory, estimate);
        appendBounded(errorHistory, error);
    }

    private static <T> void appendBounded(List<T> list, T value) {
        list.add(value);
        if (list.size() > HISTORY_CAPACITY) {
            list.remove(0);
        }
    }

    private static double normalize(int rawValue) {
        return Math.floorMod(rawValue, RANDOM_RANGE) / RANDOM_MAX;
    }

    private static boolean isInsideCircle(double x, double y) {
        double dx = x - 0.5;
        double dy = y - 0.5;
        return dx * dx + dy * dy <= 0.25;
    }

    private double currentEstimate() {
        return pointCount == 0 ? 0.0 : 4.0 * insideCount / pointCount;
    }

    private double currentError() {
        return pointCount == 0 ? 0.0 : Math.abs(currentEstimate() - Math.PI);
    }

    private static int interpolateX(Rectangle plot, int index, int size) {
        if (size <= 1) {
            return plot.x;
        }
        return plot.x + (int) Math.round((double) index / (size - 1) * plot.width);
    }

    private static double normalizeRange(double value, double min, double max) {
        if (max <= min) {
            return 0.5;
        }
        return Math.max(0.0, Math.min(1.0, (value - min) / (max - min)));
    }

    private static String formatWithGrouping(int value) {
        return String.format(java.util.Locale.US, "%,d", value);
    }

    private static String percent(double value) {
        return String.format(java.util.Locale.US, "%.4f%%", value * 100.0);
    }

    private static String formatScientific(double value) {
        if (value == 0.0) {
            return "0";
        }
        return String.format(java.util.Locale.US, "%.2e", value);
    }

    private static void drawAxisValue(Graphics2D g, String text, int x, int y) {
        g.drawString(text, x, y);
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
