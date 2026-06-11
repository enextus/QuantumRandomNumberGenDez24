package org.ThreeDotsSierpinski.mode.chaos;

import org.ThreeDotsSierpinski.mode.*;
import org.ThreeDotsSierpinski.mode.chaos.*;
import org.ThreeDotsSierpinski.mode.montecarlo.*;
import org.ThreeDotsSierpinski.mode.physics.*;
import org.ThreeDotsSierpinski.mode.stochastic.*;

import org.ThreeDotsSierpinski.app.*;
import org.ThreeDotsSierpinski.config.*;
import org.ThreeDotsSierpinski.math.*;
import org.ThreeDotsSierpinski.model.*;
import org.ThreeDotsSierpinski.rng.*;
import org.ThreeDotsSierpinski.stats.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * Режим визуализации: Chaos Game Representation для битового потока.
 * <p>
 * Каждое 16-битное случайное число разбивается на 8 двухбитных символов.
 * Символы 00, 01, 10, 11 соответствуют четырём углам квадрата. Текущая
 * точка каждый раз смещается на половину расстояния к выбранному углу.
 * Для равномерного потока рисунок заполняется почти равномерно; устойчивые
 * пустоты, полосы или симметрии могут указывать на структуру в последовательности.
 */
public class ChaosGameRepresentationMode implements VisualizationMode {

    private static final String ID = "cgr-bitstream";
    private static final String NAME = "CGR Bit Stream";
    private static final String DESCRIPTION =
            "Битовый поток переводится в 2D-карту через символы 00/01/10/11.\n"
                    + "Пустоты, полосы и симметрии показывают скрытую структуру последовательности.";
    private static final String ICON = "▣";

    private static final int RANDOM_RANGE = 65_536;
    private static final int RANDOM_VALUES_PER_STEP = 96;
    private static final int SYMBOL_BITS = 2;
    private static final int SYMBOLS_PER_VALUE = 8;
    private static final int HIGHEST_SYMBOL_SHIFT = 14;
    private static final int SYMBOL_MASK = 0b11;

    private static final int MIN_CANVAS_SIZE = 1;
    private static final int CANVAS_ORIGIN_X = 0;
    private static final int CANVAS_ORIGIN_Y = 0;
    private static final int MIN_DOT_SIZE = 1;
    private static final int MAX_DOT_SIZE = 2;

    private static final int PLOT_MARGIN = 38;
    private static final int LABEL_FONT_SIZE = 12;
    private static final int CORNER_LABEL_OFFSET = 16;

