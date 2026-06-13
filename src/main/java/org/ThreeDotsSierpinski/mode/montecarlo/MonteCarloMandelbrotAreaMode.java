package org.ThreeDotsSierpinski.mode.montecarlo;

import org.ThreeDotsSierpinski.app.DotController;
import org.ThreeDotsSierpinski.mode.RngStepBudget;
import org.ThreeDotsSierpinski.mode.VisualizationCategory;
import org.ThreeDotsSierpinski.mode.VisualizationMode;
import org.ThreeDotsSierpinski.rng.RNProvider;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.OptionalInt;

/**
 * Режим визуализации: Monte Carlo Mandelbrot area estimation.
 * <p>
 * Случайные точки бросаются в прямоугольник комплексной плоскости:
 * x ∈ [-2.0, 1.0], y ∈ [-1.5, 1.5].
 * <p>
 * Для каждой точки проверяется, принадлежит ли она множеству Мандельброта:
 * z₀ = 0
 * zₙ₊₁ = zₙ² + c
 * <p>
 * Если точка не "убегает" за заданное количество итераций, она считается
 * bounded/inside. Доля bounded-точек даёт Monte Carlo оценку площади.
 * <p>
 * Оптимизации относительно исходной версии:
 * <ol>
 *   <li>Аналитическая проверка главной кардиоиды и бутона периода-2 —
 *       основная масса внутренних точек классифицируется без итераций.</li>
 *   <li>Кэширование квадратов zx²/zy² в основном цикле (−2 умножения на итерацию).</li>
 *   <li>Таблица цветов (LUT) вместо {@code Color.getHSBColor} на каждую точку.</li>
 *   <li>Статичная "обвязка" (фон, заголовок, панель, сетка) запекается в отдельный
 *       слой и переиспользуется между кадрами вместо полной перерисовки.</li>
 * </ol>
 */
public class MonteCarloMandelbrotAreaMode implements VisualizationMode {

    private static final String ID = "monte-carlo-mandelbrot-area";
    private static final String NAME = "Monte Carlo Mandelbrot Area";
    private static final String DESCRIPTION =
            "Случайные точки исследуют комплексную плоскость.\n"
                    + "Из облака постепенно проступает множество Мандельброта и оценка его площади.";
    private static final String ICON = "𝕄";

    private static final double RANDOM_MAX = 65_535.0;
    private static final int RANDOM_RANGE = 65_536;

    private static final double MIN_REAL = -2.0;
    private static final double MAX_REAL = 1.0;
    private static final double MIN_IMAG = -1.5;
    private static final double MAX_IMAG = 1.5;

    private static final double PLANE_WIDTH = MAX_REAL - MIN_REAL;
    private static final double PLANE_HEIGHT = MAX_IMAG - MIN_IMAG;
    private static final double PLANE_AREA = PLANE_WIDTH * PLANE_HEIGHT;

    /**
     * Численная справочная оценка площади множества Мандельброта.
     * Точное аналитическое значение неизвестно; это reference value только для UI-ориентира.
     */
    private static final double REFERENCE_AREA_ESTIMATE = 1.5065918849;

    private static final int DEFAULT_MAX_ITERATIONS = 96;
    private static final int ITERATIONS_PRESET_LOW = 96;
    private static final int ITERATIONS_PRESET_MEDIUM = 256;
    private static final int ITERATIONS_PRESET_HIGH = 512;
    private static final Integer[] ITERATION_PRESETS = {
            ITERATIONS_PRESET_LOW,
            ITERATIONS_PRESET_MEDIUM,
            ITERATIONS_PRESET_HIGH
    };

    private static final double ESCAPE_RADIUS_SQUARED = 4.0;
    private static final int SAMPLES_PER_STEP = 220;
    private static final int QUANTUM_SAMPLES_PER_STEP = 16;

    private static final int FAST_ESCAPE_THRESHOLD = 4;

