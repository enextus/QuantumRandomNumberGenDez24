package org.ThreeDotsSierpinski.mode.physics;

import org.ThreeDotsSierpinski.app.DotController;
import org.ThreeDotsSierpinski.mode.RngStepBudget;
import org.ThreeDotsSierpinski.mode.VisualizationCategory;
import org.ThreeDotsSierpinski.mode.VisualizationMode;
import org.ThreeDotsSierpinski.rng.RNProvider;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.List;
import java.util.OptionalInt;

/**
 * Abelian sandpile / Bak-Tang-Wiesenfeld self-organized criticality model.
 *
 * <p>QRNG/PSEUDO values choose where grains are added. Cells topple when their
 * height reaches four, producing avalanches and critical lattice patterns.</p>
 */
public class AbelianSandpileMode implements VisualizationMode {

    private static final String ID = "abelian-sandpile";
    private static final String NAME = "Abelian Sandpile";
    private static final String DESCRIPTION =
            "Self-organized criticality: random grains trigger avalanche cascades.\n"
                    + "QRNG chooses drop sites; the lattice reveals critical sandpile patterns.";
    private static final String ICON = "▥";

    private static final double RANDOM_MAX = 65_535.0;
    private static final int DEFAULT_BATCH_SIZE = 20;
    private static final int QUANTUM_BATCH_SIZE = 8;
    private static final int[] BATCH_PRESETS = {10, 20, 50};
    private static final int TOPPLE_THRESHOLD = 4;
    private static final int MAX_TOPPLES_PER_STEP = 8_000;

    private static final int OUTER_PADDING = 18;
    private static final int HEADER_HEIGHT = 76;
    private static final int FOOTER_HEIGHT = 54;
    private static final int PANEL_RADIUS = 20;
    private static final int CONTROL_HEIGHT = 28;
    private static final int RESET_BUTTON_WIDTH = 82;
    private static final int BATCH_COMBO_WIDTH = 92;

    private static final String RESET_TEXT = "Reset";
    private static final String BATCH_LABEL_TEXT = "Grains";
    private static final String RESET_TOOLTIP = "Restart the sandpile lattice";
    private static final String BATCH_TOOLTIP = "Number of random grains dropped per animation step";

