package org.ThreeDotsSierpinski.mode.chaos;

import org.ThreeDotsSierpinski.app.DotController;
import org.ThreeDotsSierpinski.config.Config;
import org.ThreeDotsSierpinski.math.SierpinskiAlgorithm;
import org.ThreeDotsSierpinski.mode.VisualizationMode;
import org.ThreeDotsSierpinski.mode.VisualizationStyle;
import org.ThreeDotsSierpinski.rng.RNProvider;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * Режим визуализации: треугольник Серпинского (Chaos Game).
 * <p>
 * Дополнительно поддерживает переключаемые визуальные стили:
 * - Default: текущий рабочий стиль проекта;
 * - AppleMac: ретро grayscale-style в духе ранних GUI-компьютеров.
 */
public class SierpinskiMode implements VisualizationMode {

    private static final String ID = "Sierpinski";
    private static final String NAME = "Sierpinski Triangle";
    private static final String DESCRIPTION =
            "Фрактал из хаоса: случайные числа определяют вершину,\n"
                    + "точка прыгает на полпути — и возникает треугольник Серпинского.";
    private static final String ICON = "△";

    private static final String ERROR_CANVAS_NULL = "Canvas cannot be null";
    private static final String ERROR_PROVIDER_NULL = "Provider cannot be null";
    private static final String ERROR_INVALID_CANVAS_SIZE = "Canvas size must be positive";

    private static final String DARK_MODE_TEXT = "Dark";
    private static final String DARK_MODE_TOOLTIP =
            "Switch Sierpinski Triangle between light and dark palette";

    private static final String STYLE_LABEL_TEXT = "Mod:";
    private static final String STYLE_TOOLTIP = "Choose visual style for Sierpinski UI";

    private static final boolean DEFAULT_DARK_MODE_ENABLED = true;
    private static final int DOTS_PER_STEP = Config.getInt("dots.per.update");
    private static final int CENTER_DIVISOR = 2;
    private static final int MIN_DOT_SIZE = 1;
    private static final int EMPTY_RESERVED_AREA_COUNT = 0;

    private static final Color LIGHT_BACKGROUND_COLOR = Color.WHITE;
    private static final Color DARK_BACKGROUND_COLOR = new Color(5, 10, 18);
    private static final Color APPLE_MAC_BACKGROUND_COLOR = new Color(238, 238, 236);

    private static final Color LIGHT_NEW_POINT_COLOR = Color.RED;
    private static final Color DARK_NEW_POINT_COLOR = new Color(255, 92, 122);
    private static final Color APPLE_MAC_NEW_POINT_COLOR = Color.BLACK;

    private static final Color LIGHT_STABLE_POINT_COLOR = Color.BLACK;
    private static final Color DARK_STABLE_POINT_COLOR = new Color(215, 240, 255);
    private static final Color APPLE_MAC_STABLE_POINT_COLOR = Color.BLACK;
    private final List<Point> pointHistory = new ArrayList<>();
    private final List<Rectangle> reservedDrawingAreas = new ArrayList<>();
    private SierpinskiAlgorithm algorithm;
    private Point currentPoint;
    private int pointCount = 0;
    private int randomNumbersUsed = 0;
    private boolean darkMode = DEFAULT_DARK_MODE_ENABLED;
    private VisualizationStyle visualizationStyle = VisualizationStyle.APPLE_MAC;

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
    public void initialize(BufferedImage canvas, int width, int height) {
        Objects.requireNonNull(canvas, ERROR_CANVAS_NULL);

        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(ERROR_INVALID_CANVAS_SIZE);
        }

        algorithm = new SierpinskiAlgorithm(width, height);
        currentPoint = new Point(width / CENTER_DIVISOR, height / CENTER_DIVISOR);
        pointCount = 0;
        randomNumbersUsed = 0;
        pointHistory.clear();

