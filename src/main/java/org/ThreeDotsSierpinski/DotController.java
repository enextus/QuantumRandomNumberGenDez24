package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Универсальный контроллер визуализации случайных чисел.
 * <p>
 * Принимает любой {@link VisualizationMode} и управляет анимацией,
 * рендерингом, статусом и сохранением изображений.
 * <p>
 * Thread safety:
 * - Все операции с offscreenImage — только на EDT
 * - usedRandomNumbers итерируется под synchronized
 */
public class DotController extends JPanel {

    private static final String CONFIG_PANEL_WIDTH = "panel.size.width";
    private static final String CONFIG_PANEL_HEIGHT = "panel.size.height";
    private static final String CONFIG_DOT_SIZE = "dot.size";
    private static final String CONFIG_TIMER_DELAY = "timer.delay";
    private static final String CONFIG_WINDOW_SCALE_WIDTH = "window.scale.width";
    private static final String CONFIG_WINDOW_SCALE_HEIGHT = "window.scale.height";

    private static final int SIZE_WIDTH = Config.getInt(CONFIG_PANEL_WIDTH);
    private static final int SIZE_HEIGHT = Config.getInt(CONFIG_PANEL_HEIGHT);
    private static final int DOT_SIZE = Config.getInt(CONFIG_DOT_SIZE);
    private static final int TIMER_DELAY = Config.getInt(CONFIG_TIMER_DELAY);

    private static final int MIN_DIGIT_GROUP = 1;
    private static final int MAX_DIGIT_GROUP = 5;

    private static final int RANDOM_STACK_CELL_HORIZONTAL_PADDING = 4;
    private static final int RANDOM_STACK_TOP_MARGIN = 18;
    private static final int RANDOM_STACK_RIGHT_MARGIN = 12;
    private static final int RANDOM_STACK_COLUMN_GAP = 4;
    private static final int RANDOM_STACK_HEADER_HEIGHT = 18;
    private static final int RANDOM_STACK_CELL_HEIGHT = 18;
    private static final int RANDOM_STACK_BOTTOM_RESERVED_SPACE = 250;

    private static final int LIGHT_MODE_EXTRA_WIDTH = 300;

    private static final int RECOLOR_DELAY_MS = 1_000;
    private static final boolean RECOLOR_TIMER_REPEATS = false;

    private static final int MIN_CANVAS_SIZE = 1;
    private static final int CANVAS_ORIGIN_X = 0;
    private static final int CANVAS_ORIGIN_Y = 0;

    private static final int INFO_TEXT_X = 10;
    private static final int INFO_TEXT_Y = 20;

    private static final int POINT_COUNTER_LEFT_MARGIN = 10;
    private static final int POINT_COUNTER_RIGHT_MARGIN = 10;
    private static final int POINT_COUNTER_Y = 80;

    private static final double POINT_COUNTER_TOP_CENTER_X_RATIO = 0.66;

    private static final int RNG_LABEL_X = 10;
    private static final int RNG_LABEL_Y = 100;

    private static final int ERROR_TEXT_X = 10;
    private static final int ERROR_TEXT_Y = 120;

    private static final int INFO_FONT_SIZE = 12;
    private static final int POINT_COUNTER_FONT_SIZE = 48;
    private static final int RNG_LABEL_FONT_SIZE = 12;
    private static final int ERROR_FONT_SIZE = 12;

    private static final String FONT_SANS_SERIF = "SansSerif";
    private static final String FONT_MONOSPACED = "Monospaced";

    private static final Font INFO_FONT = new Font(FONT_SANS_SERIF, Font.PLAIN, INFO_FONT_SIZE);
    private static final Font POINT_COUNTER_FONT = new Font(FONT_SANS_SERIF, Font.BOLD, POINT_COUNTER_FONT_SIZE);
    private static final Font RNG_LABEL_FONT = new Font(FONT_SANS_SERIF, Font.BOLD, RNG_LABEL_FONT_SIZE);
    private static final Font ERROR_FONT = new Font(FONT_SANS_SERIF, Font.PLAIN, ERROR_FONT_SIZE);

    private static final Font RANDOM_STACK_HEADER_FONT = new Font(FONT_MONOSPACED, Font.PLAIN, 10);
    private static final Font RANDOM_STACK_VALUE_FONT = new Font(FONT_MONOSPACED, Font.PLAIN, 11);

