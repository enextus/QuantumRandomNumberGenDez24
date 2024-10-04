package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;

public class App {
    private static final String DOT_MOVER = "Dot Mover";
    private static final int DELAY = 100; // Интервал между обновлениями в миллисекундах

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

            // Запуск движения точек
            dotController.startDotMovement();
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
