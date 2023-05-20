package org.ThreeDotsSerpinski;

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

        Timer timer = new Timer(25, e -> {
            dotMover.moveDot();
            frame.setTitle(DOT_MOVER_DOTS + dotMover.getDotCounter()); // Обновление заголовка окна с показаниями счетчика точек
        });
        timer.start();

        frame.setVisible(true);
    }

}
