package org.ThreeDotsSierpinski;

import java.io.IOException;
import java.nio.file.*;
import java.util.logging.*;

public class LoggerConfig {
    private static final Logger LOGGER = Logger.getLogger(LoggerConfig.class.getName());
    private static final String LOG_FILE_NAME = "app.log";

    static {
        try {
            // Define the path to the log file
            Path logFilePath = Paths.get(LOG_FILE_NAME);

            // Delete the log file if it exists to ensure a fresh start
            Files.deleteIfExists(logFilePath);

            // Initialize FileHandler with append set tоo false to overwrite the log file
            FileHandler fileHandler = new FileHandler(LOG_FILE_NAME, false);

            // Set a simple formatter
            fileHandler.setFormatter(new SimpleFormatter());

            // Add the handler to the root logger
            Logger rootLogger = Logger.getLogger("");
            // Remove default handlers to prevent duplicate logging to console
            Handler[] handlers = rootLogger.getHandlers();
            for (Handler handler : handlers) {
                if (handler instanceof ConsoleHandler) {
                    rootLogger.removeHandler(handler);
                }
            }
            rootLogger.addHandler(fileHandler);

            // Set the desired log level
            rootLogger.setLevel(Level.ALL);

            LOGGER.info("Logging initialized successfully.");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize logger handler.", e);
        }
    }

    /**
     * Retrieves the global logger instance.
     *
     * @return The global Logger.
     */
    public static Logger getLogger() {
        return Logger.getLogger("");
    }
}
