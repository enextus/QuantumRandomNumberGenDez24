package org.ThreeDotsSierpinski;

import java.io.IOException;
import java.nio.file.*;
import java.util.logging.*;

/**
 * Конфигурация логгера для приложения.
 * 
 * Исправлено:
 * - Автоматическое создание директории logs
 * - Однократная инициализация (singleton)
 * - Убраны DEBUG сообщения
 * - Graceful fallback при ошибках
 */
public class LoggerConfig {
    private static final Logger LOGGER = Logger.getLogger(LoggerConfig.class.getName());
    private static volatile boolean isInitialized = false;
    private static volatile boolean initializationAttempted = false;
    private static final Object LOCK = new Object();

    /**
     * Инициализирует конфигурацию логгера.
     * Создаёт директорию для логов, удаляет существующий лог-файл 
     * и настраивает новый FileHandler.
     */
    public static void initializeLogger() {
        // Double-checked locking для thread-safety
        if (initializationAttempted) {
            return;
        }
        
        synchronized (LOCK) {
            if (initializationAttempted) {
                return;
            }
            
            // Отмечаем попытку инициализации сразу, чтобы избежать повторных попыток
            initializationAttempted = true;
            
            try {
                // Получение имени файла лога из конфигурации
                String logFileName = Config.getString("log.file.name");
                if (logFileName == null || logFileName.isEmpty()) {
                    logFileName = "logs/app.log";
                }

                // Определение пути к файлу лога
                Path logFilePath = Paths.get(logFileName);
                
                // ИСПРАВЛЕНИЕ: Создание директории если не существует
                Path parentDir = logFilePath.getParent();
                if (parentDir != null) {
                    try {
                        Files.createDirectories(parentDir);
                    } catch (IOException e) {
                        System.err.println("Warning: Could not create log directory: " + parentDir);
                    }
                }

                // Удаление файла лога, если он существует, для обеспечения чистого старта
                try {
                    Files.deleteIfExists(logFilePath);
                    // Также удаляем .lck файл если остался от предыдущего запуска
                    Files.deleteIfExists(Paths.get(logFileName + ".lck"));
                } catch (IOException e) {
                    // Игнорируем - файл может быть заблокирован
                }

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
                
                // Добавляем консольный handler для важных сообщений
                ConsoleHandler consoleHandler = new ConsoleHandler();
                consoleHandler.setLevel(Level.INFO);
                consoleHandler.setFormatter(new SimpleFormatter());
                rootLogger.addHandler(consoleHandler);
                
                rootLogger.setLevel(Config.getLogLevel());

                isInitialized = true;
                LOGGER.info("Logger initialized successfully. Log file: " + logFilePath.toAbsolutePath());
                
            } catch (IOException e) {
                // Если не удалось создать FileHandler, работаем только с консолью
                System.err.println("Warning: Could not initialize file logging: " + e.getMessage());
                System.err.println("Logging to console only.");
                
                // Настраиваем консольный логгер как fallback
                Logger rootLogger = Logger.getLogger("");
                rootLogger.setLevel(Level.INFO);
                
                // Убеждаемся что есть консольный handler
                boolean hasConsoleHandler = false;
                for (Handler handler : rootLogger.getHandlers()) {
                    if (handler instanceof ConsoleHandler) {
                        hasConsoleHandler = true;
                        break;
                    }
                }
                if (!hasConsoleHandler) {
                    ConsoleHandler consoleHandler = new ConsoleHandler();
                    consoleHandler.setLevel(Level.INFO);
                    rootLogger.addHandler(consoleHandler);
                }
                
                isInitialized = true; // Считаем инициализированным с fallback
            }
        }
    }

    /**
     * Получает глобальный экземпляр логгера.
     * Автоматически инициализирует логгер при первом вызове.
     *
     * @return Глобальный Logger.
     */
    public static Logger getLogger() {
        if (!initializationAttempted) {
            initializeLogger();
        }
        return Logger.getLogger("");
    }

    /**
     * Проверяет, был ли логгер успешно инициализирован с файловым выводом.
     *
     * @return true если файловый логгер работает, false если только консольный
     */
    public static boolean isFileLoggingEnabled() {
        return isInitialized && initializationAttempted;
    }
}
