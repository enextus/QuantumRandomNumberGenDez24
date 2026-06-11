package org.ThreeDotsSierpinski.app;

import org.ThreeDotsSierpinski.mode.VisualizationCategory;
import org.ThreeDotsSierpinski.mode.VisualizationMode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Диалог выбора режима визуализации.
 *
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
    private static final String DESCRIPTION_LINE_SEPARATOR = "\\n";
    private static final String CARD_ARROW_TEXT = "→";
    private static final String BACK_BUTTON_TEXT = "← Back to categories";
    private static final String CATEGORY_COUNT_SUFFIX_ONE = " mode";
    private static final String CATEGORY_COUNT_SUFFIX_MANY = " modes";

    private static final String FONT_SANS_SERIF = "SansSerif";

    private static final int DIALOG_LAYOUT_H_GAP = 0;
    private static final int DIALOG_LAYOUT_V_GAP = 0;

    private static final int DIALOG_WIDTH = 1080;
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

    private static final int TITLE_FONT_SIZE = 20;
    private static final int SUBTITLE_FONT_SIZE = 13;
    private static final int FOOTER_FONT_SIZE = 10;
    private static final int ICON_FONT_SIZE = 32;
    private static final int MODE_NAME_FONT_SIZE = 15;
    private static final int MODE_DESCRIPTION_FONT_SIZE = 12;
    private static final int CATEGORY_COUNT_FONT_SIZE = 11;
    private static final int ARROW_FONT_SIZE = 20;

    private static final int SCREEN_CENTER_DIVISOR = 2;
    private static final int MIN_USABLE_SCREEN_SIZE = 1;
    private static final int NO_INTERSECTION_AREA = 0;
    private static final int MIN_CENTERING_OFFSET = 0;

    private static final Color HEADER_BACKGROUND = new Color(245, 245, 242);
    private static final Color FOOTER_BACKGROUND = new Color(245, 245, 242);
    private static final Color CARDS_BACKGROUND = Color.WHITE;
    private static final Color BACK_BAR_BACKGROUND = Color.WHITE;
    private static final Color CARD_BACKGROUND = Color.WHITE;
    private static final Color CARD_HOVER_BACKGROUND = new Color(240, 245, 255);
    private static final Color CARD_BORDER_COLOR = new Color(220, 220, 215);
    private static final Color CARD_HOVER_BORDER_COLOR = new Color(100, 140, 200);
    private static final Color SUBTITLE_COLOR = new Color(120, 120, 120);
    private static final Color FOOTER_COLOR = new Color(160, 160, 160);
    private static final Color DESCRIPTION_COLOR = new Color(100, 100, 100);
    private static final Color CATEGORY_COUNT_COLOR = new Color(130, 130, 130);
    private static final Color ARROW_COLOR = new Color(180, 180, 180);

    private static final Font TITLE_FONT = new Font(FONT_SANS_SERIF, Font.BOLD, TITLE_FONT_SIZE);
    private static final Font SUBTITLE_FONT = new Font(FONT_SANS_SERIF, Font.PLAIN, SUBTITLE_FONT_SIZE);
    private static final Font FOOTER_FONT = new Font(FONT_SANS_SERIF, Font.PLAIN, FOOTER_FONT_SIZE);
    private static final Font ICON_FONT = new Font(FONT_SANS_SERIF, Font.PLAIN, ICON_FONT_SIZE);
    private static final Font MODE_NAME_FONT = new Font(FONT_SANS_SERIF, Font.BOLD, MODE_NAME_FONT_SIZE);
    private static final Font MODE_DESCRIPTION_FONT = new Font(FONT_SANS_SERIF, Font.PLAIN, MODE_DESCRIPTION_FONT_SIZE);
    private static final Font CATEGORY_COUNT_FONT = new Font(FONT_SANS_SERIF, Font.BOLD, CATEGORY_COUNT_FONT_SIZE);
    private static final Font ARROW_FONT = new Font(FONT_SANS_SERIF, Font.PLAIN, ARROW_FONT_SIZE);

    private VisualizationMode selectedMode = null;
    private GraphicsConfiguration lastDialogGraphicsConfiguration = null;

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
            GraphicsConfiguration targetGraphicsConfiguration
    ) {
        selectedMode = null;
        lastDialogGraphicsConfiguration = targetGraphicsConfiguration;

        VisualizationMode[] modes = VisualizationMode.allModes();
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

        showCategorySelection(contentPanel, modesByCategory, dialog, subtitleLabel);

        dialog.setSize(DIALOG_WIDTH, DIALOG_MAX_HEIGHT);
        dialog.setMinimumSize(new Dimension(DIALOG_MIN_WIDTH, DIALOG_MIN_HEIGHT));
        centerDialog(dialog, parent, targetGraphicsConfiguration);
        dialog.setVisible(true);

        lastDialogGraphicsConfiguration = resolveDialogGraphicsConfiguration(dialog);

        return selectedMode;
    }

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
                cardsPanel.add(createCategoryCard(
                        category,
                        categoryModes,
                        modesByCategory,
                        contentPanel,
                        dialog,
                        subtitleLabel
                ));
            }
        }

        return createCardsScrollPane(cardsPanel);
    }

    private JScrollPane createModeCardsScrollPane(List<VisualizationMode> modes, JDialog dialog) {
        var cardsPanel = createCardsPanel();

        for (var mode : modes) {
            cardsPanel.add(createModeCard(mode, dialog));
        }

        return createCardsScrollPane(cardsPanel);
    }

    private static JPanel createCardsPanel() {
        var cardsPanel = new ResponsiveCardsPanel(
                MODE_GRID_COLUMNS,
                MODE_GRID_H_GAP,
                MODE_GRID_V_GAP,
                CARD_MIN_WIDTH,
                CARD_PREF_HEIGHT
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

    private static int countNonEmptyCategories(Map<VisualizationCategory, List<VisualizationMode>> modesByCategory) {
        int count = 0;
        for (VisualizationCategory category : VisualizationCategory.values()) {
            if (!modesByCategory.getOrDefault(category, List.of()).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private static void addGridFillersIfNeeded(JPanel cardsPanel, int itemCount) {
        int remainder = itemCount % MODE_GRID_COLUMNS;

        if (remainder == 0) {
            return;
        }

        int fillersToAdd = MODE_GRID_COLUMNS - remainder;
        for (int i = 0; i < fillersToAdd; i++) {
            var filler = new JPanel();
            filler.setOpaque(false);
            cardsPanel.add(filler);
        }
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
        backButton.setFont(MODE_DESCRIPTION_FONT);
        backButton.addActionListener(ignored -> showCategorySelection(
                contentPanel,
                modesByCategory,
                dialog,
                subtitleLabel
        ));
        backBar.add(backButton);

        return backBar;
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

    /**
     * Возвращает монитор, на котором фактически находился диалог выбора режима
     * в момент выбора карточки/закрытия. Это нужно для multi-monitor workflow:
     * если пользователь перетащил main UI выбора режима на другой монитор,
     * окно визуализации должно открыться именно там.
     */
    private static GraphicsConfiguration resolveDialogGraphicsConfiguration(Window dialog) {
        if (dialog == null) {
            return getDefaultGraphicsConfiguration();
        }

        Rectangle dialogBounds = dialog.getBounds();

        if (!dialogBounds.isEmpty()) {
            GraphicsConfiguration largestIntersectionGraphicsConfiguration =
                    findGraphicsConfigurationWithLargestIntersection(dialogBounds);
            if (largestIntersectionGraphicsConfiguration != null) {
                return largestIntersectionGraphicsConfiguration;
            }

            Point dialogCenter = new Point(
                    dialogBounds.x + dialogBounds.width / SCREEN_CENTER_DIVISOR,
                    dialogBounds.y + dialogBounds.height / SCREEN_CENTER_DIVISOR
            );

            GraphicsConfiguration centerGraphicsConfiguration = findGraphicsConfiguration(dialogCenter);
            if (centerGraphicsConfiguration != null) {
                return centerGraphicsConfiguration;
            }
        }

        GraphicsConfiguration currentGraphicsConfiguration = dialog.getGraphicsConfiguration();
        if (currentGraphicsConfiguration != null) {
            return currentGraphicsConfiguration;
        }

        return getDefaultGraphicsConfiguration();
    }

    private static GraphicsConfiguration findGraphicsConfigurationWithLargestIntersection(Rectangle windowBounds) {
        GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();

        GraphicsConfiguration bestGraphicsConfiguration = null;
        long bestIntersectionArea = NO_INTERSECTION_AREA;

        for (GraphicsDevice screenDevice : graphicsEnvironment.getScreenDevices()) {
            GraphicsConfiguration graphicsConfiguration = screenDevice.getDefaultConfiguration();
            Rectangle intersection = graphicsConfiguration.getBounds().intersection(windowBounds);

            long intersectionArea = (long) Math.max(NO_INTERSECTION_AREA, intersection.width)
                    * Math.max(NO_INTERSECTION_AREA, intersection.height);

            if (intersectionArea > bestIntersectionArea) {
                bestIntersectionArea = intersectionArea;
                bestGraphicsConfiguration = graphicsConfiguration;
            }
        }

        return bestGraphicsConfiguration;
    }

    private static GraphicsConfiguration findGraphicsConfiguration(Point point) {
        GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();

        for (GraphicsDevice screenDevice : graphicsEnvironment.getScreenDevices()) {
            GraphicsConfiguration graphicsConfiguration = screenDevice.getDefaultConfiguration();
            if (graphicsConfiguration.getBounds().contains(point)) {
                return graphicsConfiguration;
            }
        }

        return null;
    }

    private static GraphicsConfiguration getDefaultGraphicsConfiguration() {
        return GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration();
    }

    private static void centerDialog(
            JDialog dialog,
            JFrame parent,
            GraphicsConfiguration targetGraphicsConfiguration
    ) {
        if (parent != null) {
            dialog.setLocationRelativeTo(parent);
            return;
        }

        if (targetGraphicsConfiguration == null) {
            dialog.setLocationRelativeTo(null);
            return;
        }

        Rectangle usableBounds = getUsableScreenBounds(targetGraphicsConfiguration);

        int x = usableBounds.x + Math.max(
                MIN_CENTERING_OFFSET,
                (usableBounds.width - dialog.getWidth()) / SCREEN_CENTER_DIVISOR
        );
        int y = usableBounds.y + Math.max(
                MIN_CENTERING_OFFSET,
                (usableBounds.height - dialog.getHeight()) / SCREEN_CENTER_DIVISOR
        );

        dialog.setLocation(x, y);
    }

    private static Rectangle getUsableScreenBounds(GraphicsConfiguration graphicsConfiguration) {
        Rectangle bounds = graphicsConfiguration.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(graphicsConfiguration);

        return new Rectangle(
                bounds.x + insets.left,
                bounds.y + insets.top,
                Math.max(MIN_USABLE_SCREEN_SIZE, bounds.width - insets.left - insets.right),
                Math.max(MIN_USABLE_SCREEN_SIZE, bounds.height - insets.top - insets.bottom)
        );
    }

    /**
     * Создаёт карточку категории режимов.
     */
    private JPanel createCategoryCard(
            VisualizationCategory category,
            List<VisualizationMode> modes,
            Map<VisualizationCategory, List<VisualizationMode>> modesByCategory,
            JPanel contentPanel,
            JDialog dialog,
            JLabel subtitleLabel
    ) {
        var card = createBaseCard();

        var icon = new JLabel(category.getIcon());
        icon.setFont(ICON_FONT);
        icon.setPreferredSize(new Dimension(CARD_ICON_SIZE, CARD_ICON_SIZE));
        icon.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(icon, BorderLayout.WEST);

        card.add(createCategoryTextPanel(category, modes.size()), BorderLayout.CENTER);

        var arrow = new JLabel(CARD_ARROW_TEXT);
        arrow.setFont(ARROW_FONT);
        arrow.setForeground(ARROW_COLOR);
        card.add(arrow, BorderLayout.EAST);

        attachCardHoverAndClick(card, () -> showModeSelection(
                category,
                modes,
                modesByCategory,
                contentPanel,
                dialog,
                subtitleLabel
        ));

        return card;
    }

    /**
     * Создаёт карточку одного режима.
     */
    private JPanel createModeCard(VisualizationMode mode, JDialog dialog) {
        var card = createBaseCard();

        var icon = new JLabel(mode.getIcon());
        icon.setFont(ICON_FONT);
        icon.setPreferredSize(new Dimension(CARD_ICON_SIZE, CARD_ICON_SIZE));
        icon.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(icon, BorderLayout.WEST);

        var textPanel = createModeTextPanel(mode);
        card.add(textPanel, BorderLayout.CENTER);

        var arrow = new JLabel(CARD_ARROW_TEXT);
        arrow.setFont(ARROW_FONT);
        arrow.setForeground(ARROW_COLOR);
        card.add(arrow, BorderLayout.EAST);

        attachCardHoverAndClick(card, () -> {
            selectedMode = mode;
            dialog.dispose();
        });

        return card;
    }

    private static JPanel createBaseCard() {
        var card = new JPanel(new BorderLayout(CARD_LAYOUT_H_GAP, CARD_LAYOUT_V_GAP));
        card.setBorder(createCardBorder(CARD_BORDER_COLOR));
        card.setBackground(CARD_BACKGROUND);
        card.setPreferredSize(new Dimension(CARD_MIN_WIDTH, CARD_PREF_HEIGHT));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return card;
    }

    private static void attachCardHoverAndClick(JPanel card, Runnable onClick) {
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(CARD_HOVER_BACKGROUND);
                card.setBorder(createCardBorder(CARD_HOVER_BORDER_COLOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(CARD_BACKGROUND);
                card.setBorder(createCardBorder(CARD_BORDER_COLOR));
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                onClick.run();
            }
        });
    }

    private static JPanel createCategoryTextPanel(VisualizationCategory category, int modeCount) {
        var textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        var name = new JLabel(category.getDisplayName());
        name.setFont(MODE_NAME_FONT);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        textPanel.add(name);

        textPanel.add(Box.createVerticalStrut(CARD_DESCRIPTION_TOP_SPACING));

        var desc = new JLabel(category.getDescription());
        desc.setFont(MODE_DESCRIPTION_FONT);
        desc.setForeground(DESCRIPTION_COLOR);
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);
        textPanel.add(desc);

        textPanel.add(Box.createVerticalStrut(CARD_DESCRIPTION_TOP_SPACING));

        String countText = modeCount + (modeCount == 1 ? CATEGORY_COUNT_SUFFIX_ONE : CATEGORY_COUNT_SUFFIX_MANY);
        var count = new JLabel(countText);
        count.setFont(CATEGORY_COUNT_FONT);
        count.setForeground(CATEGORY_COUNT_COLOR);
        count.setAlignmentX(Component.LEFT_ALIGNMENT);
        textPanel.add(count);

        return textPanel;
    }

    private static JPanel createModeTextPanel(VisualizationMode mode) {
        var textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        var name = new JLabel(mode.getName());
        name.setFont(MODE_NAME_FONT);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        textPanel.add(name);

        textPanel.add(Box.createVerticalStrut(CARD_DESCRIPTION_TOP_SPACING));

        for (String line : mode.getDescription().split(DESCRIPTION_LINE_SEPARATOR)) {
            var desc = new JLabel(line);
            desc.setFont(MODE_DESCRIPTION_FONT);
            desc.setForeground(DESCRIPTION_COLOR);
            desc.setAlignmentX(Component.LEFT_ALIGNMENT);
            textPanel.add(desc);
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

    /**
     * Responsive fixed-height grid for mode/category cards.
     *
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
