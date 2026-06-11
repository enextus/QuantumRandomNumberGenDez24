package org.ThreeDotsSierpinski.app;

import org.ThreeDotsSierpinski.app.*;
import org.ThreeDotsSierpinski.config.*;
import org.ThreeDotsSierpinski.math.*;
import org.ThreeDotsSierpinski.mode.*;
import org.ThreeDotsSierpinski.model.*;
import org.ThreeDotsSierpinski.rng.*;
import org.ThreeDotsSierpinski.stats.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Диалог выбора режима визуализации.
 * Показывается при запуске приложения. Отображает карточки
 * с описанием каждого доступного режима.
 */
public class ModeSelectionDialog {

    private static final String DIALOG_TITLE = "Quantum Random Visualizer";
    private static final String SUBTITLE_TEXT = "Выберите визуализацию";
    private static final String FOOTER_TEXT =
            "Powered by ANU Quantum Random Numbers API + L128X256MixRandom fallback";
    private static final String DESCRIPTION_LINE_SEPARATOR = "\\n";
    private static final String CARD_ARROW_TEXT = "→";

    private static final String FONT_SANS_SERIF = "SansSerif";

    private static final int DIALOG_LAYOUT_H_GAP = 0;
    private static final int DIALOG_LAYOUT_V_GAP = 0;

    private static final int DIALOG_WIDTH = 1350;
    private static final int DIALOG_BASE_HEIGHT = 180;
    private static final int DIALOG_ROW_HEIGHT = 120;
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
    private static final int CARD_PREF_WIDTH = 420;
    private static final int CARD_PREF_HEIGHT = 90;
    private static final int CARD_ICON_SIZE = 48;
    private static final int CARD_DESCRIPTION_TOP_SPACING = 4;

    private static final int TITLE_FONT_SIZE = 20;
    private static final int SUBTITLE_FONT_SIZE = 13;
    private static final int FOOTER_FONT_SIZE = 10;
    private static final int ICON_FONT_SIZE = 32;
    private static final int MODE_NAME_FONT_SIZE = 15;
    private static final int MODE_DESCRIPTION_FONT_SIZE = 12;
    private static final int ARROW_FONT_SIZE = 20;

    private static final int SCREEN_CENTER_DIVISOR = 2;
    private static final int MIN_USABLE_SCREEN_SIZE = 1;
    private static final int NO_INTERSECTION_AREA = 0;
    private static final int MIN_CENTERING_OFFSET = 0;

    private static final Color HEADER_BACKGROUND = new Color(245, 245, 242);
    private static final Color FOOTER_BACKGROUND = new Color(245, 245, 242);
    private static final Color CARDS_BACKGROUND = Color.WHITE;
    private static final Color CARD_BACKGROUND = Color.WHITE;
    private static final Color CARD_HOVER_BACKGROUND = new Color(240, 245, 255);
    private static final Color CARD_BORDER_COLOR = new Color(220, 220, 215);
    private static final Color CARD_HOVER_BORDER_COLOR = new Color(100, 140, 200);
    private static final Color SUBTITLE_COLOR = new Color(120, 120, 120);
    private static final Color FOOTER_COLOR = new Color(160, 160, 160);
    private static final Color DESCRIPTION_COLOR = new Color(100, 100, 100);
    private static final Color ARROW_COLOR = new Color(180, 180, 180);

    private static final Font TITLE_FONT = new Font(FONT_SANS_SERIF, Font.BOLD, TITLE_FONT_SIZE);
    private static final Font SUBTITLE_FONT = new Font(FONT_SANS_SERIF, Font.PLAIN, SUBTITLE_FONT_SIZE);
    private static final Font FOOTER_FONT = new Font(FONT_SANS_SERIF, Font.PLAIN, FOOTER_FONT_SIZE);
    private static final Font ICON_FONT = new Font(FONT_SANS_SERIF, Font.PLAIN, ICON_FONT_SIZE);
    private static final Font MODE_NAME_FONT = new Font(FONT_SANS_SERIF, Font.BOLD, MODE_NAME_FONT_SIZE);
    private static final Font MODE_DESCRIPTION_FONT = new Font(FONT_SANS_SERIF, Font.PLAIN, MODE_DESCRIPTION_FONT_SIZE);
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
        var modes = VisualizationMode.allModes();

        var dialog = new JDialog(parent, DIALOG_TITLE, true);
        dialog.setLayout(new BorderLayout(DIALOG_LAYOUT_H_GAP, DIALOG_LAYOUT_V_GAP));
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        dialog.add(createHeaderPanel(), BorderLayout.NORTH);
        dialog.add(createModeCardsScrollPane(modes, dialog), BorderLayout.CENTER);
        dialog.add(createFooterPanel(), BorderLayout.SOUTH);

        int rows = (modes.length + MODE_GRID_COLUMNS - 1) / MODE_GRID_COLUMNS;
        int dialogHeight = Math.min(DIALOG_MAX_HEIGHT, DIALOG_BASE_HEIGHT + rows * DIALOG_ROW_HEIGHT);

        dialog.setSize(DIALOG_WIDTH, dialogHeight);
        dialog.setMinimumSize(new Dimension(DIALOG_MIN_WIDTH, DIALOG_MIN_HEIGHT));
        centerDialog(dialog, parent, targetGraphicsConfiguration);
        dialog.setVisible(true);

        lastDialogGraphicsConfiguration = resolveDialogGraphicsConfiguration(dialog);

        return selectedMode;
    }

    private static JPanel createHeaderPanel() {
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

        var subtitle = new JLabel(SUBTITLE_TEXT);
        subtitle.setFont(SUBTITLE_FONT);
        subtitle.setForeground(SUBTITLE_COLOR);
        header.add(subtitle, BorderLayout.SOUTH);

        return header;
    }

    private JScrollPane createModeCardsScrollPane(VisualizationMode[] modes, JDialog dialog) {
        var cardsPanel = new JPanel(new GridLayout(
                0,
                MODE_GRID_COLUMNS,
                MODE_GRID_H_GAP,
                MODE_GRID_V_GAP
        ));
        cardsPanel.setBorder(BorderFactory.createEmptyBorder(
                CARDS_BORDER_TOP,
                CARDS_BORDER_LEFT,
                CARDS_BORDER_BOTTOM,
                CARDS_BORDER_RIGHT
        ));
        cardsPanel.setBackground(CARDS_BACKGROUND);

        for (var mode : modes) {
            cardsPanel.add(createModeCard(mode, dialog));
        }

        addGridFillersIfNeeded(cardsPanel, modes.length);

        var scrollPane = new JScrollPane(cardsPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_UNIT_INCREMENT);

        return scrollPane;
    }

    private static void addGridFillersIfNeeded(JPanel cardsPanel, int modeCount) {
        int remainder = modeCount % MODE_GRID_COLUMNS;

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
     * Создаёт карточку одного режима.
     */
    private JPanel createModeCard(VisualizationMode mode, JDialog dialog) {
        var card = new JPanel(new BorderLayout(CARD_LAYOUT_H_GAP, CARD_LAYOUT_V_GAP));
        card.setBorder(createCardBorder(CARD_BORDER_COLOR));
        card.setBackground(CARD_BACKGROUND);
        card.setPreferredSize(new Dimension(CARD_PREF_WIDTH, CARD_PREF_HEIGHT));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

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
                selectedMode = mode;
                dialog.dispose();
            }
        });

        return card;
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
}