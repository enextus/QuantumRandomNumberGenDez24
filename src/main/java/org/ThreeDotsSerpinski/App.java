package org.ThreeDotsSerpinski;

import javax.swing.*;

public class App {

    public static final String DOT_MOVER = "Dot Mover";

    public static void main(String[] args) {
        JFrame frame = new JFrame(DOT_MOVER);
        DotMover dotMover = new DotMover();
        frame.add(dotMover);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        new Timer(1, e -> dotMover.moveDot()).start();
    }

}
