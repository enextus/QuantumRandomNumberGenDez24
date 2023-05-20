/*package org.ThreeDotsSerpinski;

import javax.swing.*;

public class App {
    public static final String DOT_MOVER = "Dot Mover";
    public static final String DOT_MOVER_DOTS = "Dot Mover - Dots: ";

    public static void main(String[] args) {
        JFrame frame = new JFrame(DOT_MOVER); // Заголовок окна "Dot Mover"
        DotMover dotMover = new DotMover();
        frame.add(dotMover);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Timer timer = new Timer(75, e -> {
            dotMover.moveDot();
            frame.setTitle(DOT_MOVER_DOTS + dotMover.getDotCounter()); // Обновление заголовка окна с показаниями счетчика точек
        });
        timer.start();

        frame.setVisible(true);
    }

}*/
package org.ThreeDotsSerpinski;

import javax.swing.*;

public class App {
    public static final String DOT_MOVER = "Dot Mover";
    public static final String DOT_MOVER_DOTS = "Dot Mover - Dots: ";
    public static final int DELAY = 75;
    public static final String RANDOM_VALUE_STRING = " (Random Value: ";

    public static void main(String[] args) {
        JFrame frame = new JFrame(DOT_MOVER); // Заголовок окна "Dot Mover"
        DiceRoller diceRoller = new DiceRoller();
        DotMover dotMover = new DotMover(diceRoller);
        frame.add(dotMover);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Timer timer = new Timer(DELAY, e -> {
            dotMover.moveDot();
            int randomValue = diceRoller.rollDice(); // Получение значения случайного числа из DiceRoller
            frame.setTitle(DOT_MOVER_DOTS + dotMover.getDotCounter() + RANDOM_VALUE_STRING + randomValue + ")"); // Обновление заголовка окна с показаниями счетчика точек и значением случайного числа
        });
        timer.start();

        frame.setVisible(true);
    }
}
