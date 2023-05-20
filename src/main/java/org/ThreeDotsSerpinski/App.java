package org.ThreeDotsSerpinski;

import javax.swing.*;

public class App {

    public static final String DOT_MOVER = "Dot Mover";

    public static void main(String[] args) {
        JFrame frame = new JFrame("Dot Mover"); // Заголовок окна "Dot Mover"
        DotMover dotMover = new DotMover();
        frame.add(dotMover);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Timer timer = new Timer(75, e -> {
            dotMover.moveDot();
            frame.setTitle("Dot Mover - Dots: " + dotMover.getDotCounter()); // Обновление заголовка окна с показаниями счетчика точек
        });
        timer.start();

        frame.setVisible(true);
    }


}

