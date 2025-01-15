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
public class DotDisplayController extends JPanel implements RandomNumberLoadListener {
    // Panel parameters
    private static final int SIZE_WIDTH = Config.getInt("panel.size.width");
    private static final int SIZE_HEIGHT = Config.getInt("panel.size.height");

    // Dot and timer parameters
    private static final int DOT_SIZE = Config.getInt("dot.size");
    private static final int TIMER_DELAY = Config.getInt("timer.delay");
    private static final int DOTS_PER_UPDATE = Config.getInt("dots.per.update");

    // Random number range
    private static final long MIN_RANDOM_VALUE = Config.getLong("random.min.value");
    private static final long MAX_RANDOM_VALUE = Config.getLong("random.max.value");

    // Constants for messages and logging
    private static final String ERROR_NO_RANDOM_NUMBERS = "No available random numbers: ";
    private static final String LOG_DOTS_PROCESSED = "Processed %d new dots.";
    private static final String LOG_ERROR_MOVEMENT = "Error moving dots: %s";

    // Random number stack visualization parameters
    private static final int COLUMN_WIDTH = Config.getInt("column.width");
    private static final int ROW_HEIGHT = Config.getInt("row.height");
    private static final int COLUMN_SPACING = Config.getInt("column.spacing");
    private static final int MAX_COLUMNS = Config.getInt("max.columns");

    private final List<Dot> dots; // List of dots
    private final List<Long> usedRandomNumbers; // List of used random numbers for visualization
    private final RandomNumberProvider randomNumberProvider; // Provider for random numbers
    private volatile String errorMessage; // Error message
    private Point currentPoint; // Current position of the dot
    private final BufferedImage offscreenImage; // Offscreen buffer for drawing
    private final ScheduledExecutorService scheduler; // Scheduler for changing dot color

    private int currentRandomValueIndex = 0; // Current random number index
    private Long currentRandomValue; // Current random number

    private static final Logger LOGGER = LoggerConfig.getLogger();

    private final JLabel statusLabel; // Status label to display loading status

