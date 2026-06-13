package org.ThreeDotsSierpinski.mode.stochastic;

import org.ThreeDotsSierpinski.app.DotController;
import org.ThreeDotsSierpinski.mode.VisualizationCategory;
import org.ThreeDotsSierpinski.mode.VisualizationMode;
import org.ThreeDotsSierpinski.rng.RNProvider;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * Elementary cellular automaton visualizer.
 *
 * <p>The left panel evolves an elementary CA, defaulting to Rule 30. The first
 * row is seeded by the current RNG stream. The right panel draws the raw RNG bit
 * stream with the same row geometry so the deterministic-chaotic CA output can
 * be compared against the direct QRNG/PSEUDO bits.</p>
 */
public class Rule30AutomatonMode implements VisualizationMode {

    private static final String ID = "rule-30-automaton";
    private static final String NAME = "Rule 30 Automaton";
    private static final String DESCRIPTION =
            "Elementary cellular automaton: Rule 30 creates a black-white chaos triangle.\n"
                    + "Left: CA stream seeded by RNG. Right: raw RNG bit stream.";
    private static final String ICON = "▥";

    private static final int DEFAULT_RULE_CODE = 30;
    private static final int MIN_RANDOM_RULE_CODE = 1;
    private static final int RANDOM_RULE_VARIANTS = 254;
    private static final int MAX_RULE_CODE = 255;

    private static final int ROWS_PER_STEP = 3;
    private static final int SIDE_MARGIN = 18;
    private static final int PANEL_GAP = 24;
    private static final int TOP_HUD_HEIGHT = 34;
    private static final int BOTTOM_HUD_HEIGHT = 30;
    private static final int PANEL_TOP_GAP = 10;
    private static final int PANEL_BOTTOM_GAP = 8;
    private static final int TOP_MARGIN = TOP_HUD_HEIGHT + PANEL_TOP_GAP;
    private static final int BOTTOM_MARGIN = BOTTOM_HUD_HEIGHT + PANEL_BOTTOM_GAP;
    private static final int LABEL_X_PADDING = 8;
    private static final int TITLE_BASELINE_Y = 22;
    private static final int METRICS_BASELINE_OFFSET = 10;
    private static final int PANEL_BORDER_ALPHA = 140;
    private static final int LABEL_ALPHA = 240;
    private static final int HUD_BACKGROUND_ALPHA = 225;
    private static final int HUD_DIVIDER_ALPHA = 150;
    private static final int MUTATION_RATE_MASK = 0xFF;
    private static final int MIN_CANVAS_SIZE = 1;
    private static final int MIN_PANEL_WIDTH = 1;
    private static final int MIN_COLUMN_COUNT = 24;
    private static final int TARGET_CELL_SIZE = 3;
    private static final int MIN_CELL_SIZE = 1;
    private static final int MAX_CELL_SIZE = 5;
    private static final int BITS_PER_RANDOM_VALUE = 16;
    private static final int RANDOM_VALUE_MASK = 0xFFFF;
    private static final int BIT_MASK = 1;
    private static final int CANVAS_ORIGIN_X = 0;
    private static final int CANVAS_ORIGIN_Y = 0;

    private static final String BUTTON_RESET_TEXT = "Reset Rule 30";
    private static final String BUTTON_RANDOM_RULE_TEXT = "Random rule";
    private static final String TOGGLE_MUTATIONS_TEXT = "Mutations";
    private static final String TOGGLE_MUTATIONS_TOOLTIP = "Toggle QRNG/PSEUDO-driven rare bit mutations";
    private static final String BUTTON_RANDOM_RULE_TOOLTIP = "Pick elementary CA rule 1..254 from the current RNG stream";

    private static final Font LABEL_FONT = new Font("SansSerif", Font.BOLD, 13);
    private static final Font METRIC_FONT = new Font("Monospaced", Font.BOLD, 12);

