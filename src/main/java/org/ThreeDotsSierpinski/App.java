package org.ThreeDotsSierpinski;

import javax.swing.*;

public class App {
    public static final String DOT_MOVER = "Dot Mover";
    public static final String DOT_MOVER_DOTS = "Dot Mover - Dots: ";
    public static final String RANDOM_VALUE_STRING = " (Random Value: ";
    public static final int DELAY = 16;

    public static void main(String[] args) {
        RndNumGeneratorService qrngService = new RndNumGeneratorService();
        DotController dotController = new DotController(qrngService);

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(DOT_MOVER); // Window title "Dot Mover"
            RndNumProvider rndNumProvider = new RndNumProvider(qrngService);

            frame.add(dotController);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

            Timer timer = new Timer(DELAY, e -> {
                dotController.moveDot();
                int randomValue = rndNumProvider.rollDice(); // Get a random number value from RndNumProvider
                frame.setTitle(DOT_MOVER_DOTS + dotController.getDotCounter() + RANDOM_VALUE_STRING + randomValue + ")"); // Update the window title with the dot counter readings and the value of the random number
            });

            timer.start();
            frame.setVisible(true);
        });
    }

}
