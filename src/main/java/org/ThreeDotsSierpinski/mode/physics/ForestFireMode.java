package org.ThreeDotsSierpinski.mode.physics;

import org.ThreeDotsSierpinski.app.DotController;
import org.ThreeDotsSierpinski.mode.RngStepBudget;
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
 * Режим визуализации: стохастическая модель Forest Fire.
 * <p>
 * Случайные числа управляют ростом деревьев и ударами молнии.
 * Пожар распространяется по соседним клеткам, затем выгорает в пепел,
 * а лес постепенно восстанавливается. Получается динамическая картина:
 * локальная случайность рождает волны огня, острова леса и циклы регенерации.
 */
public class ForestFireMode implements VisualizationMode {

    private static final String ID = "forest-fire";
    private static final String NAME = "Forest Fire";
    private static final String DESCRIPTION =
            "Деревья растут случайно, молнии зажигают пожар,\n"
                    + "огонь распространяется — и лес снова восстанавливается.";
    private static final String ICON = "🔥";

    private static final int CELL_SIZE = 4;
    private static final int MIN_GRID_SIZE = 1;

    private static final byte EMPTY = 0;
    private static final byte TREE = 1;
    private static final byte BURNING = 2;
    private static final byte ASH = 3;

    private static final int INITIAL_POINT_COUNT = 0;
    private static final int INITIAL_RANDOM_NUMBERS_USED = 0;
    private static final int INITIAL_STEP_COUNT = 0;

    private static final int INITIAL_TREE_DENSITY_PERCENT = 38;
    private static final int TREE_DENSITY_RANDOM_DENOMINATOR = 100;

    private static final int GROWTH_ATTEMPTS_MIN = 120;
    private static final int GROWTH_AREA_DIVISOR = 120;
    private static final int QUANTUM_GROWTH_ATTEMPTS = 16;

    private static final int LIGHTNING_CHANCE_PERCENT = 9;
    private static final int LIGHTNING_CHANCE_WHEN_QUIET_PERCENT = 28;
    private static final int LIGHTNING_RANDOM_DENOMINATOR = 100;
    private static final int LIGHTNING_SEARCH_ATTEMPTS = 16;

    private static final int FIRE_LIFESPAN_TICKS = 4;
    private static final int ASH_LIFESPAN_TICKS = 18;
    private static final int TREE_MAX_AGE = 120;
    private static final int TREE_INITIAL_RANDOM_AGE_RANGE = 80;

    private static final int SPREAD_ORTHOGONAL_CHANCE_PERCENT = 88;
    private static final int SPREAD_DIAGONAL_CHANCE_PERCENT = 48;
    private static final int SPREAD_RANDOM_DENOMINATOR = 100;

    private static final int ORIGIN_X = 0;
    private static final int ORIGIN_Y = 0;

    private static final int LEGEND_X = 12;
    private static final int LEGEND_Y = 22;
    private static final int LEGEND_LINE_HEIGHT = 18;
    private static final int LEGEND_FONT_SIZE = 12;

    private static final String LABEL_LIGHTNING = "Молнии";
    private static final String LABEL_ON = "ВКЛ.";
    private static final String LABEL_OFF = "ВЫКЛ.";
    private static final String BUTTON_SPARK = "Искра";
    private static final String TOOLTIP_LIGHTNING = "Включить/выключить случайные удары молнии";
    private static final String TOOLTIP_SPARK = "Запросить ручную искру на следующем шаге";

    private static final Color BACKGROUND_COLOR = new Color(5, 7, 8);
    private static final Color EMPTY_COLOR = new Color(8, 10, 11);
    private static final Color ASH_DARK_COLOR = new Color(28, 26, 24);
    private static final Color ASH_LIGHT_COLOR = new Color(70, 64, 56);
    private static final Color YOUNG_TREE_COLOR = new Color(52, 190, 68);
    private static final Color OLD_TREE_COLOR = new Color(16, 92, 32);
    private static final Color FIRE_CORE_COLOR = new Color(255, 240, 90);
    private static final Color FIRE_MID_COLOR = new Color(255, 110, 24);
    private static final Color FIRE_EDGE_COLOR = new Color(170, 22, 8);
    private static final Color LEGEND_COLOR = new Color(210, 235, 210);

    private static final int[] NEIGHBOR_DX = {-1, 0, 1, -1, 1, -1, 0, 1};
    private static final int[] NEIGHBOR_DY = {-1, -1, -1, 0, 0, 1, 1, 1};