    private static final Color HUD_BACKGROUND_COLOR = new Color(8, 12, 18, HUD_BACKGROUND_ALPHA);
    private static final Color HUD_DIVIDER_COLOR = new Color(150, 170, 205, HUD_DIVIDER_ALPHA);
    private static final Color HUD_TITLE_COLOR = new Color(245, 248, 255, LABEL_ALPHA);
    private static final Color HUD_METRIC_COLOR = new Color(205, 224, 255, LABEL_ALPHA);
    private static final Color PANEL_BORDER_COLOR = new Color(210, 210, 210, PANEL_BORDER_ALPHA);

    private int width;
    private int height;
    private int rulePanelX;
    private int rawPanelX;
    private int panelY;
    private int rulePanelWidth;
    private int rawPanelWidth;
    private int panelHeight;
    private int cellSize;
    private int rowHeight;
    private int ruleColumns;
    private int rawColumns;
    private int maxRows;

    private boolean[] currentCells = new boolean[0];
    private boolean[] nextCells = new boolean[0];
    private int seedFillIndex = 0;
    private int rowIndex = 0;
    private boolean seeded = false;
    private boolean mutationsEnabled = true;
    private boolean pendingReset = false;
    private boolean pendingRandomRule = false;

    private int ruleCode = DEFAULT_RULE_CODE;
    private int rngBitBuffer = 0;
    private int rngBitsRemaining = 0;
    private int pointCount = 0;
    private int randomNumbersUsed = 0;
    private int mutationCount = 0;
    private int resetCount = 0;

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
        return VisualizationCategory.RANDOM_PROCESSES;
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
    public boolean usesRandomNumbersStackOverlay() {
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
    public PointCounterOverlayPlacement getPointCounterOverlayPlacement() {
        return PointCounterOverlayPlacement.TOP_CENTER;
    }

    @Override
    public void initialize(BufferedImage canvas, int width, int height) {
        if (canvas == null) {
            throw new IllegalArgumentException("Canvas cannot be null");
        }

        this.width = Math.max(MIN_CANVAS_SIZE, width);
        this.height = Math.max(MIN_CANVAS_SIZE, height);
        configureLayout();
        clearAutomatonState();
        pointCount = 0;
        randomNumbersUsed = 0;
        mutationCount = 0;
        drawBackground(canvas);
    }

    @Override
    public List<Point> step(RNProvider provider, BufferedImage canvas, int dotSize) {
        if (provider == null || canvas == null) {
            return List.of();
        }

        if (pendingReset) {
            clearAutomatonState();
            pointCount = 0;
            mutationCount = 0;
            pendingReset = false;
            drawBackground(canvas);
        }

        if (pendingRandomRule && !selectRandomRule(provider)) {
            return List.of();
        }

        var drawnPoints = new ArrayList<Point>(Math.max(ruleColumns, rawColumns) * ROWS_PER_STEP);

        if (!seeded && !seedInitialRow(provider, canvas, drawnPoints)) {
            return drawnPoints;
        }

        for (int i = 0; i < ROWS_PER_STEP; i++) {
            if (rowIndex >= maxRows) {
                resetCount++;
                clearAutomatonState();
                drawBackground(canvas);
                if (!seedInitialRow(provider, canvas, drawnPoints)) {
                    break;
                }
            }

            if (!drawRawRandomRow(provider, canvas, rowIndex, drawnPoints)) {
                break;
            }

            evolveRuleRow(provider, canvas, drawnPoints);
        }

        drawHud(canvas);
        return drawnPoints;
    }

    @Override
    public void redraw(BufferedImage canvas, int width, int height, int dotSize) {
        if (canvas == null) {
            return;
        }

        if (this.width != Math.max(MIN_CANVAS_SIZE, width) || this.height != Math.max(MIN_CANVAS_SIZE, height)) {
            this.width = Math.max(MIN_CANVAS_SIZE, width);
            this.height = Math.max(MIN_CANVAS_SIZE, height);
            configureLayout();
            pendingReset = true;
        }

        if (pendingReset) {
            clearAutomatonState();
            pointCount = 0;
            mutationCount = 0;
            pendingReset = false;
        }

        drawBackground(canvas);
    }

    @Override
    public List<JComponent> createModeControls(DotController controller) {
        var resetButton = new JButton(BUTTON_RESET_TEXT);
        resetButton.addActionListener(_ -> {
            ruleCode = DEFAULT_RULE_CODE;
            pendingRandomRule = false;
            requestReset(controller);
        });

        var randomRuleButton = new JButton(BUTTON_RANDOM_RULE_TEXT);
        randomRuleButton.setToolTipText(BUTTON_RANDOM_RULE_TOOLTIP);
        randomRuleButton.addActionListener(_ -> {
            pendingRandomRule = true;
            requestReset(controller);
        });

        var mutationToggle = new JToggleButton(TOGGLE_MUTATIONS_TEXT, mutationsEnabled);
        mutationToggle.setToolTipText(TOGGLE_MUTATIONS_TOOLTIP);
        mutationToggle.addActionListener(_ -> mutationsEnabled = mutationToggle.isSelected());

        return List.of(resetButton, randomRuleButton, mutationToggle);
    }

    @Override
    public int getPointCount() {
        return pointCount;
    }

    @Override
    public int getRandomNumbersUsed() {
        return randomNumbersUsed;
    }

    private void configureLayout() {
        int availableWidth = Math.max(MIN_PANEL_WIDTH, width - SIDE_MARGIN * 2 - PANEL_GAP);
        rulePanelWidth = Math.max(MIN_PANEL_WIDTH, availableWidth / 2);
        rawPanelWidth = Math.max(MIN_PANEL_WIDTH, availableWidth - rulePanelWidth);
        panelY = TOP_MARGIN;
        panelHeight = Math.max(MIN_CANVAS_SIZE, height - TOP_MARGIN - BOTTOM_MARGIN);
        rulePanelX = SIDE_MARGIN;
        rawPanelX = SIDE_MARGIN + rulePanelWidth + PANEL_GAP;

        cellSize = Math.max(
                MIN_CELL_SIZE,
                Math.min(MAX_CELL_SIZE, Math.max(MIN_CELL_SIZE, Math.min(rulePanelWidth, rawPanelWidth) / 180))
        );
        if (cellSize < TARGET_CELL_SIZE && Math.min(rulePanelWidth, rawPanelWidth) >= TARGET_CELL_SIZE * MIN_COLUMN_COUNT) {
            cellSize = TARGET_CELL_SIZE;
        }

        rowHeight = cellSize;
        ruleColumns = Math.max(MIN_COLUMN_COUNT, rulePanelWidth / cellSize);
        rawColumns = Math.max(MIN_COLUMN_COUNT, rawPanelWidth / cellSize);
        maxRows = Math.max(1, panelHeight / rowHeight);

        currentCells = new boolean[ruleColumns];
        nextCells = new boolean[ruleColumns];
    }

    private void clearAutomatonState() {
        currentCells = new boolean[ruleColumns];
        nextCells = new boolean[ruleColumns];
        seedFillIndex = 0;
        rowIndex = 0;
        seeded = false;
        rngBitBuffer = 0;
        rngBitsRemaining = 0;
    }

    private void requestReset(DotController controller) {
        pendingReset = true;
        if (controller != null) {
            controller.refreshVisualization();
        }
    }

    private boolean selectRandomRule(RNProvider provider) {
        OptionalInt value = provider.getNextRandomNumber();
        if (value.isEmpty()) {
            return false;
        }

        randomNumbersUsed++;
        ruleCode = MIN_RANDOM_RULE_CODE + Math.floorMod(value.getAsInt(), RANDOM_RULE_VARIANTS);
        pendingRandomRule = false;
        return true;
    }

    private boolean seedInitialRow(RNProvider provider, BufferedImage canvas, List<Point> drawnPoints) {
        while (seedFillIndex < currentCells.length) {
            OptionalInt nextBit = nextRandomBit(provider);
            if (nextBit.isEmpty()) {
                return false;
            }
            currentCells[seedFillIndex] = nextBit.getAsInt() == 1;
            seedFillIndex++;
        }

        seeded = true;
        drawRuleRow(canvas, 0, currentCells, drawnPoints);
        rowIndex = 1;
        return true;
    }

    private void evolveRuleRow(RNProvider provider, BufferedImage canvas, List<Point> drawnPoints) {
        for (int x = 0; x < currentCells.length; x++) {
            boolean left = currentCells[Math.floorMod(x - 1, currentCells.length)];
            boolean center = currentCells[x];
            boolean right = currentCells[(x + 1) % currentCells.length];
            nextCells[x] = applyElementaryRule(left, center, right);
        }

        applyRareMutation(provider, nextCells);
        drawRuleRow(canvas, rowIndex, nextCells, drawnPoints);

        boolean[] swap = currentCells;
        currentCells = nextCells;
        nextCells = swap;
        rowIndex++;
    }

    private boolean applyElementaryRule(boolean left, boolean center, boolean right) {
        int neighborhood = (left ? 4 : 0) | (center ? 2 : 0) | (right ? 1 : 0);
        return ((ruleCode >> neighborhood) & BIT_MASK) == 1;
    }

    private void applyRareMutation(RNProvider provider, boolean[] row) {
        if (!mutationsEnabled || row.length == 0) {
            return;
        }

        OptionalInt roll = provider.getNextRandomNumber();
        if (roll.isEmpty()) {
            return;
        }

        randomNumbersUsed++;
        int value = roll.getAsInt();
        if ((value & MUTATION_RATE_MASK) == 0) {
            int index = Math.floorMod(value >>> 8, row.length);
            row[index] = !row[index];
            mutationCount++;
        }
    }

    private boolean drawRawRandomRow(
            RNProvider provider,
            BufferedImage canvas,
            int targetRowIndex,
            List<Point> drawnPoints
    ) {
        boolean[] rawRow = new boolean[rawColumns];
        for (int x = 0; x < rawColumns; x++) {
            OptionalInt bit = nextRandomBit(provider);
            if (bit.isEmpty()) {
                return false;
            }
            rawRow[x] = bit.getAsInt() == 1;
        }

        drawRawRow(canvas, targetRowIndex, rawRow, drawnPoints);
        return true;
    }

    private OptionalInt nextRandomBit(RNProvider provider) {
        if (rngBitsRemaining == 0) {
            OptionalInt nextValue = provider.getNextRandomNumber();
            if (nextValue.isEmpty()) {
                return OptionalInt.empty();
            }

            rngBitBuffer = nextValue.getAsInt() & RANDOM_VALUE_MASK;
            rngBitsRemaining = BITS_PER_RANDOM_VALUE;
            randomNumbersUsed++;
        }

        int bit = rngBitBuffer & BIT_MASK;
        rngBitBuffer >>>= 1;
        rngBitsRemaining--;
        return OptionalInt.of(bit);
    }

    private void drawRuleRow(BufferedImage canvas, int targetRowIndex, boolean[] row, List<Point> drawnPoints) {
        int y = panelY + targetRowIndex * rowHeight;
        if (y >= panelY + panelHeight) {
            return;
        }

        Graphics2D g2d = canvas.createGraphics();
        try {
            for (int x = 0; x < row.length; x++) {
                if (row[x]) {
                    int screenX = rulePanelX + x * cellSize;
                    g2d.setColor(Color.WHITE);
                    g2d.fillRect(screenX, y, cellSize, rowHeight);
                    drawnPoints.add(new Point(screenX, y));
                    pointCount++;
                }
            }
        } finally {
            g2d.dispose();
        }
    }

    private void drawRawRow(BufferedImage canvas, int targetRowIndex, boolean[] row, List<Point> drawnPoints) {
        int y = panelY + targetRowIndex * rowHeight;
        if (y >= panelY + panelHeight) {
            return;
        }

        Graphics2D g2d = canvas.createGraphics();
        try {
            for (int x = 0; x < row.length; x++) {
                if (row[x]) {
                    int screenX = rawPanelX + x * cellSize;
                    g2d.setColor(Color.WHITE);
                    g2d.fillRect(screenX, y, cellSize, rowHeight);
                    drawnPoints.add(new Point(screenX, y));
                    pointCount++;
                }
            }
        } finally {
            g2d.dispose();
        }
    }

    private void drawBackground(BufferedImage canvas) {
        Graphics2D g2d = canvas.createGraphics();
        try {
            g2d.setColor(Color.BLACK);
            g2d.fillRect(CANVAS_ORIGIN_X, CANVAS_ORIGIN_Y, width, height);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            drawHud(g2d);
        } finally {
            g2d.dispose();
        }
    }

    private void drawPanelFrames(Graphics2D g2d) {
        g2d.setColor(PANEL_BORDER_COLOR);
        g2d.drawRect(rulePanelX, panelY, rulePanelWidth, panelHeight);
        g2d.drawRect(rawPanelX, panelY, rawPanelWidth, panelHeight);
    }

    private void drawHud(BufferedImage canvas) {
        Graphics2D g2d = canvas.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            drawHud(g2d);
        } finally {
            g2d.dispose();
        }
    }

