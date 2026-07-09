package org.ThreeDotsSierpinski.app;

import com.formdev.flatlaf.FlatLightLaf;
import org.ThreeDotsSierpinski.config.LoggerConfig;
import org.ThreeDotsSierpinski.mode.VisualizationCategory;
import org.ThreeDotsSierpinski.mode.VisualizationMode;
import org.ThreeDotsSierpinski.rng.FallbackReason;
import org.ThreeDotsSierpinski.rng.RNLoadListenerImpl;
import org.ThreeDotsSierpinski.rng.RNProvider;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Главный класс приложения.
 * Запуск: диалог выбора режима → основное окно визуализации.
 */
public class App {
    private static final Set<String> visitedModeIds = new HashSet<>();
    private static final String LOG_APP_STARTED = "Application started.";
    private static final String LOG_GUI_STARTED = "GUI successfully launched.";
    private static final String LOG_APP_SHUTTING_DOWN = "Shutting down application.";
    private static final String LOG_WAITING_FOR_DATA = "Waiting for initial random numbers...";
    private static final String LOG_DATA_READY = "Initial data loaded, starting animation.";
    private static final String LOG_DATA_TIMEOUT = "Timeout waiting for initial data.";
    private static final String LOG_NO_MODE_SELECTED = "No mode selected, exiting.";
    private static final String LOG_SELECTED_MODE_PREFIX = "Selected mode: ";
    private static final String LOG_VISUALIZATION_FINISHED = "Visualization finished, returning to mode selection.";
    private static final String LOG_TARGET_SCREEN_BOUNDS_PREFIX = "Target screen bounds: ";
    private static final String LOG_RETURN_SCREEN_BOUNDS_PREFIX = "Return screen bounds: ";
    private static final String LOG_SELECTION_SCREEN_BOUNDS_PREFIX = "Mode selection screen bounds: ";
    private static final String BUTTON_PLAY = "► Play";
    private static final String BUTTON_STOP = "Stop";
    private static final int INITIAL_DATA_TIMEOUT_MS = 15_000;
    private static final Logger LOGGER = LoggerConfig.getLogger();
    private static VisualizationCategory lastSelectedCategory = null;
    private static String lastSelectedModeId = null;

    static void main(String[] args) {
        FlatLightLaf.setup();

        LoggerConfig.initializeLogger();
        LOGGER.info(LOG_APP_STARTED);

        SwingUtilities.invokeLater(() -> {
            GraphicsConfiguration targetGraphicsConfiguration = resolveLaunchGraphicsConfiguration();
            LOGGER.info(LOG_TARGET_SCREEN_BOUNDS_PREFIX + targetGraphicsConfiguration.getBounds());
            showModeSelectionLoop(targetGraphicsConfiguration);
        });
    }

    private static void showModeSelectionLoop(GraphicsConfiguration targetGraphicsConfiguration) {
        var selector = new ModeSelectionDialog();
        var selectedMode = selector.showAndWait(
                null,
                targetGraphicsConfiguration,
                lastSelectedCategory,
                visitedModeIds,
                lastSelectedModeId
        );

        GraphicsConfiguration currentSelectionGraphicsConfiguration = selector.getLastDialogGraphicsConfiguration();
        if (currentSelectionGraphicsConfiguration == null) {
            currentSelectionGraphicsConfiguration = targetGraphicsConfiguration;
        }

        if (selectedMode == null) {
            LOGGER.info(LOG_NO_MODE_SELECTED);
            LOGGER.info(LOG_APP_SHUTTING_DOWN);
            System.exit(0);
            return;
        }

        lastSelectedCategory = selectedMode.getCategory();
        visitedModeIds.add(selectedMode.getId());
        lastSelectedModeId = selectedMode.getId();

        LOGGER.info(LOG_SELECTED_MODE_PREFIX + selectedMode.getName());
        LOGGER.info(LOG_SELECTION_SCREEN_BOUNDS_PREFIX + currentSelectionGraphicsConfiguration.getBounds());
        launchMainWindow(selectedMode, currentSelectionGraphicsConfiguration);
    }

    private static void launchMainWindow(
            VisualizationMode mode,
            GraphicsConfiguration targetGraphicsConfiguration
    ) {
        RNProvider randomNumberProvider = new RNProvider();
        JLabel statusLabel = new JLabel("Initializing...");
        AtomicBoolean visualizationFinished = new AtomicBoolean(false);

        var frame = VisualizationFrameFactory.createFrame(mode, targetGraphicsConfiguration);
        var dotController = new DotController(randomNumberProvider, mode, statusLabel);
        frame.add(dotController, BorderLayout.CENTER);

        var controls = VisualizationControlsPanel.create(statusLabel, mode, dotController);
        var playStopButton = controls.playStopButton();
        var rngToggle = controls.rngToggle();
        var testButton = controls.testButton();
        var saveButton = controls.saveButton();
        var finishButton = controls.finishButton();
        var syncToggleLabel = controls.syncToggleLabel();
        frame.add(controls.bottomPanel(), BorderLayout.SOUTH);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                finishVisualization(frame, dotController, randomNumberProvider, visualizationFinished);
            }
        });

        frame.setVisible(true);
        LOGGER.info(LOG_GUI_STARTED);

        randomNumberProvider.addDataLoadListener(new RNLoadListenerImpl(dotController, frame, rngToggle));

