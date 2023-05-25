package org.ThreeDotsSierpinski;

import javax.swing.*;

public class App {
    public static final String DOT_MOVER = "Dot Mover";
    public static final String DOT_MOVER_DOTS = "Dot Mover - Dots: ";
    public static final String RANDOM_VALUE_STRING = " (Random Value: ";
    public static final int DELAY = 1; // fast
//    public static final int DELAY = 1000; // norm
//    public static final int DELAY = 5000; // slow

    public static void main(String[] args) {

        RandomNumberService RandomNumberService = new RandomNumberService();
        DotController dotController = new DotController(RandomNumberService);

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(DOT_MOVER);
            RandomNumberProvider randomNumberProvider = new RandomNumberProvider(RandomNumberService);

            frame.add(dotController);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

            Timer timer = new Timer(DELAY, e -> {
                dotController.moveDot();
                int randomValue = randomNumberProvider.getNextRandomNumber(); // Get a random number value from RandomNumberProvider
                frame.setTitle(DOT_MOVER_DOTS + dotController.getDotCounter() + RANDOM_VALUE_STRING + randomValue + ")"); // Update the window title with the dot counter readings and the value of the random number
            });

            timer.start();
            frame.setVisible(true);
        });
    }

}
