package org.ThreeDotsSierpinski.app;

import org.ThreeDotsSierpinski.mode.VisualizationCategory;
import org.ThreeDotsSierpinski.mode.VisualizationMode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Builds category/mode cards for {@link ModeSelectionDialog}.
 */
final class ModeCardFactory {

    private static final String DESCRIPTION_LINE_SEPARATOR = "\\n";
    private static final String CARD_ARROW_TEXT = "→";
    private static final String CARD_CHECKED_TEXT = "✓";
    private static final String CATEGORY_COUNT_SUFFIX_ONE = " mode";
    private static final String CATEGORY_COUNT_SUFFIX_MANY = " modes";
    private static final String FONT_SANS_SERIF = "SansSerif";

    private static final int CARD_LAYOUT_H_GAP = 12;
    private static final int CARD_LAYOUT_V_GAP = 0;
    private static final int CARD_BORDER_THICKNESS = 1;
    private static final int CARD_BORDER_TOP = 14;
    private static final int CARD_BORDER_LEFT = 16;
    private static final int CARD_BORDER_BOTTOM = 14;
    private static final int CARD_BORDER_RIGHT = 16;
    private static final int CARD_MIN_WIDTH = 260;
    private static final int CARD_PREF_HEIGHT = 118;
    private static final int CARD_ICON_SIZE = 48;
    private static final int CARD_DESCRIPTION_TOP_SPACING = 4;

    private static final int ICON_FONT_SIZE = 32;
    private static final int MODE_NAME_FONT_SIZE = 15;
    private static final int MODE_DESCRIPTION_FONT_SIZE = 12;
    private static final int CATEGORY_COUNT_FONT_SIZE = 11;
    private static final int ARROW_FONT_SIZE = 20;

    private static final Color CARD_BACKGROUND = Color.WHITE;
    private static final Color CARD_HOVER_BACKGROUND = new Color(240, 245, 255);
    private static final Color CARD_SELECTED_BACKGROUND = new Color(232, 240, 255);
    private static final Color CARD_VISITED_BACKGROUND = new Color(232, 250, 235);
    private static final Color CARD_BORDER_COLOR = new Color(220, 220, 215);
    private static final Color CARD_HOVER_BORDER_COLOR = new Color(100, 140, 200);
    private static final Color CARD_SELECTED_BORDER_COLOR = new Color(120, 150, 210);
    private static final Color CARD_VISITED_BORDER_COLOR = new Color(115, 170, 130);
    private static final Color CARD_LAST_VISITED_BORDER_COLOR = new Color(210, 70, 70);
    private static final Color DESCRIPTION_COLOR = new Color(100, 100, 100);
    private static final Color CATEGORY_COUNT_COLOR = new Color(130, 130, 130);
    private static final Color ARROW_COLOR = new Color(180, 180, 180);

    private static final Font ICON_FONT = new Font(FONT_SANS_SERIF, Font.PLAIN, ICON_FONT_SIZE);
    private static final Font MODE_NAME_FONT = new Font(FONT_SANS_SERIF, Font.BOLD, MODE_NAME_FONT_SIZE);
    private static final Font MODE_DESCRIPTION_FONT = new Font(FONT_SANS_SERIF, Font.PLAIN, MODE_DESCRIPTION_FONT_SIZE);
    private static final Font CATEGORY_COUNT_FONT = new Font(FONT_SANS_SERIF, Font.BOLD, CATEGORY_COUNT_FONT_SIZE);
    private static final Font ARROW_FONT = new Font(FONT_SANS_SERIF, Font.PLAIN, ARROW_FONT_SIZE);

    private ModeCardFactory() {
    }

    static JPanel createCategoryCard(
            VisualizationCategory category,
            int modeCount,
            boolean highlighted,
            boolean visited,
            boolean lastVisited,
            Runnable onClick
    ) {
        JPanel card = createBaseCard(highlighted, visited, lastVisited);

        JLabel icon = new JLabel(category.getIcon());
        icon.setFont(ICON_FONT);
        icon.setPreferredSize(new Dimension(CARD_ICON_SIZE, CARD_ICON_SIZE));
        icon.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(icon, BorderLayout.WEST);

        card.add(createCategoryTextPanel(category, modeCount), BorderLayout.CENTER);

        JLabel arrow = new JLabel(visited ? CARD_CHECKED_TEXT : CARD_ARROW_TEXT);
        arrow.setFont(ARROW_FONT);
        arrow.setForeground(visited ? CARD_VISITED_BORDER_COLOR : ARROW_COLOR);
        card.add(arrow, BorderLayout.EAST);

        attachCardHoverAndClick(card, highlighted, visited, lastVisited, onClick);
        return card;
    }