    private static final Color DARK_BACKGROUND_COLOR = Color.BLACK;
    private static final Color LIGHT_BACKGROUND_COLOR = Color.WHITE;

    private static final Color DARK_INFO_COLOR = new Color(180, 200, 255);
    private static final Color LIGHT_INFO_COLOR = Color.BLUE;

    private static final Color DARK_COUNTER_COLOR = new Color(255, 100, 100);
    private static final Color LIGHT_COUNTER_COLOR = Color.RED;

    private static final Color DARK_QUANTUM_COLOR = new Color(100, 220, 100);
    private static final Color LIGHT_QUANTUM_COLOR = new Color(34, 139, 34);

    private static final Color DARK_PSEUDO_COLOR = new Color(255, 180, 60);
    private static final Color LIGHT_PSEUDO_COLOR = new Color(204, 120, 0);

    private static final Color DARK_ERROR_COLOR = new Color(255, 120, 120);
    private static final Color LIGHT_ERROR_COLOR = Color.RED;

    private static final Color DARK_RECOLOR_TARGET_COLOR = new Color(215, 240, 255);

    private static final Color RANDOM_STACK_HEADER_COLOR = new Color(130, 130, 130);
    private static final Color RANDOM_STACK_VALUE_COLOR = Color.BLACK;
    private static final Color RANDOM_STACK_ROW_BACKGROUND = new Color(245, 245, 245);

    private static final Color DARK_RANDOM_STACK_HEADER_COLOR = new Color(142, 163, 188);
    private static final Color DARK_RANDOM_STACK_VALUE_COLOR = new Color(230, 238, 248);
    private static final Color DARK_RANDOM_STACK_ROW_BACKGROUND = new Color(14, 26, 42, 220);

    private static final String RNG_LABEL_QUANTUM_STATUS = "QUANTUM (API)";
    private static final String RNG_LABEL_PSEUDO_STATUS = "PSEUDO (Local)";

    private static final String RNG_MODE_LABEL_QUANTUM = "● QUANTUM";
    private static final String RNG_MODE_LABEL_PSEUDO = "● PSEUDO (L128X256MixRandom)";

    private static final String INFO_SEPARATOR = "  |  ";
    private static final String POINTS_LABEL = "Points: ";
    private static final String RANDOM_NUMBERS_LABEL = "Random numbers: ";

    private static final String ERROR_LOG_PREFIX = "Error: ";
    private static final String ANIMATION_STARTED_LOG_PREFIX = "Animation started: ";
    private static final String ANIMATION_STOPPED_LOG = "Animation stopped.";

    private static final String TRANSPARENT_FILE_SUFFIX = "_transparent.png";
    private static final String WHITE_FILE_SUFFIX = ".png";
    private static final String IMAGE_FORMAT_PNG = "PNG";

    private static final String SAVED_TRANSPARENT_LOG_PREFIX = "Saved (transparent): ";
    private static final String SAVED_WHITE_BACKGROUND_LOG_PREFIX = "Saved (white bg): ";
    private static final String FAILED_TRANSPARENT_SAVE_LOG_PREFIX = "Failed to save transparent image: ";
    private static final String FAILED_WHITE_BACKGROUND_SAVE_LOG_PREFIX = "Failed to save white-bg image: ";

    private static final Logger LOGGER = LoggerConfig.getLogger();

    private final VisualizationMode mode;
    private final RNProvider randomNumberProvider;
    private final String errorMessage;
    private BufferedImage offscreenImage;
    private boolean canvasInitialized = false;
    private final JLabel statusLabel;

    private Timer animationTimer;
    private volatile boolean isRunning = false;

    private final List<Point> pendingRecolorPoints = new ArrayList<>();
    private final Timer recolorTimer;