    private void drawHud(Graphics2D g2d) {
        drawHudStrips(g2d);
        drawPanelFrames(g2d);
        drawHudTitles(g2d);
        drawHudMetrics(g2d);
    }

    private void drawHudStrips(Graphics2D g2d) {
        g2d.setColor(HUD_BACKGROUND_COLOR);
        g2d.fillRect(CANVAS_ORIGIN_X, CANVAS_ORIGIN_Y, width, TOP_HUD_HEIGHT);
        g2d.fillRect(CANVAS_ORIGIN_X, Math.max(CANVAS_ORIGIN_Y, height - BOTTOM_HUD_HEIGHT), width, BOTTOM_HUD_HEIGHT);

        g2d.setColor(HUD_DIVIDER_COLOR);
        g2d.drawLine(CANVAS_ORIGIN_X, TOP_HUD_HEIGHT, width, TOP_HUD_HEIGHT);
        g2d.drawLine(CANVAS_ORIGIN_X, Math.max(CANVAS_ORIGIN_Y, height - BOTTOM_HUD_HEIGHT),
                width, Math.max(CANVAS_ORIGIN_Y, height - BOTTOM_HUD_HEIGHT));
    }

    private void drawHudTitles(Graphics2D g2d) {
        g2d.setFont(LABEL_FONT);
        g2d.setColor(HUD_TITLE_COLOR);
        g2d.drawString("Rule " + ruleCode + " cellular automaton", rulePanelX + LABEL_X_PADDING, TITLE_BASELINE_Y);
        g2d.drawString("Raw RNG bit stream", rawPanelX + LABEL_X_PADDING, TITLE_BASELINE_Y);
    }

    private void drawHudMetrics(Graphics2D g2d) {
        int baselineY = height - METRICS_BASELINE_OFFSET;

        g2d.setFont(METRIC_FONT);
        g2d.setColor(HUD_METRIC_COLOR);
        String metrics = "seed=RNG  mutations=" + (mutationsEnabled ? mutationCount : "off")
                + "  resets=" + resetCount
                + "  numbers=" + randomNumbersUsed;
        g2d.drawString(metrics, rulePanelX + LABEL_X_PADDING, baselineY);
        g2d.drawString("left: deterministic chaos from seed    right: direct RNG bits",
                rawPanelX + LABEL_X_PADDING, baselineY);
    }
}
