package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The controller for managing and displaying points of the Sierpinski fractal.
 */
public class DotController extends JPanel {
    private static final int SIZE = 900; // Panel size
    private static final int DOT_SIZE = 2; // Dot size
    private final List<Dot> dots; // List of dots
    private final RandomNumberProvider randomNumberProvider; // Random number provider
    private int dotCounter; // Dot counter
    private String errorMessage; // Error message
    private Point currentPoint; // Current point position
    private final BufferedImage offscreenImage; // Offscreen image buffer for drawing

    private static final Logger LOGGER = LoggerUtility.getLogger();

    /**
     * Constructor that takes a RandomNumberProvider.
     *
     * @param randomNumberProvider Random number provider
     */
    public DotController(RandomNumberProvider randomNumberProvider) {
        currentPoint = new Point(SIZE / 2, SIZE / 2); // Initial point in the center
        setPreferredSize(new Dimension(SIZE, SIZE)); // Setting the panel size
        setBackground(Color.WHITE); // White background for better visibility
        dots = new CopyOnWriteArrayList<>(); // Initializing a thread-safe list of dots
        this.randomNumberProvider = randomNumberProvider; // Assigning the random number provider
        dotCounter = 0; // Initializing the dot counter
        errorMessage = null; // Initially, there is no error

        // Initializing the offscreen image buffer
        offscreenImage = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
    }

    /**
     * Gets the error message.
     *
     * @return The error message or null if there is no error.
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Moves the dot to a new position.
     * Starts a background task to update the dot's position and add new dots.
     */
    public void moveDot() {
        // If there is already an error message, do not continue
        if (errorMessage != null) {
            return;
        }

        new SwingWorker<Void, Dot>() {
            @Override
            protected Void doInBackground() {
                long MinValue = -99999999L; // Minimum value for the random number range
                long MaxValue = 100000000L; // Maximum value for the random number range

                for (int i = 0; i < 10000; i++) { // Loop to add 10,000 dots
                    try {
                        // Getting the next random number in the specified range
                        long randomValue = randomNumberProvider.getNextRandomNumberInRange(MinValue, MaxValue);
                        // Calculating the new dot position based on the random number
                        currentPoint = calculateNewDotPosition(currentPoint, randomValue);
                        // Creating a new dot
                        Dot newDot = new Dot(new Point(currentPoint));
                        dotCounter++; // Incrementing the dot counter
                        publish(newDot); // Publishing the dot for drawing
                    } catch (NoSuchElementException e) {
                        // Setting the error message only once
                        if (errorMessage == null) {
                            errorMessage = e.getMessage();
                            LOGGER.log(Level.WARNING, "No more random numbers available: " + e.getMessage());
                        }
                        break; // Exiting the loop
                    }
                }
                return null;
            }

            @Override
            protected void process(List<Dot> chunks) {
                dots.addAll(chunks); // Adding new dots to the list
                drawDots(chunks); // Drawing new dots on the buffer
                repaint(); // Repainting the panel
                LOGGER.fine("Processed " + chunks.size() + " new dots.");
            }

            @Override
            protected void done() {
                if (errorMessage != null) {
                    repaint(); // Repainting the panel to display the error message
                    LOGGER.severe("Error encountered during dot movement: " + errorMessage);
                }
            }
        }.execute(); // Starting the SwingWorker
    }

    /**
     * Calculates the new dot position based on a random number.
     *
     * @param currentPoint Current point position
     * @param randomValue  Random number to determine the movement direction
     * @return New dot position
     */
    private Point calculateNewDotPosition(Point currentPoint, long randomValue) {
        long MinValue = -99999999L; // Minimum range value
        long MaxValue = 100000000L; // Maximum range value

        // Fixed vertices of the triangle
        Point A = new Point(SIZE / 2, 0); // Top vertex
        Point B = new Point(0, SIZE); // Bottom-left vertex
        Point C = new Point(SIZE, SIZE); // Bottom-right vertex

        long rangePart = (MaxValue - MinValue) / 3; // Dividing the range into three parts

        int x = currentPoint.x;
        int y = currentPoint.y;

        if (randomValue <= MinValue + rangePart) {
            // Moving towards point A
            x = (x + A.x) / 2;
            y = (y + A.y) / 2;
        } else if (randomValue <= MinValue + 2 * rangePart) {
            // Moving towards point B
            x = (x + B.x) / 2;
            y = (y + B.y) / 2;
        } else {
            // Moving towards point C
            x = (x + C.x) / 2;
            y = (y + C.y) / 2;
        }

        return new Point(x, y); // Returning the new dot position
    }

    /**
     * Draws new dots on the buffer.
     *
     * @param newDots List of new dots to draw
     */
    private void drawDots(List<Dot> newDots) {
        Graphics2D g2d = offscreenImage.createGraphics(); // Obtaining the graphics context of the buffer
        g2d.setColor(Color.BLACK); // Setting the color for drawing dots
        for (Dot dot : newDots) {
            g2d.fillRect(dot.point().x, dot.point().y, DOT_SIZE, DOT_SIZE); // Drawing the dot
        }
        g2d.dispose(); // Disposing of the graphics context
    }

    /**
     * Paints the panel.
     *
     * @param g Graphics object for drawing
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // Calling the superclass method for basic painting
        g.drawImage(offscreenImage, 0, 0, null); // Drawing the offscreen image
        if (errorMessage != null) {
            g.setColor(Color.RED); // Setting red color for the error text
            g.drawString(errorMessage, 10, 20); // Drawing the error message
        }
    }

    /**
     * Gets the number of created dots.
     *
     * @return Number of dots
     */
    public int getDotCounter() {
        return dotCounter;
    }

    /**
     * Exports the list of dots to a specified text file.
     *
     * @param filename The name of the file to export the dots to.
     * @throws IOException If an I/O error occurs.
     */
    public void exportDotsToFile(String filename) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (Dot dot : dots) {
                Point p = dot.point();
                writer.write(p.x + "," + p.y);
                writer.newLine();
            }
            LOGGER.info("Dots have been successfully exported to " + filename);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to export dots to file: " + filename, e);
            throw e; // Rethrow to allow higher-level handling
        }
    }
}
