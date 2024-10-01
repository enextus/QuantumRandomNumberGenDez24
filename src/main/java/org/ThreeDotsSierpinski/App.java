package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.awt.*;

/**
 * Класс приложения для визуализации фрактала Серпинского с использованием случайных чисел.
 *
 * The App class for visualizing the Sierpinski fractal using random numbers.
 */
public class App {
    public static final String CLOSING_PARENTHESIS = ")"; // Закрывающая скобка
    public static final String DOT_MOVER = "Dot Mover"; // Название приложения
    public static final String DOT_MOVER_DOTS = "Dot Mover - Dots: "; // Заголовок с количеством точек
    public static final int DELAY = 1000; // Интервал между обновлениями в миллисекундах
    // Interval between updates in milliseconds

    /**
     * Главный метод приложения.
     *
     * The main method of the application.
     *
     * @param args Аргументы командной строки
     *             Command-line arguments
     */
    public static void main(String[] args) {
        // Создание объектов
        // Creating objects
        RandomNumberProvider randomNumberProvider = new RandomNumberProvider();
        DotController dotController = new DotController(randomNumberProvider);

        // Запуск GUI
        // Launching the GUI
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(DOT_MOVER); // Создание окна приложения
            // Creating the application window
            frame.setLayout(new BorderLayout()); // Установка менеджера компоновки
            // Setting the layout manager
            frame.add(dotController, BorderLayout.CENTER); // Добавление контроллера точек в центр
            // Adding the dot controller to the center
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Настройка действия при закрытии окна
            // Setting the default close operation
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH); // Развертывание окна на весь экран
            // Maximizing the window to full screen

            // Таймер для обновления точек
            // Timer for updating dots
            Timer timer = new Timer(DELAY, e -> {
                // Проверяем, нет ли сообщения об ошибке
                // Checking if there is no error message
                if (dotController.getErrorMessage() == null) {
                    dotController.moveDot(); // Перемещение точек
                    // Moving the dots
                    frame.setTitle(String.format("%s%d%s", DOT_MOVER_DOTS, dotController.getDotCounter(), CLOSING_PARENTHESIS));
                    // Updating the window title with the number of dots
                } else {
                    // Останавливаем таймер
                    // Stopping the timer
                    ((Timer) e.getSource()).stop();
                    // Отображаем сообщение об ошибке пользователю
                    // Displaying an error message to the user
                    JOptionPane.showMessageDialog(frame, dotController.getErrorMessage(),
                            "Ошибка", JOptionPane.ERROR_MESSAGE);
                    // JOptionPane.showMessageDialog(frame, dotController.getErrorMessage(),
                    //         "Error", JOptionPane.ERROR_MESSAGE);
                }
            });

            // Запуск таймера
            // Starting the timer
            timer.start();
            frame.setVisible(true); // Отображение окна
            // Making the window visible
        });
    }
}
