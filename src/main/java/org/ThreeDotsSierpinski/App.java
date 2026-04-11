package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.awt.*;
import java.util.List;
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

            // Кнопка сохранения изображения
            JButton saveButton = new JButton("Save PNG");
            saveButton.setPreferredSize(new Dimension(100, 28));
            statusPanel.add(saveButton);

            frame.add(statusPanel, BorderLayout.SOUTH);

            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setSize(finalWidth, finalHeight);
            frame.setLocationRelativeTo(null);

            // Показываем окно сразу
            frame.setVisible(true);
            LOGGER.info(LOG_GUI_STARTED);

            // Регистрируем listener с окном "Raw Data" под главным окном
            randomNumberProvider.addDataLoadListener(new RNLoadListenerImpl(dotController, frame));

            // Обработчик кнопки Play/Stop
            playStopButton.addActionListener(_ -> {
                boolean running = dotController.toggle();
                playStopButton.setText(running ? BUTTON_STOP : BUTTON_PLAY);
                if (running) {
                    statusLabel.setText("Drawing...");
                } else {
                    statusLabel.setText("Paused. Points: " + dotController.getUsedRandomNumbers().size());
                }
            });

            // Обработчик кнопки «Проверить качество» — запускает все тесты
            testButton.addActionListener(_ -> {
                List<Long> numbers = dotController.getUsedRandomNumbers();

                if (numbers.size() < 10) {
                    statusLabel.setText("Нужно минимум 10 точек для тестов");
                    return;
                }

                RandomnessTestSuite suite = new RandomnessTestSuite();
                List<TestResult> results = suite.runAll(numbers, 0.05);
                String report = RandomnessTestSuite.formatResults(results);

                long passed = results.stream().filter(TestResult::passed).count();
                statusLabel.setText("Тесты: " + passed + "/" + results.size() + " пройдено (" + numbers.size() + " точек)");

                JOptionPane.showMessageDialog(
                        frame,
                        report,
                        "Результаты тестов случайности (" + numbers.size() + " чисел)",
                        passed == results.size() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE
                );
            });

            // Обработчик кнопки «Save PNG» — сохраняет 2 файла (прозрачный + белый фон)
            saveButton.addActionListener(_ -> {
                int points = dotController.getUsedRandomNumbers().size();
                var timestamp = java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
                var baseName = "sierpinski_" + timestamp + "_" + points + "pts";

                var dirChooser = new JFileChooser();
                dirChooser.setDialogTitle("Выберите папку для сохранения");
                dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                dirChooser.setAcceptAllFileFilterUsed(false);

                if (dirChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    var directory = dirChooser.getSelectedFile();
                    int saved = dotController.saveImages(directory, baseName);
                    statusLabel.setText("Saved " + saved + "/2 files → " + directory.getName() + "/");
                }
            });

            // Ожидаем загрузки данных в виртуальном потоке (Java 21+)
            Thread.startVirtualThread(() -> {
                LOGGER.info(LOG_WAITING_FOR_DATA);
                SwingUtilities.invokeLater(() -> statusLabel.setText("Connecting to API..."));

                // Ожидание
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
                    SwingUtilities.invokeLater(() -> statusLabel.setText("Error: " + (error != null ? error : "Timeout")));
                }
            });

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