    private static final int HEADER_HEIGHT = 96;
    private static final int OUTER_PADDING = 18;
    private static final int PANEL_INSET = 16;
    private static final int PANEL_RADIUS = 22;
    private static final int STAT_CARD_HEIGHT = 74;
    private static final int STAT_CARD_GAP = 10;
    private static final int STAT_CARD_COUNT = 6;
    private static final int RESET_BUTTON_WIDTH = 82;
    private static final int ITERATIONS_COMBO_WIDTH = 92;
    private static final int CONTROL_HEIGHT = 28;

    private static final String RESET_TEXT = "Reset";
    private static final String RESET_TOOLTIP = "Restart Monte Carlo Mandelbrot area estimation";
    private static final String ITERATIONS_LABEL_TEXT = "Iterations";
    private static final String ITERATIONS_TOOLTIP =
            "Higher values are slower but classify Mandelbrot boundary points more accurately";

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
    private static final Color ERROR_GOOD_COLOR = new Color(90, 230, 160);
    private static final Color ERROR_WARN_COLOR = new Color(255, 210, 95);

    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 29);
    private static final Font SUBTITLE_FONT = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font PANEL_TITLE_FONT = new Font("SansSerif", Font.BOLD, 18);
    private static final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font VALUE_FONT = new Font("SansSerif", Font.BOLD, 18);
    private static final Font SMALL_FONT = new Font("SansSerif", Font.PLAIN, 11);
    private final Rectangle[] statCards = new Rectangle[STAT_CARD_COUNT];
    private int width;
    private int height;
    private int pointCount;
    private int randomNumbersUsed;
    private int insideCount;
    private int escapedCount;
    private int maxIterations = DEFAULT_MAX_ITERATIONS;
    private BufferedImage sampleLayer;
    private BufferedImage chromeLayer;
    private Color[] escapeColorLut;
    private Rectangle plotBounds = new Rectangle();
    private Rectangle panelBounds = new Rectangle();
    private DotController controller;

    private static Color computeEscapeColor(int iteration, int iterationLimit) {
        if (iteration <= FAST_ESCAPE_THRESHOLD) {
            return FAST_ESCAPE_COLOR;
        }

        float hue = 0.62f - Math.min(0.48f, iteration / (float) iterationLimit * 0.48f);
        float saturation = 0.85f;
        float brightness = 0.95f;
        return Color.getHSBColor(hue, saturation, brightness);
    }

    /**
     * Возвращает число итераций до выхода за радиус убегания, либо
     * {@code iterationLimit}, если точка считается ограниченной.
     * <p>
     * Перед основным циклом выполняется аналитическая проверка двух крупнейших
     * областей множества — главной кардиоиды и бутона периода-2. Все их точки
     * доказуемо ограничены, поэтому возвращается {@code iterationLimit} без
     * итераций (результат идентичен полному циклу).
     * <p>
     * В основном цикле квадраты zx²/zy² кэшируются и переиспользуются для
     * следующего шага и проверки выхода (−2 умножения на итерацию).
     */
    private static int escapeIterations(double cx, double cy, int iterationLimit) {
        // Главная кардиоида: q·(q + (cx − 1/4)) ≤ 1/4·cy²
        double xMinusQuarter = cx - 0.25;
        double cy2 = cy * cy;
        double q = xMinusQuarter * xMinusQuarter + cy2;
        if (q * (q + xMinusQuarter) <= 0.25 * cy2) {
            return iterationLimit;
        }

        // Бутон периода-2: круг радиуса 1/4 с центром в (−1, 0)
        double xPlusOne = cx + 1.0;
        if (xPlusOne * xPlusOne + cy2 <= 0.0625) {
            return iterationLimit;
        }

        double zx = 0.0;
        double zy = 0.0;
        double zx2 = 0.0;
        double zy2 = 0.0;

        for (int iteration = 0; iteration < iterationLimit; iteration++) {
            zy = 2.0 * zx * zy + cy;   // использует старое zx
            zx = zx2 - zy2 + cx;       // использует кэшированные квадраты
            zx2 = zx * zx;
            zy2 = zy * zy;

            if (zx2 + zy2 > ESCAPE_RADIUS_SQUARED) {
                return iteration;
            }
        }

        return iterationLimit;
    }

    private static double normalize(int value) {
        return Math.floorMod(value, RANDOM_RANGE) / RANDOM_MAX;
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
    public VisualizationCategory getCategory() {
        return VisualizationCategory.MONTE_CARLO;
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
        resetButton.addActionListener(ignored -> restart());

        JLabel iterationsLabel = new JLabel(ITERATIONS_LABEL_TEXT);
        JComboBox<Integer> iterationsComboBox = createIterationsComboBox();

        return List.of(resetButton, iterationsLabel, iterationsComboBox);
    }

    private JComboBox<Integer> createIterationsComboBox() {
        JComboBox<Integer> comboBox = new JComboBox<>(ITERATION_PRESETS);
        comboBox.setSelectedItem(maxIterations);
        comboBox.setPreferredSize(new Dimension(ITERATIONS_COMBO_WIDTH, CONTROL_HEIGHT));
        comboBox.setToolTipText(ITERATIONS_TOOLTIP);
        comboBox.addActionListener(ignored -> {
            Object selectedItem = comboBox.getSelectedItem();
            if (selectedItem instanceof Integer selectedIterations && selectedIterations != maxIterations) {
                maxIterations = selectedIterations;
                restart();
            }
        });
        return comboBox;
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

        int drawSize = Math.max(1, dotSize);
        int drawnThisStep = 0;

        Graphics2D sampleGraphics = sampleLayer.createGraphics();
        try {
            sampleGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            sampleGraphics.setComposite(AlphaComposite.SrcOver);

            int samplesThisStep = RngStepBudget.forProvider(provider, SAMPLES_PER_STEP, QUANTUM_SAMPLES_PER_STEP);

            for (int i = 0; i < samplesThisStep; i++) {
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

                int escapeIteration = escapeIterations(real, imaginary, maxIterations);
                boolean inside = escapeIteration >= maxIterations;

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

    private void refreshController() {
        if (controller != null) {
            controller.refreshVisualization();
        }
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

        buildColorLut();

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

    /**
     * Предрасчёт цвета для каждого возможного числа итераций выхода.
     * Перестраивается только при смене {@code maxIterations}.
     */
    private void buildColorLut() {
        escapeColorLut = new Color[maxIterations + 1];
        for (int i = 0; i <= maxIterations; i++) {
            escapeColorLut[i] = computeEscapeColor(i, maxIterations);
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

        int availablePlotSize = Math.min(
                panelBounds.width - PANEL_INSET * 2,
                panelBounds.height - PANEL_INSET * 2 - 34
        );
        int plotSize = Math.max(80, availablePlotSize);

        int plotX = panelBounds.x + (panelBounds.width - plotSize) / 2;
        int plotY = panelBounds.y + PANEL_INSET + 34;

        plotBounds = new Rectangle(plotX, plotY, plotSize, plotSize);

        int availableCardsWidth = width - OUTER_PADDING * 2 - STAT_CARD_GAP * (STAT_CARD_COUNT - 1);
        int cardWidth = Math.max(120, availableCardsWidth / STAT_CARD_COUNT);

        for (int i = 0; i < STAT_CARD_COUNT; i++) {
            int x = OUTER_PADDING + i * (cardWidth + STAT_CARD_GAP);
            statCards[i] = new Rectangle(x, cardsY, cardWidth, STAT_CARD_HEIGHT);
        }

        buildChrome();
    }

    /**
     * Запекает статичные элементы кадра (фон, заголовок, панель, заголовок панели,
     * сетку) в отдельный непрозрачный слой. Пересобирается только при изменении
     * геометрии или {@code maxIterations} (через {@link #layoutDashboard()}).
     */
    private void buildChrome() {
        chromeLayer = new BufferedImage(
                Math.max(1, width),
                Math.max(1, height),
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D g = chromeLayer.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g.setColor(BACKGROUND);
            g.fillRect(0, 0, width, height);

            drawHeader(g);

            drawPanelBox(g, panelBounds);
            g.setFont(PANEL_TITLE_FONT);
            g.setColor(TEXT_PRIMARY);
            g.drawString("RANDOM SAMPLE SPACE OVER COMPLEX PLANE", panelBounds.x + PANEL_INSET, panelBounds.y + 24);

            drawGrid(g);
        } finally {
            g.dispose();
        }
    }

    private void render(BufferedImage canvas) {
        Graphics2D g = canvas.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // 1. Статичная обвязка (фон, заголовок, панель, сетка) — один blit.
            g.drawImage(chromeLayer, 0, 0, null);

            // 2. Накопленные сэмплы.
            g.drawImage(sampleLayer, 0, 0, null);

            // 3. Лёгкие динамичные/верхние элементы.
            drawPlotDecorations(g);
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
                String.format(
                        java.util.Locale.US,
                        "window: Re ∈ [%.1f, %.1f], Im ∈ [%.1f, %.1f], max iterations = %d",
                        MIN_REAL,
                        MAX_REAL,
                        MIN_IMAG,
                        MAX_IMAG,
                        maxIterations
                ),
                OUTER_PADDING,
                86
        );
    }

    /**
     * Элементы поверх облака сэмплов: рамка графика, подписи осей, легенда.
     * Дешёвые, перерисовываются каждый кадр для сохранения исходного порядка слоёв.
     */
    private void drawPlotDecorations(Graphics2D g) {
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
        double areaEstimate = areaEstimate();
        double absoluteAreaError = Math.abs(areaEstimate - REFERENCE_AREA_ESTIMATE);

        drawStatCard(g, statCards[0], "TOTAL SAMPLES", Integer.toString(pointCount), "random complex points", TITLE_COLOR);
        drawStatCard(g, statCards[1], "BOUNDED", Integer.toString(insideCount), formatPercent(insideRatio()), INSIDE_COLOR);
        drawStatCard(g, statCards[2], "ESCAPED", Integer.toString(escapedCount), formatPercent(escapedRatio()), TEXT_PRIMARY);
        drawStatCard(g, statCards[3], "AREA ESTIMATE", formatArea(areaEstimate), "area ≈ 9 × bounded / total", TITLE_COLOR);
        drawStatCard(g, statCards[4], "MAX ITERATIONS", Integer.toString(maxIterations), "higher = stricter boundary test", BORDER_COLOR);
        drawStatCard(
                g,
                statCards[5],
                "ABS ERROR VS REF",
                formatArea(absoluteAreaError),
                "ref ≈ " + formatArea(REFERENCE_AREA_ESTIMATE),
                absoluteAreaError < 0.05 ? ERROR_GOOD_COLOR : ERROR_WARN_COLOR
        );
    }

    private void drawStatCard(
            Graphics2D g,
            Rectangle bounds,
            String label,
            String value,
            String smallText,
            Color valueColor
    ) {
        if (bounds == null) {
            return;
        }

        drawPanelBox(g, bounds);

        g.setFont(LABEL_FONT);
        g.setColor(TEXT_MUTED);
        g.drawString(label, bounds.x + 14, bounds.y + 22);

        g.setFont(VALUE_FONT);
        g.setColor(valueColor);
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
        int px = plotBounds.x + Math.clamp(
                Math.round((real - MIN_REAL) / PLANE_WIDTH * plotBounds.width),
                0,
                Math.max(0, plotBounds.width - 1)
        );

        int py = plotBounds.y + Math.clamp(
                Math.round((MAX_IMAG - imaginary) / PLANE_HEIGHT * plotBounds.height),
                0,
                Math.max(0, plotBounds.height - 1)
        );

        if (inside) {
            g.setColor(INSIDE_COLOR);
        } else {
            g.setColor(escapeColor(escapeIteration));
        }

        g.fillRect(px, py, drawSize, drawSize);
    }

    private Color escapeColor(int iteration) {
        Color[] lut = escapeColorLut;
        if (lut != null && iteration >= 0 && iteration < lut.length) {
            return lut[iteration];
        }
        return computeEscapeColor(iteration, maxIterations);
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
}