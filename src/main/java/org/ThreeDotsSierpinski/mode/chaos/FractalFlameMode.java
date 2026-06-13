package org.ThreeDotsSierpinski.mode.chaos;

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
 * Fractal Flame style chaos-game visualization.
 *
 * <p>Random numbers choose weighted nonlinear transformations. The resulting
 * density slowly builds a luminous IFS-like flame on a dark background.</p>
 */
public class FractalFlameMode implements VisualizationMode {

    private static final String ID = "fractal-flame";
    private static final String NAME = "Fractal Flame";
    private static final String DESCRIPTION =
            "Nonlinear IFS / chaos-game renderer with luminous density trails.\n"
                    + "QRNG selects flame transforms: swirl, sinusoidal and bubble-like variations.";
    private static final String ICON = "✺";

    private static final double RANDOM_MAX = 65_535.0;
    private static final int SAMPLES_PER_STEP = 1_200;
    private static final int QUANTUM_SAMPLES_PER_STEP = 32;
    private static final int WARMUP_STEPS = 20;

    private static final int OUTER_PADDING = 18;
    private static final int HEADER_HEIGHT = 76;
    private static final int FOOTER_HEIGHT = 54;
    private static final int PANEL_RADIUS = 20;
    private static final int CONTROL_HEIGHT = 28;
    private static final int RESET_BUTTON_WIDTH = 82;
    private static final int PRESET_COMBO_WIDTH = 118;

    private static final String RESET_TEXT = "Reset";
    private static final String PRESET_LABEL_TEXT = "Flame";
    private static final String RESET_TOOLTIP = "Restart Fractal Flame accumulation";
    private static final String PRESET_TOOLTIP = "Choose a predefined nonlinear flame variation mix";
    private static final String[] PRESETS = {"Swirl", "Sinusoidal", "Bubble"};

