package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.awt.*;

class RNLoadListenerImpl implements RNLoadListener {
    private final DotController controller;
    private final JTextArea rawDataTextArea;

    /**
     * Создаёт listener с окном "Raw Data", расположенным под главным окном.
     *
     * @param controller  контроллер для обновления статуса
     * @param mainFrame   главное окно приложения (для позиционирования)
     */
    public RNLoadListenerImpl(DotController controller, JFrame mainFrame) {
        this.controller = controller;
        this.rawDataTextArea = new JTextArea();
        this.rawDataTextArea.setEditable(false);

        JFrame rawDataFrame = new JFrame("Raw Data");
        rawDataFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        rawDataFrame.add(new JScrollPane(rawDataTextArea));

        // Позиционируем под главным окном: та же ширина, та же X-координата
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
        rawDataTextArea.append(rawData + "\n");
    }

}