package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class DotDisplayController extends JPanel {
    private static final int SIZE_WIDTH = Config.getInt("panel.size.width");
    private static final int SIZE_HEIGHT = Config.getInt("panel.size.height");
    private static final int DOT_SIZE = Config.getInt("dot.size");
    private static final int TIMER_DELAY = Config.getInt("timer.delay");
    private static final Logger LOGGER = LoggerConfig.getLogger();

    private final List<Dot> dots = new ArrayList<>();
    private final BufferedImage offscreenImage;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final RandomNumberProvider randomNumberProvider;
    private final JLabel statusLabel;
    private Timer timer;

    public DotDisplayController(RandomNumberProvider randomNumberProvider, JLabel statusLabel) {
        this.randomNumberProvider = randomNumberProvider;
        this.statusLabel = statusLabel;
        setPreferredSize(new Dimension(SIZE_WIDTH, SIZE_HEIGHT));
        setBackground(Color.WHITE);
        offscreenImage = new BufferedImage(SIZE_WIDTH, SIZE_HEIGHT, BufferedImage.TYPE_INT_ARGB);

        randomNumberProvider.addDataLoadListener(new RandomNumberLoadHandler(statusLabel));
    }

    public void startDotMovement() {
        timer = new Timer(TIMER_DELAY, e -> updateDots());
        timer.start();
    }

    private void updateDots() {
        try {
            Dot newDot = new Dot(new Point((int) randomNumberProvider.getNextRandomNumberInRange(0, SIZE_WIDTH),
                    (int) randomNumberProvider.getNextRandomNumberInRange(0, SIZE_HEIGHT)));
            dots.add(newDot);
            drawDots(Color.RED);
            repaint();

            scheduler.schedule(() -> {
                drawDots(Color.BLACK);
                repaint();
            }, 1, TimeUnit.SECONDS);
        } catch (NoSuchElementException ex) {
            LOGGER.warning("Failed to update dots: " + ex.getMessage());
            SwingUtilities.invokeLater(() -> statusLabel.setText("Error: " + ex.getMessage()));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(offscreenImage, 0, 0, null);
    }

    private void drawDots(Color color) {
        Graphics2D g2d = offscreenImage.createGraphics();
        g2d.setColor(color);
        for (Dot dot : dots) {
            g2d.fillOval(dot.point().x, dot.point().y, DOT_SIZE, DOT_SIZE);
        }
        g2d.dispose();
    }

    // Call this method to properly shut down the scheduler and timer
    public void shutdown() {
        if (timer != null) {
            timer.stop(); // Stop the timer
        }
        scheduler.shutdownNow(); // Attempt to stop all actively executing tasks
        try {
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                scheduler.shutdownNow(); // Cancel currently executing tasks
                if (!scheduler.awaitTermination(1, TimeUnit.SECONDS))
                    LOGGER.severe("Scheduler did not terminate");
            }
        } catch (InterruptedException ie) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
