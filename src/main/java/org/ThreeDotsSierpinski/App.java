package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;

/**
 * The main application class that sets up the GUI and initializes components.
 */
public class App {
    private static final String APPLICATION_TITLE = "Dot Mover";
    private static final String LOG_APP_STARTED = "Application started.";
    private static final String LOG_GUI_STARTED = "GUI successfully launched.";
    private static final String LOG_APP_SHUTTING_DOWN = "Shutting down application.";
    private static final String LOG_WAITING_FOR_DATA = "Waiting for initial random numbers...";
    private static final String LOG_DATA_READY = "Initial data loaded, starting animation.";
    private static final String LOG_DATA_TIMEOUT = "Timeout waiting for initial data.";

    private static final Logger LOGGER = LoggerConfig.getLogger();

    public static void main(String[] args) {
        LoggerConfig.initializeLogger();
        LOGGER.info(LOG_APP_STARTED);

        // Создаём RNProvider заранее, чтобы начать загрузку
        RNProvider randomNumberProvider = new RNProvider();
        JLabel statusLabel = new JLabel("Initializing...");

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(APPLICATION_TITLE);
            frame.setLayout(new BorderLayout());

            int basePanelWidth = Config.getInt("panel.size.width");
            int basePanelHeight = Config.getInt("panel.size.height");
            double scaleWidth = Config.getDouble("window.scale.width");
            double scaleHeight = Config.getDouble("window.scale.height");

            int finalWidth = (int) Math.round(basePanelWidth * scaleWidth);
            int finalHeight = (int) Math.round(basePanelHeight * scaleHeight);

            DotController dotController = new DotController(randomNumberProvider, statusLabel);

            frame.add(dotController, BorderLayout.CENTER);

            JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            statusPanel.add(statusLabel);
            frame.add(statusPanel, BorderLayout.SOUTH);

            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setSize(finalWidth, finalHeight);
            frame.setLocationRelativeTo(null);

            // Показываем окно сразу
            frame.setVisible(true);
            LOGGER.info(LOG_GUI_STARTED);

            // Добавляем кнопку теста
            JButton testButton = new JButton("Проверить качество случайных чисел.");
            statusPanel.add(testButton);
            testButton.addActionListener(e -> {
                RandomnessTest test = new KolmogorovSmirnovTest();
                java.util.List<Long> numbers = dotController.getUsedRandomNumbers();
                try {
                    boolean result = test.test(numbers, 0.05);
                    statusLabel.setText(test.getTestName() + ": " + (result ? "Тест успешно пройден." : "Тест не пройден."));
                } catch (IllegalArgumentException ex) {
                    statusLabel.setText("Ошибка: " + ex.getMessage());
                }
            });

            // Ожидаем загрузки данных в отдельном потоке
            new Thread(() -> {
                LOGGER.info(LOG_WAITING_FOR_DATA);
                SwingUtilities.invokeLater(() -> statusLabel.setText("Loading random numbers from QRNG API..."));

                // Ждём до 10 секунд
                boolean dataReady = randomNumberProvider.waitForInitialData(10000);

                if (dataReady) {
                    LOGGER.info(LOG_DATA_READY);
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Data loaded. Starting animation...");
                        dotController.startDotMovement();
                    });
                } else {
                    LOGGER.warning(LOG_DATA_TIMEOUT);
                    String error = randomNumberProvider.getLastError();
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Error: " + (error != null ? error : "Timeout loading data"));
                    });
                }
            }).start();

            // Window close handler
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                    LOGGER.info(LOG_APP_SHUTTING_DOWN);
                    randomNumberProvider.shutdown();
                    super.windowClosing(windowEvent);
                }
            });
        });
    }
}