    public DotController(RNProvider randomNumberProvider, VisualizationMode mode, JLabel statusLabel) {
        this.statusLabel = statusLabel;
        this.mode = mode;
        this.randomNumberProvider = randomNumberProvider;

        int prefWidth = mode.usesDarkBackground()
                ? (int) (SIZE_WIDTH * Config.getDouble(CONFIG_WINDOW_SCALE_WIDTH))
                : SIZE_WIDTH + LIGHT_MODE_EXTRA_WIDTH;

        int prefHeight = mode.usesDarkBackground()
                ? (int) (SIZE_HEIGHT * Config.getDouble(CONFIG_WINDOW_SCALE_HEIGHT))
                : SIZE_HEIGHT;

        setPreferredSize(new Dimension(prefWidth, prefHeight));
        setBackground(mode.usesDarkBackground() ? DARK_BACKGROUND_COLOR : LIGHT_BACKGROUND_COLOR);

        initModeMouseForwarding();

        errorMessage = null;

        initAnimationTimer();

        recolorTimer = new Timer(RECOLOR_DELAY_MS, e -> {
            synchronized (pendingRecolorPoints) {
                if (pendingRecolorPoints.isEmpty() || offscreenImage == null) {
                    return;
                }

                var g2d = offscreenImage.createGraphics();
                try {
                    g2d.setColor(getRecolorAnimationTargetColor());

                    for (var point : pendingRecolorPoints) {
                        g2d.fillRect(point.x, point.y, DOT_SIZE, DOT_SIZE);
                    }
                } finally {
                    g2d.dispose();
                }

                pendingRecolorPoints.clear();
                repaint();
            }
        });

        recolorTimer.setRepeats(RECOLOR_TIMER_REPEATS);
    }

