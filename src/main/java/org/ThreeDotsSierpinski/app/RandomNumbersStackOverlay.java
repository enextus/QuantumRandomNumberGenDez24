package org.ThreeDotsSierpinski.app;

import org.ThreeDotsSierpinski.mode.VisualizationStyle;
import org.ThreeDotsSierpinski.rng.RNProvider;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Draws the consumed random numbers stack overlay.
 */
final class RandomNumbersStackOverlay {

    private static final int MIN_CANVAS_SIZE = 1;
    private static final int MIN_DIGIT_GROUP = 1;
    private static final int MAX_DIGIT_GROUP = 5;

    private static final int CELL_HORIZONTAL_PADDING = 4;
    private static final int TOP_MARGIN = 18;
    private static final int RIGHT_MARGIN = 12;
    private static final int COLUMN_GAP = 4;
    private static final int HEADER_HEIGHT = 18;
    private static final int CELL_HEIGHT = 18;
    private static final int BOTTOM_RESERVED_SPACE = 250;
    private static final int DIGIT_GROUP_COUNT = MAX_DIGIT_GROUP - MIN_DIGIT_GROUP + 1;
    private static final int HISTORY_OVERSCAN_FACTOR = 4;
    private static final int MIN_SNAPSHOT_SIZE = 5_000;

    private static final Font HEADER_FONT = new Font("Monospaced", Font.PLAIN, 10);
    private static final Font VALUE_FONT = new Font("Monospaced", Font.PLAIN, 11);

    private static final Color DARK_HEADER_COLOR = new Color(142, 163, 188);
    private static final Color DARK_VALUE_COLOR = new Color(230, 238, 248);
    private static final Color DARK_ROW_BACKGROUND = new Color(14, 26, 42, 220);

    private static final Color HEADER_COLOR = new Color(130, 130, 130);
    private static final Color VALUE_COLOR = Color.BLACK;
    private static final Color ROW_BACKGROUND = new Color(245, 245, 245);

    private RandomNumbersStackOverlay() {
    }

