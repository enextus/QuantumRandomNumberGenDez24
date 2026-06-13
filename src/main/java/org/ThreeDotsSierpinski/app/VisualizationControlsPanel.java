package org.ThreeDotsSierpinski.app;

import org.ThreeDotsSierpinski.mode.VisualizationMode;

import javax.swing.*;
import java.awt.*;

/**
 * Builds the bottom controls area for the visualization window.
 *
 * <p>The class owns component layout only. Runtime behavior and listeners stay
 * in {@link App}, which remains the application coordinator.</p>
 */
final class VisualizationControlsPanel {

    private static final String BUTTON_PLAY = "► Play";
    private static final String BUTTON_TEST_VALUES_QUALITY = "Test RNG";
    private static final String BUTTON_TEST_VALUES_QUALITY_TOOLTIP = "Test values quality";
    private static final String BUTTON_FINISH_VISUALIZATION = "Выйти";
    private static final int STATUS_PANEL_HORIZONTAL_GAP = 10;
    private static final int STATUS_PANEL_VERTICAL_GAP = 5;
    private static final int STATUS_LABEL_WIDTH = 250;
    private static final int STATUS_LABEL_HEIGHT = 20;
    private static final int RNG_FONT_SIZE = 11;
    private static final int RNG_SPACER_WIDTH = 5;
    private static final int TEST_BUTTON_WIDTH = 95;
    private static final int SAVE_BUTTON_WIDTH = 100;
    private static final int FINISH_BUTTON_WIDTH = 78;
    private static final int STATUS_BUTTON_HEIGHT = 28;
    private static final int STATUS_SCROLL_UNIT_INCREMENT = 16;

    private VisualizationControlsPanel() {
    }

    static Controls create(JLabel statusLabel, VisualizationMode mode, DotController dotController) {
        JPanel statusPanel = new JPanel(new FlowLayout(
                FlowLayout.LEFT,
                STATUS_PANEL_HORIZONTAL_GAP,
                STATUS_PANEL_VERTICAL_GAP
        ));

        statusLabel.setPreferredSize(new Dimension(STATUS_LABEL_WIDTH, STATUS_LABEL_HEIGHT));
        statusPanel.add(statusLabel);

        JButton playStopButton = new JButton(BUTTON_PLAY);
        playStopButton.setEnabled(false);
        statusPanel.add(playStopButton);

        JLabel rngLabel = new JLabel("...");
        rngLabel.setFont(new Font("SansSerif", Font.PLAIN, RNG_FONT_SIZE));
        statusPanel.add(rngLabel);

        statusPanel.add(Box.createHorizontalStrut(RNG_SPACER_WIDTH));

        JToggleButton rngToggle = new JToggleButton("RNG");
        rngToggle.putClientProperty("JToggleButton.buttonType", "toggle");
        rngToggle.setSelected(false);
        rngToggle.setEnabled(false);
        rngToggle.setToolTipText("Toggle between QUANTUM and PSEUDO random number sources");
        statusPanel.add(rngToggle);

        Runnable syncToggleLabel = () -> {
            if (rngToggle.isSelected()) {
                rngLabel.setText("QUANTUM (API)");
                rngToggle.setText("QUANTUM");
            } else {
                rngLabel.setText("PSEUDO (Local)");
                rngToggle.setText("PSEUDO");
            }
        };
        rngToggle.addChangeListener(_ -> syncToggleLabel.run());
        syncToggleLabel.run();

        JButton testButton = new JButton(BUTTON_TEST_VALUES_QUALITY);
        testButton.setToolTipText(BUTTON_TEST_VALUES_QUALITY_TOOLTIP);
        testButton.setPreferredSize(new Dimension(TEST_BUTTON_WIDTH, STATUS_BUTTON_HEIGHT));
        statusPanel.add(testButton);

        JButton saveButton = new JButton("Save PNG");
        saveButton.setPreferredSize(new Dimension(SAVE_BUTTON_WIDTH, STATUS_BUTTON_HEIGHT));
        statusPanel.add(saveButton);

        for (JComponent modeControl : mode.createModeControls(dotController)) {
            statusPanel.add(modeControl);
        }

        JButton finishButton = new JButton(BUTTON_FINISH_VISUALIZATION);
        finishButton.setPreferredSize(new Dimension(FINISH_BUTTON_WIDTH, STATUS_BUTTON_HEIGHT));

        JScrollPane controlsScrollPane = new JScrollPane(
                statusPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        controlsScrollPane.setBorder(null);
        controlsScrollPane.getHorizontalScrollBar().setUnitIncrement(STATUS_SCROLL_UNIT_INCREMENT);

        JPanel exitPanel = new JPanel(new FlowLayout(
                FlowLayout.RIGHT,
                STATUS_PANEL_HORIZONTAL_GAP,
                STATUS_PANEL_VERTICAL_GAP
        ));
        exitPanel.add(finishButton);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(controlsScrollPane, BorderLayout.CENTER);
        bottomPanel.add(exitPanel, BorderLayout.EAST);

        return new Controls(
                bottomPanel,
                playStopButton,
                rngToggle,
                testButton,
                saveButton,
                finishButton,
                syncToggleLabel
        );
    }

    record Controls(
            JPanel bottomPanel,
            JButton playStopButton,
            JToggleButton rngToggle,
            JButton testButton,
            JButton saveButton,
            JButton finishButton,
            Runnable syncToggleLabel
    ) {
    }
}