    private void initModeMouseForwarding() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                mode.handleMouseClicked(e.getPoint(), DotController.this);
            }
        });
    }

    private void initAnimationTimer() {
        animationTimer = new Timer(TIMER_DELAY, e -> {
            if (errorMessage == null) {
                var newPoints = mode.step(randomNumberProvider, offscreenImage, DOT_SIZE);

                repaint();

                if (mode.usesRecolorAnimation() && !newPoints.isEmpty()) {
                    synchronized (pendingRecolorPoints) {
                        pendingRecolorPoints.addAll(newPoints);
                    }

                    if (!recolorTimer.isRunning()) {
                        recolorTimer.restart();
                    }
                }
            } else {
                stop();
                repaint();
                LOGGER.severe(ERROR_LOG_PREFIX + errorMessage);
            }
        });
    }

    public void startDotMovement() {
        start();
    }

    public void start() {
        if (!isRunning && errorMessage == null) {
            animationTimer.start();
            isRunning = true;
            LOGGER.info(ANIMATION_STARTED_LOG_PREFIX + mode.getName());
        }
    }

    public void stop() {
        if (isRunning) {
            animationTimer.stop();
            isRunning = false;
            LOGGER.info(ANIMATION_STOPPED_LOG);
        }
    }

    public boolean toggle() {
        if (isRunning) {
            stop();
        } else {
            start();
        }

        return isRunning;
    }

    public boolean isRunning() {
        return isRunning;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;

        if (!canvasInitialized || offscreenImage == null
                || offscreenImage.getWidth() != getWidth()
                || offscreenImage.getHeight() != getHeight()) {

            int width = Math.max(MIN_CANVAS_SIZE, getWidth());
            int height = Math.max(MIN_CANVAS_SIZE, getHeight());

            offscreenImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            mode.initialize(offscreenImage, width, height);
            canvasInitialized = true;
        }

        g.drawImage(offscreenImage, CANVAS_ORIGIN_X, CANVAS_ORIGIN_Y, null);

        g2d.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        );

        boolean dark = mode.usesDarkBackground();

        drawInfoText(g2d, dark);
        drawPointCounter(g2d, dark);
        drawRngModeIndicator(g2d, dark);
        drawErrorMessage(g2d, dark);

        if (shouldDrawRandomNumbersStackOverlay()) {
            drawRandomNumbersStack(g, dark);
        }
    }

    private void drawInfoText(Graphics2D g2d, boolean dark) {
        g2d.setFont(INFO_FONT);
        g2d.setColor(dark ? DARK_INFO_COLOR : LIGHT_INFO_COLOR);

        String rngName = randomNumberProvider.getMode() == RNProvider.Mode.QUANTUM
                ? RNG_LABEL_QUANTUM_STATUS
                : RNG_LABEL_PSEUDO_STATUS;

        String infoText = mode.getName()
                + INFO_SEPARATOR
                + POINTS_LABEL
                + mode.getPointCount()
                + INFO_SEPARATOR
                + RANDOM_NUMBERS_LABEL
                + rngName;

        g2d.drawString(infoText, INFO_TEXT_X, INFO_TEXT_Y);
    }

    private void drawPointCounter(Graphics2D g2d, boolean dark) {
        String pointCounterText = String.valueOf(mode.getPointCount());

        g2d.setFont(POINT_COUNTER_FONT);
        g2d.setColor(dark ? DARK_COUNTER_COLOR : LIGHT_COUNTER_COLOR);

        int pointCounterX = calculatePointCounterX(g2d, pointCounterText);

        g2d.drawString(pointCounterText, pointCounterX, POINT_COUNTER_Y);
    }

    private int calculatePointCounterX(Graphics2D g2d, String pointCounterText) {
        return switch (mode.getPointCounterOverlayPlacement()) {
            case LEFT -> POINT_COUNTER_LEFT_MARGIN;
            case TOP_CENTER -> calculateTopCenterPointCounterX(g2d, pointCounterText);
            case RIGHT -> calculateRightAlignedPointCounterX(g2d, pointCounterText);
        };
    }

    private int calculateRightAlignedPointCounterX(Graphics2D g2d, String pointCounterText) {
        FontMetrics fontMetrics = g2d.getFontMetrics();
        int pointCounterWidth = fontMetrics.stringWidth(pointCounterText);

        return Math.max(
                POINT_COUNTER_RIGHT_MARGIN,
                getWidth() - POINT_COUNTER_RIGHT_MARGIN - pointCounterWidth
        );
    }

    private int calculateTopCenterPointCounterX(Graphics2D g2d, String pointCounterText) {
        FontMetrics fontMetrics = g2d.getFontMetrics();
        int pointCounterWidth = fontMetrics.stringWidth(pointCounterText);

        int preferredCenterX = (int) Math.round(getWidth() * POINT_COUNTER_TOP_CENTER_X_RATIO);
        int preferredX = preferredCenterX - pointCounterWidth / 2;

        int maxX = getWidth() - POINT_COUNTER_RIGHT_MARGIN - pointCounterWidth;

        return Math.clamp(
                preferredX,
                POINT_COUNTER_LEFT_MARGIN,
                Math.max(POINT_COUNTER_LEFT_MARGIN, maxX)
        );
    }

    private void drawRngModeIndicator(Graphics2D g2d, boolean dark) {
        var rngMode = randomNumberProvider.getMode();
        boolean isQuantum = rngMode == RNProvider.Mode.QUANTUM;

        g2d.setFont(RNG_LABEL_FONT);
        g2d.setColor(isQuantum
                ? (dark ? DARK_QUANTUM_COLOR : LIGHT_QUANTUM_COLOR)
                : (dark ? DARK_PSEUDO_COLOR : LIGHT_PSEUDO_COLOR));

        String modeLabel = isQuantum ? RNG_MODE_LABEL_QUANTUM : RNG_MODE_LABEL_PSEUDO;
        g2d.drawString(modeLabel, RNG_LABEL_X, RNG_LABEL_Y);
    }

    private void drawErrorMessage(Graphics2D g2d, boolean dark) {
        if (errorMessage == null) {
            return;
        }

        g2d.setColor(dark ? DARK_ERROR_COLOR : LIGHT_ERROR_COLOR);
        g2d.setFont(ERROR_FONT);
        g2d.drawString(errorMessage, ERROR_TEXT_X, ERROR_TEXT_Y);
    }

    private boolean shouldDrawRandomNumbersStackOverlay() {
        return !mode.usesDarkBackground() || mode instanceof SierpinskiMode;
    }

    private Color getRecolorAnimationTargetColor() {
        return mode.usesDarkBackground()
                ? DARK_RECOLOR_TARGET_COLOR
                : DARK_BACKGROUND_COLOR;
    }

    private void drawRandomNumbersStack(Graphics g, boolean dark) {
        List<Long> numbers = randomNumberProvider.getConsumedNumbers();
        if (numbers.isEmpty()) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g.create();
        try {
            g2d.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON
            );

            Map<Integer, List<Long>> numbersByDigits = groupNumbersByDigitCount(numbers);

            int stackWidth = calculateRandomStackWidth(g2d);

            int currentX = Math.max(
                    RANDOM_STACK_RIGHT_MARGIN,
                    getWidth() - RANDOM_STACK_RIGHT_MARGIN - stackWidth
            );

            int startY = RANDOM_STACK_TOP_MARGIN;

            for (int digitCount = MIN_DIGIT_GROUP; digitCount <= MAX_DIGIT_GROUP; digitCount++) {
                List<Long> columnNumbers = numbersByDigits.getOrDefault(digitCount, List.of());

                int columnWidth = calculateDigitColumnWidth(g2d, digitCount);
                drawDigitColumn(g2d, columnNumbers, digitCount, currentX, startY, columnWidth, dark);

                currentX += columnWidth + RANDOM_STACK_COLUMN_GAP;
            }
        } finally {
            g2d.dispose();
        }
    }

    private static Map<Integer, List<Long>> groupNumbersByDigitCount(List<Long> numbers) {
        Map<Integer, List<Long>> groupedNumbers = new LinkedHashMap<>();

        for (int digitCount = MIN_DIGIT_GROUP; digitCount <= MAX_DIGIT_GROUP; digitCount++) {
            groupedNumbers.put(digitCount, new ArrayList<>());
        }

        for (long number : numbers) {
            int digitCount = calculateDigitCount(number);
            if (digitCount >= MIN_DIGIT_GROUP && digitCount <= MAX_DIGIT_GROUP) {
                groupedNumbers.get(digitCount).add(number);
            }
        }

        return groupedNumbers;
    }

    private static int calculateDigitCount(long number) {
        long absNumber = Math.abs(number);

        if (absNumber < 10) {
            return 1;
        }
        if (absNumber < 100) {
            return 2;
        }
        if (absNumber < 1_000) {
            return 3;
        }
        if (absNumber < 10_000) {
            return 4;
        }

        return 5;
    }

    private static int calculateRandomStackWidth(Graphics2D g2d) {
        int totalWidth = 0;

        for (int digitCount = MIN_DIGIT_GROUP; digitCount <= MAX_DIGIT_GROUP; digitCount++) {
            if (digitCount > MIN_DIGIT_GROUP) {
                totalWidth += RANDOM_STACK_COLUMN_GAP;
            }

            totalWidth += calculateDigitColumnWidth(g2d, digitCount);
        }

        return totalWidth;
    }

    private static int calculateDigitColumnWidth(Graphics2D g2d, int digitCount) {
        FontMetrics valueMetrics = g2d.getFontMetrics(RANDOM_STACK_VALUE_FONT);

        int digitWidth = valueMetrics.charWidth('0');
        int valueWidth = digitWidth * digitCount;

        return valueWidth + RANDOM_STACK_CELL_HORIZONTAL_PADDING * 2;
    }

    private void drawDigitColumn(
            Graphics2D g2d,
            List<Long> numbers,
            int digitCount,
            int x,
            int y,
            int columnWidth,
            boolean dark
    ) {
        drawDigitColumnHeader(g2d, digitCount, x, y, columnWidth, dark);

        int visibleRows = calculateVisibleRandomStackRows(y);
        int fromIndex = Math.max(0, numbers.size() - visibleRows);
        List<Long> visibleNumbers = numbers.subList(fromIndex, numbers.size());

        int rowY = y + RANDOM_STACK_HEADER_HEIGHT;

        for (Long number : visibleNumbers) {
            drawDigitColumnValue(g2d, number, digitCount, x, rowY, columnWidth, dark);
            rowY += RANDOM_STACK_CELL_HEIGHT;
        }
    }

    private static void drawDigitColumnHeader(
            Graphics2D g2d,
            int digitCount,
            int x,
            int y,
            int columnWidth,
            boolean dark
    ) {
        String header = digitCount + "d";

        g2d.setFont(RANDOM_STACK_HEADER_FONT);
        g2d.setColor(dark ? DARK_RANDOM_STACK_HEADER_COLOR : RANDOM_STACK_HEADER_COLOR);

        FontMetrics metrics = g2d.getFontMetrics();
        int textX = x + Math.max(0, (columnWidth - metrics.stringWidth(header)) / 2);
        int textY = y + metrics.getAscent();

        g2d.drawString(header, textX, textY);
    }

    private static void drawDigitColumnValue(
            Graphics2D g2d,
            long number,
            int digitCount,
            int x,
            int y,
            int columnWidth,
            boolean dark
    ) {
        String text = formatNumberForDigitColumn(number, digitCount);

        g2d.setColor(dark ? DARK_RANDOM_STACK_ROW_BACKGROUND : RANDOM_STACK_ROW_BACKGROUND);
        g2d.fillRect(x, y, columnWidth, RANDOM_STACK_CELL_HEIGHT - 1);

        g2d.setFont(RANDOM_STACK_VALUE_FONT);
        g2d.setColor(dark ? DARK_RANDOM_STACK_VALUE_COLOR : RANDOM_STACK_VALUE_COLOR);

        FontMetrics metrics = g2d.getFontMetrics();

        int textX = x + columnWidth - RANDOM_STACK_CELL_HORIZONTAL_PADDING - metrics.stringWidth(text);
        int textY = y + metrics.getAscent();

        g2d.drawString(text, textX, textY);
    }

    private static String formatNumberForDigitColumn(long number, int digitCount) {
        return String.format(Locale.US, "%" + digitCount + "d", number);
    }

    private int calculateVisibleRandomStackRows(int startY) {
        int availableHeight = Math.max(
                0,
                getHeight()
                        - startY
                        - RANDOM_STACK_HEADER_HEIGHT
                        - RANDOM_STACK_BOTTOM_RESERVED_SPACE
        );

        return Math.max(1, availableHeight / RANDOM_STACK_CELL_HEIGHT);
    }

    public void updateStatusLabel(String message) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(message));
    }

    public List<Long> getUsedRandomNumbers() {
        return randomNumberProvider.getConsumedNumbers();
    }

    public RNProvider getRandomNumberProvider() {
        return randomNumberProvider;
    }

    /**
     * Перерисовывает текущее состояние режима без выполнения нового animation step
     * и без потребления новых случайных чисел.
     */
    public void refreshVisualization() {
        SwingUtilities.invokeLater(() -> {
            if (offscreenImage == null || !canvasInitialized) {
                repaint();
                return;
            }

            mode.redraw(
                    offscreenImage,
                    offscreenImage.getWidth(),
                    offscreenImage.getHeight(),
                    DOT_SIZE
            );
            repaint();
        });
    }

    public void shutdown() {
        stop();

        if (recolorTimer != null) {
            recolorTimer.stop();
        }
    }

    public int saveImages(java.io.File directory, String baseName) {
        int saved = 0;

        var transparentFile = new java.io.File(directory, baseName + TRANSPARENT_FILE_SUFFIX);

        try {
            javax.imageio.ImageIO.write(offscreenImage, IMAGE_FORMAT_PNG, transparentFile);
            LOGGER.info(SAVED_TRANSPARENT_LOG_PREFIX + transparentFile.getAbsolutePath());
            saved++;
        } catch (java.io.IOException e) {
            LOGGER.severe(FAILED_TRANSPARENT_SAVE_LOG_PREFIX + e.getMessage());
        }

        var whiteFile = new java.io.File(directory, baseName + WHITE_FILE_SUFFIX);

        try {
            var whiteImage = new BufferedImage(
                    offscreenImage.getWidth(),
                    offscreenImage.getHeight(),
                    BufferedImage.TYPE_INT_RGB
            );

            var g = whiteImage.createGraphics();

            g.setColor(Color.WHITE);
            g.fillRect(CANVAS_ORIGIN_X, CANVAS_ORIGIN_Y, whiteImage.getWidth(), whiteImage.getHeight());
            g.drawImage(offscreenImage, CANVAS_ORIGIN_X, CANVAS_ORIGIN_Y, null);
            g.dispose();

            javax.imageio.ImageIO.write(whiteImage, IMAGE_FORMAT_PNG, whiteFile);
            LOGGER.info(SAVED_WHITE_BACKGROUND_LOG_PREFIX + whiteFile.getAbsolutePath());
            saved++;
        } catch (java.io.IOException e) {
            LOGGER.severe(FAILED_WHITE_BACKGROUND_SAVE_LOG_PREFIX + e.getMessage());
        }

        return saved;
    }
}
