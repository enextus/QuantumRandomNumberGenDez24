package org.ThreeDotsSierpinski;

import javax.swing.*;

class RNLoadListenerImpl implements RNLoadListener {
    private final DotController controller;
    private final JFrame mainFrame;
    private final JTextArea rawDataTextArea;

    private JFrame rawDataFrame; // Окно будет создано только при необходимости
    private boolean quantumDataReceived = false;

    /**
     * Создаёт listener. Окно "Raw Data" создается ТОЛЬКО при поступлении реальных данных.
     *
     * @param controller  контроллер для обновления статуса
     * @param mainFrame   главное окно приложения (для позиционирования)
     */
    public RNLoadListenerImpl(DotController controller, JFrame mainFrame) {
        this.controller = controller;
        this.mainFrame = mainFrame;

        this.rawDataTextArea = new JTextArea();
        this.rawDataTextArea.setEditable(false);

        // ИЗМЕНЕНИЕ: Убрали создание JFrame из конструктора. Больше никаких side-effects!
    }

    /**
     * Ленивое создание и показ окна. Вызывается только один раз, при первых данных.
     */
    private void showRawDataWindowIfNeeded() {
        if (rawDataFrame != null) {
            return; // Окно уже создано
        }

        rawDataFrame = new JFrame("Raw Data");
        rawDataFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        rawDataFrame.add(new JScrollPane(rawDataTextArea));

        // Позиционируем под главным окном
        int mainX = mainFrame.getX();
        int mainY = mainFrame.getY();
        int mainWidth = mainFrame.getWidth();
        int mainHeight = mainFrame.getHeight();

        int rawDataHeight = 150;
        int windowShadowOffset = 7; // Компенсация тени оконной рамки Windows
        rawDataFrame.setSize(mainWidth, rawDataHeight);
        rawDataFrame.setLocation(mainX, mainY + mainHeight - windowShadowOffset);

        rawDataFrame.setVisible(true);
    }

    @Override
    public void onLoadingStarted() {
        controller.updateStatusLabel("Loading data...");
    }

    @Override
    public void onLoadingCompleted() {
        controller.updateStatusLabel("Data loaded successfully.");
    }

    @Override
    public void onError(String errorMessage) {
        controller.updateStatusLabel("Error: " + errorMessage);
    }

    @Override
    public void onRawDataReceived(String rawData) {
        // ИЗМЕНЕНИЕ: Ставим флаг и показываем окно ТОЛЬКО здесь.
        // Если это событие никогда не вызовется (нет ключа API), окно не появится.
        quantumDataReceived = true;
        showRawDataWindowIfNeeded();

        rawDataTextArea.append(rawData + "\n");
    }

    @Override
    public void onModeChanged(RNProvider.Mode mode) {
        // ИЗМЕНЕНИЕ: Если режим переключился в PSEUDO во время работы,
        // мы НЕ закрываем окно (согласно твоему ТЗ). Просто игнорируем.
        // Окно останется висеть с последними полученными данными.
    }
}