    private static final Color BACKGROUND = new Color(3, 4, 10);
    private static final Color PANEL_BACKGROUND = new Color(10, 11, 24);
    private static final Color PANEL_BORDER = new Color(84, 80, 135, 180);
    private static final Color TITLE_COLOR = new Color(245, 240, 255);
    private static final Color TEXT_PRIMARY = new Color(228, 224, 246);
    private static final Color TEXT_MUTED = new Color(154, 144, 190);
    private static final Color TEXT_ACCENT = new Color(255, 180, 112);

    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 27);
    private static final Font SUBTITLE_FONT = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font VALUE_FONT = new Font("Monospaced", Font.PLAIN, 12);

    private int width;
    private int height;
    private int pointCount;
    private int randomNumbersUsed;
    private int transformCount;
    private int rejectedCount;
    private double currentX = 0.12;
    private double currentY = -0.23;
    private String preset = PRESETS[0];

    private Rectangle headerBounds = new Rectangle();
    private Rectangle plotBounds = new Rectangle();
    private Rectangle footerBounds = new Rectangle();
    private BufferedImage chromeLayer;
    private BufferedImage flameLayer;
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

        JLabel presetLabel = new JLabel(PRESET_LABEL_TEXT);
        JComboBox<String> presetComboBox = new JComboBox<>(PRESETS);
        presetComboBox.setSelectedItem(preset);
        presetComboBox.setPreferredSize(new Dimension(PRESET_COMBO_WIDTH, CONTROL_HEIGHT));
        presetComboBox.setToolTipText(PRESET_TOOLTIP);
        presetComboBox.addActionListener(event -> {
            Object selected = presetComboBox.getSelectedItem();
            if (selected instanceof String selectedPreset && !selectedPreset.equals(preset)) {
                preset = selectedPreset;
                restart();
            }
        });

        return List.of(resetButton, presetLabel, presetComboBox);
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
        if (canvas == null || flameLayer == null) {
            return List.of();
        }

        lastObservedMode = provider.getMode();
        int consumed = 0;

        Graphics2D graphics = flameLayer.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            graphics.setComposite(AlphaComposite.SrcOver);

            int samplesThisStep = RngStepBudget.forProvider(provider, SAMPLES_PER_STEP, QUANTUM_SAMPLES_PER_STEP);

            for (int sample = 0; sample < samplesThisStep; sample++) {
                OptionalInt raw = provider.getNextRandomNumber();
                if (raw.isEmpty()) {
                    break;
                }
                randomNumbersUsed++;
                consumed++;
                transformCount++;
                applyTransform(raw.getAsInt());

                if (transformCount > WARMUP_STEPS) {
                    drawCurrentPoint(graphics, raw.getAsInt());
                }
            }
        } finally {
            graphics.dispose();
        }

        if (consumed > 0) {
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

    private void applyTransform(int raw) {
        int branch = Math.floorMod(raw, 4);
        double x = currentX;
        double y = currentY;

        double affineX;
        double affineY;
        switch (branch) {
            case 0 -> {
                affineX = 0.62 * x - 0.42 * y + 0.05;
                affineY = 0.42 * x + 0.62 * y + 0.18;
            }
            case 1 -> {
                affineX = -0.45 * x + 0.58 * y - 0.20;
                affineY = -0.58 * x - 0.45 * y + 0.02;
            }
            case 2 -> {
                affineX = 0.38 * x + 0.22 * y + 0.36;
                affineY = -0.22 * x + 0.38 * y - 0.28;
            }
            default -> {
                affineX = -0.30 * x - 0.48 * y - 0.26;
                affineY = 0.48 * x - 0.30 * y + 0.30;
            }
        }

        applyVariation(affineX, affineY);
    }

    private void applyVariation(double x, double y) {
        double radiusSquared = x * x + y * y + 1.0e-8;
        switch (preset) {
            case "Sinusoidal" -> {
                currentX = Math.sin(x) + 0.22 * Math.sin(3.0 * y);
                currentY = Math.sin(y) + 0.22 * Math.cos(3.0 * x);
            }
            case "Bubble" -> {
                double factor = 4.0 / (radiusSquared + 4.0);
                currentX = factor * x + 0.18 * Math.sin(y * 2.0);
                currentY = factor * y + 0.18 * Math.cos(x * 2.0);
            }
            default -> {
                double sine = Math.sin(radiusSquared);
                double cosine = Math.cos(radiusSquared);
                currentX = x * sine - y * cosine;
                currentY = x * cosine + y * sine;
            }
        }

        currentX = Math.clamp(currentX, -2.2, 2.2);
        currentY = Math.clamp(currentY, -2.2, 2.2);
    }

    private void drawCurrentPoint(Graphics2D graphics, int raw) {
        int plotX = (int) Math.floor((currentX + 2.2) / 4.4 * Math.max(1, plotBounds.width));
        int plotY = plotBounds.height - 1 - (int) Math.floor((currentY + 2.2) / 4.4 * Math.max(1, plotBounds.height));

        if (plotX < 0 || plotX >= plotBounds.width || plotY < 0 || plotY >= plotBounds.height) {
            rejectedCount++;
            return;
        }

        float hue = (float) (0.78 + 0.20 * normalize(raw));
        int rgb = Color.HSBtoRGB(hue > 1.0f ? hue - 1.0f : hue, 0.64f, 1.0f);
        graphics.setColor(new Color((rgb & 0x00FFFFFF) | 0x2E000000, true));
        graphics.fillRect(plotX, plotY, 1, 1);
        pointCount++;
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
        transformCount = 0;
        rejectedCount = 0;
        currentX = 0.12;
        currentY = -0.23;
    }

    private void ensureInitialized(BufferedImage canvas) {
        if (canvas == null) {
            return;
        }
        if (flameLayer == null || canvas.getWidth() != width || canvas.getHeight() != height) {
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
        plotBounds = new Rectangle(OUTER_PADDING, plotY, Math.max(1, safeWidth - OUTER_PADDING * 2), Math.max(1, plotBottom - plotY));

        chromeLayer = new BufferedImage(safeWidth, safeHeight, BufferedImage.TYPE_INT_ARGB);
        flameLayer = new BufferedImage(Math.max(1, plotBounds.width), Math.max(1, plotBounds.height), BufferedImage.TYPE_INT_ARGB);
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
            graphics.drawString("Randomly selected nonlinear IFS transforms accumulate into a luminous flame density", headerBounds.x + 18, headerBounds.y + 55);
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
            graphics.drawImage(flameLayer, plotBounds.x, plotBounds.y, null);
            drawLabels(graphics);
            drawMetrics(graphics);
        } finally {
            graphics.dispose();
        }
    }

    private void drawLabels(Graphics2D graphics) {
        graphics.setFont(LABEL_FONT);
        graphics.setColor(TEXT_PRIMARY);
        graphics.drawString("chaos-game flame density", plotBounds.x + 14, plotBounds.y + 21);
        graphics.setColor(TEXT_MUTED);
        graphics.drawString("nonlinear variations: " + preset, plotBounds.x + 14, plotBounds.y + 38);
    }

    private void drawMetrics(Graphics2D graphics) {
        graphics.setFont(VALUE_FONT);
        graphics.setColor(TEXT_PRIMARY);
        graphics.drawString(
                String.format("preset=%s  transforms=%d  plotted=%d  rejected=%d  numbers=%d  rng=%s", preset, transformCount, pointCount, rejectedCount, randomNumbersUsed, lastObservedMode),
                footerBounds.x + 14,
                footerBounds.y + 22
        );
        graphics.setColor(TEXT_ACCENT);
        graphics.drawString("QRNG selects each transform; density reveals stable structure from random choices", footerBounds.x + 14, footerBounds.y + 40);
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
}
