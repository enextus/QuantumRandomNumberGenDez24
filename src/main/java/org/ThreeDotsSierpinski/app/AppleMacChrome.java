package org.ThreeDotsSierpinski.app;

import org.ThreeDotsSierpinski.rng.RNProvider;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;

/**
 * Apple Macintosh inspired frame, layout and component styling helpers.
 */
final class AppleMacChrome {

    static final int FRAME_INSET = 5;
    static final int FRAME_INNER_INSET = 3;
    static final int TABLE_GAP = 8;
    static final int TABLE_TOP_MARGIN = 31;
    static final int TABLE_MAX_ROWS = 32;
    static final int RANDOM_STACK_CELL_HEIGHT = 16;
    static final int RANDOM_STACK_HEADER_HEIGHT = 22;
    static final int RANDOM_STACK_HEADER_Y_OFFSET = 6;
    static final int RANDOM_STACK_VALUES_Y_OFFSET = 6;
    static final int RANDOM_STACK_COLUMN_GAP = 0;
    static final int RANDOM_STACK_CELL_HORIZONTAL_PADDING = 6;
    static final int RANDOM_STACK_RIGHT_TRIM = 2;

    static final Font INFO_FONT = new Font("DialogInput", Font.BOLD, 12);
    static final Font POINT_COUNTER_FONT = new Font("DialogInput", Font.BOLD, 54);
    static final Font RNG_LABEL_FONT = new Font("DialogInput", Font.BOLD, 12);
    static final Font RANDOM_STACK_FONT = new Font("DialogInput", Font.BOLD, 12);
    static final Font CONTROL_FONT = new Font("DialogInput", Font.BOLD, 12);

    static final Color BACKGROUND_COLOR = new Color(238, 238, 236);
    static final Color PANEL_BACKGROUND_COLOR = new Color(225, 225, 225);
    static final Color TEXT_COLOR = Color.BLACK;
    static final Color BORDER_COLOR = Color.BLACK;
    static final Color STACK_ROW_BACKGROUND = new Color(248, 248, 248);
    static final Color STACK_HEADER_BACKGROUND = new Color(230, 230, 230);

    private static final int MIN_CANVAS_SIZE = 1;
    private static final int INSET = 6;
    private static final int INFO_SEPARATOR_Y = 28;
    private static final int PLOT_CONTENT_INSET = 7;
    private static final int CANVAS_BOTTOM_MARGIN = 8;
    private static final int CANVAS_TOP = INFO_SEPARATOR_Y + 4;

    private static final int COUNTER_BLOCK_MARGIN = 8;
    private static final int COUNTER_BLOCK_PADDING = 10;
    private static final int COUNTER_BLOCK_MIN_WIDTH = 250;
    private static final int COUNTER_BLOCK_HEIGHT = 100;
    private static final int COUNTER_LABEL_BASELINE_OFFSET = 18;
    private static final int COUNTER_VALUE_BASELINE_OFFSET = 64;
    private static final int COUNTER_SEPARATOR_OFFSET = 72;
    private static final int COUNTER_RNG_BASELINE_OFFSET = 91;

    private static final Color HIGHLIGHT_COLOR = Color.WHITE;
    private static final Color SHADOW_COLOR = new Color(110, 110, 110);

    private static final String RNG_MODE_LABEL_QUANTUM = "■ QUANTUM";
    private static final String RNG_MODE_LABEL_PSEUDO = "■ PSEUDO (L128X256MixRandom)";
    private static final String COUNTER_BLOCK_TITLE = "POINTS";

    private AppleMacChrome() {
    }

    static Rectangle calculatePlotContentArea(Graphics2D g2d, int panelWidth, int panelHeight) {
        Rectangle plotArea = calculatePlotArea(g2d, panelWidth, panelHeight);
        int inset = PLOT_CONTENT_INSET;

        return new Rectangle(
                plotArea.x + inset,
                plotArea.y + inset,
                Math.max(MIN_CANVAS_SIZE, plotArea.width - inset * 2),
                Math.max(MIN_CANVAS_SIZE, plotArea.height - inset * 2)
        );
    }