    private static final Color BACKGROUND = new Color(4, 4, 8);
    private static final Color PANEL_BACKGROUND = new Color(14, 12, 10);
    private static final Color PANEL_BORDER = new Color(132, 105, 64, 180);
    private static final Color TITLE_COLOR = new Color(255, 245, 225);
    private static final Color TEXT_PRIMARY = new Color(236, 224, 204);
    private static final Color TEXT_MUTED = new Color(170, 150, 118);
    private static final Color TEXT_ACCENT = new Color(255, 198, 92);
    private static final Color AVALANCHE_COLOR = new Color(255, 230, 125, 220);

    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 27);
    private static final Font SUBTITLE_FONT = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font VALUE_FONT = new Font("Monospaced", Font.PLAIN, 12);

    private int width;
    private int height;
    private int pointCount;
    private int randomNumbersUsed;
    private int gridSize;
    private int batchSize = DEFAULT_BATCH_SIZE;
    private int totalTopples;
    private int lastAvalancheSize;
    private int maxAvalancheSize;
    private long grainCount;

    private int[][] heights;
    private boolean[][] avalancheMask;
    private Rectangle headerBounds = new Rectangle();
    private Rectangle plotBounds = new Rectangle();
    private Rectangle footerBounds = new Rectangle();
    private BufferedImage chromeLayer;
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
    public VisualizationCategory getCategory() {
        return VisualizationCategory.STATISTICAL_PHYSICS;
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

        JLabel batchLabel = new JLabel(BATCH_LABEL_TEXT);
        JComboBox<Integer> batchComboBox = new JComboBox<>(toIntegerArray(BATCH_PRESETS));
        batchComboBox.setSelectedItem(batchSize);
        batchComboBox.setPreferredSize(new Dimension(BATCH_COMBO_WIDTH, CONTROL_HEIGHT));
        batchComboBox.setToolTipText(BATCH_TOOLTIP);
        batchComboBox.addActionListener(event -> {
            Object selected = batchComboBox.getSelectedItem();
            if (selected instanceof Integer selectedBatch) {
                batchSize = selectedBatch;
            }
        });

        return List.of(resetButton, batchLabel, batchComboBox);
    }

    @Override
    public void initialize(BufferedImage canvas, int width, int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        resetState();
        layoutDashboard();
        initializeGrid();
        render(canvas);
    }

    @Override
    public List<Point> step(RNProvider provider, BufferedImage canvas, int dotSize) {
        ensureInitialized(canvas);
        if (canvas == null || heights == null) {
            return List.of();
        }

        lastObservedMode = provider.getMode();
        clearAvalancheMask();
        lastAvalancheSize = 0;
        int droppedThisStep = 0;
        int grainsThisStep = RngStepBudget.forProvider(provider, batchSize, QUANTUM_BATCH_SIZE);

        for (int i = 0; i < grainsThisStep; i++) {
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

            int x = randomToIndex(rawX.getAsInt());
            int y = randomToIndex(rawY.getAsInt());
            heights[y][x]++;
            droppedThisStep++;
            grainCount++;
            pointCount++;

            if (heights[y][x] >= TOPPLE_THRESHOLD) {
                toppleFrom(x, y);
            }
        }

        if (lastAvalancheSize > maxAvalancheSize) {
            maxAvalancheSize = lastAvalancheSize;
        }

        if (droppedThisStep > 0) {
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
            initializeGrid();
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

    private void toppleFrom(int startX, int startY) {
        ArrayDeque<Point> queue = new ArrayDeque<>();
        queue.add(new Point(startX, startY));
        int processed = 0;

        while (!queue.isEmpty() && processed < MAX_TOPPLES_PER_STEP) {
            Point point = queue.removeFirst();
            int x = point.x;
            int y = point.y;
            if (x < 0 || x >= gridSize || y < 0 || y >= gridSize || heights[y][x] < TOPPLE_THRESHOLD) {
                continue;
            }

            heights[y][x] -= TOPPLE_THRESHOLD;
            avalancheMask[y][x] = true;
            lastAvalancheSize++;
            totalTopples++;
            processed++;

            addGrainToNeighbor(x + 1, y, queue);
            addGrainToNeighbor(x - 1, y, queue);
            addGrainToNeighbor(x, y + 1, queue);
            addGrainToNeighbor(x, y - 1, queue);
        }
    }

    private void addGrainToNeighbor(int x, int y, ArrayDeque<Point> queue) {
        if (x < 0 || x >= gridSize || y < 0 || y >= gridSize) {
            return;
        }
        heights[y][x]++;
        if (heights[y][x] >= TOPPLE_THRESHOLD) {
            queue.add(new Point(x, y));
        }
    }

    private void restart() {
        resetState();
        layoutDashboard();
        initializeGrid();
        if (controller != null) {
            controller.refreshVisualization();
        }
    }

    private void resetState() {
        pointCount = 0;
        randomNumbersUsed = 0;
        totalTopples = 0;
        lastAvalancheSize = 0;
        maxAvalancheSize = 0;
        grainCount = 0L;
    }

    private void ensureInitialized(BufferedImage canvas) {
        if (canvas == null) {
            return;
        }
        if (heights == null || canvas.getWidth() != width || canvas.getHeight() != height) {
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
        int availableWidth = Math.max(1, safeWidth - OUTER_PADDING * 2);
        int availableHeight = Math.max(1, plotBottom - plotY);
        int squareSize = Math.max(1, Math.min(availableWidth, availableHeight));
        plotBounds = new Rectangle(OUTER_PADDING + (availableWidth - squareSize) / 2, plotY, squareSize, squareSize);

        gridSize = Math.max(32, Math.min(130, squareSize / 5));
        chromeLayer = new BufferedImage(safeWidth, safeHeight, BufferedImage.TYPE_INT_ARGB);
        renderChromeLayer();
    }

    private void initializeGrid() {
        heights = new int[gridSize][gridSize];
        avalancheMask = new boolean[gridSize][gridSize];
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
            graphics.drawString("Random grain drops self-organize into critical avalanche patterns", headerBounds.x + 18, headerBounds.y + 55);
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
            drawGrid(graphics);
            drawMetrics(graphics);
        } finally {
            graphics.dispose();
        }
    }

    private void drawGrid(Graphics2D graphics) {
        if (heights == null) {
            return;
        }

        int cellSize = Math.max(1, plotBounds.width / gridSize);
        int xOffset = plotBounds.x + (plotBounds.width - cellSize * gridSize) / 2;
        int yOffset = plotBounds.y + (plotBounds.height - cellSize * gridSize) / 2;

        for (int y = 0; y < gridSize; y++) {
            for (int x = 0; x < gridSize; x++) {
                graphics.setColor(resolveCellColor(Math.floorMod(heights[y][x], TOPPLE_THRESHOLD), avalancheMask[y][x]));
                graphics.fillRect(xOffset + x * cellSize, yOffset + y * cellSize, cellSize, cellSize);
            }
        }

        graphics.setColor(PANEL_BORDER);
        graphics.drawRect(xOffset, yOffset, cellSize * gridSize, cellSize * gridSize);
    }

    private Color resolveCellColor(int height, boolean avalanche) {
        if (avalanche) {
            return AVALANCHE_COLOR;
        }
        return switch (height) {
            case 0 -> new Color(8, 9, 12);
            case 1 -> new Color(68, 45, 28);
            case 2 -> new Color(166, 100, 45);
            default -> new Color(238, 178, 70);
        };
    }

    private void drawMetrics(Graphics2D graphics) {
        graphics.setFont(VALUE_FONT);
        graphics.setColor(TEXT_PRIMARY);
        graphics.drawString(
                String.format("grid=%dx%d  grains=%d  last-avalanche=%d  max-avalanche=%d", gridSize, gridSize, grainCount, lastAvalancheSize, maxAvalancheSize),
                footerBounds.x + 14,
                footerBounds.y + 22
        );
        graphics.setColor(TEXT_ACCENT);
        graphics.drawString(
                String.format("topples=%d  batch=%d  numbers=%d  rng=%s", totalTopples, batchSize, randomNumbersUsed, lastObservedMode),
                footerBounds.x + 14,
                footerBounds.y + 40
        );
    }

    private void drawPanel(Graphics2D graphics, Rectangle bounds) {
        graphics.setColor(PANEL_BACKGROUND);
        graphics.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, PANEL_RADIUS, PANEL_RADIUS);
        graphics.setColor(PANEL_BORDER);
        graphics.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, PANEL_RADIUS, PANEL_RADIUS);
    }

    private void clearAvalancheMask() {
        if (avalancheMask == null) {
            return;
        }
        for (int y = 0; y < gridSize; y++) {
            for (int x = 0; x < gridSize; x++) {
                avalancheMask[y][x] = false;
            }
        }
    }

    private int randomToIndex(int raw) {
        int index = (int) Math.floor(Math.clamp(raw / RANDOM_MAX, 0.0, 1.0) * gridSize);
        return Math.min(gridSize - 1, Math.max(0, index));
    }

    private Integer[] toIntegerArray(int[] values) {
        Integer[] result = new Integer[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = values[i];
        }
        return result;
    }
}
