package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Универсальный контроллер визуализации случайных чисел.
 * <p>
 * Принимает любой {@link VisualizationMode} и управляет анимацией,
 * рендерингом, статусом и сохранением изображений.
 * <p>
 * Thread safety:
 * - Все операции с offscreenImage — только на EDT
 * - usedRandomNumbers итерируется под synchronized
 */
public class DotController extends JPanel {

    private static final int SIZE_WIDTH = Config.getInt("panel.size.width");
    private static final int SIZE_HEIGHT = Config.getInt("panel.size.height");
    private static final int DOT_SIZE = Config.getInt("dot.size");
    private static final int TIMER_DELAY = Config.getInt("timer.delay");

    private static final int COLUMN_WIDTH = Config.getInt("column.width");
    private static final int ROW_HEIGHT = Config.getInt("row.height");
    private static final int COLUMN_SPACING = Config.getInt("column.spacing");
    private static final int MAX_COLUMNS = Config.getInt("max.columns");


    private static final Logger LOGGER = LoggerConfig.getLogger();

    private final VisualizationMode mode;
    // private final List<Long> usedRandomNumbers;
    private final RNProvider randomNumberProvider;
    private volatile String errorMessage;
    private final BufferedImage offscreenImage;
    private Long currentRandomValue;
    private final JLabel statusLabel;

    private Timer animationTimer;
    private volatile boolean isRunning = false;

    private final List<Point> pendingRecolorPoints = new ArrayList<>();
    private Timer recolorTimer;

    public DotController(RNProvider randomNumberProvider, VisualizationMode mode, JLabel statusLabel) {
        this.statusLabel = statusLabel;
        this.mode = mode;
        this.randomNumberProvider = randomNumberProvider;
        setPreferredSize(new Dimension(SIZE_WIDTH + 300, SIZE_HEIGHT));
        setBackground(Color.WHITE);
        // usedRandomNumbers = Collections.synchronizedList(new ArrayList<>());
        errorMessage = null;
        offscreenImage = new BufferedImage(SIZE_WIDTH, SIZE_HEIGHT, BufferedImage.TYPE_INT_ARGB);

        // Инициализируем выбранный режим
        mode.initialize(offscreenImage, SIZE_WIDTH, SIZE_HEIGHT);

        initAnimationTimer();

        // ИСПРАВЛЕНИЕ 2.3: Единый таймер для перекраски вместо создания новых в каждом тике
        recolorTimer = new Timer(1000, e -> {
            synchronized (pendingRecolorPoints) {
                if (!pendingRecolorPoints.isEmpty()) {
                    var g2d = offscreenImage.createGraphics();
                    g2d.setColor(Color.BLACK);
                    for (var p : pendingRecolorPoints) {
                        g2d.fillRect(p.x, p.y, DOT_SIZE, DOT_SIZE);
                    }
                    g2d.dispose();
                    pendingRecolorPoints.clear();
                    repaint();
                }
            }
        });
        recolorTimer.setRepeats(false);
    } // <--- Здесь заканчивается конструктор DotController (скобка ОДНА)

    private void initAnimationTimer() { // <--- Метод внутри класса
        animationTimer = new Timer(TIMER_DELAY, e -> {

            if (errorMessage == null) {
                try {
                    // Делегируем шаг визуализации выбранному режиму
                    var newPoints = mode.step(randomNumberProvider, offscreenImage, DOT_SIZE);

                    // Запоминаем использованные числа (для тестов качества)
                    // Берём последние из provider — уже потреблены в step()
                    // Используем pointCount как proxy
                    repaint();

                    // ИСПРАВЛЕНИЕ 2.3: Кладём точки в очередь и перезапускаем единый таймер
                    if (!newPoints.isEmpty()) {
                        synchronized (pendingRecolorPoints) {
                            pendingRecolorPoints.addAll(newPoints);
                        }
                        if (!recolorTimer.isRunning()) {
                            recolorTimer.restart();
                        }
                    }

                } catch (NoSuchElementException ex) {
                    String msg = ex.getMessage();
                    if (msg != null && msg.startsWith("Reached maximum")) {
                        errorMessage = msg;
                        LOGGER.log(Level.WARNING, "No available random numbers: " + msg);
                        updateStatusLabel("Error: " + msg);
                        stop();
                    } else {
                        LOGGER.fine("Buffer empty, skipping tick. " + msg);
                        updateStatusLabel("Loading data...");
                    }
                }
            } else {
                stop();
                repaint();
                LOGGER.severe("Error: " + errorMessage);
            }
        });
    }

    public void startDotMovement() {
        start();
    }

    public void start() {
        if (!isRunning && errorMessage == null) {
            animationTimer.start();
            isRunning = true;
            LOGGER.info("Animation started: " + mode.getName());
        }
    }

    public void stop() {
        if (isRunning) {
            animationTimer.stop();
            isRunning = false;
            LOGGER.info("Animation stopped.");
        }
    }

    public boolean toggle() {
        if (isRunning) stop();
        else start();
        return isRunning;
    }

    public boolean isRunning() {
        return isRunning;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(offscreenImage, 0, 0, null);

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Инфо
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2d.setColor(Color.BLUE);
        g2d.drawString(mode.getName() + "  |  Points: " + mode.getPointCount()
                + "  |  Random numbers used: " + mode.getRandomNumbersUsed(), 10, 20);

        // Крупный счётчик точек
        g2d.setFont(new Font("SansSerif", Font.BOLD, 48));
        g2d.setColor(Color.RED);
        g2d.drawString(String.valueOf(mode.getPointCount()), 10, 80);

        // Индикатор режима RNG
        var rngMode = randomNumberProvider.getMode();
        boolean isQuantum = rngMode == RNProvider.Mode.QUANTUM;
        g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2d.setColor(isQuantum ? new Color(34, 139, 34) : new Color(204, 120, 0));
        String modeLabel = isQuantum ? "\u25CF QUANTUM" : "\u25CF PSEUDO (L128X256MixRandom)";
        g2d.drawString(modeLabel, 10, 100);

        if (errorMessage != null) {
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2d.drawString(errorMessage, 10, 120);
        }

        drawRandomNumbersStack(g);
    }

    private void drawDots(List<Point> points, Color color) {
        var g2d = offscreenImage.createGraphics();
        g2d.setColor(color);
        for (var p : points) {
            g2d.fillRect(p.x, p.y, DOT_SIZE, DOT_SIZE);
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

        for (Long randomValue : randomNumberProvider.getConsumedNumbers()) {
            int numDigits = String.valueOf(Math.abs(randomValue)).length();
            if (numDigits >= 1 && numDigits <= 5) {
                digitBuckets.get(numDigits - 1).add(randomValue);
            }
        }

        List<Integer> visibleColumns = new ArrayList<>();
        for (int i = 0; i < MAX_COLUMNS; i++) {
            if (!digitBuckets.get(i).isEmpty()) visibleColumns.add(i);
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
        return randomNumberProvider.getConsumedNumbers();
    }

    /**
     * Имя текущего режима визуализации
     */
    public String getModeName() {
        return mode.getName();
    }

    public void shutdown() {
        stop();
        if (recolorTimer != null) {
            recolorTimer.stop();
        }
    }

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
