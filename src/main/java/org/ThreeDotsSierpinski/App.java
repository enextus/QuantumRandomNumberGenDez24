package org.ThreeDotsSierpinski;

import javax.swing.*;

public class App {
    public static final String CLOSING_PARENTHESIS = ")";
    public static final String DOT_MOVER = "Dot Mover";
    public static final String DOT_MOVER_DOTS = "Dot Mover - Dots: ";
    public static final String RANDOM_VALUE_STRING = "Random Value: ";
    public static final int DELAY = 0; // 5000 for slow

    public static void main(String[] args) {
        DotController dotController = new DotController();
        RandomNumberProvider randomNumberProvider = new RandomNumberProvider();
        NumberDisplayWindow displayWindow = new NumberDisplayWindow();
        displayWindow.setVisible(true);

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(DOT_MOVER);
            frame.add(dotController);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            Timer timer = new Timer(DELAY, e -> {
                dotController.moveDot();
                // Get a random number value from RandomNumberProvider
                int randomValue = randomNumberProvider.getNextRandomNumber();
                // Update the window title with the dot counter readings and the value of the random number
                frame.setTitle(String.format("%s%d%s%d%s", DOT_MOVER_DOTS, dotController.getDotCounter(), RANDOM_VALUE_STRING, randomValue, CLOSING_PARENTHESIS));
                // Display the random number in the second window
                displayWindow.addNumber(randomValue);
            });
            timer.start();
            frame.setVisible(true);
        });
    }
}