    static Rectangle calculateCounterBlockArea(
            Graphics2D g2d,
            int panelWidth,
            int panelHeight,
            int pointCount,
            RNProvider.Mode rngMode
    ) {
        Rectangle plotArea = calculatePlotArea(g2d, panelWidth, panelHeight);

        g2d.setFont(POINT_COUNTER_FONT);
        int counterWidth = g2d.getFontMetrics().stringWidth(String.valueOf(pointCount));

        g2d.setFont(RNG_LABEL_FONT);
        int rngWidth = g2d.getFontMetrics().stringWidth(formatRngModeLabel(rngMode));

        int preferredWidth = Math.max(
                COUNTER_BLOCK_MIN_WIDTH,
                Math.max(counterWidth, rngWidth) + COUNTER_BLOCK_PADDING * 2
        );
        int maxWidth = Math.max(
                COUNTER_BLOCK_MIN_WIDTH,
                plotArea.width - COUNTER_BLOCK_MARGIN * 2
        );

        int width = Math.min(preferredWidth, maxWidth);
        int height = Math.clamp(
                plotArea.height - COUNTER_BLOCK_MARGIN * 2,
                MIN_CANVAS_SIZE,
                COUNTER_BLOCK_HEIGHT
        );

        return new Rectangle(
                plotArea.x + COUNTER_BLOCK_MARGIN,
                plotArea.y + COUNTER_BLOCK_MARGIN,
                width,
                height
        );
    }

    static void drawFrame(Graphics2D g2d, int panelWidth, int panelHeight) {
        drawDoubleFrame(
                g2d,
                INSET,
                INSET,
                panelWidth - INSET * 2 - 1,
                panelHeight - INSET * 2 - 1
        );

        drawTitleStrip(g2d, panelWidth);

        Rectangle plotArea = calculatePlotArea(g2d, panelWidth, panelHeight);
        Rectangle sidebarArea = calculateSidebarArea(g2d, panelWidth, panelHeight);

        drawDoubleFrame(g2d, plotArea.x, plotArea.y, plotArea.width, plotArea.height);
        drawDoubleFrame(g2d, sidebarArea.x, sidebarArea.y, sidebarArea.width, sidebarArea.height);
    }

    static void drawCounterBlock(
            Graphics2D g2d,
            int panelWidth,
            int panelHeight,
            int pointCount,
            RNProvider.Mode rngMode
    ) {
        Rectangle block = calculateCounterBlockArea(g2d, panelWidth, panelHeight, pointCount, rngMode);

        g2d.setColor(PANEL_BACKGROUND_COLOR);
        g2d.fillRect(block.x, block.y, block.width, block.height);
        drawDoubleFrame(g2d, block.x, block.y, block.width, block.height);

        int textX = block.x + COUNTER_BLOCK_PADDING;

        g2d.setFont(INFO_FONT);
        g2d.setColor(TEXT_COLOR);
        g2d.drawString(COUNTER_BLOCK_TITLE, textX, block.y + COUNTER_LABEL_BASELINE_OFFSET);

        g2d.setFont(POINT_COUNTER_FONT);
        g2d.drawString(String.valueOf(pointCount), textX, block.y + COUNTER_VALUE_BASELINE_OFFSET);

        g2d.drawLine(
                block.x + FRAME_INNER_INSET,
                block.y + COUNTER_SEPARATOR_OFFSET,
                block.x + block.width - FRAME_INNER_INSET,
                block.y + COUNTER_SEPARATOR_OFFSET
        );

        g2d.setFont(RNG_LABEL_FONT);
        g2d.drawString(formatRngModeLabel(rngMode), textX, block.y + COUNTER_RNG_BASELINE_OFFSET);
    }

