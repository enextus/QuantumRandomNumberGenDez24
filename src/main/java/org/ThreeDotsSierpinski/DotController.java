package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The DotController class manages the drawing and updating of dots on the panel.
 * It also listens to data loading events to update the UI status.
 *
 * Thread safety:
 * - All access to offscreenImage happens on EDT (Swing Timer + single-shot Timer for recolor)
 * - usedRandomNumbers is a synchronizedList, iterated under synchronized block
 * - No background threads touch mutable rendering state
 */
public class DotController extends JPanel {

    private static final int SIZE_WIDTH = Config.getInt("panel.size.width");
    private static final int SIZE_HEIGHT = Config.getInt("panel.size.height");
    private static final int DOT_SIZE = Config.getInt("dot.size");
    private static final int TIMER_DELAY = Config.getInt("timer.delay");
    private static final int DOTS_PER_UPDATE = Config.getInt("dots.per.update");
    private static final long MIN_RANDOM_VALUE = Config.getLong("random.min.value");
    private static final long MAX_RANDOM_VALUE = Config.getLong("random.max.value");

    private static final String ERROR_NO_RANDOM_NUMBERS = "No available random numbers: ";
    private static final String LOG_DOTS_PROCESSED = "Processed %d new dots.";
    private static final String LOG_ERROR_MOVEMENT = "Error moving dots: %s";

    private static final int COLUMN_WIDTH = Config.getInt("column.width");
    private static final int ROW_HEIGHT = Config.getInt("row.height");
    private static final int COLUMN_SPACING = Config.getInt("column.spacing");
    private static final int MAX_COLUMNS = Config.getInt("max.columns");

    // FIX #2: removed `List<Dot> dots` — was accumulated but never read, unbounded memory leak
    private final List<Long> usedRandomNumbers;
    private final RNProvider randomNumberProvider;
    private final SierpinskiAlgorithm algorithm;
    private volatile String errorMessage;
    private Point currentPoint;
    private final BufferedImage offscreenImage;
    private int currentRandomValueIndex = 0;
    private Long currentRandomValue;
    private static final Logger LOGGER = LoggerConfig.getLogger();
    private final JLabel statusLabel;

    // Play/Stop functionality
    private Timer animationTimer;
    private volatile boolean isRunning = false;

    public DotController(RNProvider randomNumberProvider, JLabel statusLabel) {
        this.statusLabel = statusLabel;
        this.algorithm = new SierpinskiAlgorithm();
        currentPoint = new Point(SIZE_WIDTH / 2, SIZE_HEIGHT / 2);
        setPreferredSize(new Dimension(SIZE_WIDTH + 300, SIZE_HEIGHT));
        setBackground(Color.WHITE);
        usedRandomNumbers = Collections.synchronizedList(new ArrayList<>());
        this.randomNumberProvider = randomNumberProvider;
        errorMessage = null;
        offscreenImage = new BufferedImage(SIZE_WIDTH, SIZE_HEIGHT, BufferedImage.TYPE_INT_ARGB);

        // FIX #1: removed ScheduledExecutorService — recolor now uses single-shot Swing Timer (EDT)

        initAnimationTimer();
    }

    /**
     * Инициализирует таймер анимации.
     *
     * FIX #1: Перекрашивание RED→BLACK теперь через одноразовый javax.swing.Timer
     * вместо ScheduledExecutorService. Это гарантирует, что ВСЕ операции
     * с offscreenImage происходят на EDT — никаких race conditions.
     */
    private void initAnimationTimer() {
        animationTimer = new Timer(TIMER_DELAY, e -> {
            if (errorMessage == null) {
                var newDots = new ArrayList<Dot>();
                for (int i = 0; i < DOTS_PER_UPDATE; i++) {
                    try {
                        currentRandomValueIndex++;

                        long randomValue = randomNumberProvider.getNextRandomNumberInRange(
                                MIN_RANDOM_VALUE, MAX_RANDOM_VALUE);

                        currentRandomValue = randomValue;
                        usedRandomNumbers.add(currentRandomValue);
                        currentPoint = algorithm.calculateNewDotPosition(currentPoint, randomValue);
                        newDots.add(new Dot(new Point(currentPoint)));
                    } catch (NoSuchElementException ex) {
                        String msg = ex.getMessage();
                        if (msg != null && msg.startsWith("Reached maximum")) {
                            errorMessage = msg;
                            LOGGER.log(Level.WARNING, ERROR_NO_RANDOM_NUMBERS + msg);
                            updateStatusLabel("Error: " + msg);
                            stop();
                        } else {
                            LOGGER.fine("Buffer empty, skipping tick. " + msg);
                            updateStatusLabel("Loading data...");
                        }
                        break;
                    }
                }

                drawDots(newDots, Color.RED);
                repaint();
                LOGGER.fine(String.format(LOG_DOTS_PROCESSED, newDots.size()));

                // FIX #1: одноразовый Swing Timer — выполняется на EDT, не на фоновом потоке
                var recolorTimer = new Timer(1000, _ -> {
                    drawDots(newDots, Color.BLACK);
                    repaint();
                });
                recolorTimer.setRepeats(false);
                recolorTimer.start();

            } else {
                stop();
                repaint();
                LOGGER.severe(String.format(LOG_ERROR_MOVEMENT, errorMessage));
            }
        });
    }

