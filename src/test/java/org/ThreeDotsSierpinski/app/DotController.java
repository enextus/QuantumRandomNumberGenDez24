package org.ThreeDotsSierpinski.app;

import org.ThreeDotsSierpinski.config.Config;
import org.ThreeDotsSierpinski.config.LoggerConfig;
import org.ThreeDotsSierpinski.mode.VisualizationMode;
import org.ThreeDotsSierpinski.mode.VisualizationStyle;
import org.ThreeDotsSierpinski.rng.RNProvider;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

/**
 * Универсальный контроллер визуализации случайных чисел.
 * Принимает любой {@link VisualizationMode} и управляет анимацией,
 * рендерингом, статусом и сохранением изображений.
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
    private static final Font INFO_FONT = new Font(FONT_SANS_SERIF, Font.PLAIN, INFO_FONT_SIZE);
    private static final Font POINT_COUNTER_FONT = new Font(FONT_SANS_SERIF, Font.BOLD, POINT_COUNTER_FONT_SIZE);
    private static final Font RNG_LABEL_FONT = new Font(FONT_SANS_SERIF, Font.BOLD, RNG_LABEL_FONT_SIZE);
    private static final Font ERROR_FONT = new Font(FONT_SANS_SERIF, Font.PLAIN, ERROR_FONT_SIZE);

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

    private static final String RNG_LABEL_QUANTUM_STATUS = "QUANTUM (API)";
    private static final String RNG_LABEL_PSEUDO_STATUS = "PSEUDO (Local)";

    private static final String RNG_MODE_LABEL_QUANTUM = "■ QUANTUM";
    private static final String RNG_MODE_LABEL_PSEUDO = "■ PSEUDO (L128X256MixRandom)";

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
    private final JLabel statusLabel;
    private final List<Point> pendingRecolorPoints = new ArrayList<>();
    private final Timer recolorTimer;
    private BufferedImage offscreenImage;
    private boolean canvasInitialized = false;
    private VisualizationStyle initializedStyle = null;
    private BufferedImage modeCanvas;
    private Rectangle modeCanvasBounds = new Rectangle();
    private Timer animationTimer;
    private volatile boolean isRunning = false;

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
        setBackground(resolvePanelBackgroundColor());

        initModeMouseForwarding();

        errorMessage = null;

        initAnimationTimer();

        recolorTimer = new Timer(RECOLOR_DELAY_MS, e -> {
            synchronized (pendingRecolorPoints) {
                if (pendingRecolorPoints.isEmpty() || offscreenImage == null) {
                    return;
                }

                BufferedImage activeModeCanvas = getModeCanvasForDrawing();
                if (activeModeCanvas == null) {
                    return;
                }

                var g2d = activeModeCanvas.createGraphics();
                try {
                    g2d.setColor(mode.getRecolorAnimationTargetColor());

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

    private static BufferedImage createModeCanvasView(BufferedImage image, Rectangle bounds) {
        int safeX = Math.clamp(bounds.x, 0, Math.max(0, image.getWidth() - MIN_CANVAS_SIZE));
        int safeY = Math.clamp(bounds.y, 0, Math.max(0, image.getHeight() - MIN_CANVAS_SIZE));
        int safeWidth = Math.clamp(bounds.width, MIN_CANVAS_SIZE, image.getWidth() - safeX);
        int safeHeight = Math.clamp(bounds.height, MIN_CANVAS_SIZE, image.getHeight() - safeY);

        return image.getSubimage(safeX, safeY, safeWidth, safeHeight);
    }

    private static Color resolveInfoColor(boolean dark, VisualizationStyle style) {
        if (style == VisualizationStyle.APPLE_MAC) {
            return AppleMacChrome.TEXT_COLOR;
        }

        return dark ? DARK_INFO_COLOR : LIGHT_INFO_COLOR;
    }

    private static Color resolveCounterColor(boolean dark, VisualizationStyle style) {
        if (style == VisualizationStyle.APPLE_MAC) {
            return AppleMacChrome.TEXT_COLOR;
        }

        return dark ? DARK_COUNTER_COLOR : LIGHT_COUNTER_COLOR;
    }

    private static Color resolveRngColor(boolean quantum, boolean dark, VisualizationStyle style) {
        if (style == VisualizationStyle.APPLE_MAC) {
            return AppleMacChrome.TEXT_COLOR;
        }

        if (quantum) {
            return dark ? DARK_QUANTUM_COLOR : LIGHT_QUANTUM_COLOR;
        }

        return dark ? DARK_PSEUDO_COLOR : LIGHT_PSEUDO_COLOR;
    }

    private static Color resolveErrorColor(boolean dark, VisualizationStyle style) {
        if (style == VisualizationStyle.APPLE_MAC) {
            return AppleMacChrome.TEXT_COLOR;
        }

        return dark ? DARK_ERROR_COLOR : LIGHT_ERROR_COLOR;
    }

    private void initModeMouseForwarding() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                mode.handleMouseClicked(toModeCanvasPoint(e.getPoint()), DotController.this);
            }
        });
    }

    private void initAnimationTimer() {
        animationTimer = new Timer(TIMER_DELAY, e -> {
            if (errorMessage == null) {
                refreshReservedDrawingAreasIfNeeded();

                BufferedImage activeModeCanvas = getModeCanvasForDrawing();
                if (activeModeCanvas == null) {
                    repaint();
                    return;
                }

                var newPoints = mode.step(randomNumberProvider, activeModeCanvas, DOT_SIZE);

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
        setBackground(resolvePanelBackgroundColor());
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        VisualizationStyle style = mode.getVisualizationStyle();

        if (!canvasInitialized || offscreenImage == null
                || offscreenImage.getWidth() != getWidth()
                || offscreenImage.getHeight() != getHeight()
                || initializedStyle != style) {

            int width = Math.max(MIN_CANVAS_SIZE, getWidth());
            int height = Math.max(MIN_CANVAS_SIZE, getHeight());

            offscreenImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            modeCanvasBounds = calculateModeCanvasBounds(g2d, width, height, style);
            modeCanvas = createModeCanvasView(offscreenImage, modeCanvasBounds);
            updateReservedDrawingAreas(g2d, style);
            mode.initialize(modeCanvas, modeCanvas.getWidth(), modeCanvas.getHeight());
            initializedStyle = style;
            canvasInitialized = true;
        }

        g.drawImage(offscreenImage, CANVAS_ORIGIN_X, CANVAS_ORIGIN_Y, null);

        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        boolean dark = mode.usesDarkBackground();

        if (style == VisualizationStyle.APPLE_MAC) {
            AppleMacChrome.drawFrame(g2d, getWidth(), getHeight());
        }

        if (mode.usesInfoTextOverlay()) {
            drawInfoText(g2d, dark, style);
        }

        if (style == VisualizationStyle.APPLE_MAC) {
            if (mode.usesPointCounterOverlay() || mode.usesRngModeIndicatorOverlay()) {
                AppleMacChrome.drawCounterBlock(
                        g2d,
                        getWidth(),
                        getHeight(),
                        mode.getPointCount(),
                        randomNumberProvider.getMode()
                );
            }
        } else {
            if (mode.usesPointCounterOverlay()) {
                drawPointCounter(g2d, dark, style);
            }
            if (mode.usesRngModeIndicatorOverlay()) {
                drawRngModeIndicator(g2d, dark, style);
            }
        }
        drawErrorMessage(g2d, dark, style);

        if (mode.usesRandomNumbersStackOverlay()) {
            RandomNumbersStackOverlay.draw(g, getWidth(), getHeight(), dark, style, randomNumberProvider);
        }
    }

    private void refreshReservedDrawingAreasIfNeeded() {
        if (offscreenImage == null || modeCanvasBounds == null) {
            return;
        }

        VisualizationStyle style = mode.getVisualizationStyle();
        if (style != VisualizationStyle.APPLE_MAC) {
            mode.setReservedDrawingAreas(List.of());
            return;
        }

        Graphics2D g2d = offscreenImage.createGraphics();
        try {
            updateReservedDrawingAreas(g2d, style);
        } finally {
            g2d.dispose();
        }
    }

    private Rectangle calculateModeCanvasBounds(
            Graphics2D g2d,
            int panelWidth,
            int panelHeight,
            VisualizationStyle style
    ) {
        if (style == VisualizationStyle.APPLE_MAC) {
            return AppleMacChrome.calculatePlotContentArea(g2d, panelWidth, panelHeight);
        }

        return new Rectangle(0, 0, panelWidth, panelHeight);
    }

    private void updateReservedDrawingAreas(Graphics2D g2d, VisualizationStyle style) {
        if (style != VisualizationStyle.APPLE_MAC || modeCanvasBounds == null) {
            mode.setReservedDrawingAreas(List.of());
            return;
        }

        Rectangle counterBlock = AppleMacChrome.calculateCounterBlockArea(
                g2d,
                getWidth(),
                getHeight(),
                mode.getPointCount(),
                randomNumberProvider.getMode()
        );
        Rectangle reservedArea = new Rectangle(
                counterBlock.x - modeCanvasBounds.x,
                counterBlock.y - modeCanvasBounds.y,
                counterBlock.width,
                counterBlock.height
        );

        reservedArea = reservedArea.intersection(new Rectangle(
                0,
                0,
                Math.max(MIN_CANVAS_SIZE, modeCanvasBounds.width),
                Math.max(MIN_CANVAS_SIZE, modeCanvasBounds.height)
        ));

        if (reservedArea.width > 0 && reservedArea.height > 0) {
            mode.setReservedDrawingAreas(List.of(reservedArea));
        } else {
            mode.setReservedDrawingAreas(List.of());
        }
    }

    private BufferedImage getModeCanvasForDrawing() {
        return modeCanvas != null ? modeCanvas : offscreenImage;
    }

    private Point toModeCanvasPoint(Point point) {
        if (point == null || modeCanvasBounds == null || !modeCanvasBounds.contains(point)) {
            return point;
        }

        return new Point(point.x - modeCanvasBounds.x, point.y - modeCanvasBounds.y);
    }

    private void drawInfoText(Graphics2D g2d, boolean dark, VisualizationStyle style) {
        g2d.setFont(style == VisualizationStyle.APPLE_MAC ? AppleMacChrome.INFO_FONT : INFO_FONT);
        g2d.setColor(resolveInfoColor(dark, style));

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

    private void drawPointCounter(Graphics2D g2d, boolean dark, VisualizationStyle style) {
        String pointCounterText = String.valueOf(mode.getPointCount());

        g2d.setFont(style == VisualizationStyle.APPLE_MAC ? AppleMacChrome.POINT_COUNTER_FONT : POINT_COUNTER_FONT);
        g2d.setColor(resolveCounterColor(dark, style));

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

    private void drawRngModeIndicator(Graphics2D g2d, boolean dark, VisualizationStyle style) {
        var rngMode = randomNumberProvider.getMode();
        boolean isQuantum = rngMode == RNProvider.Mode.QUANTUM;

        g2d.setFont(style == VisualizationStyle.APPLE_MAC ? AppleMacChrome.RNG_LABEL_FONT : RNG_LABEL_FONT);
        g2d.setColor(resolveRngColor(isQuantum, dark, style));

        String modeLabel = isQuantum ? RNG_MODE_LABEL_QUANTUM : RNG_MODE_LABEL_PSEUDO;
        g2d.drawString(modeLabel, RNG_LABEL_X, RNG_LABEL_Y);
    }

    private void drawErrorMessage(Graphics2D g2d, boolean dark, VisualizationStyle style) {
        if (errorMessage == null) {
            return;
        }

        g2d.setColor(resolveErrorColor(dark, style));
        g2d.setFont(ERROR_FONT);
        g2d.drawString(errorMessage, ERROR_TEXT_X, ERROR_TEXT_Y);
    }

    private Color resolvePanelBackgroundColor() {
        if (mode.getVisualizationStyle() == VisualizationStyle.APPLE_MAC) {
            return AppleMacChrome.BACKGROUND_COLOR;
        }

        return mode.usesDarkBackground() ? DARK_BACKGROUND_COLOR : LIGHT_BACKGROUND_COLOR;
    }

    public void updateStatusLabel(String message) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(message));
    }

    public List<Long> getUsedRandomNumbers() {
        return randomNumberProvider.getConsumedNumbers();
    }

    public int getUsedRandomNumberCount() {
        return randomNumberProvider.getConsumedCount();
    }

    public RNProvider getRandomNumberProvider() {
        return randomNumberProvider;
    }

    /**
     * Применяет смену стиля режима и полностью перерисовывает canvas.
     */
    public void applyModeStyle() {
        SwingUtilities.invokeLater(() -> {
            setBackground(resolvePanelBackgroundColor());
            applyWindowComponentStyle(mode.getVisualizationStyle());
            canvasInitialized = false;
            offscreenImage = null;
            modeCanvas = null;
            modeCanvasBounds = new Rectangle();
            mode.setReservedDrawingAreas(List.of());
            revalidate();
            repaint();
        });
    }

    private void applyWindowComponentStyle(VisualizationStyle style) {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window == null || style != VisualizationStyle.APPLE_MAC) {
            return;
        }

        AppleMacChrome.applyStyleRecursively(window, this);
        window.invalidate();
        window.validate();
        window.repaint();
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

            BufferedImage activeModeCanvas = getModeCanvasForDrawing();
            if (activeModeCanvas == null) {
                repaint();
                return;
            }

            mode.redraw(
                    activeModeCanvas,
                    activeModeCanvas.getWidth(),
                    activeModeCanvas.getHeight(),
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
