package org.ThreeDotsSerpinski;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Dot Mover");
        DotMover dotMover = new DotMover();
        frame.add(dotMover);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        new Timer(100, e -> dotMover.moveDot()).start(); // двигаем точку каждые 100 миллисекунд
    }

}
