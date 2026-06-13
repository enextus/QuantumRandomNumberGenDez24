package org.ThreeDotsSierpinski.mode.montecarlo;

import org.ThreeDotsSierpinski.app.DotController;
import org.ThreeDotsSierpinski.mode.RngStepBudget;
import org.ThreeDotsSierpinski.mode.VisualizationCategory;
import org.ThreeDotsSierpinski.mode.VisualizationMode;
import org.ThreeDotsSierpinski.rng.RNProvider;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;

/**
 * Режим визуализации: 3D escape-relief множества Мандельброта поверх
 * Monte Carlo оценки площади.
 * <p>
 * Случайные точки бросаются в прямоугольник комплексной плоскости
 * (Re ∈ [-2, 1], Im ∈ [-1.5, 1.5]). Для каждой точки считается сглаженное
 * (smooth) число итераций до выхода за радиус убегания. Это число становится
 * высотой Z: внутренние точки образуют плато, приграничные — гребень,
 * быстро убегающие — плоское «дно».
 * <p>
 * Облако точек удерживается в кольцевом буфере и перепроецируется каждый кадр
 * вращающейся камерой-турникетом (авто-yaw вокруг вертикали + наклон pitch).
 * Рендер — аддитивное накопление яркости в {@code int[]} с тон-маппингом по LUT,
 * что даёт эффект свечения (glow) в плотных областях. Сортировка по глубине не
 * нужна (аддитивное смешение от порядка не зависит).
 * <p>
 * Оценка площади (доля bounded × площадь окна) считается по кумулятивным
 * счётчикам всех обработанных точек и не зависит от размера кольцевого буфера.
 * <p>
 * Константы экспозиции/масштаба ({@link #TONE_EXPOSURE}, {@link #CENTER_WEIGHT},
 * {@link #PIXEL_SCALE_FACTOR}, {@link #HEIGHT_SCALE}) вынесены для подстройки вида.
 */
public class MonteCarloMandelbrot3DAreaMode implements VisualizationMode {

    private static final String ID = "monte-carlo-mandelbrot-3d-area";
    private static final String NAME = "Monte Carlo Mandelbrot 3D Area";
    private static final String DESCRIPTION =
            "Случайные точки комплексной плоскости формируют 3D-рельеф множества Мандельброта.\n"
                    + "Высота = скорость убегания. Облако вращается; параллельно оценивается площадь.";
    private static final String ICON = "⛰";

    // --- Источник случайности -------------------------------------------------
    private static final double RANDOM_MAX = 65_535.0;
    private static final int RANDOM_RANGE = 65_536;

    // --- Окно комплексной плоскости -------------------------------------------
    private static final double MIN_REAL = -2.0;
    private static final double MAX_REAL = 1.0;
    private static final double MIN_IMAG = -1.5;
    private static final double MAX_IMAG = 1.5;

    private static final double PLANE_WIDTH = MAX_REAL - MIN_REAL;
    private static final double PLANE_HEIGHT = MAX_IMAG - MIN_IMAG;
    private static final double PLANE_AREA = PLANE_WIDTH * PLANE_HEIGHT;

    private static final double REFERENCE_AREA_ESTIMATE = 1.5065918849;

    // --- Итерации Мандельброта ------------------------------------------------
    private static final int DEFAULT_MAX_ITERATIONS = 96;
    private static final Integer[] ITERATION_PRESETS = {96, 256, 512};
    private static final double ESCAPE_RADIUS_SQUARED = 4.0;
    private static final double LOG2 = Math.log(2.0);

    private static final int SAMPLES_PER_STEP = 300;
    private static final int QUANTUM_SAMPLES_PER_STEP = 16;

    // --- Кольцевой буфер удерживаемых точек -----------------------------------
    private static final int POINT_CAPACITY = 140_000;

    // --- Геометрия 3D-проекции ------------------------------------------------
    private static final double RE_CENTER = -0.5;
    private static final double IM_CENTER = 0.0;
    private static final double XY_HALF_SPAN = 1.5;   // нормирует Re/Im в [-1, 1]
    private static final double HEIGHT_SCALE = 0.85;  // вертикальный масштаб рельефа
    private static final double CAM_DISTANCE = 3.0;   // мягкая перспектива
    private static final double PIXEL_SCALE_FACTOR = 0.62;
    private static final double YAW_SPEED = 0.45;     // рад/сек авто-вращения
    private static final double DEFAULT_PITCH_DEG = 60.0;
    private static final int MIN_PITCH_DEG = 20;
    private static final int MAX_PITCH_DEG = 82;