    private int canvasWidth;
    private int canvasHeight;
    private int gridWidth;
    private int gridHeight;
    private int cellCount;

    private byte[] cells;
    private byte[] nextCells;
    private byte[] treeAge;
    private byte[] fireAge;
    private byte[] ashAge;

    private int pointCount = INITIAL_POINT_COUNT;
    private int randomNumbersUsed = INITIAL_RANDOM_NUMBERS_USED;
    private int stepCount = INITIAL_STEP_COUNT;
    private int treeCount;
    private int burningCount;
    private int ashCount;

    private boolean lightningEnabled = true;
    private boolean manualSparkRequested = false;

    private static Color blend(Color from, Color to, double t) {
        double clamped = Math.max(0.0, Math.min(1.0, t));
        int r = (int) Math.round(from.getRed() + (to.getRed() - from.getRed()) * clamped);
        int g = (int) Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * clamped);
        int b = (int) Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * clamped);
        return new Color(r, g, b);
    }

    @Override
    public boolean usesLeftPointCounterOverlay() {
        return true;
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
        return VisualizationCategory.STATISTICAL_PHYSICS;
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
    public void initialize(BufferedImage canvas, int width, int height) {
        canvasWidth = Math.max(MIN_GRID_SIZE, width);
        canvasHeight = Math.max(MIN_GRID_SIZE, height);
        gridWidth = Math.max(MIN_GRID_SIZE, (int) Math.ceil((double) canvasWidth / CELL_SIZE));
        gridHeight = Math.max(MIN_GRID_SIZE, (int) Math.ceil((double) canvasHeight / CELL_SIZE));
        cellCount = gridWidth * gridHeight;

        cells = new byte[cellCount];
        nextCells = new byte[cellCount];
        treeAge = new byte[cellCount];
        fireAge = new byte[cellCount];
        ashAge = new byte[cellCount];

        pointCount = INITIAL_POINT_COUNT;
        randomNumbersUsed = INITIAL_RANDOM_NUMBERS_USED;
        stepCount = INITIAL_STEP_COUNT;
        treeCount = 0;
        burningCount = 0;
        ashCount = 0;
        manualSparkRequested = false;

        seedInitialForest();
        renderForest(canvas);
    }

    @Override
    public List<Point> step(RNProvider provider, BufferedImage canvas, int dotSize) {
        ensureInitialized(canvas);

        var newPoints = new ArrayList<Point>();

        spreadAndAgeFire(provider);
        growTrees(provider, newPoints);
        maybeStrikeLightning(provider, newPoints);
        recalculateCounts();
        renderForest(canvas);

        stepCount++;
        return newPoints;
    }

    @Override
    public void redraw(BufferedImage canvas, int width, int height, int dotSize) {
        ensureInitialized(canvas);
        renderForest(canvas);
    }

    @Override
    public List<JComponent> createModeControls(DotController controller) {
        var controls = new ArrayList<JComponent>();

        var lightningLabel = new JLabel(LABEL_LIGHTNING);
        var lightningToggle = new JToggleButton(lightningEnabled ? LABEL_ON : LABEL_OFF);
        lightningToggle.setSelected(lightningEnabled);
        lightningToggle.setToolTipText(TOOLTIP_LIGHTNING);
        lightningToggle.addActionListener(_ -> {
            lightningEnabled = lightningToggle.isSelected();
            lightningToggle.setText(lightningEnabled ? LABEL_ON : LABEL_OFF);
            controller.refreshVisualization();
        });

        var sparkButton = new JButton(BUTTON_SPARK);
        sparkButton.setToolTipText(TOOLTIP_SPARK);
        sparkButton.addActionListener(_ -> manualSparkRequested = true);

        controls.add(lightningLabel);
        controls.add(lightningToggle);
        controls.add(sparkButton);

        return controls;
    }

    private void ensureInitialized(BufferedImage canvas) {
        if (cells == null || canvasWidth != canvas.getWidth() || canvasHeight != canvas.getHeight()) {
            initialize(canvas, canvas.getWidth(), canvas.getHeight());
        }
    }

    /**
     * Начальное заселение без RNGProvider: это стартовое поле, а не поток QRNG.
     * Дальнейшая динамика уже управляется provider-числами.
     */
    private void seedInitialForest() {
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                int index = indexOf(x, y);
                int deterministicNoise = Math.floorMod(x * 73 + y * 151 + x * y * 17, TREE_DENSITY_RANDOM_DENOMINATOR);
                if (deterministicNoise < INITIAL_TREE_DENSITY_PERCENT) {
                    cells[index] = TREE;
                    treeAge[index] = (byte) Math.floorMod(x * 11 + y * 23, TREE_INITIAL_RANDOM_AGE_RANGE);
                    pointCount++;
                }
            }
        }
        recalculateCounts();
    }

    private void spreadAndAgeFire(RNProvider provider) {
        System.arraycopy(cells, 0, nextCells, 0, cellCount);

        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                int index = indexOf(x, y);

                if (cells[index] == TREE) {
                    treeAge[index] = (byte) Math.min(TREE_MAX_AGE, Byte.toUnsignedInt(treeAge[index]) + 1);
                    continue;
                }

                if (cells[index] == ASH) {
                    int newAshAge = Byte.toUnsignedInt(ashAge[index]) + 1;
                    ashAge[index] = (byte) newAshAge;
                    if (newAshAge >= ASH_LIFESPAN_TICKS) {
                        nextCells[index] = EMPTY;
                        ashAge[index] = 0;
                    }
                    continue;
                }

                if (cells[index] != BURNING) {
                    continue;
                }

                igniteNeighbors(provider, x, y);

                int newFireAge = Byte.toUnsignedInt(fireAge[index]) + 1;
                fireAge[index] = (byte) newFireAge;
                if (newFireAge >= FIRE_LIFESPAN_TICKS) {
                    nextCells[index] = ASH;
                    fireAge[index] = 0;
                    ashAge[index] = 0;
                }
            }
        }

        byte[] swap = cells;
        cells = nextCells;
        nextCells = swap;
    }

    private void igniteNeighbors(RNProvider provider, int x, int y) {
        for (int i = 0; i < NEIGHBOR_DX.length; i++) {
            int nx = x + NEIGHBOR_DX[i];
            int ny = y + NEIGHBOR_DY[i];

            if (!isInsideGrid(nx, ny)) {
                continue;
            }

            int neighborIndex = indexOf(nx, ny);
            if (cells[neighborIndex] != TREE || nextCells[neighborIndex] == BURNING) {
                continue;
            }

            OptionalInt spreadRandom = provider.getNextRandomNumber();
            if (spreadRandom.isEmpty()) {
                return;
            }
            randomNumbersUsed++;

            boolean diagonal = NEIGHBOR_DX[i] != 0 && NEIGHBOR_DY[i] != 0;
            int threshold = diagonal ? SPREAD_DIAGONAL_CHANCE_PERCENT : SPREAD_ORTHOGONAL_CHANCE_PERCENT;

            if (Math.floorMod(spreadRandom.getAsInt(), SPREAD_RANDOM_DENOMINATOR) < threshold) {
                nextCells[neighborIndex] = BURNING;
                fireAge[neighborIndex] = 0;
            }
        }
    }

    private void growTrees(RNProvider provider, List<Point> newPoints) {
        int pseudoGrowthAttempts = Math.max(GROWTH_ATTEMPTS_MIN, cellCount / GROWTH_AREA_DIVISOR);
        int growthAttempts = RngStepBudget.forProvider(provider, pseudoGrowthAttempts, QUANTUM_GROWTH_ATTEMPTS);

        for (int i = 0; i < growthAttempts; i++) {
            OptionalInt rx = provider.getNextRandomNumber();
            OptionalInt ry = provider.getNextRandomNumber();
            OptionalInt ageRandom = provider.getNextRandomNumber();

            if (rx.isEmpty() || ry.isEmpty() || ageRandom.isEmpty()) {
                return;
            }

            randomNumbersUsed += 3;

            int x = Math.floorMod(rx.getAsInt(), gridWidth);
            int y = Math.floorMod(ry.getAsInt(), gridHeight);
            int index = indexOf(x, y);

            if (cells[index] == EMPTY || cells[index] == ASH) {
                cells[index] = TREE;
                treeAge[index] = (byte) Math.floorMod(ageRandom.getAsInt(), TREE_INITIAL_RANDOM_AGE_RANGE);
                fireAge[index] = 0;
                ashAge[index] = 0;
                pointCount++;
                newPoints.add(cellCenterPoint(x, y));
            }
        }
    }

    private void maybeStrikeLightning(RNProvider provider, List<Point> newPoints) {
        if (!lightningEnabled && !manualSparkRequested) {
            return;
        }

        boolean forceSpark = manualSparkRequested;
        manualSparkRequested = false;

        if (!forceSpark) {
            OptionalInt chanceRandom = provider.getNextRandomNumber();
            if (chanceRandom.isEmpty()) {
                return;
            }
            randomNumbersUsed++;

            int threshold = burningCount == 0
                    ? LIGHTNING_CHANCE_WHEN_QUIET_PERCENT
                    : LIGHTNING_CHANCE_PERCENT;

            if (Math.floorMod(chanceRandom.getAsInt(), LIGHTNING_RANDOM_DENOMINATOR) >= threshold) {
                return;
            }
        }

        for (int attempt = 0; attempt < LIGHTNING_SEARCH_ATTEMPTS; attempt++) {
            OptionalInt rx = provider.getNextRandomNumber();
            OptionalInt ry = provider.getNextRandomNumber();
            if (rx.isEmpty() || ry.isEmpty()) {
                return;
            }
            randomNumbersUsed += 2;

            int x = Math.floorMod(rx.getAsInt(), gridWidth);
            int y = Math.floorMod(ry.getAsInt(), gridHeight);
            int index = indexOf(x, y);

            if (cells[index] == TREE) {
                cells[index] = BURNING;
                fireAge[index] = 0;
                newPoints.add(cellCenterPoint(x, y));
                return;
            }
        }
    }

    private void recalculateCounts() {
        int trees = 0;
        int burning = 0;
        int ash = 0;

        for (byte cell : cells) {
            if (cell == TREE) {
                trees++;
            } else if (cell == BURNING) {
                burning++;
            } else if (cell == ASH) {
                ash++;
            }
        }

        treeCount = trees;
        burningCount = burning;
        ashCount = ash;
    }

    private void renderForest(BufferedImage canvas) {
        Graphics2D g = canvas.createGraphics();
        try {
            g.setColor(BACKGROUND_COLOR);
            g.fillRect(ORIGIN_X, ORIGIN_Y, canvasWidth, canvasHeight);

            for (int y = 0; y < gridHeight; y++) {
                for (int x = 0; x < gridWidth; x++) {
                    int index = indexOf(x, y);
                    byte cell = cells[index];

                    if (cell == EMPTY) {
                        continue;
                    }

                    g.setColor(getColorForCell(index, cell));
                    g.fillRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                }
            }

            drawLegend(g);
        } finally {
            g.dispose();
        }
    }

    private Color getColorForCell(int index, byte cell) {
        return switch (cell) {
            case TREE -> blend(YOUNG_TREE_COLOR, OLD_TREE_COLOR,
                    Byte.toUnsignedInt(treeAge[index]) / (double) TREE_MAX_AGE);
            case BURNING -> getFireColor(Byte.toUnsignedInt(fireAge[index]));
            case ASH -> blend(ASH_LIGHT_COLOR, ASH_DARK_COLOR,
                    Byte.toUnsignedInt(ashAge[index]) / (double) ASH_LIFESPAN_TICKS);
            default -> EMPTY_COLOR;
        };
    }

    private Color getFireColor(int age) {
        if (age <= 1) {
            return FIRE_CORE_COLOR;
        }
        if (age == 2) {
            return FIRE_MID_COLOR;
        }
        return FIRE_EDGE_COLOR;
    }

    private void drawLegend(Graphics2D g) {
        g.setColor(LEGEND_COLOR);
        g.setFont(new Font("SansSerif", Font.PLAIN, LEGEND_FONT_SIZE));
        g.drawString(
                "Forest Fire | trees=" + treeCount
                        + " fire=" + burningCount
                        + " ash=" + ashCount
                        + " steps=" + stepCount
                        + " lightning=" + (lightningEnabled ? "ON" : "OFF"),
                LEGEND_X,
                LEGEND_Y
        );
        g.drawString(
                "random growth + lightning → spreading fire → ash → regrowth",
                LEGEND_X,
                LEGEND_Y + LEGEND_LINE_HEIGHT
        );
    }

    private Point cellCenterPoint(int x, int y) {
        return new Point(
                x * CELL_SIZE + CELL_SIZE / 2,
                y * CELL_SIZE + CELL_SIZE / 2
        );
    }

    private int indexOf(int x, int y) {
        return y * gridWidth + x;
    }

    private boolean isInsideGrid(int x, int y) {
        return x >= 0 && x < gridWidth && y >= 0 && y < gridHeight;
    }

    @Override
    public int getPointCount() {
        return pointCount;
    }

    @Override
    public int getRandomNumbersUsed() {
        return randomNumbersUsed;
    }
}