        clearCanvas(canvas);
    }

    @Override
    public List<Point> step(RNProvider provider, BufferedImage canvas, int dotSize) {
        Objects.requireNonNull(provider, ERROR_PROVIDER_NULL);
        Objects.requireNonNull(canvas, ERROR_CANVAS_NULL);

        ensureInitialized(canvas);

        int safeDotSize = Math.max(MIN_DOT_SIZE, dotSize);
        var newPoints = new ArrayList<Point>(DOTS_PER_STEP);

        Graphics2D g2d = canvas.createGraphics();
        try {
            g2d.setColor(newPointColor());

            for (int i = 0; i < DOTS_PER_STEP; i++) {
                OptionalInt randomOpt = provider.getNextRandomNumber();
                if (randomOpt.isEmpty()) {
                    break;
                }

                long randomValue = randomOpt.getAsInt();
                randomNumbersUsed++;

                currentPoint = algorithm.calculateNewDotPosition(currentPoint, randomValue);

                Point drawnPoint = new Point(currentPoint);
                if (!isInsideReservedDrawingArea(drawnPoint, safeDotSize)) {
                    pointHistory.add(drawnPoint);

                    g2d.fillRect(drawnPoint.x, drawnPoint.y, safeDotSize, safeDotSize);
                    newPoints.add(drawnPoint);
                    pointCount++;
                }
            }
        } finally {
            g2d.dispose();
        }

        return newPoints;
    }

    @Override
    public void redraw(BufferedImage canvas, int width, int height, int dotSize) {
        if (canvas == null) {
            return;
        }

        clearCanvas(canvas);

        Graphics2D g2d = canvas.createGraphics();
        try {
            g2d.setColor(stablePointColor());
            int safeDotSize = Math.max(MIN_DOT_SIZE, dotSize);

            for (Point point : pointHistory) {
                if (!isInsideReservedDrawingArea(point, safeDotSize)) {
                    g2d.fillRect(point.x, point.y, safeDotSize, safeDotSize);
                }
            }
        } finally {
            g2d.dispose();
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

    @Override
    public boolean usesLeftPointCounterOverlay() {
        return true;
    }

    @Override
    public List<JComponent> createModeControls(DotController controller) {
        JLabel styleLabel = new JLabel(STYLE_LABEL_TEXT);

        JComboBox<VisualizationStyle> styleComboBox = new JComboBox<>(new VisualizationStyle[]{
                VisualizationStyle.DEFAULT,
                VisualizationStyle.APPLE_MAC
        });
        styleComboBox.setSelectedItem(visualizationStyle);
        styleComboBox.setToolTipText(STYLE_TOOLTIP);
        styleComboBox.setFocusable(false);

        JCheckBox darkModeToggle = new JCheckBox(DARK_MODE_TEXT, darkMode);
        darkModeToggle.setToolTipText(DARK_MODE_TOOLTIP);
        darkModeToggle.setEnabled(!isAppleMacStyle());

        styleComboBox.addActionListener(ignored -> {
            Object selected = styleComboBox.getSelectedItem();
            if (selected instanceof VisualizationStyle selectedStyle) {
                visualizationStyle = selectedStyle;
            }

            darkModeToggle.setEnabled(!isAppleMacStyle());
            controller.applyModeStyle();
        });

        darkModeToggle.addActionListener(ignored -> {
            darkMode = darkModeToggle.isSelected();
            controller.applyModeStyle();
        });

        return List.of(styleLabel, styleComboBox, darkModeToggle);
    }

    @Override
    public void setReservedDrawingAreas(List<Rectangle> reservedAreas) {
        reservedDrawingAreas.clear();

        if (reservedAreas == null || reservedAreas.isEmpty()) {
            return;
        }

        for (Rectangle reservedArea : reservedAreas) {
            if (reservedArea != null && reservedArea.width > 0 && reservedArea.height > 0) {
                reservedDrawingAreas.add(new Rectangle(reservedArea));
            }
        }
    }

    @Override
    public boolean usesDarkBackground() {
        return visualizationStyle == VisualizationStyle.DEFAULT && darkMode;
    }

    @Override
    public boolean usesRandomNumbersStackOverlay() {
        return true;
    }

    @Override
    public Color getRecolorAnimationTargetColor() {
        return stablePointColor();
    }

    @Override
    public VisualizationStyle getVisualizationStyle() {
        return visualizationStyle;
    }

    private boolean isInsideReservedDrawingArea(Point point, int dotSize) {
        if (point == null || reservedDrawingAreas.size() == EMPTY_RESERVED_AREA_COUNT) {
            return false;
        }

        Rectangle dotBounds = new Rectangle(
                point.x,
                point.y,
                Math.max(MIN_DOT_SIZE, dotSize),
                Math.max(MIN_DOT_SIZE, dotSize)
        );

        for (Rectangle reservedArea : reservedDrawingAreas) {
            if (reservedArea.intersects(dotBounds)) {
                return true;
            }
        }

        return false;
    }

    private void ensureInitialized(BufferedImage canvas) {
        if (algorithm == null || currentPoint == null) {
            initialize(canvas, canvas.getWidth(), canvas.getHeight());
        }
    }

    private void clearCanvas(BufferedImage canvas) {
        Graphics2D g2d = canvas.createGraphics();
        try {
            g2d.setColor(backgroundColor());
            g2d.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        } finally {
            g2d.dispose();
        }
    }

    private boolean isAppleMacStyle() {
        return visualizationStyle == VisualizationStyle.APPLE_MAC;
    }

    private Color backgroundColor() {
        if (isAppleMacStyle()) {
            return APPLE_MAC_BACKGROUND_COLOR;
        }

        return darkMode ? DARK_BACKGROUND_COLOR : LIGHT_BACKGROUND_COLOR;
    }

    private Color newPointColor() {
        if (isAppleMacStyle()) {
            return APPLE_MAC_NEW_POINT_COLOR;
        }

        return darkMode ? DARK_NEW_POINT_COLOR : LIGHT_NEW_POINT_COLOR;
    }

    private Color stablePointColor() {
        if (isAppleMacStyle()) {
            return APPLE_MAC_STABLE_POINT_COLOR;
        }

        return darkMode ? DARK_STABLE_POINT_COLOR : LIGHT_STABLE_POINT_COLOR;
    }
}