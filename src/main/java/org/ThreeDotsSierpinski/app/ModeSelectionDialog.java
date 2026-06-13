package org.ThreeDotsSierpinski.app;

import org.ThreeDotsSierpinski.mode.VisualizationCategory;
import org.ThreeDotsSierpinski.mode.VisualizationMode;
import org.ThreeDotsSierpinski.mode.VisualizationModes;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Диалог выбора режима визуализации.
 * <p>
 * Сначала показывает категории научных визуализаций, затем — режимы внутри
 * выбранной категории. Это удерживает меню компактным даже при большом
 * количестве режимов.
 */
public class ModeSelectionDialog {

    private static final String DIALOG_TITLE = "Quantum Random Visualizer";
    private static final String SUBTITLE_CATEGORY_TEXT = "Выберите категорию визуализаций";
    private static final String SUBTITLE_MODE_SEPARATOR = " — ";
    private static final String SUBTITLE_MODE_SUFFIX = " modes";
    private static final String FOOTER_TEXT =
            "Powered by ANU Quantum Random Numbers API + L128X256MixRandom fallback";
    private static final String BACK_BUTTON_TEXT = "← Back to categories";

    private static final String FONT_SANS_SERIF = "SansSerif";

    private static final int DIALOG_LAYOUT_H_GAP = 0;
    private static final int DIALOG_LAYOUT_V_GAP = 0;

    private static final int DIALOG_WIDTH = 1200;
    private static final int DIALOG_MAX_HEIGHT = 900;
    private static final int DIALOG_MIN_WIDTH = 700;
    private static final int DIALOG_MIN_HEIGHT = 300;

    private static final int MODE_GRID_COLUMNS = 3;
    private static final int MODE_GRID_H_GAP = 12;
    private static final int MODE_GRID_V_GAP = 12;

    private static final int SCROLL_UNIT_INCREMENT = 16;

    private static final int HEADER_BORDER_TOP = 20;
    private static final int HEADER_BORDER_LEFT = 24;
    private static final int HEADER_BORDER_BOTTOM = 10;
    private static final int HEADER_BORDER_RIGHT = 24;

    private static final int CARDS_BORDER_TOP = 10;
    private static final int CARDS_BORDER_LEFT = 20;
    private static final int CARDS_BORDER_BOTTOM = 20;
    private static final int CARDS_BORDER_RIGHT = 20;

    private static final int BACK_BAR_BORDER_TOP = 8;
    private static final int BACK_BAR_BORDER_LEFT = 20;
    private static final int BACK_BAR_BORDER_BOTTOM = 4;
    private static final int BACK_BAR_BORDER_RIGHT = 20;

    private static final int FOOTER_BORDER_TOP = 4;
    private static final int FOOTER_BORDER_LEFT = 0;
    private static final int FOOTER_BORDER_BOTTOM = 12;
    private static final int FOOTER_BORDER_RIGHT = 0;

    private static final int TITLE_FONT_SIZE = 20;
    private static final int SUBTITLE_FONT_SIZE = 13;
    private static final int FOOTER_FONT_SIZE = 10;
    private static final int BACK_BUTTON_FONT_SIZE = 12;

    private static final Color HEADER_BACKGROUND = new Color(245, 245, 242);
    private static final Color FOOTER_BACKGROUND = new Color(245, 245, 242);
    private static final Color CARDS_BACKGROUND = Color.WHITE;
    private static final Color BACK_BAR_BACKGROUND = Color.WHITE;
    private static final Color SUBTITLE_COLOR = new Color(120, 120, 120);
    private static final Color FOOTER_COLOR = new Color(160, 160, 160);
    private static final Font TITLE_FONT = new Font(FONT_SANS_SERIF, Font.BOLD, TITLE_FONT_SIZE);
    private static final Font SUBTITLE_FONT = new Font(FONT_SANS_SERIF, Font.PLAIN, SUBTITLE_FONT_SIZE);
    private static final Font FOOTER_FONT = new Font(FONT_SANS_SERIF, Font.PLAIN, FOOTER_FONT_SIZE);
    private static final Font BACK_BUTTON_FONT = new Font(FONT_SANS_SERIF, Font.PLAIN, BACK_BUTTON_FONT_SIZE);
    private VisualizationMode selectedMode = null;
    private VisualizationCategory highlightedCategory = null;
    private Set<String> visitedModeIds = Set.of();
    private String lastSelectedModeId = null;
    private GraphicsConfiguration lastDialogGraphicsConfiguration = null;

