package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.logging.Logger;

/**
 * Главный класс приложения.
 *
 * Запуск: диалог выбора режима → основное окно визуализации.
 */
public class App {
    private static final String LOG_APP_STARTED = "Application started.";
    private static final String LOG_GUI_STARTED = "GUI successfully launched.";
    private static final String LOG_APP_SHUTTING_DOWN = "Shutting down application.";
    private static final String LOG_WAITING_FOR_DATA = "Waiting for initial random numbers...";
    private static final String LOG_DATA_READY = "Initial data loaded, starting animation.";
    private static final String LOG_DATA_TIMEOUT = "Timeout waiting for initial data.";

    private static final String BUTTON_PLAY = "► Play";
    private static final String BUTTON_STOP = "■ Stop";

    private static final Logger LOGGER = LoggerConfig.getLogger();

    public static void main(String[] args) {
        LoggerConfig.initializeLogger();
        LOGGER.info(LOG_APP_STARTED);

        // Шаг 1: Выбор режима визуализации
        SwingUtilities.invokeLater(() -> {
            var selector = new ModeSelectionDialog();
            var selectedMode = selector.showAndWait(null);

            if (selectedMode == null) {
                LOGGER.info("No mode selected, exiting.");
                System.exit(0);
                return;
            }

            LOGGER.info("Selected mode: " + selectedMode.getName());

            // Шаг 2: Запуск основного окна
            launchMainWindow(selectedMode);
        });
    }

    private static void launchMainWindow(VisualizationMode mode) {
        RNProvider randomNumberProvider = new RNProvider();
        JLabel statusLabel = new JLabel("Initializing...");

        String windowTitle = "Quantum Visualizer — " + mode.getName();
        var frame = new JFrame(windowTitle);
        frame.setLayout(new BorderLayout());

        int basePanelWidth = Config.getInt("panel.size.width");
        int basePanelHeight = Config.getInt("panel.size.height");
        double scaleWidth = Config.getDouble("window.scale.width");
        double scaleHeight = Config.getDouble("window.scale.height");

        int finalWidth = (int) Math.round(basePanelWidth * scaleWidth);
        int finalHeight = (int) Math.round(basePanelHeight * scaleHeight);

        var dotController = new DotController(randomNumberProvider, mode, statusLabel);
        frame.add(dotController, BorderLayout.CENTER);

        // Панель статуса
        var statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        statusLabel.setPreferredSize(new Dimension(250, 20));
        statusPanel.add(statusLabel);

        var playStopButton = new JButton(BUTTON_STOP);
        playStopButton.setEnabled(false);
        playStopButton.setPreferredSize(new Dimension(90, 28));
        statusPanel.add(playStopButton);

        var testButton = new JButton("Проверить качество");
        testButton.setPreferredSize(new Dimension(160, 28));
        statusPanel.add(testButton);

        var saveButton = new JButton("Save PNG");
        saveButton.setPreferredSize(new Dimension(100, 28));
        statusPanel.add(saveButton);

        frame.add(statusPanel, BorderLayout.SOUTH);

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(finalWidth, finalHeight);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        LOGGER.info(LOG_GUI_STARTED);

        // Listener для Raw Data окна
        randomNumberProvider.addDataLoadListener(new RNLoadListenerImpl(dotController, frame));

        // Play/Stop
        playStopButton.addActionListener(_ -> {
            boolean running = dotController.toggle();
            playStopButton.setText(running ? BUTTON_STOP : BUTTON_PLAY);
            if (running) {
                statusLabel.setText("Drawing...");
            } else {
                statusLabel.setText("Paused. Points: " + dotController.getUsedRandomNumbers().size());
            }
        });

        // Проверить качество
        testButton.addActionListener(_ -> {
            List<Long> numbers = dotController.getUsedRandomNumbers();

            if (numbers.size() < 10) {
                statusLabel.setText("Нужно минимум 10 точек для тестов");
                return;
            }

            RandomnessTestSuite suite = new RandomnessTestSuite();
            List<TestResult> results = suite.runAll(numbers, 0.05);

            long passed = results.stream().filter(TestResult::passed).count();
            statusLabel.setText("Тесты: " + passed + "/" + results.size()
                    + " пройдено (" + numbers.size() + " точек)");

            var panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));

            for (TestResult result : results) {
                var row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));