// Play/Stop
        playStopButton.addActionListener(_ -> {
            boolean running = dotController.toggle();
            playStopButton.setText(running ? BUTTON_STOP : BUTTON_PLAY);
            if (running) {
                // показываем режим при рисовании
                boolean isQuantum = rngToggle.isSelected() && rngToggle.isEnabled();
                statusLabel.setText(isQuantum ? "Drawing... (Quantum)" : "Drawing... (Pseudo-random)");
            } else {
                statusLabel.setText("Paused. Points: " + dotController.getUsedRandomNumberCount());
            }
        });

// selected=true → QUANTUM, selected=false → PSEUDO
        rngToggle.addActionListener(_ -> {
            // Если toggle disabled (нет API ключа), показываем диалог
            if (!rngToggle.isEnabled()) {
                JOptionPane.showMessageDialog(
                        frame,
                        """
                                API key not configured.
                                
                                Quantum random numbers require a valid API key.
                                Set QRNG_API_KEY environment variable or add it to .env file.""",
                        "API Key Required",
                        JOptionPane.WARNING_MESSAGE
                );
                rngToggle.setSelected(false);
                syncToggleLabel.run();
                return;
            }

            boolean wantsQuantum = rngToggle.isSelected();

            // Проверяем rate limit перед переключением
            boolean isRateLimit = randomNumberProvider.getFallbackReasonCode() == FallbackReason.RATE_LIMIT;

            if (wantsQuantum && isRateLimit) {
                // Пытаемся переключиться на QUANTUM, но rate limit активен
                JOptionPane.showMessageDialog(
                        frame,
                        """
                                Daily API limit exceeded.
                                
                                Quantum random numbers are temporarily unavailable.
                                Please try again later or continue using PSEUDO mode.""",
                        "Rate Limit Exceeded",
                        JOptionPane.WARNING_MESSAGE
                );
                // Возвращаем toggle в PSEUDO
                rngToggle.setSelected(false);
                syncToggleLabel.run();
                return;
            }

            randomNumberProvider.setForcedPseudo(!wantsQuantum);
            dotController.refreshVisualization();

            if (dotController.isRunning()) {
                dotController.updateStatusLabel(wantsQuantum
                        ? "Drawing... (Quantum)"
                        : "Drawing... (Pseudo-random)");
            }
        });

        // Проверить качество
        testButton.addActionListener(_ -> RandomnessQualityCheck.run(frame, statusLabel, testButton, dotController, visualizationFinished));

        // Save PNG
        saveButton.addActionListener(_ -> {
            int points = mode.getPointCount();
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

        // Закончить визуализацию → вернуться к выбору режима
        finishButton.addActionListener(_ ->
                finishVisualization(frame, dotController, randomNumberProvider, visualizationFinished)
        );

        // Ожидание инициализации
        Thread.startVirtualThread(() -> {
            LOGGER.info(LOG_WAITING_FOR_DATA);
            SwingUtilities.invokeLater(() -> {
                if (!visualizationFinished.get() && frame.isDisplayable()) {
                    statusLabel.setText("Connecting to API...");
                }
            });

            boolean dataReady = randomNumberProvider.waitForInitialData(INITIAL_DATA_TIMEOUT_MS);

            SwingUtilities.invokeLater(() -> {
                if (visualizationFinished.get() || !frame.isDisplayable()) {
                    return;
                }

                if (dataReady) {
                    LOGGER.info(LOG_DATA_READY);
                    var rngMode = randomNumberProvider.getMode();

                    rngToggle.setEnabled(randomNumberProvider.isApiKeyConfigured());
                    rngToggle.setSelected(rngMode == RNProvider.Mode.QUANTUM);
                    syncToggleLabel.run();
                    dotController.refreshVisualization();

                    if (rngMode == RNProvider.Mode.PSEUDO) {
                        String displayReason = randomNumberProvider.getFallbackReasonDisplayText();
                        if (displayReason == null || displayReason.isBlank()) {
                            displayReason = "fallback";
                        }

                        statusLabel.setText("PSEUDO mode (" + displayReason + ")");
                    } else {
                        statusLabel.setText("Ready. (QUANTUM)");
                    }
                } else {
                    LOGGER.warning(LOG_DATA_TIMEOUT);
                    rngToggle.setEnabled(randomNumberProvider.isApiKeyConfigured());
                    rngToggle.setSelected(false);
                    syncToggleLabel.run();
                    dotController.refreshVisualization();

                    if (!randomNumberProvider.isApiKeyConfigured()) {
                        statusLabel.setText("PSEUDO mode (no API key)");
                    } else {
                        statusLabel.setText("PSEUDO mode (API unavailable)");
                    }
                }

                playStopButton.setEnabled(true);
            });
        });
    }

    private static void finishVisualization(
            JFrame frame,
            DotController dotController,
            RNProvider randomNumberProvider,
            AtomicBoolean visualizationFinished
    ) {
        if (!visualizationFinished.compareAndSet(false, true)) {
            return;
        }

        GraphicsConfiguration returnGraphicsConfiguration = resolveWindowGraphicsConfiguration(frame);

        dotController.shutdown();
        randomNumberProvider.shutdown();
        frame.dispose();

        LOGGER.info(LOG_VISUALIZATION_FINISHED);
        LOGGER.info(LOG_RETURN_SCREEN_BOUNDS_PREFIX + returnGraphicsConfiguration.getBounds());

        SwingUtilities.invokeLater(() -> showModeSelectionLoop(returnGraphicsConfiguration));
    }

    /**
     * Возвращает монитор, на котором фактически находилось окно визуализации
     * в момент завершения. Это важно для multi-monitor workflow: если пользователь
     * перетащил окно модуса на другой монитор и нажал «Закончить визуализацию»,
     * следующий ModeSelectionDialog должен открыться именно там.
     */
    private static GraphicsConfiguration resolveWindowGraphicsConfiguration(Window window) {
        if (window == null) {
            return getDefaultGraphicsConfiguration();
        }

        Rectangle windowBounds = window.getBounds();

        if (windowBounds != null && !windowBounds.isEmpty()) {
            GraphicsConfiguration largestIntersectionGraphicsConfiguration =
                    findGraphicsConfigurationWithLargestIntersection(windowBounds);
            if (largestIntersectionGraphicsConfiguration != null) {
                return largestIntersectionGraphicsConfiguration;
            }

            Point windowCenter = new Point(
                    windowBounds.x + windowBounds.width / 2,
                    windowBounds.y + windowBounds.height / 2
            );

            GraphicsConfiguration centerGraphicsConfiguration = findGraphicsConfiguration(windowCenter);
            if (centerGraphicsConfiguration != null) {
                return centerGraphicsConfiguration;
            }
        }

        GraphicsConfiguration currentGraphicsConfiguration = window.getGraphicsConfiguration();
        if (currentGraphicsConfiguration != null) {
            return currentGraphicsConfiguration;
        }

        return getDefaultGraphicsConfiguration();
    }

    private static GraphicsConfiguration findGraphicsConfigurationWithLargestIntersection(Rectangle windowBounds) {
        GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();

        GraphicsConfiguration bestGraphicsConfiguration = null;
        long bestIntersectionArea = 0;

        for (GraphicsDevice screenDevice : graphicsEnvironment.getScreenDevices()) {
            GraphicsConfiguration graphicsConfiguration = screenDevice.getDefaultConfiguration();
            Rectangle intersection = graphicsConfiguration.getBounds().intersection(windowBounds);

            long intersectionArea = (long) Math.max(0, intersection.width)
                    * Math.max(0, intersection.height);

            if (intersectionArea > bestIntersectionArea) {
                bestIntersectionArea = intersectionArea;
                bestGraphicsConfiguration = graphicsConfiguration;
            }
        }

        return bestGraphicsConfiguration;
    }

    private static GraphicsConfiguration getDefaultGraphicsConfiguration() {
        return GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration();
    }

    /**
     * Java/Swing не знает, из какого внешнего окна Windows был запущен процесс
     * (IDEA, VS Code, Terminal). Поэтому выбираем монитор под курсором мыши в момент старта.
     * Обычно курсор находится именно на том мониторе, где пользователь нажал Run / запустил команду.
     */
    private static GraphicsConfiguration resolveLaunchGraphicsConfiguration() {
        try {
            PointerInfo pointerInfo = MouseInfo.getPointerInfo();
            if (pointerInfo != null) {
                Point pointerLocation = pointerInfo.getLocation();
                GraphicsConfiguration pointerGraphicsConfiguration = findGraphicsConfiguration(pointerLocation);
                if (pointerGraphicsConfiguration != null) {
                    return pointerGraphicsConfiguration;
                }
            }
        } catch (HeadlessException e) {
            LOGGER.warning("Cannot resolve pointer screen in headless environment: " + e.getMessage());
        }

        return getDefaultGraphicsConfiguration();
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

}
