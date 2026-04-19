package org.ThreeDotsSierpinski;

import javax.swing.*;

class RNLoadListenerImpl implements RNLoadListener {
    private final DotController controller;
    private final JFrame mainFrame;
    private final ToggleSwitch toggleSwitch;

    private final JTextArea rawDataTextArea;
    private JFrame rawDataFrame;
    private boolean quantumDataReceived = false;

    public RNLoadListenerImpl(DotController controller, JFrame mainFrame, ToggleSwitch toggleSwitch) {
        this.controller = controller;
        this.mainFrame = mainFrame;
        this.toggleSwitch = toggleSwitch;

        this.rawDataTextArea = new JTextArea();
        this.rawDataTextArea.setEditable(false);
    }

    private void showRawDataWindowIfNeeded() {
        if (rawDataFrame != null) return;

        rawDataFrame = new JFrame("Raw Data");
        rawDataFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        rawDataFrame.add(new JScrollPane(rawDataTextArea));

        int mainX = mainFrame.getX();
        int mainY = mainFrame.getY();
        int mainWidth = mainFrame.getWidth();
        int mainHeight = mainFrame.getHeight();

        int rawDataHeight = 150;
        int windowShadowOffset = 7;
        rawDataFrame.setSize(mainWidth, rawDataHeight);
        rawDataFrame.setLocation(mainX, mainY + mainHeight - windowShadowOffset);

        rawDataFrame.setVisible(true);
    }

    @Override
    public void onLoadingStarted() {
        SwingUtilities.invokeLater(() -> controller.updateStatusLabel("Loading data..."));
    }

    @Override
    public void onLoadingCompleted() {
        SwingUtilities.invokeLater(() -> controller.updateStatusLabel("Data loaded successfully."));
    }

    @Override
    public void onError(String errorMessage) {
        SwingUtilities.invokeLater(() -> controller.updateStatusLabel("Error: " + errorMessage));
    }

    @Override
    public void onRawDataReceived(String rawData) {
        quantumDataReceived = true;
        SwingUtilities.invokeLater(() -> {
            showRawDataWindowIfNeeded();
            rawDataTextArea.append(rawData + "\n");
        });
    }

    @Override
    public void onModeChanged(RNProvider.Mode mode) {
        SwingUtilities.invokeLater(() -> {
            if (mode == RNProvider.Mode.PSEUDO) {
                toggleSwitch.setSelected(false); // Двигаем влево
            }
        });
    }

    @Override
    public void onApiAvailabilityChanged(boolean isAvailable) {
        // ВАЖНО: Обязательно оборачиваем в invokeLater, чтобы Swing корректно обработал отключение кликов!
        SwingUtilities.invokeLater(() -> {
            toggleSwitch.setEnabled(isAvailable);

            if (!isAvailable) {
                if (quantumDataReceived) {
                    controller.updateStatusLabel("API недоступно. Переключено на PSEUDO (Local).");
                }
            } else {
                if (toggleSwitch.isSelected()) {
                    controller.updateStatusLabel("Подключение к API восстановлено.");
                }
            }
        });
    }
}
