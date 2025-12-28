package org.ThreeDotsSierpinski;

import java.io.IOException;
import java.nio.file.*;
import java.util.logging.*;

/**
 * Конфигурация логгера для приложения.
 */
public class LoggerConfig {
    private static final Logger LOGGER = Logger.getLogger(LoggerConfig.class.getName());
    private static boolean isInitialized = false;

    /**
     * Инициализирует конфигурацию логгера.
     * Удаляет существующий лог-файл и настраивает новый FileHandler.
     */
    public static synchronized void initializeLogger() {
        if (isInitialized) {
            return; // Предотвращает повторную инициализацию
        }

        try {
            // Получение имени файла лога из конфигурации
            String logFileName = Config.getString("log.file.name");

            System.out.println("DEBUG Config resource = " +
                    LoggerConfig.class.getClassLoader().getResource("config.properties"));

            System.out.println("DEBUG log.file = " + Config.getString("log.file"));
            System.out.println("DEBUG log.path = " + Config.getString("log.path"));
            System.out.println("DEBUG logging.file = " + Config.getString("logging.file"));
            System.out.println("DEBUG logging.path = " + Config.getString("logging.path"));
            System.out.println("DEBUG user.dir = " + System.getProperty("user.dir"));



            // Определение пути к файлу лога
            Path logFilePath = Paths.get(logFileName);

            // Удаление файла лога, если он существует, для обеспечения чистого старта
            Files.deleteIfExists(logFilePath);

            // Инициализация FileHandler с append=false для перезаписи файла лога
            FileHandler fileHandler = new FileHandler(logFileName, false);
            fileHandler.setFormatter(new SimpleFormatter());

            // Получение корневого логгера
            Logger rootLogger = Logger.getLogger("");
            // Удаление стандартных консольных обработчиков для предотвращения дублирования логов
            Handler[] handlers = rootLogger.getHandlers();
            for (Handler handler : handlers) {
                if (handler instanceof ConsoleHandler) {
                    rootLogger.removeHandler(handler);
                }
            }

            // Добавление FileHandler к корневому логгеру
            rootLogger.addHandler(fileHandler);
            rootLogger.setLevel(Config.getLogLevel()); // Установка уровня логирования из конфигурации

            LOGGER.info("Логгирование успешно инициализировано.");
            isInitialized = true;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Не удалось инициализировать FileHandler для логгера.", e);
        }
    }

    /**
     * Получает глобальный экземпляр логгера.
     *
     * @return Глобальный Logger.
     */
    public static Logger getLogger() {
        if (!isInitialized) {
            initializeLogger();
        }
        return Logger.getLogger("");
    }

}
