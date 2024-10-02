package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;

public class App {
    private static final String DOT_MOVER = "Dot Mover";
    private static final String DOT_MOVER_DOTS = "Dots: ";
    private static final int DELAY = 1000; // Интервал между обновлениями в миллисекундах

    private static final Logger LOGGER = LoggerConfig.getLogger();

    public static void main(String[] args) {
        // Инициализация логгирования
        LoggerConfig.initializeLogger();
        LOGGER.info("Приложение запущено.");

        // Создание объектов
        RandomNumberProvider randomNumberProvider = new RandomNumberProvider();
        DotController dotController = new DotController(randomNumberProvider);

        // Запуск GUI
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(DOT_MOVER); // Создание окна приложения
            frame.setLayout(new BorderLayout()); // Установка менеджера компоновки
            frame.add(dotController, BorderLayout.CENTER); // Добавление контроллера точек в центр
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Установка операции закрытия по умолчанию
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH); // Развертывание окна на весь экран

            // Таймер для обновления точек
            Timer timer = new Timer(DELAY, e -> {
                // Проверка отсутствия сообщения об ошибке
                if (dotController.getErrorMessage() == null) {
                    dotController.moveDot(); // Перемещение точек
                    frame.setTitle(String.format("%s%d", DOT_MOVER_DOTS, dotController.getDotCounter()));
                    LOGGER.fine("Точки обновлены. Текущее количество: " + dotController.getDotCounter());
                } else {
                    // Остановка таймера
                    ((Timer) e.getSource()).stop();
                    // Логгирование ошибки
                    LOGGER.severe("Обнаружена ошибка: " + dotController.getErrorMessage());
                    // Отображение сообщения об ошибке пользователю
                    JOptionPane.showMessageDialog(frame, "Не удалось экспортировать точки в файл.",
                            "Ошибка экспорта", JOptionPane.ERROR_MESSAGE);

                    // Экспорт точек в файл
                    try {
                        dotController.exportDotsToFile("dots.txt");
                        LOGGER.info("Точки успешно экспортированы в dots.txt.");
                    } catch (IOException ex) {
                        LOGGER.log(Level.SEVERE, "Не удалось экспортировать точки в файл.", ex);
                        JOptionPane.showMessageDialog(frame, "Не удалось экспортировать точки в файл.",
                                "Ошибка экспорта", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            // Запуск таймера
            timer.start();
            frame.setVisible(true); // Отображение окна
            LOGGER.info("GUI успешно запущен.");

            // Добавление обработчика закрытия окна для корректного завершения ExecutorService
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                    LOGGER.info("Завершение работы приложения.");
                    randomNumberProvider.shutdown(); // Корректное завершение пула потоков
                    super.windowClosing(windowEvent);
                }
            });
        });
    }
}
