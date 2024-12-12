package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;

public class App {
    // Константы для строковых значений
    private static final String APPLICATION_TITLE = "Dot Mover";
    private static final String LOG_APP_STARTED = "Приложение запущено.";
    private static final String LOG_GUI_STARTED = "GUI успешно запущен.";
    private static final String LOG_APP_SHUTTING_DOWN = "Завершение работы приложения.";

    private static final Logger LOGGER = LoggerConfig.getLogger();

    public static void main(String[] args) {
        // Инициализация логгирования
        LoggerConfig.initializeLogger();
        LOGGER.info(LOG_APP_STARTED);

        // Создание объектов
        RandomNumberProvider randomNumberProvider = new RandomNumberProvider();
        DotController dotController = new DotController(randomNumberProvider);

        // Запуск GUI
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(APPLICATION_TITLE); // Создание окна приложения
            frame.setLayout(new BorderLayout()); // Установка менеджера компоновки
            frame.add(dotController, BorderLayout.CENTER); // Добавление контроллера точек в центр
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

            // Получение размера панели из конфигурации
            int panelWidth = Config.getInt("panel.size.width");
            int panelHeight = Config.getInt("panel.size.height");
            frame.setSize(panelWidth, panelHeight); // Установка размера окна

            // Альтернативно, можно использовать максимальное развертывание, если это задано
            // frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

            // Запуск движения точек
            dotController.startDotMovement();
            frame.setVisible(true); // Отображение окна
            LOGGER.info(LOG_GUI_STARTED);

            // Добавление обработчика закрытия окна для корректного завершения ExecutorService
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                    LOGGER.info(LOG_APP_SHUTTING_DOWN);
                    randomNumberProvider.shutdown(); // Корректное завершение пула потоков
                    super.windowClosing(windowEvent);
                }
            });
        });
    }
}
