package org.ThreeDotsSierpinski;

import javax.swing.*;

class RNLoadListenerImpl implements RNLoadListener {
    private final dotController controller;
    private final JTextArea rawDataTextArea; // поле для вывода

    public RNLoadListenerImpl(dotController controller) {
        this.controller = controller;
        this.rawDataTextArea = new JTextArea();

        // Создаём окно и добавляем в него rawDataTextArea как локальную переменную
        JFrame frame = new JFrame("Raw Data");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLocationRelativeTo(null);
        frame.add(new JScrollPane(rawDataTextArea));
        frame.setVisible(true);
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
