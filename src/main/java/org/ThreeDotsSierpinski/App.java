package org.ThreeDotsSierpinski;

import javax.swing.*;

public class App {
    public static final String DOT_MOVER = "Dot Mover";
    public static final String DOT_MOVER_DOTS = "Dot Mover - Dots: ";
    public static final String RANDOM_VALUE_STRING = " (Random Value: ";
    public static final int DELAY = 5;

    public static void main(String[] args) {
        QuantumRandomNumberGeneratorService qrngService = new QuantumRandomNumberGeneratorService();
        DotMover dotMover = new DotMover(qrngService);

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(DOT_MOVER); // Window title "Dot Mover"
            DiceRoller diceRoller = new DiceRoller(qrngService);

            frame.add(dotMover);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

            Timer timer = new Timer(DELAY, e -> {
                dotMover.moveDot();
                int randomValue = diceRoller.rollDice(); // Get a random number value from DiceRoller
                frame.setTitle(DOT_MOVER_DOTS + dotMover.getDotCounter() + RANDOM_VALUE_STRING + randomValue + ")"); // Update the window title with the dot counter readings and the value of the random number
            });

            timer.start();
            frame.setVisible(true);
        });
    }
}