    static JPanel createModeCard(
            VisualizationMode mode,
            boolean visited,
            boolean lastVisited,
            Runnable onClick
    ) {
        JPanel card = createBaseCard(false, visited, lastVisited);

        JLabel icon = new JLabel(mode.getIcon());
        icon.setFont(ICON_FONT);
        icon.setPreferredSize(new Dimension(CARD_ICON_SIZE, CARD_ICON_SIZE));
        icon.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(icon, BorderLayout.WEST);

        card.add(createModeTextPanel(mode), BorderLayout.CENTER);

        JLabel arrow = new JLabel(visited ? CARD_CHECKED_TEXT : CARD_ARROW_TEXT);
        arrow.setFont(ARROW_FONT);
        arrow.setForeground(visited ? CARD_VISITED_BORDER_COLOR : ARROW_COLOR);
        card.add(arrow, BorderLayout.EAST);

        attachCardHoverAndClick(card, false, visited, lastVisited, onClick);
        return card;
    }

    private static JPanel createBaseCard(boolean highlighted, boolean visited, boolean lastVisited) {
        JPanel card = new JPanel(new BorderLayout(CARD_LAYOUT_H_GAP, CARD_LAYOUT_V_GAP));
        card.setBorder(createCardBorder(resolveCardBorderColor(highlighted, visited, lastVisited)));
        card.setBackground(resolveCardBackground(highlighted, visited));
        card.setPreferredSize(new Dimension(CARD_MIN_WIDTH, CARD_PREF_HEIGHT));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return card;
    }

    private static void attachCardHoverAndClick(
            JPanel card,
            boolean highlighted,
            boolean visited,
            boolean lastVisited,
            Runnable onClick
    ) {
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(CARD_HOVER_BACKGROUND);
                card.setBorder(createCardBorder(CARD_HOVER_BORDER_COLOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(resolveCardBackground(highlighted, visited));
                card.setBorder(createCardBorder(resolveCardBorderColor(highlighted, visited, lastVisited)));
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                onClick.run();
            }
        });
    }

    private static Color resolveCardBackground(boolean highlighted, boolean visited) {
        if (visited) {
            return CARD_VISITED_BACKGROUND;
        }
        return highlighted ? CARD_SELECTED_BACKGROUND : CARD_BACKGROUND;
    }

    private static Color resolveCardBorderColor(boolean highlighted, boolean visited, boolean lastVisited) {
        if (lastVisited) {
            return CARD_LAST_VISITED_BORDER_COLOR;
        }
        if (visited) {
            return CARD_VISITED_BORDER_COLOR;
        }
        return highlighted ? CARD_SELECTED_BORDER_COLOR : CARD_BORDER_COLOR;
    }

    private static JPanel createCategoryTextPanel(VisualizationCategory category, int modeCount) {
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel name = new JLabel(category.getDisplayName());
        name.setFont(MODE_NAME_FONT);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        textPanel.add(name);

        textPanel.add(Box.createVerticalStrut(CARD_DESCRIPTION_TOP_SPACING));

        JLabel description = new JLabel(category.getDescription());
        description.setFont(MODE_DESCRIPTION_FONT);
        description.setForeground(DESCRIPTION_COLOR);
        description.setAlignmentX(Component.LEFT_ALIGNMENT);
        textPanel.add(description);

        textPanel.add(Box.createVerticalStrut(CARD_DESCRIPTION_TOP_SPACING));

        String countText = modeCount + (modeCount == 1 ? CATEGORY_COUNT_SUFFIX_ONE : CATEGORY_COUNT_SUFFIX_MANY);
        JLabel count = new JLabel(countText);
        count.setFont(CATEGORY_COUNT_FONT);
        count.setForeground(CATEGORY_COUNT_COLOR);
        count.setAlignmentX(Component.LEFT_ALIGNMENT);
        textPanel.add(count);

        return textPanel;
    }

    private static JPanel createModeTextPanel(VisualizationMode mode) {
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel name = new JLabel(mode.getName());
        name.setFont(MODE_NAME_FONT);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        textPanel.add(name);

        textPanel.add(Box.createVerticalStrut(CARD_DESCRIPTION_TOP_SPACING));

        for (String line : mode.getDescription().split(DESCRIPTION_LINE_SEPARATOR)) {
            JLabel description = new JLabel(line);
            description.setFont(MODE_DESCRIPTION_FONT);
            description.setForeground(DESCRIPTION_COLOR);
            description.setAlignmentX(Component.LEFT_ALIGNMENT);
            textPanel.add(description);
        }

        return textPanel;
    }

    private static javax.swing.border.Border createCardBorder(Color color) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, CARD_BORDER_THICKNESS, true),
                BorderFactory.createEmptyBorder(
                        CARD_BORDER_TOP,
                        CARD_BORDER_LEFT,
                        CARD_BORDER_BOTTOM,
                        CARD_BORDER_RIGHT
                )
        );
    }
}