                var indicator = new JLabel("\u25CF");
                indicator.setFont(new Font("SansSerif", Font.BOLD, 16));
                indicator.setForeground(switch (result.quality()) {
                    case STRONG   -> new Color(34, 139, 34);
                    case MARGINAL -> new Color(204, 153, 0);
                    case FAIL     -> new Color(204, 0, 0);
                });
                row.add(indicator);

                var mark = new JLabel(switch (result.quality()) {
                    case STRONG   -> "\u2713";
                    case MARGINAL -> "\u25CB";
                    case FAIL     -> "\u2717";
                });
                mark.setFont(new Font("SansSerif", Font.BOLD, 14));
                mark.setForeground(indicator.getForeground());
                row.add(mark);

                var text = new JLabel(result.statistic() + "    " + result.testName());
                text.setFont(new Font("Monospaced", Font.PLAIN, 13));
                row.add(text);

                panel.add(row);
            }

            panel.add(Box.createVerticalStrut(8));
            var summary = new JLabel("Итого: " + passed + "/" + results.size() + " тестов пройдено");
            summary.setFont(new Font("SansSerif", Font.BOLD, 13));
            summary.setAlignmentX(Component.LEFT_ALIGNMENT);
            summary.setBorder(BorderFactory.createEmptyBorder(4, 8, 0, 0));
            panel.add(summary);

            var legend = new JLabel("<html><font color='#228B22'>\u25CF отлично</font>"
                    + "   <font color='#CC9900'>\u25CF приемлемо</font>"
                    + "   <font color='#CC0000'>\u25CF не пройден</font></html>");
            legend.setFont(new Font("SansSerif", Font.PLAIN, 11));
            legend.setBorder(BorderFactory.createEmptyBorder(6, 8, 0, 0));
            legend.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(legend);

            JOptionPane.showMessageDialog(
                    frame, panel,
                    "Результаты тестов случайности (" + numbers.size() + " чисел)",
                    passed == results.size() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE
            );
        });

        // Save PNG
        saveButton.addActionListener(_ -> {
            int points = dotController.getUsedRandomNumbers().size();
            var timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            var baseName = mode.getId() + "_" + timestamp + "_" + points + "pts";

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

        // Ожидание данных
        // Ожидание данных
        Thread.startVirtualThread(() -> {
            LOGGER.info(LOG_WAITING_FOR_DATA);
            SwingUtilities.invokeLater(() -> statusLabel.setText("Connecting to API..."));

            boolean dataReady = randomNumberProvider.waitForInitialData(15000);

            if (dataReady) {
                LOGGER.info(LOG_DATA_READY);
                var rngMode = randomNumberProvider.getMode();
                SwingUtilities.invokeLater(() -> {
                    // ИСПРАВЛЕНИЕ: Показываем конкретную причину, если произошел fallback
                    if (rngMode == RNProvider.Mode.PSEUDO) {
                        String reason = randomNumberProvider.getFallbackReason();
                        statusLabel.setText(reason != null ? reason : "Drawing... (Pseudo-random fallback)");
                    } else {
                        statusLabel.setText("Drawing... (Quantum)");
                    }
                    playStopButton.setEnabled(true);
                    dotController.startDotMovement();
                });
            } else {
                LOGGER.warning(LOG_DATA_TIMEOUT);
                String error = randomNumberProvider.getLastError();
                SwingUtilities.invokeLater(() ->
                        statusLabel.setText("Error: " + (error != null ? error : "Timeout")));
            }
        });

        // Window close
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                LOGGER.info(LOG_APP_SHUTTING_DOWN);
                dotController.shutdown();
                randomNumberProvider.shutdown();
                super.windowClosing(e);
            }
        });
    }
}
