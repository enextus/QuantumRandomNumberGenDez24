package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.util.NoSuchElementException;

public class App {
    public static final String CLOSING_PARENTHESIS = ")";
    public static final String DOT_MOVER = "Dot Mover";
    public static final String DOT_MOVER_DOTS = "Dot Mover - Dots: ";
    public static final String RANDOM_VALUE_STRING = "Random Value: ";
    public static final int DELAY = 60000; // 60 секунд

    public static void main(String[] args) {
        DotController dotController = new DotController();
        RandomNumberProvider randomNumberProvider = new RandomNumberProvider();
        NumberDisplayWindow displayWindow = new NumberDisplayWindow();
        displayWindow.setVisible(true);

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(DOT_MOVER);
            frame.add(dotController);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            Timer timer = new Timer(DELAY, e -> {
                try {
                    dotController.moveDot();
                    // Получаем случайное число из RandomNumberProvider
                    int randomValue = randomNumberProvider.getNextRandomNumber();
                    // Обновляем заголовок окна с счетчиком точек и значением случайного числа
                    frame.setTitle(String.format("%s%d%s%d%s", DOT_MOVER_DOTS, dotController.getDotCounter(), RANDOM_VALUE_STRING, randomValue, CLOSING_PARENTHESIS));
                    // Отображаем случайное число во втором окне
                    displayWindow.addNumber(randomValue);
                } catch (NoSuchElementException ex) {
                    JOptionPane.showMessageDialog(frame, "Нет доступных случайных чисел. Попробуйте позже.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                    ((Timer) e.getSource()).stop(); // Останавливаем таймер
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Ошибка: Не удалось подключиться к провайдеру случайных чисел.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                    ((Timer) e.getSource()).stop(); // Останавливаем таймер
                }
            });
            timer.start();
            frame.setVisible(true);
        });
    }
}