    // --- Аддитивный рендер ----------------------------------------------------
    private static final int TONE_MAX = 4000;
    private static final double TONE_EXPOSURE = 170.0;
    private static final double CENTER_WEIGHT = 0.30;
    private static final int BASE_R = 4;
    private static final int BASE_G = 9;
    private static final int BASE_B = 18;
    private static final int COLOR_LUT_SIZE = 256;

    // --- Дашборд --------------------------------------------------------------
    private static final int HEADER_HEIGHT = 96;
    private static final int OUTER_PADDING = 18;
    private static final int PANEL_INSET = 16;
    private static final int PANEL_RADIUS = 22;
    private static final int STAT_CARD_HEIGHT = 74;
    private static final int STAT_CARD_GAP = 10;
    private static final int STAT_CARD_COUNT = 6;
    private static final int CONTROL_HEIGHT = 28;
    private static final int RESET_BUTTON_WIDTH = 82;
    private static final int ITERATIONS_COMBO_WIDTH = 92;
    private static final int TILT_SLIDER_WIDTH = 140;

    private static final String RESET_TEXT = "Reset";
    private static final String SPIN_TEXT = "Spin";
    private static final String ITERATIONS_LABEL_TEXT = "Iterations";
    private static final String TILT_LABEL_TEXT = "Tilt";

    private static final Color BACKGROUND = new Color(2, 7, 14);
    private static final Color PANEL_BACKGROUND = new Color(8, 16, 30);
    private static final Color PANEL_BORDER = new Color(55, 90, 130, 180);
    private static final Color BOX_COLOR = new Color(90, 130, 175, 120);

    private static final Color TITLE_COLOR = new Color(238, 244, 252);
    private static final Color TEXT_PRIMARY = new Color(224, 235, 248);
    private static final Color TEXT_MUTED = new Color(140, 160, 190);
    private static final Color TEXT_DIM = new Color(90, 110, 136);

