package org.ThreeDotsSierpinski.app;

import org.ThreeDotsSierpinski.config.LoggerConfig;
import org.ThreeDotsSierpinski.stats.RandomnessTestSuite;
import org.ThreeDotsSierpinski.stats.TestResult;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Runs statistical randomness checks without blocking the Swing event dispatch thread.
 */
final class RandomnessQualityCheck {
    private static final Logger LOGGER = LoggerConfig.getLogger();
    private static final int MIN_SAMPLE_SIZE = 10;
    private static final double ALPHA = 0.05;
    private static final String BUTTON_TESTING = "Testing...";

    private RandomnessQualityCheck() {
    }

    static void run(
            JFrame frame,
            JLabel statusLabel,
            JButton testButton,
            DotController dotController,
            AtomicBoolean visualizationFinished
    ) {
        List<Long> numbers = List.copyOf(dotController.getUsedRandomNumbers());
        if (numbers.size() < MIN_SAMPLE_SIZE) {
            statusLabel.setText("Нужно минимум 10 точек для тестов");
            return;
        }

        String previousButtonText = testButton.getText();
        testButton.setEnabled(false);
        testButton.setText(BUTTON_TESTING);
        statusLabel.setText("Running randomness tests... (" + numbers.size() + " numbers)");

        var worker = new Worker(
                numbers,
                ALPHA,
                new RandomnessTestSuite()::runAll,
                new Callback() {
                    @Override
                    public void onSuccess(Result result) {
                        if (!canUpdateUi(frame, visualizationFinished)) {
                            return;
                        }

                        restoreButton(testButton, previousButtonText);
                        showResults(frame, statusLabel, result);
                    }

                    @Override
                    public void onFailure(Throwable error) {
                        if (!canUpdateUi(frame, visualizationFinished)) {
                            return;
                        }

                        restoreButton(testButton, previousButtonText);
                        showFailure(frame, statusLabel, error);
                    }
                }
        );
        worker.execute();
    }

    private static boolean canUpdateUi(JFrame frame, AtomicBoolean visualizationFinished) {
        return !visualizationFinished.get() && frame.isDisplayable();
    }

    private static void restoreButton(JButton testButton, String previousButtonText) {
        testButton.setEnabled(true);
        testButton.setText(previousButtonText);
    }

    private static void showResults(JFrame frame, JLabel statusLabel, Result result) {
        List<TestResult> results = result.results();
        long passed = result.passedCount();

        statusLabel.setText("Тесты: " + passed + "/" + results.size()
                + " пройдено (" + result.sampleSize() + " чисел)");

        var panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));

        for (TestResult testResult : results) {
            panel.add(createResultRow(testResult));
        }

        panel.add(Box.createVerticalStrut(8));
        var summary = new JLabel("Итого: " + passed + "/" + results.size() + " тестов пройдено");
        summary.setFont(new Font("SansSerif", Font.BOLD, 13));
        summary.setAlignmentX(Component.LEFT_ALIGNMENT);
        summary.setBorder(BorderFactory.createEmptyBorder(4, 8, 0, 0));
        panel.add(summary);

        var legend = new JLabel("<html><font color='#228B22'>● отлично</font>"
                + "   <font color='#CC9900'>● приемлемо</font>"
                + "   <font color='#CC0000'>● не пройден</font></html>");
        legend.setFont(new Font("SansSerif", Font.PLAIN, 11));
        legend.setBorder(BorderFactory.createEmptyBorder(6, 8, 0, 0));
        legend.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(legend);

        JOptionPane.showMessageDialog(
                frame, panel,
                "Результаты тестов случайности (" + result.sampleSize() + " чисел)",
                passed == results.size() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE
        );
    }

    private static JPanel createResultRow(TestResult result) {
        var row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        var indicator = new JLabel("●");
        indicator.setFont(new Font("SansSerif", Font.BOLD, 16));
        indicator.setForeground(switch (result.quality()) {
            case STRONG -> new Color(34, 139, 34);
            case MARGINAL -> new Color(204, 153, 0);
            case FAIL -> new Color(204, 0, 0);
        });
        row.add(indicator);

        var mark = new JLabel(switch (result.quality()) {
            case STRONG -> "✓";
            case MARGINAL -> "○";
            case FAIL -> "✗";
        });
        mark.setFont(new Font("SansSerif", Font.BOLD, 14));
        mark.setForeground(indicator.getForeground());
        row.add(mark);

        var text = new JLabel(result.statistic() + "    " + result.testName());
        text.setFont(new Font("Monospaced", Font.PLAIN, 13));
        row.add(text);
        return row;
    }

    private static void showFailure(JFrame frame, JLabel statusLabel, Throwable error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) {
            message = error.getClass().getSimpleName();
        }

        LOGGER.warning("Randomness quality tests failed: " + message);
        statusLabel.setText("Randomness tests failed: " + message);

        JOptionPane.showMessageDialog(
                frame,
                "Randomness quality tests failed:\n" + message,
                "Randomness Test Error",
                JOptionPane.ERROR_MESSAGE
        );
    }

    @FunctionalInterface
    interface Runner {
        List<TestResult> run(List<Long> numbers, double alpha);
    }

    interface Callback {
        void onSuccess(Result result);

        void onFailure(Throwable error);
    }

    record Result(int sampleSize, List<TestResult> results) {
        Result {
            results = List.copyOf(results);
        }

        long passedCount() {
            return results.stream().filter(TestResult::passed).count();
        }
    }

    static final class Worker extends SwingWorker<Result, Void> {
        private final List<Long> numbers;
        private final double alpha;
        private final Runner runner;
        private final Callback callback;

        Worker(List<Long> numbers, double alpha, Runner runner, Callback callback) {
            this.numbers = List.copyOf(numbers);
            this.alpha = alpha;
            this.runner = runner;
            this.callback = callback;
        }

        @Override
        protected Result doInBackground() {
            return new Result(numbers.size(), runner.run(numbers, alpha));
        }

        @Override
        protected void done() {
            if (isCancelled()) {
                return;
            }

            try {
                callback.onSuccess(get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                callback.onFailure(e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                callback.onFailure(cause == null ? e : cause);
            }
        }
    }
}