    /**
     * Constructor accepting RandomNumberProvider and JLabel for status updates.
     *
     * @param randomNumberProvider Provider for random numbers
     * @param statusLabel          JLabel to display status messages
     */
    public DotDisplayController(RandomNumberProvider randomNumberProvider, JLabel statusLabel) {
        this.statusLabel = statusLabel;
        currentPoint = new Point(SIZE_WIDTH / 2, SIZE_HEIGHT / 2); // Starting point in the center
        setPreferredSize(new Dimension(SIZE_WIDTH + 300, SIZE_HEIGHT)); // Increase panel width for random number stack visualization
        setBackground(Color.WHITE); // White background for better visibility
        dots = Collections.synchronizedList(new ArrayList<>()); // Initialize synchronized list of dots
        usedRandomNumbers = new ArrayList<>(); // Initialize list of used random numbers
        this.randomNumberProvider = randomNumberProvider; // Assign random number provider
        errorMessage = null; // Initially no error

        // Initialize offscreen buffer
        offscreenImage = new BufferedImage(SIZE_WIDTH, SIZE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        scheduler = Executors.newScheduledThreadPool(1); // Initialize scheduler

        // Register as a listener to RandomNumberProvider
        this.randomNumberProvider.addDataLoadListener(this);
    }

    /**
     * Starts the dot movement using a Timer.
     */
    public void startDotMovement() {
        Timer timer = new Timer(TIMER_DELAY, e -> {
            if (errorMessage == null) {
                List<Dot> newDots = new ArrayList<>();

                for (int i = 0; i < DOTS_PER_UPDATE; i++) { // Loop to add dots
                    try {
                        currentRandomValueIndex++;

                        // Get the next random number in the specified range
                        long randomValue = randomNumberProvider.getNextRandomNumberInRange(MIN_RANDOM_VALUE, MAX_RANDOM_VALUE);

                        // Save the current random number and add it to the used numbers list
                        currentRandomValue = randomValue;
                        usedRandomNumbers.add(currentRandomValue);

                        // Calculate the new dot position based on the random number
                        currentPoint = calculateNewDotPosition(currentPoint, randomValue);

                        // Create a new dot
                        Dot newDot = new Dot(new Point(currentPoint));
                        newDots.add(newDot);
                    } catch (NoSuchElementException ex) {
                        if (errorMessage == null) {
                            errorMessage = ex.getMessage();
                            LOGGER.log(Level.WARNING, ERROR_NO_RANDOM_NUMBERS + ex.getMessage());
                            updateStatusLabel("Error: " + ex.getMessage());
                        }
                        ((Timer) e.getSource()).stop();
                        break; // Exit the loop
                    }
                }

                dots.addAll(newDots); // Add all new dots to the list
                drawDots(newDots, Color.RED); // Draw new dots in red
                repaint(); // Repaint the panel after adding new dots
                LOGGER.fine(String.format(LOG_DOTS_PROCESSED, newDots.size()));

                // Schedule to change the color of the dots to black after 1 second
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

    /**
     * Paints the panel.
     *
     * @param g The Graphics object for drawing.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // Call superclass method for basic painting
        g.drawImage(offscreenImage, 0, 0, null); // Draw the offscreen image

        // Display the random number index
        g.setColor(Color.BLUE);
        g.drawString("Random sample index: " + currentRandomValueIndex, 10, 20);

        // Display the current random number
        if (currentRandomValue != null) {
            g.setColor(Color.BLACK);
            g.drawString("Current random number: " + currentRandomValue, 10, 40);
        }

        // Display error message if present
        if (errorMessage != null) {
            g.setColor(Color.RED);
            g.drawString(errorMessage, 10, 60);
        }

        // Draw the stack of used random numbers to the right of the triangle
        drawRandomNumbersStack(g);
    }

    /**
     * Calculates the new position of the dot based on the random number.
     *
     * @param currentPoint Current position of the dot
     * @param randomValue  Random number determining the movement direction
     * @return New position of the dot
     */
    private Point calculateNewDotPosition(Point currentPoint, long randomValue) {
        long MinValue = MIN_RANDOM_VALUE;

        // Fixed vertices of the triangle
        Point A = new Point(SIZE_WIDTH / 2, 0); // Top vertex
        Point B = new Point(0, SIZE_HEIGHT); // Bottom-left vertex
        Point C = new Point(SIZE_WIDTH, SIZE_HEIGHT); // Bottom-right vertex

        long rangePart = (MAX_RANDOM_VALUE - MinValue) / 3; // Divide the range into three parts

        int x = currentPoint.x;
        int y = currentPoint.y;

        if (randomValue <= MinValue + rangePart) {
            // Move towards vertex A
            x = (x + A.x) / 2;
            y = (y + A.y) / 2;
        } else if (randomValue <= MinValue + 2 * rangePart) {
            // Move towards vertex B
            x = (x + B.x) / 2;
            y = (y + B.y) / 2;
        } else {
            // Move towards vertex C
            x = (x + C.x) / 2;
            y = (y + C.y) / 2;
        }

        return new Point(x, y); // Return new position
    }

    /**
     * Draws new dots on the offscreen buffer.
     *
     * @param newDots List of new dots to draw
     * @param color   Color to draw the dots
     */
    private void drawDots(List<Dot> newDots, Color color) {
        Graphics2D g2d = offscreenImage.createGraphics(); // Get graphics context from buffer
        g2d.setColor(color); // Set color for drawing
        for (Dot dot : newDots) {
            g2d.fillRect(dot.point().x, dot.point().y, DOT_SIZE, DOT_SIZE); // Draw the dot
        }
        g2d.dispose(); // Dispose graphics context
    }

    /**
     * Draws the stack of used random numbers to the right of the triangle.
     *
     * @param g The Graphics object for drawing.
     */
    private void drawRandomNumbersStack(Graphics g) {
        g.setColor(Color.BLACK);

        // Получить параметры визуализации из конфигурации
        int maxColumns = MAX_COLUMNS; // Всего 6 колонок
        int maxRowsPerColumn = SIZE_HEIGHT / ROW_HEIGHT;

        // Начальные позиции для рисования чисел
        int startX = SIZE_WIDTH + 20; // Начальная X позиция, справа от треугольника
        int startY = SIZE_HEIGHT - ROW_HEIGHT; // Начальная Y позиция, снизу панели

        // Списки для хранения чисел по разрядам
        List<List<Long>> digitBuckets = new ArrayList<>();
        for (int i = 0; i < maxColumns; i++) {
            digitBuckets.add(new ArrayList<>());
        }

        // Распределяем числа по разрядам
        for (Long randomValue : usedRandomNumbers) {
            int numDigits = String.valueOf(Math.abs(randomValue)).length(); // Количество разрядов в числе
            if (numDigits >= 3 && numDigits <= 8) { // Только числа от 3 до 8 разрядов
                digitBuckets.get(numDigits - 3).add(randomValue);
            }
        }

        // Рисуем числа по колонкам
        for (int column = 0; column < maxColumns; column++) {
            List<Long> columnNumbers = digitBuckets.get(column);
            int row = maxRowsPerColumn - 1; // Начинаем с нижней строки

            for (int i = columnNumbers.size() - 1; i >= 0; i--) { // Итерация в обратном порядке
                if (row < 0) {
                    break; // Если достигнут верх колонки, выходим
                }
                int x = startX + column * (COLUMN_WIDTH + COLUMN_SPACING); // X-координата
                int y = startY - row * ROW_HEIGHT; // Y-координата

                g.drawString(columnNumbers.get(i).toString(), x, y); // Рисуем число
                row--;
            }
        }
    }






    /**
     * Updates the status label with the given message.
     *
     * @param message The message to display.
     */
    private void updateStatusLabel(String message) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(message));
    }

    /**
     * Called when data loading starts.
     */
    @Override
    public void onLoadingStarted() {
        updateStatusLabel("Loading data...");
    }

    /**
     * Called when data loading completes successfully.
     */
    @Override
    public void onLoadingCompleted() {
        updateStatusLabel("Data loaded successfully.");
    }

    /**
     * Called when an error occurs during data loading.
     *
     * @param errorMessage The error message.
     */
    @Override
    public void onError(String errorMessage) {
        updateStatusLabel("Error: " + errorMessage);
    }
}