    static void draw(
            Graphics g,
            int panelWidth,
            int panelHeight,
            boolean dark,
            VisualizationStyle style,
            RNProvider randomNumberProvider
    ) {
        int startY = getTopMargin(style);
        int visibleRows = calculateVisibleRows(panelHeight, startY, style);
        int snapshotLimit = calculateSnapshotLimit(visibleRows);

        List<Long> numbers = randomNumberProvider.getLastConsumedNumbers(snapshotLimit);
        if (numbers.isEmpty()) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Map<Integer, List<Long>> numbersByDigits = groupNumbersByDigitCount(numbers);

            int stackWidth = calculateStackWidth(g2d, style);
            int currentX = style == VisualizationStyle.APPLE_MAC
                    ? calculateAppleMacStackX(g2d, panelWidth)
                    : Math.max(
                    getRightMargin(style),
                    panelWidth - getRightMargin(style) - stackWidth
            );
            int startX = currentX;
            int stackFrameY = startY + getFrameTopOffset(style);
            int stackHeight = getFrameHeight(style, visibleRows);

            for (int digitCount = MIN_DIGIT_GROUP; digitCount <= MAX_DIGIT_GROUP; digitCount++) {
                List<Long> columnNumbers = numbersByDigits.getOrDefault(digitCount, List.of());

                int columnWidth = calculateDigitColumnWidth(g2d, digitCount, style);
                drawDigitColumn(
                        g2d,
                        columnNumbers,
                        digitCount,
                        currentX,
                        startY,
                        columnWidth,
                        visibleRows,
                        dark,
                        style
                );

                currentX += columnWidth + getColumnGap(style);
            }

            if (style == VisualizationStyle.APPLE_MAC) {
                drawOuterBorder(g2d, startX, stackFrameY, stackWidth, stackHeight);
            }
        } finally {
            g2d.dispose();
        }
    }

    static int calculateAppleMacStackX(Graphics2D g2d, int panelWidth) {
        int stackWidth = calculateStackWidth(g2d, VisualizationStyle.APPLE_MAC);
        int rightMargin = getRightMargin(VisualizationStyle.APPLE_MAC);

        return Math.max(
                rightMargin,
                panelWidth - rightMargin - stackWidth
        );
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

    private static int calculateStackWidth(Graphics2D g2d, VisualizationStyle style) {
        int totalWidth = 0;

        for (int digitCount = MIN_DIGIT_GROUP; digitCount <= MAX_DIGIT_GROUP; digitCount++) {
            if (digitCount > MIN_DIGIT_GROUP) {
                totalWidth += getColumnGap(style);
            }

            totalWidth += calculateDigitColumnWidth(g2d, digitCount, style);
        }

        return totalWidth;
    }

    private static int calculateDigitColumnWidth(Graphics2D g2d, int digitCount, VisualizationStyle style) {
        Font valueFont = style == VisualizationStyle.APPLE_MAC ? AppleMacChrome.RANDOM_STACK_FONT : VALUE_FONT;
        FontMetrics valueMetrics = g2d.getFontMetrics(valueFont);

        int digitWidth = valueMetrics.charWidth('0');
        int valueWidth = digitWidth * digitCount;

        int width = valueWidth + getCellHorizontalPadding(style) * 2;

        if (style == VisualizationStyle.APPLE_MAC && digitCount == MAX_DIGIT_GROUP) {
            width -= AppleMacChrome.RANDOM_STACK_RIGHT_TRIM;
        }

        return Math.max(MIN_CANVAS_SIZE, width);
    }

    private static void drawDigitColumn(
            Graphics2D g2d,
            List<Long> numbers,
            int digitCount,
            int x,
            int y,
            int columnWidth,
            int visibleRows,
            boolean dark,
            VisualizationStyle style
    ) {
        int headerY = y + getHeaderYOffset(style);
        drawDigitColumnHeader(g2d, digitCount, x, headerY, columnWidth, dark, style);

        int fromIndex = Math.max(0, numbers.size() - visibleRows);
        List<Long> visibleNumbers = numbers.subList(fromIndex, numbers.size());

        int rowY = y + getRowsTopOffset(style);

        for (int rowIndex = 0; rowIndex < visibleRows; rowIndex++) {
            Long number = rowIndex < visibleNumbers.size() ? visibleNumbers.get(rowIndex) : null;
            drawDigitColumnValue(g2d, number, digitCount, x, rowY, columnWidth, dark, style);
            rowY += getCellHeight(style);
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
            g2d.setColor(AppleMacChrome.STACK_HEADER_BACKGROUND);
            g2d.fillRect(x, y, columnWidth, getHeaderHeight(style) - 1);
        }

        g2d.setFont(style == VisualizationStyle.APPLE_MAC ? AppleMacChrome.RANDOM_STACK_FONT : HEADER_FONT);
        g2d.setColor(resolveHeaderColor(dark, style));

        FontMetrics metrics = g2d.getFontMetrics();
        int textX = x + Math.max(0, (columnWidth - metrics.stringWidth(header)) / 2);
        int textY = y + metrics.getAscent();

        g2d.drawString(header, textX, textY);

        if (style == VisualizationStyle.APPLE_MAC) {
            g2d.setColor(AppleMacChrome.BORDER_COLOR);
            g2d.drawRect(x, y, Math.max(0, columnWidth - 1), getHeaderHeight(style) - 1);
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

        g2d.setColor(resolveRowBackground(dark, style));
        g2d.fillRect(x, y, columnWidth, getCellHeight(style) - 1);

        if (text != null) {
            g2d.setFont(style == VisualizationStyle.APPLE_MAC ? AppleMacChrome.RANDOM_STACK_FONT : VALUE_FONT);
            g2d.setColor(resolveValueColor(dark, style));

            FontMetrics metrics = g2d.getFontMetrics();

            int textX = x + columnWidth - getCellHorizontalPadding(style) - metrics.stringWidth(text);
            int textY = y + metrics.getAscent();

            g2d.drawString(text, textX, textY);
        }

        if (style == VisualizationStyle.APPLE_MAC) {
            g2d.setColor(AppleMacChrome.BORDER_COLOR);
            g2d.drawRect(x, y, Math.max(0, columnWidth - 1), getCellHeight(style) - 1);
        }
    }

    private static String formatNumberForDigitColumn(long number, int digitCount) {
        return String.format(Locale.US, "%" + digitCount + "d", number);
    }

    private static int calculateVisibleRows(int panelHeight, int startY, VisualizationStyle style) {
        int availableHeight = Math.max(
                0,
                panelHeight - startY - getRowsTopOffset(style) - getBottomReservedSpace(style)
        );

        int visibleRows = Math.max(1, availableHeight / getCellHeight(style));
        if (style == VisualizationStyle.APPLE_MAC) {
            return Math.min(AppleMacChrome.TABLE_MAX_ROWS, visibleRows);
        }

        return visibleRows;
    }

    private static int calculateSnapshotLimit(int visibleRows) {
        long calculatedLimit = (long) Math.max(1, visibleRows) * DIGIT_GROUP_COUNT * HISTORY_OVERSCAN_FACTOR;

        return (int) Math.min(
                Integer.MAX_VALUE,
                Math.max(MIN_SNAPSHOT_SIZE, calculatedLimit)
        );
    }

    private static int getTopMargin(VisualizationStyle style) {
        return style == VisualizationStyle.APPLE_MAC ? AppleMacChrome.TABLE_TOP_MARGIN : TOP_MARGIN;
    }

    private static int getRightMargin(VisualizationStyle style) {
        return style == VisualizationStyle.APPLE_MAC
                ? AppleMacChrome.FRAME_INSET + AppleMacChrome.FRAME_INNER_INSET
                : RIGHT_MARGIN;
    }

    private static int getColumnGap(VisualizationStyle style) {
        return style == VisualizationStyle.APPLE_MAC ? AppleMacChrome.RANDOM_STACK_COLUMN_GAP : COLUMN_GAP;
    }

    private static int getValuesYOffset(VisualizationStyle style) {
        return style == VisualizationStyle.APPLE_MAC ? AppleMacChrome.RANDOM_STACK_VALUES_Y_OFFSET : 0;
    }

    private static int getHeaderHeight(VisualizationStyle style) {
        return style == VisualizationStyle.APPLE_MAC ? AppleMacChrome.RANDOM_STACK_HEADER_HEIGHT : HEADER_HEIGHT;
    }

    private static int getCellHeight(VisualizationStyle style) {
        return style == VisualizationStyle.APPLE_MAC ? AppleMacChrome.RANDOM_STACK_CELL_HEIGHT : CELL_HEIGHT;
    }

    private static int getCellHorizontalPadding(VisualizationStyle style) {
        return style == VisualizationStyle.APPLE_MAC
                ? AppleMacChrome.RANDOM_STACK_CELL_HORIZONTAL_PADDING
                : CELL_HORIZONTAL_PADDING;
    }

    private static int getBottomReservedSpace(VisualizationStyle style) {
        return style == VisualizationStyle.APPLE_MAC
                ? Math.max(BOTTOM_RESERVED_SPACE, getCellHeight(style) * 10)
                : BOTTOM_RESERVED_SPACE;
    }

    private static int getHeaderYOffset(VisualizationStyle style) {
        return style == VisualizationStyle.APPLE_MAC ? AppleMacChrome.RANDOM_STACK_HEADER_Y_OFFSET : 0;
    }

    private static int getValuesTopOffset(VisualizationStyle style) {
        return getValuesYOffset(style) + getHeaderHeight(style);
    }

    private static int getRowsTopOffset(VisualizationStyle style) {
        return Math.max(
                getHeaderYOffset(style) + getHeaderHeight(style),
                getValuesTopOffset(style)
        );
    }

    private static int getFrameTopOffset(VisualizationStyle style) {
        return Math.min(getHeaderYOffset(style), getValuesTopOffset(style));
    }

    private static int getFrameHeight(VisualizationStyle style, int visibleRows) {
        int frameTopOffset = getFrameTopOffset(style);
        int frameBottomOffset = getRowsTopOffset(style) + visibleRows * getCellHeight(style);

        return Math.max(0, frameBottomOffset - frameTopOffset);
    }

    private static void drawOuterBorder(Graphics2D g2d, int x, int y, int width, int height) {
        g2d.setColor(AppleMacChrome.BORDER_COLOR);
        g2d.drawRect(x, y, Math.max(0, width - 1), Math.max(0, height - 1));
    }

    private static Color resolveHeaderColor(boolean dark, VisualizationStyle style) {
        if (style == VisualizationStyle.APPLE_MAC) {
            return AppleMacChrome.TEXT_COLOR;
        }

        return dark ? DARK_HEADER_COLOR : HEADER_COLOR;
    }

    private static Color resolveValueColor(boolean dark, VisualizationStyle style) {
        if (style == VisualizationStyle.APPLE_MAC) {
            return AppleMacChrome.TEXT_COLOR;
        }

        return dark ? DARK_VALUE_COLOR : VALUE_COLOR;
    }

    private static Color resolveRowBackground(boolean dark, VisualizationStyle style) {
        if (style == VisualizationStyle.APPLE_MAC) {
            return AppleMacChrome.STACK_ROW_BACKGROUND;
        }

        return dark ? DARK_ROW_BACKGROUND : ROW_BACKGROUND;
    }
}