    /**
     * Запускает анимацию (для обратной совместимости).
     */
    public void startDotMovement() {
        start();
    }

    /**
     * Запускает анимацию.
     */
    public void start() {
        if (!isRunning && errorMessage == null) {
            animationTimer.start();
            isRunning = true;
            LOGGER.info("Animation started.");
        }
    }

    /**
     * Останавливает анимацию (пауза).
     */
    public void stop() {
        if (isRunning) {
            animationTimer.stop();
            isRunning = false;
            LOGGER.info("Animation stopped.");
        }
    }

    /**
     * Переключает состояние анимации (Play/Stop).
     * @return true если анимация запущена после переключения, false если остановлена
     */
    public boolean toggle() {
        if (isRunning) {
            stop();
        } else {
            start();
        }
        return isRunning;
    }

    /**
     * Проверяет, запущена ли анимация.
     */
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(offscreenImage, 0, 0, null);
        g.setColor(Color.BLUE);
        g.drawString("Random sample index: " + currentRandomValueIndex, 10, 20);
        if (currentRandomValue != null) {
            g.setColor(Color.BLACK);
            g.drawString("Current random number: " + currentRandomValue, 10, 40);
        }

        // Крупный счётчик точек (красный, жирный)
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 48));
        g2d.setColor(Color.RED);
        g2d.drawString(String.valueOf(currentRandomValueIndex), 10, 100);

        if (errorMessage != null) {
            g.setColor(Color.RED);
            g.drawString(errorMessage, 10, 130);
        }
        drawRandomNumbersStack(g);
    }

    /**
     * Рисует точки на offscreenImage.
     * ВАЖНО: вызывается ТОЛЬКО из EDT (Swing Timer), что гарантирует потокобезопасность.
     */
    private void drawDots(List<Dot> newDots, Color color) {
        Graphics2D g2d = offscreenImage.createGraphics();
        g2d.setColor(color);
        for (Dot dot : newDots) {
            g2d.fillRect(dot.point().x, dot.point().y, DOT_SIZE, DOT_SIZE);
        }
        g2d.dispose();
    }

    private void drawRandomNumbersStack(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Font monoFont = new Font("Monospaced", Font.PLAIN, 12);
        Font headerFont = new Font("SansSerif", Font.PLAIN, 11);
        Color zebraColor = new Color(245, 245, 242);
        Color headerColor = new Color(140, 140, 140);
        Color separatorColor = new Color(225, 225, 220);
        Color numberColor = new Color(50, 50, 50);

        int headerHeight = 20;
        int maxRows = (SIZE_HEIGHT - headerHeight - 4) / ROW_HEIGHT;
        int rightMargin = 40;

        List<List<Long>> digitBuckets = new ArrayList<>();
        String[] headers = new String[MAX_COLUMNS];
        for (int i = 0; i < MAX_COLUMNS; i++) {
            digitBuckets.add(new ArrayList<>());
            headers[i] = (i + 1) + "-digit";
        }

        // FIX #3: итерация synchronizedList под synchronized блоком
        synchronized (usedRandomNumbers) {
            for (Long randomValue : usedRandomNumbers) {
                int numDigits = String.valueOf(Math.abs(randomValue)).length();
                if (numDigits >= 1 && numDigits <= 5) {
                    digitBuckets.get(numDigits - 1).add(randomValue);
                }
            }
        }

        List<Integer> visibleColumns = new ArrayList<>();
        for (int i = 0; i < MAX_COLUMNS; i++) {
            if (!digitBuckets.get(i).isEmpty()) {
                visibleColumns.add(i);
            }
        }
        if (visibleColumns.isEmpty()) return;

        int totalWidth = visibleColumns.size() * (COLUMN_WIDTH + COLUMN_SPACING) - COLUMN_SPACING;
        int startX = getWidth() - totalWidth - rightMargin;

        for (int visIdx = 0; visIdx < visibleColumns.size(); visIdx++) {
            int bucketIdx = visibleColumns.get(visIdx);
            List<Long> columnNumbers = digitBuckets.get(bucketIdx);
            int colX = startX + visIdx * (COLUMN_WIDTH + COLUMN_SPACING);

            g2d.setFont(headerFont);
            g2d.setColor(headerColor);
            FontMetrics hfm = g2d.getFontMetrics();
            int headerTextWidth = hfm.stringWidth(headers[bucketIdx]);
            g2d.drawString(headers[bucketIdx], colX + (COLUMN_WIDTH - headerTextWidth) / 2, headerHeight - 4);

            g2d.setColor(separatorColor);
            g2d.drawLine(colX, headerHeight, colX + COLUMN_WIDTH, headerHeight);

            if (visIdx > 0) {
                int sepX = colX - COLUMN_SPACING / 2;
                g2d.setColor(separatorColor);
                g2d.drawLine(sepX, 0, sepX, SIZE_HEIGHT);
            }

            g2d.setFont(monoFont);
            FontMetrics fm = g2d.getFontMetrics();
            int row = 0;

            for (int i = columnNumbers.size() - 1; i >= 0 && row < maxRows; i--, row++) {
                int y = SIZE_HEIGHT - (row * ROW_HEIGHT) - 4;

                if (row % 2 == 0) {
                    g2d.setColor(zebraColor);
                    g2d.fillRect(colX, y - ROW_HEIGHT + 4, COLUMN_WIDTH, ROW_HEIGHT);
                }

                String text = columnNumbers.get(i).toString();
                int textWidth = fm.stringWidth(text);
                g2d.setColor(numberColor);
                g2d.drawString(text, colX + COLUMN_WIDTH - textWidth - 2, y);
            }
        }
    }

    public void updateStatusLabel(String message) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(message));
    }

    public List<Long> getUsedRandomNumbers() {
        return usedRandomNumbers;
    }

    /**
     * Очищает ресурсы при закрытии.
     */
    public void shutdown() {
        stop();
        // FIX #1: больше нет scheduler.shutdown() — всё на EDT через Swing Timer
    }

    /**
     * Сохраняет два PNG-файла фрактала:
     * 1) с прозрачным фоном (оригинал)
     * 2) с белым фоном
     *
     * @param directory папка для сохранения
     * @param baseName  базовое имя без расширения
     * @return количество успешно сохранённых файлов (0, 1 или 2)
     */
    public int saveImages(java.io.File directory, String baseName) {
        int saved = 0;

        var transparentFile = new java.io.File(directory, baseName + "_transparent.png");
        try {
            javax.imageio.ImageIO.write(offscreenImage, "PNG", transparentFile);
            LOGGER.info("Saved (transparent): " + transparentFile.getAbsolutePath());
            saved++;
        } catch (java.io.IOException e) {
            LOGGER.severe("Failed to save transparent image: " + e.getMessage());
        }

        var whiteFile = new java.io.File(directory, baseName + ".png");
        try {
            var whiteImage = new BufferedImage(
                    offscreenImage.getWidth(), offscreenImage.getHeight(), BufferedImage.TYPE_INT_RGB);
            var g = whiteImage.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, whiteImage.getWidth(), whiteImage.getHeight());
            g.drawImage(offscreenImage, 0, 0, null);
            g.dispose();

            javax.imageio.ImageIO.write(whiteImage, "PNG", whiteFile);
            LOGGER.info("Saved (white bg): " + whiteFile.getAbsolutePath());
            saved++;
        } catch (java.io.IOException e) {
            LOGGER.severe("Failed to save white-bg image: " + e.getMessage());
        }

        return saved;
    }

}
