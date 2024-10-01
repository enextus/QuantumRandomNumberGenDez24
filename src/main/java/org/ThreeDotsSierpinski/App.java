package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.awt.*;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;

public class App {
    public static final String CLOSING_PARENTHESIS = ")";
    public static final String DOT_MOVER = "Dot Mover";
    public static final String DOT_MOVER_DOTS = "Dot Mover - Dots: ";
    public static final int DELAY = 1000; // Интервал между обновлениями (в миллисекундах)

    // Флаг, указывающий, выполняется ли сейчас метод moveDot()
    private static final AtomicBoolean isMoving = new AtomicBoolean(false);

    public static void main(String[] args) {
        // Создание объектов для управления точками и случайными числами
        RandomNumberProvider randomNumberProvider = new RandomNumberProvider();
        DotController dotController = new DotController(randomNumberProvider);

        // Запуск GUI в отдельном потоке
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(DOT_MOVER);
            frame.setLayout(new BorderLayout());
            frame.add(dotController, BorderLayout.CENTER);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

            // Таймер, запускающий обновление позиции точки
            Timer timer = new Timer(DELAY, e -> {
                // Проверяем, не выполняется ли сейчас moveDot() и нет ли сообщения об ошибке
                if (!isMoving.get() && dotController.getErrorMessage() == null) {
                    isMoving.set(true); // Устанавливаем флаг, что moveDot() выполняется
                    dotController.moveDot(() -> isMoving.set(false)); // Передаем обратный вызов для сброса флага
                    frame.setTitle(String.format("%s%d%s", DOT_MOVER_DOTS, dotController.getDotCounter(), CLOSING_PARENTHESIS));
                } else if (dotController.getErrorMessage() != null) {
                    // Останавливаем таймер, так как достигнут лимит запросов или произошла ошибка
                    ((Timer) e.getSource()).stop();
                    JOptionPane.showMessageDialog(frame, dotController.getErrorMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            });

            // Запуск таймера
            timer.start();
            frame.setVisible(true);
        });
    }
}
