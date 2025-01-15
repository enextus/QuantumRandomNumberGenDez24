package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;

/**
 * The main application class that sets up the GUI and initializes components.
 */
public class App {
    // Constants for string values
    private static final String APPLICATION_TITLE = "Dot Mover";
    private static final String LOG_APP_STARTED = "Application started.";
    private static final String LOG_GUI_STARTED = "GUI successfully launched.";
    private static final String LOG_APP_SHUTTING_DOWN = "Shutting down application.";

    private static final Logger LOGGER = LoggerConfig.getLogger();

    public static void main(String[] args) {
        // Initialize logging
        LoggerConfig.initializeLogger();
        LOGGER.info(LOG_APP_STARTED);

        // Create objects
        RandomNumberProvider randomNumberProvider = new RandomNumberProvider();
        JLabel statusLabel = new JLabel("Ready"); // Create status label

        // Launch GUI
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(APPLICATION_TITLE); // Create application window
            frame.setLayout(new BorderLayout()); // Set layout manager

            // Get panel size and window scaling factors from configuration
            int basePanelWidth = Config.getInt("panel.size.width");
            int basePanelHeight = Config.getInt("panel.size.height");
            double scaleWidth = Config.getDouble("window.scale.width");
            double scaleHeight = Config.getDouble("window.scale.height");

            // Calculate final window size based on scaling factors
            int finalWidth = (int) Math.round(basePanelWidth * scaleWidth);
            int finalHeight = (int) Math.round(basePanelHeight * scaleHeight);

            // Create DotController with reference to statusLabel
            DotDisplayController dotController = new DotDisplayController(randomNumberProvider, statusLabel);

            frame.add(dotController, BorderLayout.CENTER); // Add DotController to center

            // Create and add status panel to the bottom of the window
            JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            statusPanel.add(statusLabel);
            frame.add(statusPanel, BorderLayout.SOUTH);

            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setSize(finalWidth, finalHeight); // Set window size
            frame.setLocationRelativeTo(null); // Center the window on the screen

            // Start dot movement
            dotController.startDotMovement();
            frame.setVisible(true); // Display the window
            LOGGER.info(LOG_GUI_STARTED);

            // Add window listener to handle shutdown
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                    LOGGER.info(LOG_APP_SHUTTING_DOWN);
                    randomNumberProvider.shutdown(); // Gracefully shutdown ExecutorService
                    super.windowClosing(windowEvent);
                }
            });
        });
    }
}
