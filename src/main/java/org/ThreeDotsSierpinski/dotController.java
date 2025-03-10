package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The DotController class manages the drawing and updating of dots on the panel.
 * It also listens to data loading events to update the UI status.
 */
public class DotDisplayController extends JPanel {

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

    private final List<Dot> dots;
    private final List<Long> usedRandomNumbers;
    private final RNProvider randomNumberProvider;
    private volatile String errorMessage;
    private Point currentPoint;
    private final BufferedImage offscreenImage;
    private final ScheduledExecutorService scheduler;
    private int currentRandomValueIndex = 0;
    private Long currentRandomValue;
    private static final Logger LOGGER = LoggerConfig.getLogger();
    private final JLabel statusLabel;

    public dotController(RNProvider randomNumberProvider, JLabel statusLabel) {
        this.statusLabel = statusLabel;
        currentPoint = new Point(SIZE_WIDTH / 2, SIZE_HEIGHT / 2);
        setPreferredSize(new Dimension(SIZE_WIDTH + 300, SIZE_HEIGHT));
        setBackground(Color.WHITE);
        dots = Collections.synchronizedList(new ArrayList<>());
        usedRandomNumbers = Collections.synchronizedList(new ArrayList<>());
        this.randomNumberProvider = randomNumberProvider;
        errorMessage = null;
        offscreenImage = new BufferedImage(SIZE_WIDTH, SIZE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        scheduler = Executors.newScheduledThreadPool(1);
        this.randomNumberProvider.addDataLoadListener(new RNLoadListenerImpl(this));
    }

    public void startDotMovement() {
        Timer timer = new Timer(TIMER_DELAY, e -> {
            if (errorMessage == null) {
                ArrayList<Dot> newDots = new ArrayList<>();
                for (int i = 0; i < DOTS_PER_UPDATE; i++) {
                    try {
                        currentRandomValueIndex++;
                        long randomValue = randomNumberProvider.getNextRandomNumberInRange(MIN_RANDOM_VALUE, MAX_RANDOM_VALUE);
                        currentRandomValue = randomValue;
                        usedRandomNumbers.add(currentRandomValue);
                        currentPoint = calculateNewDotPosition(currentPoint, randomValue);
                        Dot newDot = new Dot(new Point(currentPoint));
                        newDots.add(newDot);
                    } catch (NoSuchElementException ex) {
                        if (errorMessage == null) {
                            errorMessage = ex.getMessage();
                            LOGGER.log(Level.WARNING, ERROR_NO_RANDOM_NUMBERS + ex.getMessage());
                            updateStatusLabel("Error: " + ex.getMessage());
                        }
                        ((Timer) e.getSource()).stop();
                        break;
                    }
                }
                dots.addAll(newDots);
                drawDots(newDots, Color.RED);
                repaint();
                LOGGER.fine(String.format(LOG_DOTS_PROCESSED, newDots.size()));
                scheduler.schedule(() -> {
                    drawDots(newDots, Color.BLACK);
                    repaint();
                }, 1, TimeUnit.SECONDS);
            } else {
                ((Timer) e.getSource()).stop();
                repaint();
                LOGGER.severe(String.format(LOG_ERROR_MOVEMENT, errorMessage));
            }
        });
        timer.start();
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
        if (errorMessage != null) {
            g.setColor(Color.RED);
            g.drawString(errorMessage, 10, 60);
        }
        drawRandomNumbersStack(g);
    }

    private Point calculateNewDotPosition(Point currentPoint, long randomValue) {
        Point A = new Point(SIZE_WIDTH / 2, 0);
        Point B = new Point(0, SIZE_HEIGHT);
        Point C = new Point(SIZE_WIDTH, SIZE_HEIGHT);
        long rangePart = (MAX_RANDOM_VALUE - MIN_RANDOM_VALUE) / 3;
        int x = currentPoint.x;
        int y = currentPoint.y;
        if (randomValue <= MIN_RANDOM_VALUE + rangePart) {
            x = (x + A.x) / 2;
            y = (y + A.y) / 2;
        } else if (randomValue <= MIN_RANDOM_VALUE + 2 * rangePart) {
            x = (x + B.x) / 2;
            y = (y + B.y) / 2;
        } else {
            x = (x + C.x) / 2;
            y = (y + C.y) / 2;
        }
        return new Point(x, y);
    }

    private void drawDots(List<Dot> newDots, Color color) {
        Graphics2D g2d = offscreenImage.createGraphics();
        g2d.setColor(color);
        for (Dot dot : newDots) {
            g2d.fillRect(dot.point().x, dot.point().y, DOT_SIZE, DOT_SIZE);
        }
        g2d.dispose();
    }

    private void drawRandomNumbersStack(Graphics g) {
        g.setColor(Color.BLACK);
        int maxRowsPerColumn = SIZE_HEIGHT / ROW_HEIGHT;
        int startX = SIZE_WIDTH + 20;
        int startY = SIZE_HEIGHT - ROW_HEIGHT;
        List<List<Long>> digitBuckets = new ArrayList<>();
        for (int i = 0; i < MAX_COLUMNS; i++) {
            digitBuckets.add(new ArrayList<>());
        }
        for (Long randomValue : usedRandomNumbers) {
            int numDigits = String.valueOf(Math.abs(randomValue)).length();
            if (numDigits >= 3 && numDigits <= 8) {
                digitBuckets.get(numDigits - 3).add(randomValue);
            }
        }
        for (int column = 0; column < MAX_COLUMNS; column++) {
            List<Long> columnNumbers = digitBuckets.get(column);
            int row = maxRowsPerColumn - 1;
            for (int i = columnNumbers.size() - 1; i >= 0; i--) {
                if (row < 0) {
                    break;
                }
                int x = startX + column * (COLUMN_WIDTH + COLUMN_SPACING);
                int y = startY - row * ROW_HEIGHT;
                g.drawString(columnNumbers.get(i).toString(), x, y);
                row--;
            }
        }
    }

    public void updateStatusLabel(String message) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(message));
    }

}