    private static final Color INSIDE_LEGEND = new Color(120, 255, 205);
    private static final Color RIDGE_LEGEND = new Color(255, 215, 110);
    private static final Color GROUND_LEGEND = new Color(80, 120, 200);
    private static final Color BORDER_COLOR = new Color(255, 225, 96, 210);
    private static final Color ERROR_GOOD_COLOR = new Color(90, 230, 160);
    private static final Color ERROR_WARN_COLOR = new Color(255, 210, 95);

    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 29);
    private static final Font SUBTITLE_FONT = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font PANEL_TITLE_FONT = new Font("SansSerif", Font.BOLD, 18);
    private static final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font VALUE_FONT = new Font("SansSerif", Font.BOLD, 18);
    private static final Font SMALL_FONT = new Font("SansSerif", Font.PLAIN, 11);

    private static final int INSIDE_COLOR = packRgb(120, 255, 205);
    private final Rectangle[] statCards = new Rectangle[STAT_CARD_COUNT];
    // --- Изменяемое состояние -------------------------------------------------
    private int width;
    private int height;
    private int pointCount;
    private int randomNumbersUsed;
    private int insideCount;
    private int escapedCount;
    private int maxIterations = DEFAULT_MAX_ITERATIONS;
    // Кольцевой буфер точек (модельные координаты + предрассчитанный цвет).
    private float[] pointRe;
    private float[] pointIm;
    private float[] pointHeight;
    private int[] pointColor;
    private int storedCount;
    private int writePos;
    // Камера.
    private boolean spinning = true;
    private double yaw;
    private double pitch = Math.toRadians(DEFAULT_PITCH_DEG);
    private long lastNanos;
    // Цветовая палитра высоты (escape → rgb) и тон-маппинг.
    private int[] heightColorLut;
    private int[] toneLut;
    // Рендер-буферы плоскости графика.
    private BufferedImage plotImage;
    private int[] plotPixels;
    private int[] accumR;
    private int[] accumG;
    private int[] accumB;
    private int plotW;
    private int plotH;
    // Запечённая статичная обвязка.
    private BufferedImage chromeLayer;
    private Rectangle plotBounds = new Rectangle();
    private Rectangle panelBounds = new Rectangle();
    private DotController controller;

    /**
     * Возвращает нормализованную высоту рельефа в [0, 1].
     * Значение {@code 1.0} означает внутреннюю точку (плато); значения < 1.0 —
     * сглаженное число итераций до выхода (выше = медленнее убегает).
     */
    private static double escapeHeight(double cx, double cy, int iterationLimit) {
        // Главная кардиоида.
        double xMinusQuarter = cx - 0.25;
        double cy2 = cy * cy;
        double q = xMinusQuarter * xMinusQuarter + cy2;
        if (q * (q + xMinusQuarter) <= 0.25 * cy2) {
            return 1.0;
        }
        // Бутон периода-2.
        double xPlusOne = cx + 1.0;
        if (xPlusOne * xPlusOne + cy2 <= 0.0625) {
            return 1.0;
        }

        double zx = 0.0;
        double zy = 0.0;
        double zx2 = 0.0;
        double zy2 = 0.0;

        for (int iteration = 0; iteration < iterationLimit; iteration++) {
            zy = 2.0 * zx * zy + cy;
            zx = zx2 - zy2 + cx;
            zx2 = zx * zx;
            zy2 = zy * zy;

            double magnitudeSquared = zx2 + zy2;
            if (magnitudeSquared > ESCAPE_RADIUS_SQUARED) {
                double logZn = Math.log(magnitudeSquared) * 0.5;
                double nu = Math.log(logZn / LOG2) / LOG2;
                double smooth = (iteration + 1) - nu;
                double normalized = smooth / iterationLimit;
                if (normalized < 0.0) {
                    return 0.0;
                }
                if (normalized > 0.999) {
                    return 0.999;
                }
                return normalized;
            }
        }

        return 1.0;
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

    private static int packRgb(int r, int g, int b) {
        return (r << 16) | (g << 8) | b;
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clampByte(int value) {
        return Math.max(0, Math.min(255, value));
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
        resetButton.addActionListener(ignored -> restart());

        JLabel iterationsLabel = new JLabel(ITERATIONS_LABEL_TEXT);
        JComboBox<Integer> iterationsComboBox = createIterationsComboBox();

        JCheckBox spinCheckBox = new JCheckBox(SPIN_TEXT, spinning);
        spinCheckBox.addActionListener(ignored -> {
            spinning = spinCheckBox.isSelected();
            lastNanos = System.nanoTime();
        });

        JLabel tiltLabel = new JLabel(TILT_LABEL_TEXT);
        JSlider tiltSlider = new JSlider(MIN_PITCH_DEG, MAX_PITCH_DEG, (int) Math.round(DEFAULT_PITCH_DEG));
        tiltSlider.setPreferredSize(new Dimension(TILT_SLIDER_WIDTH, CONTROL_HEIGHT));
        tiltSlider.addChangeListener(ignored -> {
            pitch = Math.toRadians(tiltSlider.getValue());
            refreshController();
        });

        return List.of(resetButton, iterationsLabel, iterationsComboBox, spinCheckBox, tiltLabel, tiltSlider);
    }

    private JComboBox<Integer> createIterationsComboBox() {
        JComboBox<Integer> comboBox = new JComboBox<>(ITERATION_PRESETS);
        comboBox.setSelectedItem(maxIterations);
        comboBox.setPreferredSize(new Dimension(ITERATIONS_COMBO_WIDTH, CONTROL_HEIGHT));
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

        layoutDashboard();
        resetState();
        render(canvas);
    }

    @Override
    public List<Point> step(RNProvider provider, BufferedImage canvas, int dotSize) {
        ensureInitialized(canvas);

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

            double escapeHeight = escapeHeight(real, imaginary, maxIterations);
            boolean inside = escapeHeight >= 1.0;

            pointCount++;
            if (inside) {
                insideCount++;
            } else {
                escapedCount++;
            }

            storePoint(real, imaginary, escapeHeight, inside);
        }

        render(canvas);
        return List.of();
    }

    @Override
    public void redraw(BufferedImage canvas, int width, int height, int dotSize) {
        if (canvas == null) {
            return;
        }

        if (this.width != width || this.height != height || plotImage == null) {
            // Размер изменился: пересобираем раскладку и буферы, но облако точек
            // (в модельных координатах) сохраняем.
            this.width = Math.max(1, width);
            this.height = Math.max(1, height);
            layoutDashboard();
            ensurePointBuffers();
            ensureLuts();
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
        if (plotImage == null || width != canvas.getWidth() || height != canvas.getHeight()) {
            initialize(canvas, canvas.getWidth(), canvas.getHeight());
        }
    }

    private void resetState() {
        pointCount = 0;
        randomNumbersUsed = 0;
        insideCount = 0;
        escapedCount = 0;
        storedCount = 0;
        writePos = 0;
        yaw = 0.0;
        lastNanos = System.nanoTime();

        ensurePointBuffers();
        ensureLuts();
    }

    private void ensurePointBuffers() {
        if (pointRe == null) {
            pointRe = new float[POINT_CAPACITY];
            pointIm = new float[POINT_CAPACITY];
            pointHeight = new float[POINT_CAPACITY];
            pointColor = new int[POINT_CAPACITY];
        }
    }

    private void ensureLuts() {
        if (heightColorLut == null) {
            buildColorLut();
        }
        if (toneLut == null) {
            buildToneLut();
        }
    }

    private void buildColorLut() {
        heightColorLut = new int[COLOR_LUT_SIZE];
        for (int i = 0; i < COLOR_LUT_SIZE; i++) {
            float s = i / (float) (COLOR_LUT_SIZE - 1);
            float hue = 0.62f - 0.47f * s;          // синий → бирюза → зелёный → золото
            float saturation = 0.85f;
            float brightness = 0.45f + 0.55f * s;    // дно темнее, гребень ярче
            heightColorLut[i] = Color.HSBtoRGB(hue, saturation, brightness) & 0xFFFFFF;
        }
    }

    private void buildToneLut() {
        toneLut = new int[TONE_MAX + 1];
        for (int i = 0; i <= TONE_MAX; i++) {
            double mapped = 255.0 * (1.0 - Math.exp(-i / TONE_EXPOSURE));
            toneLut[i] = clampByte((int) Math.round(mapped));
        }
    }

    private void storePoint(double real, double imaginary, double escapeHeight, boolean inside) {
        int color;
        if (inside) {
            color = INSIDE_COLOR;
        } else {
            int idx = clampInt((int) (escapeHeight * (COLOR_LUT_SIZE - 1)), 0, COLOR_LUT_SIZE - 1);
            color = heightColorLut[idx];
        }

        pointRe[writePos] = (float) real;
        pointIm[writePos] = (float) imaginary;
        pointHeight[writePos] = (float) Math.min(1.0, escapeHeight);
        pointColor[writePos] = color;

        writePos = (writePos + 1) % POINT_CAPACITY;
        if (storedCount < POINT_CAPACITY) {
            storedCount++;
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

        allocatePlotBuffers(plotSize, plotSize);
        buildChrome();
    }

    private void allocatePlotBuffers(int w, int h) {
        plotW = Math.max(1, w);
        plotH = Math.max(1, h);
        plotImage = new BufferedImage(plotW, plotH, BufferedImage.TYPE_INT_RGB);
        plotPixels = ((DataBufferInt) plotImage.getRaster().getDataBuffer()).getData();
        int pixels = plotW * plotH;
        accumR = new int[pixels];
        accumG = new int[pixels];
        accumB = new int[pixels];
    }

    private void buildChrome() {
        chromeLayer = new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_RGB);
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
            g.drawString("3D ESCAPE-TIME RELIEF OVER COMPLEX PLANE", panelBounds.x + PANEL_INSET, panelBounds.y + 24);
        } finally {
            g.dispose();
        }
    }

    private void render(BufferedImage canvas) {
        advanceYaw();
        renderCloud();

        Graphics2D g = canvas.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g.drawImage(chromeLayer, 0, 0, null);
            g.drawImage(plotImage, plotBounds.x, plotBounds.y, null);

            drawBoundingBox(g);

            g.setColor(PANEL_BORDER);
            g.drawRect(plotBounds.x, plotBounds.y, plotBounds.width, plotBounds.height);

            drawLegend(g);
            drawStats(g);
        } finally {
            g.dispose();
        }
    }

    private void advanceYaw() {
        long now = System.nanoTime();
        if (spinning) {
            yaw += (now - lastNanos) * 1.0e-9 * YAW_SPEED;
            if (yaw > Math.PI * 2.0) {
                yaw -= Math.PI * 2.0;
            }
        }
        lastNanos = now;
    }

    /**
     * Перепроецирует удерживаемое облако в плоскость графика с аддитивным
     * накоплением яркости и тон-маппингом. Глубина даёт лёгкий туман
     * (дальние точки тусклее), сортировка не нужна.
     */
    private void renderCloud() {
        Arrays.fill(accumR, 0);
        Arrays.fill(accumG, 0);
        Arrays.fill(accumB, 0);

        double cosY = Math.cos(yaw);
        double sinY = Math.sin(yaw);
        double cosP = Math.cos(pitch);
        double sinP = Math.sin(pitch);

        double cx = plotW * 0.5;
        double cy = plotH * 0.5;
        double pixelScale = Math.min(plotW, plotH) * 0.5 * PIXEL_SCALE_FACTOR;

        int wLimit = plotW - 1;
        int hLimit = plotH - 1;

        for (int i = 0; i < storedCount; i++) {
            double mx = (pointRe[i] - RE_CENTER) / XY_HALF_SPAN;
            double my = (pointIm[i] - IM_CENTER) / XY_HALF_SPAN;
            double mz = (pointHeight[i] - 0.5) * HEIGHT_SCALE;

            // Yaw вокруг вертикальной (высотной) оси.
            double x1 = mx * cosY - my * sinY;
            double y1 = mx * sinY + my * cosY;

            // Наклон камеры (pitch) вокруг экранной горизонтали.
            double yRot = y1 * cosP - mz * sinP;
            double depth = y1 * sinP + mz * cosP + CAM_DISTANCE;
            if (depth <= 0.05) {
                continue;
            }
            double persp = CAM_DISTANCE / depth;

            int px = (int) (cx + x1 * persp * pixelScale);
            int py = (int) (cy + yRot * persp * pixelScale);
            if (px < 1 || px > wLimit - 1 || py < 1 || py > hLimit - 1) {
                continue;
            }

            int color = pointColor[i];
            double weight = persp * CENTER_WEIGHT;
            int cr = (int) (((color >> 16) & 0xFF) * weight);
            int cg = (int) (((color >> 8) & 0xFF) * weight);
            int cb = (int) ((color & 0xFF) * weight);

            int idx = py * plotW + px;
            accumR[idx] += cr;
            accumG[idx] += cg;
            accumB[idx] += cb;

            // Крестовой splat для мягкого свечения (соседи с втрое меньшим весом).
            int nr = cr / 3;
            int ng = cg / 3;
            int nb = cb / 3;
            splat(idx - 1, nr, ng, nb);
            splat(idx + 1, nr, ng, nb);
            splat(idx - plotW, nr, ng, nb);
            splat(idx + plotW, nr, ng, nb);
        }

        toneMap();
    }

    private void splat(int idx, int r, int g, int b) {
        accumR[idx] += r;
        accumG[idx] += g;
        accumB[idx] += b;
    }

    private void toneMap() {
        int[] tone = toneLut;
        int[] pixels = plotPixels;
        int[] aR = accumR;
        int[] aG = accumG;
        int[] aB = accumB;
        int n = pixels.length;

        for (int i = 0; i < n; i++) {
            int vr = aR[i];
            int vg = aG[i];
            int vb = aB[i];
            if (vr > TONE_MAX) vr = TONE_MAX;
            if (vg > TONE_MAX) vg = TONE_MAX;
            if (vb > TONE_MAX) vb = TONE_MAX;

            int r = BASE_R + tone[vr];
            int g = BASE_G + tone[vg];
            int b = BASE_B + tone[vb];
            if (r > 255) r = 255;
            if (g > 255) g = 255;
            if (b > 255) b = 255;

            pixels[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
        }
    }

    /**
     * Проволочный bounding-box окна (Re×Im×высота) — ориентир вращения.
     */
    private void drawBoundingBox(Graphics2D g) {
        Shape oldClip = g.getClip();
        g.setClip(plotBounds.x, plotBounds.y, plotBounds.width, plotBounds.height);
        g.setColor(BOX_COLOR);

        double[][] corners = new double[8][];
        int c = 0;
        for (int hi = 0; hi <= 1; hi++) {
            double h = hi;
            corners[c++] = project(MIN_REAL, MIN_IMAG, h);
            corners[c++] = project(MAX_REAL, MIN_IMAG, h);
            corners[c++] = project(MAX_REAL, MAX_IMAG, h);
            corners[c++] = project(MIN_REAL, MAX_IMAG, h);
        }

        // Нижний контур (0..3) и верхний (4..7).
        for (int base = 0; base <= 4; base += 4) {
            for (int k = 0; k < 4; k++) {
                drawEdge(g, corners[base + k], corners[base + (k + 1) % 4]);
            }
        }
        // Вертикальные рёбра.
        for (int k = 0; k < 4; k++) {
            drawEdge(g, corners[k], corners[k + 4]);
        }

        g.setClip(oldClip);
    }

    private void drawEdge(Graphics2D g, double[] a, double[] b) {
        g.drawLine(
                plotBounds.x + (int) a[0],
                plotBounds.y + (int) a[1],
                plotBounds.x + (int) b[0],
                plotBounds.y + (int) b[1]
        );
    }

    private double[] project(double real, double imaginary, double heightValue) {
        double mx = (real - RE_CENTER) / XY_HALF_SPAN;
        double my = (imaginary - IM_CENTER) / XY_HALF_SPAN;
        double mz = (heightValue - 0.5) * HEIGHT_SCALE;

        double cosY = Math.cos(yaw);
        double sinY = Math.sin(yaw);
        double cosP = Math.cos(pitch);
        double sinP = Math.sin(pitch);

        double x1 = mx * cosY - my * sinY;
        double y1 = mx * sinY + my * cosY;
        double yRot = y1 * cosP - mz * sinP;
        double depth = y1 * sinP + mz * cosP + CAM_DISTANCE;
        double persp = CAM_DISTANCE / Math.max(0.05, depth);

        double cx = plotW * 0.5;
        double cy = plotH * 0.5;
        double pixelScale = Math.min(plotW, plotH) * 0.5 * PIXEL_SCALE_FACTOR;

        return new double[]{cx + x1 * persp * pixelScale, cy + yRot * persp * pixelScale};
    }

    private void drawHeader(Graphics2D g) {
        g.setFont(TITLE_FONT);
        g.setColor(TITLE_COLOR);
        g.drawString("MONTE CARLO MANDELBROT — 3D RELIEF", OUTER_PADDING, 44);

        g.setFont(SUBTITLE_FONT);
        g.setColor(TEXT_MUTED);
        g.drawString(
                "Escape speed becomes height; the rotating point cloud reveals the set's relief.",
                OUTER_PADDING,
                68
        );

        g.setFont(SMALL_FONT);
        g.setColor(TEXT_DIM);
        g.drawString(
                String.format(
                        java.util.Locale.US,
                        "window: Re ∈ [%.1f, %.1f], Im ∈ [%.1f, %.1f], max iterations = %d",
                        MIN_REAL, MAX_REAL, MIN_IMAG, MAX_IMAG, maxIterations
                ),
                OUTER_PADDING,
                86
        );
    }

    private void drawLegend(Graphics2D g) {
        int x = panelBounds.x + panelBounds.width - 340;
        int y = panelBounds.y + 20;
        g.setFont(LABEL_FONT);

        g.setColor(INSIDE_LEGEND);
        g.fillOval(x, y - 8, 8, 8);
        g.drawString("inside / plateau", x + 14, y);

        g.setColor(RIDGE_LEGEND);
        g.fillOval(x + 124, y - 8, 8, 8);
        g.drawString("slow-escape ridge", x + 138, y);

        g.setColor(GROUND_LEGEND);
        g.fillOval(x + 262, y - 8, 8, 8);
        g.drawString("fast escape", x + 276, y);
    }

    private void drawStats(Graphics2D g) {
        double areaEstimate = areaEstimate();
        double absoluteAreaError = Math.abs(areaEstimate - REFERENCE_AREA_ESTIMATE);

        drawStatCard(g, statCards[0], "TOTAL SAMPLES", Integer.toString(pointCount), "random complex points", TITLE_COLOR);
        drawStatCard(g, statCards[1], "BOUNDED", Integer.toString(insideCount), formatPercent(insideRatio()), INSIDE_LEGEND);
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
        g.setColor(PANEL_BACKGROUND);
        g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, PANEL_RADIUS, PANEL_RADIUS);
        g.setColor(PANEL_BORDER);
        g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, PANEL_RADIUS, PANEL_RADIUS);
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