    private static final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, LABEL_FONT_SIZE);

    private static final Color[] SYMBOL_COLORS = {
            new Color(90, 190, 255, 150),
            new Color(120, 255, 150, 150),
            new Color(255, 220, 100, 150),
            new Color(255, 105, 165, 150)
    };

    private int width;
    private int height;
    private int plotLeft;
    private int plotTop;
    private int plotRight;
    private int plotBottom;

    private double currentX;
    private double currentY;

    private int pointCount = 0;
    private int randomNumbersUsed = 0;

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
    public boolean usesRecolorAnimation() {
        return false;
    }

    @Override
    public boolean usesDarkBackground() {
        return true;
    }

    @Override
    public void initialize(BufferedImage canvas, int width, int height) {
        if (canvas == null) {
            throw new IllegalArgumentException("Canvas cannot be null");
        }

        this.width = Math.max(MIN_CANVAS_SIZE, width);
        this.height = Math.max(MIN_CANVAS_SIZE, height);

        this.plotLeft = PLOT_MARGIN;
        this.plotTop = PLOT_MARGIN;
        this.plotRight = Math.max(plotLeft + MIN_CANVAS_SIZE, this.width - PLOT_MARGIN);
        this.plotBottom = Math.max(plotTop + MIN_CANVAS_SIZE, this.height - PLOT_MARGIN);

        this.currentX = (plotLeft + plotRight) / 2.0;
        this.currentY = (plotTop + plotBottom) / 2.0;
        this.pointCount = 0;
        this.randomNumbersUsed = 0;

        drawBackground(canvas);
    }

    @Override
    public List<Point> step(RNProvider provider, BufferedImage canvas, int dotSize) {
        int safeDotSize = Math.max(MIN_DOT_SIZE, Math.min(MAX_DOT_SIZE, dotSize));
        var newPoints = new ArrayList<Point>(RANDOM_VALUES_PER_STEP * SYMBOLS_PER_VALUE);

        Graphics2D g2d = canvas.createGraphics();

        try {
            for (int i = 0; i < RANDOM_VALUES_PER_STEP; i++) {
                OptionalInt valueOpt = provider.getNextRandomNumber();
                if (valueOpt.isEmpty()) {
                    break;
                }

                int value = Math.floorMod(valueOpt.getAsInt(), RANDOM_RANGE);
                randomNumbersUsed++;

                for (int shift = HIGHEST_SYMBOL_SHIFT; shift >= 0; shift -= SYMBOL_BITS) {
                    int symbol = (value >> shift) & SYMBOL_MASK;
                    Point point = plotSymbol(g2d, symbol, safeDotSize);
                    newPoints.add(point);
                    pointCount++;
                }
            }
        } finally {
            g2d.dispose();
        }

        return newPoints;
    }

    private Point plotSymbol(Graphics2D g2d, int symbol, int dotSize) {
        int cornerX = switch (symbol) {
            case 0, 2 -> plotLeft;
            case 1, 3 -> plotRight;
            default -> throw new IllegalArgumentException("Unexpected symbol: " + symbol);
        };

        int cornerY = switch (symbol) {
            case 0, 1 -> plotTop;
            case 2, 3 -> plotBottom;
            default -> throw new IllegalArgumentException("Unexpected symbol: " + symbol);
        };

        currentX = (currentX + cornerX) / 2.0;
        currentY = (currentY + cornerY) / 2.0;

        int x = (int) Math.round(currentX);
        int y = (int) Math.round(currentY);

        g2d.setColor(SYMBOL_COLORS[symbol]);
        g2d.fillRect(x, y, dotSize, dotSize);

        return new Point(x, y);
    }

    private void drawBackground(BufferedImage canvas) {
        Graphics2D g2d = canvas.createGraphics();

        try {
            g2d.setColor(Color.BLACK);
            g2d.fillRect(CANVAS_ORIGIN_X, CANVAS_ORIGIN_Y, width, height);

            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setFont(LABEL_FONT);

            drawFrame(g2d);
            drawCornerLabels(g2d);
        } finally {
            g2d.dispose();
        }
    }

    private void drawFrame(Graphics2D g2d) {
        g2d.setColor(new Color(180, 200, 255, 120));
        g2d.drawRect(plotLeft, plotTop, plotRight - plotLeft, plotBottom - plotTop);

        g2d.setColor(new Color(190, 210, 255, 180));
        g2d.drawString("Chaos Game Representation: 2-bit stream", 16, 22);
    }

    private void drawCornerLabels(Graphics2D g2d) {
        drawCornerLabel(g2d, "00", plotLeft, plotTop - CORNER_LABEL_OFFSET, SYMBOL_COLORS[0]);
        drawCornerLabel(g2d, "01", plotRight - CORNER_LABEL_OFFSET, plotTop - CORNER_LABEL_OFFSET, SYMBOL_COLORS[1]);
        drawCornerLabel(g2d, "10", plotLeft, plotBottom + CORNER_LABEL_OFFSET + 4, SYMBOL_COLORS[2]);
        drawCornerLabel(g2d, "11", plotRight - CORNER_LABEL_OFFSET, plotBottom + CORNER_LABEL_OFFSET + 4, SYMBOL_COLORS[3]);
    }

    private static void drawCornerLabel(Graphics2D g2d, String label, int x, int y, Color color) {
        g2d.setColor(color);
        g2d.drawString(label, x, y);
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