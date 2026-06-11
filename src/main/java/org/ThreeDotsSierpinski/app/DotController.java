package org.ThreeDotsSierpinski.app;

import org.ThreeDotsSierpinski.app.*;
import org.ThreeDotsSierpinski.config.*;
import org.ThreeDotsSierpinski.math.*;
import org.ThreeDotsSierpinski.mode.*;
import org.ThreeDotsSierpinski.mode.chaos.*;
import org.ThreeDotsSierpinski.mode.montecarlo.*;
import org.ThreeDotsSierpinski.mode.physics.*;
import org.ThreeDotsSierpinski.mode.stochastic.*;
import org.ThreeDotsSierpinski.model.*;
import org.ThreeDotsSierpinski.rng.*;
import org.ThreeDotsSierpinski.stats.*;

import javax.swing.*;
import javax.swing.border.BevelBorder;
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

    private static final int APPLE_MAC_POINT_COUNTER_FONT_SIZE = 54;
    private static final int APPLE_MAC_RANDOM_STACK_FONT_SIZE = 12;
    private static final int APPLE_MAC_CONTROL_FONT_SIZE = 12;

    private static final int APPLE_MAC_INSET = 6;
    private static final int APPLE_MAC_INFO_SEPARATOR_Y = 28;
    private static final int APPLE_MAC_FRAME_INSET = 5;
    private static final int APPLE_MAC_FRAME_INNER_INSET = 3;
    private static final int APPLE_MAC_PLOT_CONTENT_INSET = 7;
    private static final int APPLE_MAC_TABLE_GAP = 8;
    private static final int APPLE_MAC_TABLE_TOP_MARGIN = 31;
    private static final int APPLE_MAC_TABLE_MAX_ROWS = 32;
    private static final int APPLE_MAC_RANDOM_STACK_CELL_HEIGHT = 16;
    private static final int APPLE_MAC_RANDOM_STACK_HEADER_HEIGHT = 22;
    private static final int APPLE_MAC_RANDOM_STACK_HEADER_Y_OFFSET = 6;
    private static final int APPLE_MAC_RANDOM_STACK_VALUES_Y_OFFSET = 6;
    private static final int APPLE_MAC_RANDOM_STACK_COLUMN_GAP = 0;
    private static final int APPLE_MAC_RANDOM_STACK_CELL_HORIZONTAL_PADDING = 6;
    private static final int APPLE_MAC_RANDOM_STACK_RIGHT_TRIM = 2;
    private static final int APPLE_MAC_CANVAS_BOTTOM_MARGIN = 8;
    private static final int APPLE_MAC_CANVAS_TOP = APPLE_MAC_INFO_SEPARATOR_Y + 4;

    private static final int APPLE_MAC_COUNTER_BLOCK_MARGIN = 8;
    private static final int APPLE_MAC_COUNTER_BLOCK_PADDING = 10;
    private static final int APPLE_MAC_COUNTER_BLOCK_MIN_WIDTH = 250;
    private static final int APPLE_MAC_COUNTER_BLOCK_HEIGHT = 100;
    private static final int APPLE_MAC_COUNTER_LABEL_BASELINE_OFFSET = 18;
    private static final int APPLE_MAC_COUNTER_VALUE_BASELINE_OFFSET = 64;
    private static final int APPLE_MAC_COUNTER_SEPARATOR_OFFSET = 72;
    private static final int APPLE_MAC_COUNTER_RNG_BASELINE_OFFSET = 91;

    private static final String FONT_SANS_SERIF = "SansSerif";
    private static final String FONT_MONOSPACED = "Monospaced";
    private static final String FONT_APPLE_MAC = "DialogInput";

    private static final Font INFO_FONT = new Font(FONT_SANS_SERIF, Font.PLAIN, INFO_FONT_SIZE);
    private static final Font POINT_COUNTER_FONT = new Font(FONT_SANS_SERIF, Font.BOLD, POINT_COUNTER_FONT_SIZE);
    private static final Font RNG_LABEL_FONT = new Font(FONT_SANS_SERIF, Font.BOLD, RNG_LABEL_FONT_SIZE);
    private static final Font ERROR_FONT = new Font(FONT_SANS_SERIF, Font.PLAIN, ERROR_FONT_SIZE);

    private static final Font APPLE_MAC_INFO_FONT = new Font(FONT_APPLE_MAC, Font.BOLD, INFO_FONT_SIZE);
    private static final Font APPLE_MAC_POINT_COUNTER_FONT =
            new Font(FONT_APPLE_MAC, Font.BOLD, APPLE_MAC_POINT_COUNTER_FONT_SIZE);
    private static final Font APPLE_MAC_RNG_LABEL_FONT = new Font(FONT_APPLE_MAC, Font.BOLD, RNG_LABEL_FONT_SIZE);
    private static final Font APPLE_MAC_RANDOM_STACK_FONT =
            new Font(FONT_APPLE_MAC, Font.BOLD, APPLE_MAC_RANDOM_STACK_FONT_SIZE);
    private static final Font APPLE_MAC_CONTROL_FONT =
            new Font(FONT_APPLE_MAC, Font.BOLD, APPLE_MAC_CONTROL_FONT_SIZE);

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

    private static final Color DARK_RANDOM_STACK_HEADER_COLOR = new Color(142, 163, 188);
    private static final Color DARK_RANDOM_STACK_VALUE_COLOR = new Color(230, 238, 248);
    private static final Color DARK_RANDOM_STACK_ROW_BACKGROUND = new Color(14, 26, 42, 220);

    private static final Color RANDOM_STACK_HEADER_COLOR = new Color(130, 130, 130);
    private static final Color RANDOM_STACK_VALUE_COLOR = Color.BLACK;
    private static final Color RANDOM_STACK_ROW_BACKGROUND = new Color(245, 245, 245);

    private static final Color APPLE_MAC_BACKGROUND_COLOR = new Color(238, 238, 236);
    private static final Color APPLE_MAC_PANEL_BACKGROUND_COLOR = new Color(225, 225, 225);
    private static final Color APPLE_MAC_TEXT_COLOR = Color.BLACK;
    private static final Color APPLE_MAC_BORDER_COLOR = Color.BLACK;
    private static final Color APPLE_MAC_HIGHLIGHT_COLOR = Color.WHITE;
    private static final Color APPLE_MAC_SHADOW_COLOR = new Color(110, 110, 110);
    private static final Color APPLE_MAC_STACK_ROW_BACKGROUND = new Color(248, 248, 248);
    private static final Color APPLE_MAC_STACK_HEADER_BACKGROUND = new Color(230, 230, 230);

    private static final String RNG_LABEL_QUANTUM_STATUS = "QUANTUM (API)";
    private static final String RNG_LABEL_PSEUDO_STATUS = "PSEUDO (Local)";

    private static final String RNG_MODE_LABEL_QUANTUM = "■ QUANTUM";
    private static final String RNG_MODE_LABEL_PSEUDO = "■ PSEUDO (L128X256MixRandom)";

    private static final String INFO_SEPARATOR = "  |  ";
    private static final String POINTS_LABEL = "Points: ";
    private static final String RANDOM_NUMBERS_LABEL = "Random numbers: ";
    private static final String APPLE_MAC_COUNTER_BLOCK_TITLE = "POINTS";

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
    private VisualizationStyle initializedStyle = null;
    private BufferedImage modeCanvas;
    private Rectangle modeCanvasBounds = new Rectangle();
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
            drawAppleMacFrame(g2d);
        }

        drawInfoText(g2d, dark, style);
        if (style == VisualizationStyle.APPLE_MAC) {
            drawAppleMacCounterBlock(g2d);
        } else {
            drawPointCounter(g2d, dark, style);
            drawRngModeIndicator(g2d, dark, style);
        }
        drawErrorMessage(g2d, dark, style);

        if (mode.usesRandomNumbersStackOverlay()) {
            drawRandomNumbersStack(g, dark, style);
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
            return calculateAppleMacPlotContentArea(g2d, panelWidth, panelHeight);
        }

        return new Rectangle(0, 0, panelWidth, panelHeight);
    }

    private void updateReservedDrawingAreas(Graphics2D g2d, VisualizationStyle style) {
        if (style != VisualizationStyle.APPLE_MAC || modeCanvasBounds == null) {
            mode.setReservedDrawingAreas(List.of());
            return;
        }

        Rectangle counterBlock = calculateAppleMacCounterBlockArea(g2d);
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

    private static BufferedImage createModeCanvasView(BufferedImage image, Rectangle bounds) {
        int safeX = Math.clamp(bounds.x, 0, Math.max(0, image.getWidth() - MIN_CANVAS_SIZE));
        int safeY = Math.clamp(bounds.y, 0, Math.max(0, image.getHeight() - MIN_CANVAS_SIZE));
        int safeWidth = Math.clamp(bounds.width, MIN_CANVAS_SIZE, image.getWidth() - safeX);
        int safeHeight = Math.clamp(bounds.height, MIN_CANVAS_SIZE, image.getHeight() - safeY);

        return image.getSubimage(safeX, safeY, safeWidth, safeHeight);
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

    private Rectangle calculateAppleMacPlotArea(Graphics2D g2d, int panelWidth, int panelHeight) {
        int plotX = APPLE_MAC_FRAME_INSET;
        int plotY = APPLE_MAC_CANVAS_TOP;
        int sidebarX = calculateAppleMacSidebarX(g2d, panelWidth);

        int plotWidth = Math.max(
                MIN_CANVAS_SIZE,
                sidebarX - APPLE_MAC_TABLE_GAP - plotX
        );
        int plotHeight = Math.max(
                MIN_CANVAS_SIZE,
                panelHeight - plotY - APPLE_MAC_CANVAS_BOTTOM_MARGIN
        );

        return new Rectangle(plotX, plotY, plotWidth, plotHeight);
    }


    private Rectangle calculateAppleMacPlotContentArea(Graphics2D g2d, int panelWidth, int panelHeight) {
        Rectangle plotArea = calculateAppleMacPlotArea(g2d, panelWidth, panelHeight);
        int inset = APPLE_MAC_PLOT_CONTENT_INSET;

        return new Rectangle(
                plotArea.x + inset,
                plotArea.y + inset,
                Math.max(MIN_CANVAS_SIZE, plotArea.width - inset * 2),
                Math.max(MIN_CANVAS_SIZE, plotArea.height - inset * 2)
        );
    }

    private Rectangle calculateAppleMacSidebarArea(Graphics2D g2d, int panelWidth, int panelHeight) {
        int sidebarX = calculateAppleMacSidebarX(g2d, panelWidth);
        int sidebarY = APPLE_MAC_CANVAS_TOP;
        int sidebarWidth = Math.max(
                MIN_CANVAS_SIZE,
                panelWidth - APPLE_MAC_FRAME_INSET - sidebarX
        );
        int sidebarHeight = Math.max(
                MIN_CANVAS_SIZE,
                panelHeight - sidebarY - APPLE_MAC_CANVAS_BOTTOM_MARGIN
        );

        return new Rectangle(sidebarX, sidebarY, sidebarWidth, sidebarHeight);
    }

    private static int calculateAppleMacSidebarX(Graphics2D g2d, int panelWidth) {
        int stackX = calculateAppleMacStackX(g2d, panelWidth);
        return Math.max(
                APPLE_MAC_FRAME_INSET + MIN_CANVAS_SIZE + APPLE_MAC_TABLE_GAP,
                stackX - APPLE_MAC_FRAME_INNER_INSET - 2
        );
    }

    private static int calculateAppleMacStackX(Graphics2D g2d, int panelWidth) {
        int stackWidth = calculateRandomStackWidth(g2d, VisualizationStyle.APPLE_MAC);
        int rightMargin = getRandomStackRightMargin(VisualizationStyle.APPLE_MAC);

        return Math.max(
                rightMargin,
                panelWidth - rightMargin - stackWidth
        );
    }

    private void drawAppleMacFrame(Graphics2D g2d) {
        drawAppleMacDoubleFrame(
                g2d,
                APPLE_MAC_INSET,
                APPLE_MAC_INSET,
                getWidth() - APPLE_MAC_INSET * 2 - 1,
                getHeight() - APPLE_MAC_INSET * 2 - 1
        );

        drawAppleMacTitleStrip(g2d, getWidth());

        Rectangle plotArea = calculateAppleMacPlotArea(g2d, getWidth(), getHeight());
        Rectangle sidebarArea = calculateAppleMacSidebarArea(g2d, getWidth(), getHeight());

        drawAppleMacDoubleFrame(g2d, plotArea.x, plotArea.y, plotArea.width, plotArea.height);
        drawAppleMacDoubleFrame(g2d, sidebarArea.x, sidebarArea.y, sidebarArea.width, sidebarArea.height);
    }

    private static void drawAppleMacTitleStrip(Graphics2D g2d, int panelWidth) {
        g2d.setColor(APPLE_MAC_PANEL_BACKGROUND_COLOR);
        g2d.fillRect(
                APPLE_MAC_INSET + 1,
                APPLE_MAC_INSET + 1,
                Math.max(0, panelWidth - APPLE_MAC_INSET * 2 - 2),
                APPLE_MAC_INFO_SEPARATOR_Y - APPLE_MAC_INSET - 1
        );

        g2d.setColor(APPLE_MAC_BORDER_COLOR);
        g2d.drawLine(APPLE_MAC_INSET, APPLE_MAC_INFO_SEPARATOR_Y,
                panelWidth - APPLE_MAC_INSET - 1, APPLE_MAC_INFO_SEPARATOR_Y);
    }

    private static void drawAppleMacDoubleFrame(Graphics2D g2d, int x, int y, int width, int height) {
        g2d.setColor(APPLE_MAC_BORDER_COLOR);
        g2d.drawRect(x, y, width, height);
        g2d.drawRect(
                x + APPLE_MAC_FRAME_INNER_INSET,
                y + APPLE_MAC_FRAME_INNER_INSET,
                Math.max(0, width - APPLE_MAC_FRAME_INNER_INSET * 2),
                Math.max(0, height - APPLE_MAC_FRAME_INNER_INSET * 2)
        );
    }

    private void drawInfoText(Graphics2D g2d, boolean dark, VisualizationStyle style) {
        g2d.setFont(style == VisualizationStyle.APPLE_MAC ? APPLE_MAC_INFO_FONT : INFO_FONT);
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

    private void drawAppleMacCounterBlock(Graphics2D g2d) {
        Rectangle block = calculateAppleMacCounterBlockArea(g2d);

        g2d.setColor(APPLE_MAC_PANEL_BACKGROUND_COLOR);
        g2d.fillRect(block.x, block.y, block.width, block.height);
        drawAppleMacDoubleFrame(g2d, block.x, block.y, block.width, block.height);

        int textX = block.x + APPLE_MAC_COUNTER_BLOCK_PADDING;

        g2d.setFont(APPLE_MAC_INFO_FONT);
        g2d.setColor(APPLE_MAC_TEXT_COLOR);
        g2d.drawString(APPLE_MAC_COUNTER_BLOCK_TITLE, textX, block.y + APPLE_MAC_COUNTER_LABEL_BASELINE_OFFSET);

        g2d.setFont(APPLE_MAC_POINT_COUNTER_FONT);
        g2d.drawString(
                String.valueOf(mode.getPointCount()),
                textX,
                block.y + APPLE_MAC_COUNTER_VALUE_BASELINE_OFFSET
        );

        g2d.drawLine(
                block.x + APPLE_MAC_FRAME_INNER_INSET,
                block.y + APPLE_MAC_COUNTER_SEPARATOR_OFFSET,
                block.x + block.width - APPLE_MAC_FRAME_INNER_INSET,
                block.y + APPLE_MAC_COUNTER_SEPARATOR_OFFSET
        );

        g2d.setFont(APPLE_MAC_RNG_LABEL_FONT);
        String modeLabel = randomNumberProvider.getMode() == RNProvider.Mode.QUANTUM
                ? RNG_MODE_LABEL_QUANTUM
                : RNG_MODE_LABEL_PSEUDO;
        g2d.drawString(modeLabel, textX, block.y + APPLE_MAC_COUNTER_RNG_BASELINE_OFFSET);
    }

    private Rectangle calculateAppleMacCounterBlockArea(Graphics2D g2d) {
        Rectangle plotArea = calculateAppleMacPlotArea(g2d, getWidth(), getHeight());

        g2d.setFont(APPLE_MAC_POINT_COUNTER_FONT);
        int counterWidth = g2d.getFontMetrics().stringWidth(String.valueOf(mode.getPointCount()));

        g2d.setFont(APPLE_MAC_RNG_LABEL_FONT);
        String modeLabel = randomNumberProvider.getMode() == RNProvider.Mode.QUANTUM
                ? RNG_MODE_LABEL_QUANTUM
                : RNG_MODE_LABEL_PSEUDO;
        int rngWidth = g2d.getFontMetrics().stringWidth(modeLabel);

        int preferredWidth = Math.max(
                APPLE_MAC_COUNTER_BLOCK_MIN_WIDTH,
                Math.max(counterWidth, rngWidth) + APPLE_MAC_COUNTER_BLOCK_PADDING * 2
        );
        int maxWidth = Math.max(
                APPLE_MAC_COUNTER_BLOCK_MIN_WIDTH,
                plotArea.width - APPLE_MAC_COUNTER_BLOCK_MARGIN * 2
        );

        int width = Math.min(preferredWidth, maxWidth);
        int height = Math.clamp(plotArea.height - APPLE_MAC_COUNTER_BLOCK_MARGIN * 2, MIN_CANVAS_SIZE,
                APPLE_MAC_COUNTER_BLOCK_HEIGHT);

        return new Rectangle(
                plotArea.x + APPLE_MAC_COUNTER_BLOCK_MARGIN,
                plotArea.y + APPLE_MAC_COUNTER_BLOCK_MARGIN,
                width,
                height
        );
    }

    private void drawPointCounter(Graphics2D g2d, boolean dark, VisualizationStyle style) {
        String pointCounterText = String.valueOf(mode.getPointCount());

        g2d.setFont(style == VisualizationStyle.APPLE_MAC ? APPLE_MAC_POINT_COUNTER_FONT : POINT_COUNTER_FONT);
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

        g2d.setFont(style == VisualizationStyle.APPLE_MAC ? APPLE_MAC_RNG_LABEL_FONT : RNG_LABEL_FONT);
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

    private void drawRandomNumbersStack(Graphics g, boolean dark, VisualizationStyle style) {
        List<Long> numbers = randomNumberProvider.getConsumedNumbers();
        if (numbers.isEmpty()) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Map<Integer, List<Long>> numbersByDigits = groupNumbersByDigitCount(numbers);

            int stackWidth = calculateRandomStackWidth(g2d, style);
            int currentX = style == VisualizationStyle.APPLE_MAC
                    ? calculateAppleMacStackX(g2d, getWidth())
                    : Math.max(
                            getRandomStackRightMargin(style),
                            getWidth() - getRandomStackRightMargin(style) - stackWidth
                    );
            int startX = currentX;
            int startY = getRandomStackTopMargin(style);
            int visibleRows = calculateVisibleRandomStackRows(startY, style);
            int stackFrameY = startY + getRandomStackFrameTopOffset(style);
            int stackHeight = getRandomStackFrameHeight(style, visibleRows);

            for (int digitCount = MIN_DIGIT_GROUP; digitCount <= MAX_DIGIT_GROUP; digitCount++) {
                List<Long> columnNumbers = numbersByDigits.getOrDefault(digitCount, List.of());

                int columnWidth = calculateDigitColumnWidth(g2d, digitCount, style);
                drawDigitColumn(g2d, columnNumbers, digitCount, currentX, startY, columnWidth, dark, style);

                currentX += columnWidth + getRandomStackColumnGap(style);
            }

            if (style == VisualizationStyle.APPLE_MAC) {
                drawRandomNumbersStackOuterBorder(g2d, startX, stackFrameY, stackWidth, stackHeight);
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

    private static int calculateRandomStackWidth(Graphics2D g2d, VisualizationStyle style) {
        int totalWidth = 0;

        for (int digitCount = MIN_DIGIT_GROUP; digitCount <= MAX_DIGIT_GROUP; digitCount++) {
            if (digitCount > MIN_DIGIT_GROUP) {
                totalWidth += getRandomStackColumnGap(style);
            }

            totalWidth += calculateDigitColumnWidth(g2d, digitCount, style);
        }

        return totalWidth;
    }

    private static int calculateDigitColumnWidth(Graphics2D g2d, int digitCount, VisualizationStyle style) {
        Font valueFont = style == VisualizationStyle.APPLE_MAC
                ? APPLE_MAC_RANDOM_STACK_FONT
                : RANDOM_STACK_VALUE_FONT;
        FontMetrics valueMetrics = g2d.getFontMetrics(valueFont);

        int digitWidth = valueMetrics.charWidth('0');
        int valueWidth = digitWidth * digitCount;

        int width = valueWidth + getRandomStackCellHorizontalPadding(style) * 2;

        if (style == VisualizationStyle.APPLE_MAC && digitCount == MAX_DIGIT_GROUP) {
            width -= APPLE_MAC_RANDOM_STACK_RIGHT_TRIM;
        }

        return Math.max(MIN_CANVAS_SIZE, width);
    }

    private void drawDigitColumn(
            Graphics2D g2d,
            List<Long> numbers,
            int digitCount,
            int x,
            int y,
            int columnWidth,
            boolean dark,
            VisualizationStyle style
    ) {
        int headerY = y + getRandomStackHeaderYOffset(style);
        drawDigitColumnHeader(g2d, digitCount, x, headerY, columnWidth, dark, style);

        int visibleRows = calculateVisibleRandomStackRows(y, style);
        int fromIndex = Math.max(0, numbers.size() - visibleRows);
        List<Long> visibleNumbers = numbers.subList(fromIndex, numbers.size());

        int rowY = y + getRandomStackRowsTopOffset(style);

        for (int rowIndex = 0; rowIndex < visibleRows; rowIndex++) {
            Long number = rowIndex < visibleNumbers.size() ? visibleNumbers.get(rowIndex) : null;
            drawDigitColumnValue(g2d, number, digitCount, x, rowY, columnWidth, dark, style);
            rowY += getRandomStackCellHeight(style);
        }
    }

    private static void drawDigitColumnHeader(
            Graphics2D g2d,
            int digitCount,
            int x,
            int y,
            int columnWidth,
            boolean dark,
            VisualizationStyle style
    ) {
        String header = digitCount + "d";

        if (style == VisualizationStyle.APPLE_MAC) {
            g2d.setColor(APPLE_MAC_STACK_HEADER_BACKGROUND);
            g2d.fillRect(x, y, columnWidth, getRandomStackHeaderHeight(style) - 1);
        }

        g2d.setFont(style == VisualizationStyle.APPLE_MAC ? APPLE_MAC_RANDOM_STACK_FONT : RANDOM_STACK_HEADER_FONT);
        g2d.setColor(resolveRandomStackHeaderColor(dark, style));

        FontMetrics metrics = g2d.getFontMetrics();
        int textX = x + Math.max(0, (columnWidth - metrics.stringWidth(header)) / 2);
        int textY = y + metrics.getAscent();

        g2d.drawString(header, textX, textY);

        if (style == VisualizationStyle.APPLE_MAC) {
            g2d.setColor(APPLE_MAC_BORDER_COLOR);
            g2d.drawRect(
                    x,
                    y,
                    Math.max(0, columnWidth - 1),
                    getRandomStackHeaderHeight(style) - 1
            );
        }
    }

    private static void drawDigitColumnValue(
            Graphics2D g2d,
            Long number,
            int digitCount,
            int x,
            int y,
            int columnWidth,
            boolean dark,
            VisualizationStyle style
    ) {
        String text = number == null ? null : formatNumberForDigitColumn(number, digitCount);

        g2d.setColor(resolveRandomStackRowBackground(dark, style));
        g2d.fillRect(x, y, columnWidth, getRandomStackCellHeight(style) - 1);

        if (text != null) {
            g2d.setFont(style == VisualizationStyle.APPLE_MAC ? APPLE_MAC_RANDOM_STACK_FONT : RANDOM_STACK_VALUE_FONT);
            g2d.setColor(resolveRandomStackValueColor(dark, style));

            FontMetrics metrics = g2d.getFontMetrics();

            int textX = x + columnWidth - getRandomStackCellHorizontalPadding(style) - metrics.stringWidth(text);
            int textY = y + metrics.getAscent();

            g2d.drawString(text, textX, textY);
        }

        if (style == VisualizationStyle.APPLE_MAC) {
            g2d.setColor(APPLE_MAC_BORDER_COLOR);
            g2d.drawRect(
                    x,
                    y,
                    Math.max(0, columnWidth - 1),
                    getRandomStackCellHeight(style) - 1
            );
        }
    }

    private static String formatNumberForDigitColumn(long number, int digitCount) {
        return String.format(Locale.US, "%" + digitCount + "d", number);
    }

    private int calculateVisibleRandomStackRows(int startY, VisualizationStyle style) {
        int availableHeight = Math.max(
                0,
                getHeight()
                        - startY
                        - getRandomStackRowsTopOffset(style)
                        - getRandomStackBottomReservedSpace(style)
        );

        int visibleRows = Math.max(1, availableHeight / getRandomStackCellHeight(style));
        if (style == VisualizationStyle.APPLE_MAC) {
            return Math.min(APPLE_MAC_TABLE_MAX_ROWS, visibleRows);
        }

        return visibleRows;
    }

    private static int getRandomStackTopMargin(VisualizationStyle style) {
        return style == VisualizationStyle.APPLE_MAC
                ? APPLE_MAC_TABLE_TOP_MARGIN
                : RANDOM_STACK_TOP_MARGIN;
    }

    private static int getRandomStackRightMargin(VisualizationStyle style) {
        return style == VisualizationStyle.APPLE_MAC
                ? APPLE_MAC_FRAME_INSET + APPLE_MAC_FRAME_INNER_INSET
                : RANDOM_STACK_RIGHT_MARGIN;
    }

    private static int getRandomStackColumnGap(VisualizationStyle style) {
        return style == VisualizationStyle.APPLE_MAC
                ? APPLE_MAC_RANDOM_STACK_COLUMN_GAP
                : RANDOM_STACK_COLUMN_GAP;
    }

    private static int getRandomStackValuesYOffset(VisualizationStyle style) {
        return style == VisualizationStyle.APPLE_MAC
                ? APPLE_MAC_RANDOM_STACK_VALUES_Y_OFFSET
                : 0;
    }

    private static int getRandomStackHeaderHeight(VisualizationStyle style) {
        return style == VisualizationStyle.APPLE_MAC
                ? APPLE_MAC_RANDOM_STACK_HEADER_HEIGHT
                : RANDOM_STACK_HEADER_HEIGHT;
    }

    private static int getRandomStackCellHeight(VisualizationStyle style) {
        return style == VisualizationStyle.APPLE_MAC
                ? APPLE_MAC_RANDOM_STACK_CELL_HEIGHT
                : RANDOM_STACK_CELL_HEIGHT;
    }

    private static int getRandomStackCellHorizontalPadding(VisualizationStyle style) {
        return style == VisualizationStyle.APPLE_MAC
                ? APPLE_MAC_RANDOM_STACK_CELL_HORIZONTAL_PADDING
                : RANDOM_STACK_CELL_HORIZONTAL_PADDING;
    }

    private static int getRandomStackBottomReservedSpace(VisualizationStyle style) {
        return style == VisualizationStyle.APPLE_MAC
                ? Math.max(RANDOM_STACK_BOTTOM_RESERVED_SPACE, getRandomStackCellHeight(style) * 10)
                : RANDOM_STACK_BOTTOM_RESERVED_SPACE;
    }

    private static void drawRandomNumbersStackOuterBorder(
            Graphics2D g2d,
            int x,
            int y,
            int width,
            int height
    ) {
        g2d.setColor(APPLE_MAC_BORDER_COLOR);
        g2d.drawRect(
                x,
                y,
                Math.max(0, width - 1),
                Math.max(0, height - 1)
        );
    }

    private Color resolvePanelBackgroundColor() {
        if (mode.getVisualizationStyle() == VisualizationStyle.APPLE_MAC) {
            return APPLE_MAC_BACKGROUND_COLOR;
        }

        return mode.usesDarkBackground() ? DARK_BACKGROUND_COLOR : LIGHT_BACKGROUND_COLOR;
    }

    private static Color resolveInfoColor(boolean dark, VisualizationStyle style) {
        if (style == VisualizationStyle.APPLE_MAC) {
            return APPLE_MAC_TEXT_COLOR;
        }

        return dark ? DARK_INFO_COLOR : LIGHT_INFO_COLOR;
    }

    private static Color resolveCounterColor(boolean dark, VisualizationStyle style) {
        if (style == VisualizationStyle.APPLE_MAC) {
            return APPLE_MAC_TEXT_COLOR;
        }

        return dark ? DARK_COUNTER_COLOR : LIGHT_COUNTER_COLOR;
    }

    private static Color resolveRngColor(boolean quantum, boolean dark, VisualizationStyle style) {
        if (style == VisualizationStyle.APPLE_MAC) {
            return APPLE_MAC_TEXT_COLOR;
        }

        if (quantum) {
            return dark ? DARK_QUANTUM_COLOR : LIGHT_QUANTUM_COLOR;
        }

        return dark ? DARK_PSEUDO_COLOR : LIGHT_PSEUDO_COLOR;
    }

    private static Color resolveErrorColor(boolean dark, VisualizationStyle style) {
        if (style == VisualizationStyle.APPLE_MAC) {
            return APPLE_MAC_TEXT_COLOR;
        }

        return dark ? DARK_ERROR_COLOR : LIGHT_ERROR_COLOR;
    }

    private static Color resolveRandomStackHeaderColor(boolean dark, VisualizationStyle style) {
        if (style == VisualizationStyle.APPLE_MAC) {
            return APPLE_MAC_TEXT_COLOR;
        }

        return dark ? DARK_RANDOM_STACK_HEADER_COLOR : RANDOM_STACK_HEADER_COLOR;
    }

    private static Color resolveRandomStackValueColor(boolean dark, VisualizationStyle style) {
        if (style == VisualizationStyle.APPLE_MAC) {
            return APPLE_MAC_TEXT_COLOR;
        }

        return dark ? DARK_RANDOM_STACK_VALUE_COLOR : RANDOM_STACK_VALUE_COLOR;
    }

    private static Color resolveRandomStackRowBackground(boolean dark, VisualizationStyle style) {
        if (style == VisualizationStyle.APPLE_MAC) {
            return APPLE_MAC_STACK_ROW_BACKGROUND;
        }

        return dark ? DARK_RANDOM_STACK_ROW_BACKGROUND : RANDOM_STACK_ROW_BACKGROUND;
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

        applyAppleMacStyleRecursively(window);
        window.invalidate();
        window.validate();
        window.repaint();
    }

    private static void applyAppleMacStyleRecursively(Component component) {
        applyAppleMacStyle(component);

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                applyAppleMacStyleRecursively(child);
            }
        }
    }

    private static void applyAppleMacStyle(Component component) {
        if (component instanceof JScrollPane scrollPane) {
            scrollPane.setBorder(BorderFactory.createLineBorder(APPLE_MAC_BORDER_COLOR));
            scrollPane.getViewport().setBackground(APPLE_MAC_PANEL_BACKGROUND_COLOR);
        }

        if (component instanceof JPanel panel && !(component instanceof DotController)) {
            panel.setBackground(APPLE_MAC_PANEL_BACKGROUND_COLOR);
        }

        if (component instanceof JLabel label) {
            label.setFont(APPLE_MAC_CONTROL_FONT);
            label.setForeground(APPLE_MAC_TEXT_COLOR);
            label.setBackground(APPLE_MAC_PANEL_BACKGROUND_COLOR);
        }

        if (component instanceof AbstractButton button) {
            button.setFont(APPLE_MAC_CONTROL_FONT);
            button.setForeground(APPLE_MAC_TEXT_COLOR);
            button.setBackground(APPLE_MAC_PANEL_BACKGROUND_COLOR);
            button.setFocusPainted(true);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(APPLE_MAC_BORDER_COLOR),
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createBevelBorder(
                                    BevelBorder.RAISED,
                                    APPLE_MAC_HIGHLIGHT_COLOR,
                                    APPLE_MAC_SHADOW_COLOR
                            ),
                            BorderFactory.createEmptyBorder(2, 12, 2, 12)
                    )
            ));
        }

        if (component instanceof JComboBox<?> comboBox) {
            comboBox.setFont(APPLE_MAC_CONTROL_FONT);
            comboBox.setForeground(APPLE_MAC_TEXT_COLOR);
            comboBox.setBackground(APPLE_MAC_BACKGROUND_COLOR);
            comboBox.setBorder(BorderFactory.createLineBorder(APPLE_MAC_BORDER_COLOR));
        }
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

    private static int getRandomStackHeaderYOffset(VisualizationStyle style) {
        return style == VisualizationStyle.APPLE_MAC
                ? APPLE_MAC_RANDOM_STACK_HEADER_Y_OFFSET
                : 0;
    }

    private static int getRandomStackValuesTopOffset(VisualizationStyle style) {
        return getRandomStackValuesYOffset(style) + getRandomStackHeaderHeight(style);
    }

    private static int getRandomStackRowsTopOffset(VisualizationStyle style) {
        return Math.max(
                getRandomStackHeaderYOffset(style) + getRandomStackHeaderHeight(style),
                getRandomStackValuesTopOffset(style)
        );
    }

    private static int getRandomStackFrameTopOffset(VisualizationStyle style) {
        return Math.min(
                getRandomStackHeaderYOffset(style),
                getRandomStackValuesTopOffset(style)
        );
    }

    private static int getRandomStackFrameHeight(VisualizationStyle style, int visibleRows) {
        int frameTopOffset = getRandomStackFrameTopOffset(style);
        int frameBottomOffset = getRandomStackRowsTopOffset(style)
                + visibleRows * getRandomStackCellHeight(style);

        return Math.max(0, frameBottomOffset - frameTopOffset);
    }
}