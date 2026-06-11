package org.ThreeDotsSierpinski.mode.montecarlo;

import org.ThreeDotsSierpinski.app.DotController;
import org.ThreeDotsSierpinski.mode.VisualizationMode;
import org.ThreeDotsSierpinski.rng.RNProvider;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

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

    private static final int PANEL_RADIUS = 22;
    private static final int OUTER_PADDING = 18;
    private static final int HEADER_HEIGHT = 96;
    private static final int SECTION_GAP = 14;
    private static final int PANEL_INSET = 16;
    private static final int SAMPLE_AXIS_EXTRA_BOTTOM = 22;
    private static final int SAMPLE_AXIS_EXTRA_LEFT = 18;
    private static final int CARD_GAP = 10;
    private static final int METRIC_CARD_COUNT = 6;
    private static final int METRICS_ROW_MIN_HEIGHT = 74;
    private static final int METRICS_ROW_MAX_HEIGHT = 92;
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
    private static final Font CHART_VALUE_FONT = new Font("SansSerif", Font.PLAIN, 11);
    private static final Font CARD_LABEL_FONT = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font CARD_VALUE_FONT = new Font("SansSerif", Font.BOLD, 18);
    private static final Font CARD_SMALL_FONT = new Font("SansSerif", Font.PLAIN, 11);
    private static final Font STATUS_TITLE_FONT = new Font("SansSerif", Font.BOLD, 13);
    private static final Font STATUS_VALUE_FONT = new Font("SansSerif", Font.BOLD, 18);
    private static final Font STATUS_SMALL_FONT = new Font("SansSerif", Font.PLAIN, 11);

    private static final String MAIN_TITLE = "MONTE CARLO ESTIMATION OF π";
    private static final String MAIN_SUBTITLE =
            "True random numbers reveal a circle, an estimate and a converging law.";
    private static final String RANDOMNESS_SOURCE_TITLE = "RANDOMNESS SOURCE";
    private static final String SAMPLE_SPACE_TITLE = "MONTE CARLO SAMPLE SPACE";
    private static final String CONVERGENCE_TITLE = "CONVERGENCE OF π ESTIMATE";
    private static final String ABSOLUTE_ERROR_TITLE = "ABSOLUTE ERROR  |πestimate − πtrue|";

    private static final int HELP_ICON_SIZE = 16;
    private static final int HELP_ICON_MARGIN_LEFT = 8;
    private static final int HELP_ICON_VERTICAL_OFFSET = 13;
    private static final int HELP_ICON_TEXT_X_OFFSET = 5;
    private static final int HELP_ICON_TEXT_Y_OFFSET = 12;
    private static final int HELP_ICON_STROKE_WIDTH = 1;
    private static final int HELP_ICON_HIT_PADDING = 3;

    private static final int HELP_DIALOG_WIDTH = 920;
    private static final int HELP_DIALOG_HEIGHT = 520;
    private static final int HELP_DIALOG_PADDING = 18;
    private static final int HELP_DIALOG_COLUMN_GAP = 16;
    private static final int HELP_DIALOG_TITLE_FONT_SIZE = 22;
    private static final int HELP_DIALOG_LANGUAGE_FONT_SIZE = 15;
    private static final int HELP_DIALOG_TEXT_FONT_SIZE = 14;
    private static final int HELP_DIALOG_TEXT_ROWS = 16;
    private static final int HELP_DIALOG_TEXT_COLUMNS = 34;
    private static final int HELP_DIALOG_CLOSE_BUTTON_WIDTH = 96;
    private static final int HELP_DIALOG_CLOSE_BUTTON_HEIGHT = 30;

    private static final Color HELP_ICON_BORDER = new Color(120, 170, 220, 215);
    private static final Color HELP_ICON_BACKGROUND = new Color(18, 34, 55, 225);
    private static final Color HELP_ICON_TEXT = new Color(215, 235, 255);
    private static final Color HELP_DIALOG_BACKGROUND = new Color(7, 14, 26);
    private static final Color HELP_DIALOG_COLUMN_BACKGROUND = new Color(12, 22, 38);
    private static final Color HELP_DIALOG_BORDER = new Color(70, 112, 160);
    private static final Color HELP_DIALOG_TITLE_COLOR = new Color(242, 244, 250);
    private static final Color HELP_DIALOG_LANGUAGE_COLOR = new Color(95, 225, 255);
    private static final Color HELP_DIALOG_TEXT_COLOR = new Color(220, 230, 244);

    private static final Font HELP_ICON_FONT = new Font("SansSerif", Font.BOLD, 11);
    private static final Font HELP_DIALOG_TITLE_FONT =
            new Font("SansSerif", Font.BOLD, HELP_DIALOG_TITLE_FONT_SIZE);
    private static final Font HELP_DIALOG_LANGUAGE_FONT =
            new Font("SansSerif", Font.BOLD, HELP_DIALOG_LANGUAGE_FONT_SIZE);
    private static final Font HELP_DIALOG_TEXT_FONT =
            new Font("SansSerif", Font.PLAIN, HELP_DIALOG_TEXT_FONT_SIZE);
    private final List<Rectangle> metricCardBounds = new ArrayList<>();
    private final Map<HelpTopic, Rectangle> helpHitAreas = new EnumMap<>(HelpTopic.class);
    private final List<Double> estimateHistory = new ArrayList<>();
    private final List<Double> errorHistory = new ArrayList<>();
    private final List<Integer> sampleHistory = new ArrayList<>();
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
    private int metricsRowHeight;
    private DotController controller;
    private RNProvider.Mode displayedProviderMode = RNProvider.Mode.PSEUDO;
    private String displayedFallbackReason;

    private static <T> void appendBounded(List<T> list, T value) {
        list.add(value);
        if (list.size() > HISTORY_CAPACITY) {
            list.removeFirst();
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
        return Math.clamp((value - min) / (max - min), 0.0, 1.0);
    }

    private static String formatWithGrouping(int value) {
        return String.format(java.util.Locale.US, "%,d", value);
    }

    private static String percent(double value) {
        return String.format(java.util.Locale.US, "%.5f%%", value * 100.0);
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

    private static void showHelpDialog(Component parent, HelpTopic topic) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(
                owner,
                "Monte Carlo π Help — " + topic.title,
                Dialog.ModalityType.APPLICATION_MODAL
        );

        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(HELP_DIALOG_BACKGROUND);

        JLabel title = new JLabel(topic.title);
        title.setFont(HELP_DIALOG_TITLE_FONT);
        title.setForeground(HELP_DIALOG_TITLE_COLOR);
        title.setBorder(BorderFactory.createEmptyBorder(
                HELP_DIALOG_PADDING,
                HELP_DIALOG_PADDING,
                HELP_DIALOG_PADDING / 2,
                HELP_DIALOG_PADDING
        ));
        dialog.add(title, BorderLayout.NORTH);

        JPanel content = new JPanel(new GridLayout(1, 2, HELP_DIALOG_COLUMN_GAP, 0));
        content.setBackground(HELP_DIALOG_BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(
                HELP_DIALOG_PADDING / 2,
                HELP_DIALOG_PADDING,
                HELP_DIALOG_PADDING,
                HELP_DIALOG_PADDING
        ));

        content.add(createHelpColumn("Русский", topic.russianText));
        content.add(createHelpColumn("English", topic.englishText));

        dialog.add(content, BorderLayout.CENTER);

        JButton closeButton = new JButton("OK");
        closeButton.setPreferredSize(new Dimension(HELP_DIALOG_CLOSE_BUTTON_WIDTH, HELP_DIALOG_CLOSE_BUTTON_HEIGHT));
        closeButton.addActionListener(ignored -> dialog.dispose());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBackground(HELP_DIALOG_BACKGROUND);
        footer.setBorder(BorderFactory.createEmptyBorder(0, HELP_DIALOG_PADDING, HELP_DIALOG_PADDING, HELP_DIALOG_PADDING));
        footer.add(closeButton);
        dialog.add(footer, BorderLayout.SOUTH);

        dialog.setSize(HELP_DIALOG_WIDTH, HELP_DIALOG_HEIGHT);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    private static JComponent createHelpColumn(String languageTitle, String text) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(HELP_DIALOG_COLUMN_BACKGROUND);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(HELP_DIALOG_BORDER, 1, true),
                BorderFactory.createEmptyBorder(HELP_DIALOG_PADDING, HELP_DIALOG_PADDING, HELP_DIALOG_PADDING, HELP_DIALOG_PADDING)
        ));

        JLabel title = new JLabel(languageTitle);
        title.setFont(HELP_DIALOG_LANGUAGE_FONT);
        title.setForeground(HELP_DIALOG_LANGUAGE_COLOR);
        panel.add(title, BorderLayout.NORTH);

        JScrollPane scrollPane = getJScrollPane(text);
        scrollPane.getViewport().setBackground(HELP_DIALOG_COLUMN_BACKGROUND);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private static @NotNull JScrollPane getJScrollPane(String text) {
        JTextArea textArea = new JTextArea(text, HELP_DIALOG_TEXT_ROWS, HELP_DIALOG_TEXT_COLUMNS);
        textArea.setFont(HELP_DIALOG_TEXT_FONT);
        textArea.setForeground(HELP_DIALOG_TEXT_COLOR);
        textArea.setBackground(HELP_DIALOG_COLUMN_BACKGROUND);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);
        textArea.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(null);
        return scrollPane;
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
    public PointCounterOverlayPlacement getPointCounterOverlayPlacement() {
        return PointCounterOverlayPlacement.TOP_CENTER;
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
        resetButton.addActionListener(ignored -> {
            resetState();
            layoutDashboard();

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
        metricsRowHeight = Math.clamp(height / 7, METRICS_ROW_MIN_HEIGHT, METRICS_ROW_MAX_HEIGHT);
        int metricsY = height - OUTER_PADDING - metricsRowHeight;
        int mainTop = Math.min(height - 40, HEADER_HEIGHT + OUTER_PADDING);
        int mainBottom = metricsY - SECTION_GAP;
        int mainHeight = Math.max(90, mainBottom - mainTop);
        int leftMaxWidth = Math.max(120, width - OUTER_PADDING * 2);
        int leftWidth = Math.clamp((int) Math.round(width * 0.48), 120, leftMaxWidth);
        int rightX = OUTER_PADDING + leftWidth + SECTION_GAP;
        int rightWidth = Math.max(80, width - rightX - OUTER_PADDING);

        samplePanelBounds = new Rectangle(OUTER_PADDING, mainTop, leftWidth, mainHeight);

        int plotAvailableWidth = samplePanelBounds.width - PANEL_INSET * 2 - SAMPLE_AXIS_EXTRA_LEFT;
        int plotAvailableHeight = samplePanelBounds.height - PANEL_INSET * 2 - 38 - SAMPLE_AXIS_EXTRA_BOTTOM;
        int plotSize = Math.clamp(Math.min(plotAvailableWidth, plotAvailableHeight), 40, Integer.MAX_VALUE);
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

        int availableWidth = width - OUTER_PADDING * 2 - CARD_GAP * (METRIC_CARD_COUNT - 1);
        int baseCardWidth = Math.max(120, availableWidth / METRIC_CARD_COUNT);
        int usedWidth = baseCardWidth * METRIC_CARD_COUNT + CARD_GAP * (METRIC_CARD_COUNT - 1);
        int startX = OUTER_PADDING + Math.max(0, (width - OUTER_PADDING * 2 - usedWidth) / 2);
        int y = height - OUTER_PADDING - metricsRowHeight;
        int x = startX;

        for (int i = 0; i < METRIC_CARD_COUNT; i++) {
            metricCardBounds.add(new Rectangle(x, y, baseCardWidth, metricsRowHeight));
            x += baseCardWidth + CARD_GAP;
        }
    }

    private void render(BufferedImage canvas) {
        Graphics2D g = canvas.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            helpHitAreas.clear();

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
        g.drawString(MAIN_TITLE, OUTER_PADDING, 44);
        drawHelpIconAfterText(g, HelpTopic.MAIN_TITLE, MAIN_TITLE, OUTER_PADDING, 44, TITLE_FONT);

        g.setFont(SUBTITLE_FONT);
        g.setColor(SUBTITLE_COLOR);
        g.drawString(MAIN_SUBTITLE, OUTER_PADDING, 68);

        int statusX = width - OUTER_PADDING - STATUS_BOX_WIDTH;
        int statusY = 16;
        drawPanelBox(g, statusX, statusY, STATUS_BOX_WIDTH, STATUS_BOX_HEIGHT, PANEL_BACKGROUND_SOFT);

        g.setFont(STATUS_TITLE_FONT);
        g.setColor(TEXT_MUTED);
        g.drawString(RANDOMNESS_SOURCE_TITLE, statusX + 14, statusY + 20);
        drawHelpIconAfterText(g, HelpTopic.RANDOMNESS_SOURCE, RANDOMNESS_SOURCE_TITLE, statusX + 14, statusY + 20, STATUS_TITLE_FONT);

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
        g.drawString(SAMPLE_SPACE_TITLE, samplePanelBounds.x + PANEL_INSET, samplePanelBounds.y + 24);
        drawHelpIconAfterText(g, HelpTopic.SAMPLE_SPACE, SAMPLE_SPACE_TITLE,
                samplePanelBounds.x + PANEL_INSET, samplePanelBounds.y + 24, PANEL_TITLE_FONT);

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
        g.drawString(CONVERGENCE_TITLE, convergenceChartBounds.x + PANEL_INSET, convergenceChartBounds.y + 24);
        drawHelpIconAfterText(g, HelpTopic.CONVERGENCE, CONVERGENCE_TITLE,
                convergenceChartBounds.x + PANEL_INSET, convergenceChartBounds.y + 24, PANEL_TITLE_FONT);

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
        drawTrueValueLine(g, plot, normalizeRange(Math.PI, estimateScale[0], estimateScale[1]));

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
        g.drawString(ABSOLUTE_ERROR_TITLE, errorChartBounds.x + PANEL_INSET, errorChartBounds.y + 24);
        drawHelpIconAfterText(g, HelpTopic.ABSOLUTE_ERROR_PANEL, ABSOLUTE_ERROR_TITLE,
                errorChartBounds.x + PANEL_INSET, errorChartBounds.y + 24, PANEL_TITLE_FONT);

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
        if (metricCardBounds.size() < METRIC_CARD_COUNT) {
            return;
        }

        double estimate = currentEstimate();
        double absoluteError = currentError();
        double relativeError = pointCount == 0 ? 0.0 : absoluteError / Math.PI;
        double insideRatio = pointCount == 0 ? 0.0 : (double) insideCount / pointCount;
        double outsideRatio = pointCount == 0 ? 0.0 : 1.0 - insideRatio;

        drawMetricCard(g, metricCardBounds.get(0), HelpTopic.TOTAL_SAMPLES, "TOTAL SAMPLES", formatWithGrouping(pointCount),
                pointCount == 0 ? "waiting for samples" : "inside + outside", CARD_VALUE_CYAN);
        drawMetricCard(g, metricCardBounds.get(1), HelpTopic.POINTS_INSIDE, "POINTS INSIDE", formatWithGrouping(insideCount),
                pointCount == 0 ? "0.00000%" : percent(insideRatio), CARD_VALUE_GREEN);
        drawMetricCard(g, metricCardBounds.get(2), HelpTopic.POINTS_OUTSIDE, "POINTS OUTSIDE", formatWithGrouping(outsideCount),
                pointCount == 0 ? "0.00000%" : percent(outsideRatio), CARD_VALUE_ORANGE);
        drawMetricCard(g, metricCardBounds.get(3), HelpTopic.ESTIMATE_PI, "ESTIMATE OF π", pointCount == 0 ? "—" : String.format(java.util.Locale.US, "%.8f", estimate),
                "π = 4 × inside / total", CARD_VALUE_CYAN);
        drawMetricCard(g, metricCardBounds.get(4), HelpTopic.ABSOLUTE_ERROR_METRIC, "ABSOLUTE ERROR", pointCount == 0 ? "—" : formatScientific(absoluteError),
                "|πestimate − πtrue|", CARD_VALUE_YELLOW);
        drawMetricCard(g, metricCardBounds.get(5), HelpTopic.RELATIVE_ERROR, "RELATIVE ERROR", pointCount == 0 ? "—" : formatScientific(relativeError),
                "relative to πtrue", CARD_VALUE_YELLOW);
    }

    private void drawMetricCard(Graphics2D g, Rectangle bounds, HelpTopic topic, String label, String value, String meta, Color valueColor) {
        drawPanelBox(g, bounds.x, bounds.y, bounds.width, bounds.height, PANEL_BACKGROUND_SOFT);

        g.setColor(new Color(valueColor.getRed(), valueColor.getGreen(), valueColor.getBlue(), 210));
        g.fillRoundRect(bounds.x + 14, bounds.y + 12, 28, 5, 5, 5);

        g.setFont(CARD_LABEL_FONT);
        g.setColor(CARD_LABEL_COLOR);
        int labelX = bounds.x + 14;
        int labelY = bounds.y + 30;
        g.drawString(label, labelX, labelY);
        drawHelpIconAfterText(g, topic, label, labelX, labelY, CARD_LABEL_FONT);

        g.setFont(CARD_VALUE_FONT);
        g.setColor(valueColor);
        g.drawString(value, bounds.x + 14, bounds.y + 57);

        g.setFont(CARD_SMALL_FONT);
        g.setColor(TEXT_DIM);
        g.drawString(meta, bounds.x + 14, bounds.y + bounds.height - 14);
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

    private void drawTrueValueLine(Graphics2D g, Rectangle plot, double relativeY) {
        Stroke oldStroke = g.getStroke();
        g.setColor(TRUE_VALUE_LINE);
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{6f, 6f}, 0f));
        int y = plot.y + (int) Math.round(relativeY * plot.height);
        g.drawLine(plot.x, y, plot.x + plot.width, y);
        g.setStroke(oldStroke);

        g.setFont(CHART_VALUE_FONT);
        g.drawString("π true", plot.x + plot.width - 34, y - 6);
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

    private double currentEstimate() {
        return pointCount == 0 ? 0.0 : 4.0 * insideCount / pointCount;
    }

    private double currentError() {
        return pointCount == 0 ? 0.0 : Math.abs(currentEstimate() - Math.PI);
    }

    @Override
    public void handleMouseClicked(Point point, Component parent) {
        for (var entry : helpHitAreas.entrySet()) {
            if (entry.getValue().contains(point)) {
                showHelpDialog(parent, entry.getKey());
                return;
            }
        }
    }

    private void drawHelpIconAfterText(
            Graphics2D g,
            HelpTopic topic,
            String text,
            int textX,
            int baselineY,
            Font textFont
    ) {
        FontMetrics metrics = g.getFontMetrics(textFont);
        int iconX = textX + metrics.stringWidth(text) + HELP_ICON_MARGIN_LEFT;
        int iconY = baselineY - HELP_ICON_VERTICAL_OFFSET;

        Rectangle hitArea = new Rectangle(
                iconX - HELP_ICON_HIT_PADDING,
                iconY - HELP_ICON_HIT_PADDING,
                HELP_ICON_SIZE + HELP_ICON_HIT_PADDING * 2,
                HELP_ICON_SIZE + HELP_ICON_HIT_PADDING * 2
        );
        helpHitAreas.put(topic, hitArea);

        Font oldFont = g.getFont();
        Color oldColor = g.getColor();
        Stroke oldStroke = g.getStroke();

        try {
            g.setColor(HELP_ICON_BACKGROUND);
            g.fillOval(iconX, iconY, HELP_ICON_SIZE, HELP_ICON_SIZE);

            g.setColor(HELP_ICON_BORDER);
            g.setStroke(new BasicStroke(HELP_ICON_STROKE_WIDTH));
            g.drawOval(iconX, iconY, HELP_ICON_SIZE, HELP_ICON_SIZE);

            g.setFont(HELP_ICON_FONT);
            g.setColor(HELP_ICON_TEXT);
            g.drawString(
                    "?",
                    iconX + HELP_ICON_TEXT_X_OFFSET,
                    iconY + HELP_ICON_TEXT_Y_OFFSET
            );
        } finally {
            g.setFont(oldFont);
            g.setColor(oldColor);
            g.setStroke(oldStroke);
        }
    }

    @Override
    public int getPointCount() {
        return pointCount;
    }

    @Override
    public int getRandomNumbersUsed() {
        return randomNumbersUsed;
    }

    private enum HelpTopic {
        MAIN_TITLE(
                "MONTE CARLO ESTIMATION OF π",
                """
                        Это общий заголовок режима. Он показывает, что визуализация использует метод Монте-Карло для оценки числа π.
                        
                        Смысл метода: мы бросаем случайные точки в квадрат и считаем, какая доля попала внутрь вписанной окружности. Так как отношение площади окружности к площади квадрата связано с π, из случайных точек постепенно появляется численная оценка π.
                        """,
                """
                        This is the main title of the mode. It indicates that the visualization uses a Monte Carlo method to estimate π.
                        
                        The idea is to throw random points into a square and count what fraction lands inside the inscribed circle. Since the circle-to-square area ratio is related to π, the random samples gradually produce a numerical estimate of π.
                        """
        ),
        RANDOMNESS_SOURCE(
                "RANDOMNESS SOURCE",
                """
                        Этот блок показывает, откуда сейчас поступают случайные числа.
                        
                        TRUE RANDOM / QRNG означает внешний квантовый источник. PSEUDO / LOCAL PRNG означает локальный псевдослучайный генератор fallback-режима. Для самого метода Монте-Карло важно, чтобы точки были распределены как можно равномернее и независимее.
                        """,
                """
                        This block shows where the random numbers currently come from.
                        
                        TRUE RANDOM / QRNG means the external quantum source. PSEUDO / LOCAL PRNG means the local fallback pseudo-random generator. For Monte Carlo estimation, the important property is that points are as uniform and independent as possible.
                        """
        ),
        SAMPLE_SPACE(
                "MONTE CARLO SAMPLE SPACE",
                """
                        Это пространство выборки: квадрат [0..1] × [0..1], в который бросаются случайные точки.
                        
                        Голубая окружность — вписанная окружность. Точки внутри окружности учитываются как inside, точки вне окружности — как outside. Чем больше точек, тем яснее проявляется круг.
                        """,
                """
                        This is the sample space: the unit square [0..1] × [0..1] where random points are placed.
                        
                        The cyan circle is the inscribed circle. Points inside it count as inside, points outside it count as outside. As more points accumulate, the circle becomes visually clearer.
                        """
        ),
        CONVERGENCE(
                "CONVERGENCE OF π ESTIMATE",
                """
                        Этот график показывает, как текущая оценка π меняется по мере роста количества точек.
                        
                        В начале линия может сильно колебаться, потому что выборка маленькая. С ростом числа samples случайные отклонения частично компенсируются, и оценка обычно приближается к истинному значению π.
                        """,
                """
                        This chart shows how the current π estimate changes as the number of samples grows.
                        
                        At the beginning the line may fluctuate strongly because the sample size is small. As more samples are collected, random deviations partly cancel out and the estimate usually approaches the true value of π.
                        """
        ),
        ABSOLUTE_ERROR_PANEL(
                "ABSOLUTE ERROR |πestimate − πtrue|",
                """
                        Этот график показывает абсолютную ошибку оценки π.
                        
                        Ошибка равна расстоянию между текущей оценкой и истинным значением π. На логарифмической шкале удобно видеть, уменьшается ли ошибка на больших выборках.
                        """,
                """
                        This chart shows the absolute error of the π estimate.
                        
                        The error is the distance between the current estimate and the true value of π. A logarithmic scale makes it easier to see whether the error decreases as the sample size grows.
                        """
        ),
        TOTAL_SAMPLES(
                "TOTAL SAMPLES",
                """
                        Это общее количество случайных точек, уже использованных в оценке π.
                        
                        Каждая точка создаётся из пары случайных чисел: одно задаёт координату x, другое — координату y. Чем больше total samples, тем устойчивее становится статистическая оценка.
                        """,
                """
                        This is the total number of random points already used in the π estimation.
                        
                        Each point is generated from a pair of random numbers: one for x and one for y. The larger the total sample count, the more stable the statistical estimate becomes.
                        """
        ),
        POINTS_INSIDE(
                "POINTS INSIDE",
                """
                        Это количество точек, попавших внутрь вписанной окружности.
                        
                        Именно эта величина входит в формулу π ≈ 4 × inside / total. Если случайные точки равномерны, доля inside постепенно приближается к площади четверти круга относительно единичного квадрата.
                        """,
                """
                        This is the number of points that landed inside the inscribed circle.
                        
                        This value is used in the formula π ≈ 4 × inside / total. If the random points are uniform, the inside fraction gradually approaches the area ratio of the quarter circle within the unit square.
                        """
        ),
        POINTS_OUTSIDE(
                "POINTS OUTSIDE",
                """
                        Это количество точек, которые попали в квадрат, но оказались вне окружности.
                        
                        Эти точки не входят в inside-count, но они важны для total samples. Вместе inside и outside образуют полную статистическую выборку.
                        """,
                """
                        This is the number of points that landed inside the square but outside the circle.
                        
                        These points do not contribute to the inside count, but they are part of the total sample count. Inside and outside together form the complete statistical sample.
                        """
        ),
        ESTIMATE_PI(
                "ESTIMATE OF π",
                """
                        Это текущая оценка числа π, рассчитанная по формуле π ≈ 4 × inside / total.
                        
                        Оценка меняется после накопления новых точек. Она не обязана становиться лучше на каждом отдельном шаге, но на большой выборке обычно стабилизируется около истинного значения.
                        """,
                """
                        This is the current estimate of π, calculated as π ≈ 4 × inside / total.
                        
                        The estimate changes as new points are collected. It does not have to improve on every single step, but with a large sample it usually stabilizes near the true value.
                        """
        ),
        ABSOLUTE_ERROR_METRIC(
                "ABSOLUTE ERROR",
                """
                        Это текущее абсолютное отклонение оценки от истинного значения π.
                        
                        Формула: |πestimate − πtrue|. Чем меньше это число, тем ближе текущая оценка к математическому π.
                        """,
                """
                        This is the current absolute deviation of the estimate from the true value of π.
                        
                        Formula: |πestimate − πtrue|. The smaller this number is, the closer the current estimate is to mathematical π.
                        """
        ),
        RELATIVE_ERROR(
                "RELATIVE ERROR",
                """
                        Это ошибка, нормированная относительно истинного значения π.
                        
                        Она показывает масштаб ошибки не в абсолютных единицах, а как долю от πtrue. Это удобно для сравнения точности между разными экспериментами.
                        """,
                """
                        This is the error normalized relative to the true value of π.
                        
                        It shows the error not as an absolute distance, but as a fraction of πtrue. This is useful for comparing accuracy across different experiments.
                        """
        );

        private final String title;
        private final String russianText;
        private final String englishText;

        HelpTopic(String title, String russianText, String englishText) {
            this.title = title;
            this.russianText = russianText.strip();
            this.englishText = englishText.strip();
        }
    }
}