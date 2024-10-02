package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;

public class App {
    private static final String CLOSING_PARENTHESIS = ")";
    private static final String DOT_MOVER = "Dot Mover";
    private static final String DOT_MOVER_DOTS = "Dot Mover - Dots: ";
    private static final int DELAY = 1000; // Interval between updates in milliseconds

    private static final Logger LOGGER = LoggerConfig.getLogger();

    public static void main(String[] args) {
        LOGGER.info("Application started.");

        // Creating objects
        RandomNumberProvider randomNumberProvider = new RandomNumberProvider();
        DotController dotController = new DotController(randomNumberProvider);

        // Launching the GUI
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(DOT_MOVER); // Creating the application window
            frame.setLayout(new BorderLayout()); // Setting the layout manager
            frame.add(dotController, BorderLayout.CENTER); // Adding the dot controller to the center
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Setting the default close operation
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH); // Maximizing the window to full screen

            // Timer for updating dots
            Timer timer = new Timer(DELAY, e -> {
                // Checking if there is no error message
                if (dotController.getErrorMessage() == null) {
                    dotController.moveDot(); // Moving the dots
                    frame.setTitle(String.format("%s%d%s", DOT_MOVER_DOTS, dotController.getDotCounter(), CLOSING_PARENTHESIS));
                    LOGGER.fine("Dots updated. Current count: " + dotController.getDotCounter());
                } else {
                    // Stopping the timer
                    ((Timer) e.getSource()).stop();
                    // Logging the error
                    LOGGER.severe("Error encountered: " + dotController.getErrorMessage());
                    // Displaying an error message to the user
                    JOptionPane.showMessageDialog(frame, "Failed to export dots to file.",
                            "Export Error", JOptionPane.ERROR_MESSAGE);

                    // Exporting dots to a file
                    try {
                        dotController.exportDotsToFile("dots.txt");
                        LOGGER.info("Dots successfully exported to dots.txt.");
                    } catch (IOException ex) {
                        LOGGER.log(Level.SEVERE, "Failed to export dots to file.", ex);
                        JOptionPane.showMessageDialog(frame, "Failed to export dots to file.",
                                "Export Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            // Starting the timer
            timer.start();
            frame.setVisible(true); // Making the window visible
            LOGGER.info("GUI launched successfully.");
        });
    }
}