    private static Map<VisualizationCategory, List<VisualizationMode>> groupModesByCategory(VisualizationMode[] modes) {
        Map<VisualizationCategory, List<VisualizationMode>> modesByCategory =
                new EnumMap<>(VisualizationCategory.class);

        for (VisualizationCategory category : VisualizationCategory.values()) {
            modesByCategory.put(category, new ArrayList<>());
        }

        for (VisualizationMode mode : modes) {
            modesByCategory.computeIfAbsent(mode.getCategory(), ignored -> new ArrayList<>()).add(mode);
        }

        return modesByCategory;
    }

    private static JPanel createHeaderPanel(JLabel subtitleLabel) {
        var header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(
                HEADER_BORDER_TOP,
                HEADER_BORDER_LEFT,
                HEADER_BORDER_BOTTOM,
                HEADER_BORDER_RIGHT
        ));
        header.setBackground(HEADER_BACKGROUND);

        var title = new JLabel(DIALOG_TITLE);
        title.setFont(TITLE_FONT);
        header.add(title, BorderLayout.WEST);

        subtitleLabel.setFont(SUBTITLE_FONT);
        subtitleLabel.setForeground(SUBTITLE_COLOR);
        header.add(subtitleLabel, BorderLayout.SOUTH);

        return header;
    }

    private static JPanel createCardsPanel() {
        var cardsPanel = new ResponsiveCardsPanel(
                MODE_GRID_COLUMNS,
                MODE_GRID_H_GAP,
                MODE_GRID_V_GAP,
                260,
                118
        );
        cardsPanel.setBorder(BorderFactory.createEmptyBorder(
                CARDS_BORDER_TOP,
                CARDS_BORDER_LEFT,
                CARDS_BORDER_BOTTOM,
                CARDS_BORDER_RIGHT
        ));
        cardsPanel.setBackground(CARDS_BACKGROUND);
        return cardsPanel;
    }

    private static JScrollPane createCardsScrollPane(JPanel cardsPanel) {
        var scrollPane = new JScrollPane(cardsPanel);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().setBackground(CARDS_BACKGROUND);
        scrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_UNIT_INCREMENT);
        return scrollPane;
    }

    private static JPanel createFooterPanel() {
        var footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setBorder(BorderFactory.createEmptyBorder(
                FOOTER_BORDER_TOP,
                FOOTER_BORDER_LEFT,
                FOOTER_BORDER_BOTTOM,
                FOOTER_BORDER_RIGHT
        ));
        footer.setBackground(FOOTER_BACKGROUND);

        var footerLabel = new JLabel(FOOTER_TEXT);
        footerLabel.setFont(FOOTER_FONT);
        footerLabel.setForeground(FOOTER_COLOR);
        footer.add(footerLabel);

        return footer;
    }

    public GraphicsConfiguration getLastDialogGraphicsConfiguration() {
        return lastDialogGraphicsConfiguration;
    }

    /**
     * Показывает диалог и центрирует его на указанном мониторе.
     *
     * @param parent                      родительский фрейм (может быть null)
     * @param targetGraphicsConfiguration целевой монитор/экран
     * @return выбранный режим, или null если закрыли без выбора
     */
    public VisualizationMode showAndWait(
            JFrame parent,
            GraphicsConfiguration targetGraphicsConfiguration,
            VisualizationCategory initialCategory,
            Set<String> visitedModeIds,
            String lastSelectedModeId
    ) {
        selectedMode = null;
        highlightedCategory = initialCategory;
        this.visitedModeIds = visitedModeIds == null ? Set.of() : Set.copyOf(visitedModeIds);
        this.lastSelectedModeId = lastSelectedModeId;
        lastDialogGraphicsConfiguration = targetGraphicsConfiguration;

        VisualizationMode[] modes = VisualizationModes.all();
        Map<VisualizationCategory, List<VisualizationMode>> modesByCategory = groupModesByCategory(modes);

        var dialog = new JDialog(parent, DIALOG_TITLE, true);
        dialog.setLayout(new BorderLayout(DIALOG_LAYOUT_H_GAP, DIALOG_LAYOUT_V_GAP));
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        var subtitleLabel = new JLabel(SUBTITLE_CATEGORY_TEXT);
        dialog.add(createHeaderPanel(subtitleLabel), BorderLayout.NORTH);

        var contentPanel = new JPanel(new BorderLayout(DIALOG_LAYOUT_H_GAP, DIALOG_LAYOUT_V_GAP));
        contentPanel.setBackground(CARDS_BACKGROUND);
        dialog.add(contentPanel, BorderLayout.CENTER);

        dialog.add(createFooterPanel(), BorderLayout.SOUTH);

        if (initialCategory != null && !modesByCategory.getOrDefault(initialCategory, List.of()).isEmpty()) {
            showModeSelection(
                    initialCategory,
                    modesByCategory.get(initialCategory),
                    modesByCategory,
                    contentPanel,
                    dialog,
                    subtitleLabel
            );
        } else {
            showCategorySelection(contentPanel, modesByCategory, dialog, subtitleLabel);
        }

        dialog.setSize(DIALOG_WIDTH, DIALOG_MAX_HEIGHT);
        dialog.setMinimumSize(new Dimension(DIALOG_MIN_WIDTH, DIALOG_MIN_HEIGHT));
        ScreenPlacement.centerDialog(dialog, parent, targetGraphicsConfiguration);
        dialog.setVisible(true);

        lastDialogGraphicsConfiguration = ScreenPlacement.resolveGraphicsConfiguration(dialog);

        return selectedMode;
    }

    public VisualizationMode showAndWait(
            JFrame parent,
            GraphicsConfiguration targetGraphicsConfiguration,
            VisualizationCategory initialCategory,
            Set<String> visitedModeIds
    ) {
        return showAndWait(parent, targetGraphicsConfiguration, initialCategory, visitedModeIds, null);
    }

    public VisualizationMode showAndWait(
            JFrame parent,
            GraphicsConfiguration targetGraphicsConfiguration,
            VisualizationCategory initialCategory
    ) {
        return showAndWait(parent, targetGraphicsConfiguration, initialCategory, Set.of(), null);
    }

    private void showCategorySelection(
            JPanel contentPanel,
            Map<VisualizationCategory, List<VisualizationMode>> modesByCategory,
            JDialog dialog,
            JLabel subtitleLabel
    ) {
        subtitleLabel.setText(SUBTITLE_CATEGORY_TEXT);
        contentPanel.removeAll();
        contentPanel.add(createCategoryCardsScrollPane(modesByCategory, contentPanel, dialog, subtitleLabel), BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void showModeSelection(
            VisualizationCategory category,
            List<VisualizationMode> modes,
            Map<VisualizationCategory, List<VisualizationMode>> modesByCategory,
            JPanel contentPanel,
            JDialog dialog,
            JLabel subtitleLabel
    ) {
        subtitleLabel.setText(category.getDisplayName() + SUBTITLE_MODE_SEPARATOR + modes.size() + SUBTITLE_MODE_SUFFIX);
        contentPanel.removeAll();
        contentPanel.add(createBackBar(contentPanel, modesByCategory, dialog, subtitleLabel), BorderLayout.NORTH);
        contentPanel.add(createModeCardsScrollPane(modes, dialog), BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private JScrollPane createCategoryCardsScrollPane(
            Map<VisualizationCategory, List<VisualizationMode>> modesByCategory,
            JPanel contentPanel,
            JDialog dialog,
            JLabel subtitleLabel
    ) {
        var cardsPanel = createCardsPanel();

        for (VisualizationCategory category : VisualizationCategory.values()) {
            List<VisualizationMode> categoryModes = modesByCategory.getOrDefault(category, List.of());
            if (!categoryModes.isEmpty()) {
                boolean categoryVisited = containsVisitedMode(categoryModes);
                boolean categoryLastVisited = containsLastVisitedMode(categoryModes);
                cardsPanel.add(ModeCardFactory.createCategoryCard(
                        category,
                        categoryModes.size(),
                        category == highlightedCategory,
                        categoryVisited,
                        categoryLastVisited,
                        () -> {
                            highlightedCategory = category;
                            showModeSelection(
                                    category,
                                    categoryModes,
                                    modesByCategory,
                                    contentPanel,
                                    dialog,
                                    subtitleLabel
                            );
                        }
                ));
            }
        }

        return createCardsScrollPane(cardsPanel);
    }

    private JScrollPane createModeCardsScrollPane(List<VisualizationMode> modes, JDialog dialog) {
        var cardsPanel = createCardsPanel();

        for (var mode : modes) {
            boolean visited = visitedModeIds.contains(mode.getId());
            boolean lastVisited = mode.getId().equals(lastSelectedModeId);
            cardsPanel.add(ModeCardFactory.createModeCard(
                    mode,
                    visited,
                    lastVisited,
                    () -> {
                        selectedMode = mode;
                        dialog.dispose();
                    }
            ));
        }

        return createCardsScrollPane(cardsPanel);
    }

    private JPanel createBackBar(
            JPanel contentPanel,
            Map<VisualizationCategory, List<VisualizationMode>> modesByCategory,
            JDialog dialog,
            JLabel subtitleLabel
    ) {
        var backBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        backBar.setBorder(BorderFactory.createEmptyBorder(
                BACK_BAR_BORDER_TOP,
                BACK_BAR_BORDER_LEFT,
                BACK_BAR_BORDER_BOTTOM,
                BACK_BAR_BORDER_RIGHT
        ));
        backBar.setBackground(BACK_BAR_BACKGROUND);

        var backButton = new JButton(BACK_BUTTON_TEXT);
        backButton.setFont(BACK_BUTTON_FONT);
        backButton.addActionListener(ignored -> showCategorySelection(
                contentPanel,
                modesByCategory,
                dialog,
                subtitleLabel
        ));
        backBar.add(backButton);

        return backBar;
    }

    private boolean containsVisitedMode(List<VisualizationMode> modes) {
        for (VisualizationMode mode : modes) {
            if (visitedModeIds.contains(mode.getId())) {
                return true;
            }
        }
        return false;
    }

    private boolean containsLastVisitedMode(List<VisualizationMode> modes) {
        if (lastSelectedModeId == null) {
            return false;
        }

        for (VisualizationMode mode : modes) {
            if (mode.getId().equals(lastSelectedModeId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Responsive fixed-height grid for mode/category cards.
     * <p>
     * GridLayout stretches cards to the full viewport height when only a few rows
     * are present. This panel keeps cards compact and only adapts their width to
     * the current dialog width.
     */
    private static final class ResponsiveCardsPanel extends JPanel implements Scrollable {

        private final int preferredColumns;
        private final int horizontalGap;
        private final int verticalGap;
        private final int minCardWidth;
        private final int cardHeight;

        private ResponsiveCardsPanel(
                int preferredColumns,
                int horizontalGap,
                int verticalGap,
                int minCardWidth,
                int cardHeight
        ) {
            super(null);
            this.preferredColumns = Math.max(1, preferredColumns);
            this.horizontalGap = Math.max(0, horizontalGap);
            this.verticalGap = Math.max(0, verticalGap);
            this.minCardWidth = Math.max(1, minCardWidth);
            this.cardHeight = Math.max(1, cardHeight);
        }

        @Override
        public void doLayout() {
            Insets insets = getInsets();
            int availableWidth = Math.max(1, getWidth() - insets.left - insets.right);
            int columns = calculateColumns(availableWidth);
            int cardWidth = calculateCardWidth(availableWidth, columns);

            for (int index = 0; index < getComponentCount(); index++) {
                Component component = getComponent(index);
                int row = index / columns;
                int column = index % columns;

                int x = insets.left + column * (cardWidth + horizontalGap);
                int y = insets.top + row * (cardHeight + verticalGap);

                component.setBounds(x, y, cardWidth, cardHeight);
            }
        }

        @Override
        public Dimension getPreferredSize() {
            int parentWidth = getParent() == null ? 0 : getParent().getWidth();
            int width = parentWidth > 0 ? parentWidth : DIALOG_WIDTH;
            Insets insets = getInsets();
            int availableWidth = Math.max(1, width - insets.left - insets.right);
            int columns = calculateColumns(availableWidth);
            int rows = calculateRows(columns);
            int preferredHeight = insets.top
                    + insets.bottom
                    + rows * cardHeight
                    + Math.max(0, rows - 1) * verticalGap;

            return new Dimension(width, preferredHeight);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return SCROLL_UNIT_INCREMENT;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(SCROLL_UNIT_INCREMENT, visibleRect.height - SCROLL_UNIT_INCREMENT);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }

        private int calculateColumns(int availableWidth) {
            int maxColumnsByWidth = Math.max(1, (availableWidth + horizontalGap) / (minCardWidth + horizontalGap));
            return Math.max(1, Math.min(preferredColumns, maxColumnsByWidth));
        }

        private int calculateCardWidth(int availableWidth, int columns) {
            int gapsWidth = Math.max(0, columns - 1) * horizontalGap;
            return Math.max(minCardWidth, (availableWidth - gapsWidth) / columns);
        }

        private int calculateRows(int columns) {
            if (getComponentCount() == 0) {
                return 0;
            }

            return (int) Math.ceil(getComponentCount() / (double) Math.max(1, columns));
        }
    }

}
