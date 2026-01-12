package org.ThreeDotsSierpinski;

import java.io.IOException;
import java.nio.file.*;
import java.util.logging.*;

/**
 * LoggerConfig is responsible for setting up and initializing the application's logging system.
 * It configures a file-based logger, removes console handlers to avoid duplicate logs,
 * and ensures that the logger is initialized only once.
 */
public class LoggerConfig {
    // Logger instance used for internal logging within this configuration class
    private static final Logger LOGGER = Logger.getLogger(LoggerConfig.class.getName());

    // Flag to track whether the logger has already been initialized
    private static boolean isInitialized = false;

    /**
     * Initializes the logging configuration.
     * This method configures the root logger to write logs to a specified file.
     * It ensures a clean start by deleting the old log file if it exists.
     * The logger is configured with a simple text formatter and file output only (no console).
     */
    public static synchronized void initializeLogger() {
        // Prevent multiple initializations in case this method is called more than once
        if (isInitialized) {
            return;
        }

        try {
            // Retrieve the log file name from application configuration
            String logFileName = Config.getString("log.file.name");

            // If the configuration key is missing or blank, use a default fallback name
            if (logFileName == null || logFileName.isBlank()) {
                logFileName = "default-app.log";
            }

            // Convert the file name to a Path object for file operations
            Path logFilePath = Paths.get(logFileName);

            // Remove the old log file if it already exists to start fresh
            Files.deleteIfExists(logFilePath);

            // Create a FileHandler that writes to the specified file, with appending disabled
            FileHandler fileHandler = new FileHandler(logFileName, false);

            // Use the default simple formatter to make log entries human-readable
            fileHandler.setFormatter(new SimpleFormatter());

            // Obtain the root logger — this is the global logging entry point
            Logger rootLogger = Logger.getLogger("");

            // Remove all existing ConsoleHandlers from the root logger to avoid duplicate output
            Handler[] handlers = rootLogger.getHandlers();
            for (Handler handler : handlers) {
                if (handler instanceof ConsoleHandler) {
                    rootLogger.removeHandler(handler);
                }
            }

            // Attach the new file-based handler to the root logger
            rootLogger.addHandler(fileHandler);

            // Set the desired logging level, retrieved from configuration
            rootLogger.setLevel(Config.getLogLevel());

            // Log that the logger has been successfully initialized
            LOGGER.info("Logging successfully initialized.");

            // Mark logger as initialized
            isInitialized = true;

        } catch (IOException e) {
            // If any file operation fails, log the failure using the internal logger
            LOGGER.log(Level.SEVERE, "Failed to initialize FileHandler for logger.", e);
        }
    }

    /**
     * Provides the global application logger instance.
     * Ensures that the logger is initialized before use.
     *
     * @return the root Logger instance for global application logging
     */
    public static Logger getLogger() {
        // Lazy initialization check — ensure logger is ready before returning
        if (!isInitialized) {
            initializeLogger();
        }

        // Return the root logger (used across the application)
        return Logger.getLogger("");
    }

}