    static void applyStyleRecursively(Component component, Component excludedPanel) {
        applyStyle(component, excludedPanel);

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                applyStyleRecursively(child, excludedPanel);
            }
        }
    }

    private static int calculateSidebarX(Graphics2D g2d, int panelWidth) {
        int stackX = RandomNumbersStackOverlay.calculateAppleMacStackX(g2d, panelWidth);
        return Math.max(
                FRAME_INSET + MIN_CANVAS_SIZE + TABLE_GAP,
                stackX - FRAME_INNER_INSET - 2
        );
    }

    private static Rectangle calculatePlotArea(Graphics2D g2d, int panelWidth, int panelHeight) {
        int plotX = FRAME_INSET;
        int plotY = CANVAS_TOP;
        int sidebarX = calculateSidebarX(g2d, panelWidth);

        int plotWidth = Math.max(
                MIN_CANVAS_SIZE,
                sidebarX - TABLE_GAP - plotX
        );
        int plotHeight = Math.max(
                MIN_CANVAS_SIZE,
                panelHeight - plotY - CANVAS_BOTTOM_MARGIN
        );

        return new Rectangle(plotX, plotY, plotWidth, plotHeight);
    }

    private static Rectangle calculateSidebarArea(Graphics2D g2d, int panelWidth, int panelHeight) {
        int sidebarX = calculateSidebarX(g2d, panelWidth);
        int sidebarY = CANVAS_TOP;
        int sidebarWidth = Math.max(
                MIN_CANVAS_SIZE,
                panelWidth - FRAME_INSET - sidebarX
        );
        int sidebarHeight = Math.max(
                MIN_CANVAS_SIZE,
                panelHeight - sidebarY - CANVAS_BOTTOM_MARGIN
        );

        return new Rectangle(sidebarX, sidebarY, sidebarWidth, sidebarHeight);
    }

    private static void drawTitleStrip(Graphics2D g2d, int panelWidth) {
        g2d.setColor(PANEL_BACKGROUND_COLOR);
        g2d.fillRect(
                INSET + 1,
                INSET + 1,
                Math.max(0, panelWidth - INSET * 2 - 2),
                INFO_SEPARATOR_Y - INSET - 1
        );

        g2d.setColor(BORDER_COLOR);
        g2d.drawLine(INSET, INFO_SEPARATOR_Y, panelWidth - INSET - 1, INFO_SEPARATOR_Y);
    }

    private static void drawDoubleFrame(Graphics2D g2d, int x, int y, int width, int height) {
        g2d.setColor(BORDER_COLOR);
        g2d.drawRect(x, y, width, height);
        g2d.drawRect(
                x + FRAME_INNER_INSET,
                y + FRAME_INNER_INSET,
                Math.max(0, width - FRAME_INNER_INSET * 2),
                Math.max(0, height - FRAME_INNER_INSET * 2)
        );
    }

    private static void applyStyle(Component component, Component excludedPanel) {
        if (component instanceof JScrollPane scrollPane) {
            scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
            scrollPane.getViewport().setBackground(PANEL_BACKGROUND_COLOR);
        }

        if (component instanceof JPanel panel && component != excludedPanel) {
            panel.setBackground(PANEL_BACKGROUND_COLOR);
        }

        if (component instanceof JLabel label) {
            label.setFont(CONTROL_FONT);
            label.setForeground(TEXT_COLOR);
            label.setBackground(PANEL_BACKGROUND_COLOR);
        }

        if (component instanceof AbstractButton button) {
            button.setFont(CONTROL_FONT);
            button.setForeground(TEXT_COLOR);
            button.setBackground(PANEL_BACKGROUND_COLOR);
            button.setFocusPainted(true);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_COLOR),
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createBevelBorder(BevelBorder.RAISED, HIGHLIGHT_COLOR, SHADOW_COLOR),
                            BorderFactory.createEmptyBorder(2, 12, 2, 12)
                    )
            ));
        }

        if (component instanceof JComboBox<?> comboBox) {
            comboBox.setFont(CONTROL_FONT);
            comboBox.setForeground(TEXT_COLOR);
            comboBox.setBackground(BACKGROUND_COLOR);
            comboBox.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        }
    }

    private static String formatRngModeLabel(RNProvider.Mode rngMode) {
        return rngMode == RNProvider.Mode.QUANTUM
                ? RNG_MODE_LABEL_QUANTUM
                : RNG_MODE_LABEL_PSEUDO;
    }
}
