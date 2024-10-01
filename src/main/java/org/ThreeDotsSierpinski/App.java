package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.awt.*;
import java.util.NoSuchElementException;

public class App {
    public static final String CLOSING_PARENTHESIS = ")";
    public static final String DOT_MOVER = "Dot Mover";
    public static final String DOT_MOVER_DOTS = "Dot Mover - Dots: ";
    public static final int DELAY = 1000; // Интервал между обновлениями (меньше для плавности)

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
                try {
                    // Двигаем точки
                    dotController.moveDot();

                    // Обновляем заголовок окна с информацией о количестве точек
                    frame.setTitle(String.format("%s%d%s", DOT_MOVER_DOTS, dotController.getDotCounter(), CLOSING_PARENTHESIS));
                } catch (NoSuchElementException ex) {
                    // Сообщение об ошибке в случае отсутствия доступных случайных чисел
                    JOptionPane.showMessageDialog(frame, "Нет доступных случайных чисел. Попробуйте позже.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                    ((Timer) e.getSource()).stop(); // Останавливаем таймер
                } catch (Exception ex) {
                    // Сообщение об ошибке в случае проблем с подключением к API
                    JOptionPane.showMessageDialog(frame, "Ошибка: Не удалось подключиться к провайдеру случайных чисел.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                    ((Timer) e.getSource()).stop(); // Останавливаем таймер
                }
            });

            // Запуск таймера
            timer.start();
            frame.setVisible(true);
        });
    }
}
