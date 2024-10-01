package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.awt.*;

public class App {
    public static final String CLOSING_PARENTHESIS = ")";
    public static final String DOT_MOVER = "Dot Mover";
    public static final String DOT_MOVER_DOTS = "Dot Mover - Dots: ";
    public static final int DELAY = 1000; // Интервал между обновлениями в миллисекундах

    public static void main(String[] args) {
        // Создание объектов
        RandomNumberProvider randomNumberProvider = new RandomNumberProvider();
        DotController dotController = new DotController(randomNumberProvider);

        // Запуск GUI
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(DOT_MOVER);
            frame.setLayout(new BorderLayout());
            frame.add(dotController, BorderLayout.CENTER);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

            // Таймер для обновления точек
            Timer timer = new Timer(DELAY, e -> {
                // Проверяем, нет ли сообщения об ошибке
                if (dotController.getErrorMessage() == null) {
                    dotController.moveDot();
                    frame.setTitle(String.format("%s%d%s", DOT_MOVER_DOTS, dotController.getDotCounter(), CLOSING_PARENTHESIS));
                } else {
                    // Останавливаем таймер
                    ((Timer) e.getSource()).stop();
                    // Отображаем сообщение об ошибке пользователю
                    JOptionPane.showMessageDialog(frame, dotController.getErrorMessage(),
                            "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            });

            // Запуск таймера
            timer.start();
            frame.setVisible(true);
        });
    }
}
