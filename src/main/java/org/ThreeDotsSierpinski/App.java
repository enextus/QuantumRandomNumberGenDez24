package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;

/**
 * The main application class that sets up the GUI and initializes components.
 */
public class App {
    private static final String APPLICATION_TITLE = "Quantum Sierpinski Triangle";
    private static final String LOG_APP_STARTED = "Application started.";
    private static final String LOG_GUI_STARTED = "GUI successfully launched.";
    private static final String LOG_APP_SHUTTING_DOWN = "Shutting down application.";
    private static final String LOG_WAITING_FOR_DATA = "Waiting for initial random numbers from ANU Quantum API...";
    private static final String LOG_DATA_READY = "Initial data loaded, starting animation.";
    private static final String LOG_DATA_TIMEOUT = "Timeout waiting for initial data.";

    private static final String BUTTON_PLAY = "► Play";
    private static final String BUTTON_STOP = "■ Stop";

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

            // Панель статуса с фиксированными размерами элементов
            JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

            // Фиксированная ширина для статус-лейбла (устанавливаем ДО добавления)
            statusLabel.setPreferredSize(new Dimension(250, 20));
            statusPanel.add(statusLabel);

            // Кнопка Play/Stop с фиксированным размером
            JButton playStopButton = new JButton(BUTTON_STOP);
            playStopButton.setEnabled(false); // Отключена до загрузки данных
            playStopButton.setPreferredSize(new Dimension(90, 28));
            statusPanel.add(playStopButton);

            // Кнопка теста с фиксированным размером
            JButton testButton = new JButton("Проверить качество");
            testButton.setPreferredSize(new Dimension(160, 28));
            statusPanel.add(testButton);

            frame.add(statusPanel, BorderLayout.SOUTH);

            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setSize(finalWidth, finalHeight);
            frame.setLocationRelativeTo(null);

            // Показываем окно сразу
            frame.setVisible(true);
            LOGGER.info(LOG_GUI_STARTED);

            // Обработчик кнопки Play/Stop
            playStopButton.addActionListener(e -> {
                boolean running = dotController.toggle();
                playStopButton.setText(running ? BUTTON_STOP : BUTTON_PLAY);
                if (running) {
                    statusLabel.setText("Drawing...");
                } else {
                    statusLabel.setText("Paused. Points: " + dotController.getUsedRandomNumbers().size());
                }
            });

            // Обработчик кнопки теста
            testButton.addActionListener(e -> {
                RandomnessTest test = new KolmogorovSmirnovTest();
                java.util.List<Long> numbers = dotController.getUsedRandomNumbers();
                try {
                    boolean result = test.test(numbers, 0.05);
                    statusLabel.setText("K-S тест: " + (result ? "✓ Пройден" : "✗ Не пройден"));
                } catch (IllegalArgumentException ex) {
                    statusLabel.setText("Ошибка: " + ex.getMessage());
                }
            });

            // Ожидаем загрузки данных в отдельном потоке
            new Thread(() -> {
                LOGGER.info(LOG_WAITING_FOR_DATA);
                SwingUtilities.invokeLater(() -> statusLabel.setText("Connecting to API..."));

                // Ждём до 15 секунд
                boolean dataReady = randomNumberProvider.waitForInitialData(15000);

                if (dataReady) {
                    LOGGER.info(LOG_DATA_READY);
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Drawing...");
                        playStopButton.setEnabled(true);
                        dotController.startDotMovement();
                    });
                } else {
                    LOGGER.warning(LOG_DATA_TIMEOUT);
                    String error = randomNumberProvider.getLastError();
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Error: " + (error != null ? error : "Timeout"));
                    });
                }
            }).start();

            // Window close handler
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                    LOGGER.info(LOG_APP_SHUTTING_DOWN);
                    dotController.shutdown();
                    randomNumberProvider.shutdown();
                    super.windowClosing(windowEvent);
                }
            });
        });
    }
}
