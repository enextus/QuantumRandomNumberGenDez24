package org.ThreeDotsSierpinski.rng;

import org.ThreeDotsSierpinski.config.Config;
import org.ThreeDotsSierpinski.config.LoggerConfig;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dedicated log for random numbers consumed by visualizations.
 * <p>
 * Format per RNProvider lifecycle / visualization run:
 * <pre>
 * 123,456,789
 *
 * 100,200,300
 * </pre>
 * <p>
 * TRUE/QUANTUM numbers are written to {@code logs/rnds-true.log}.
 * PSEUDO numbers are intentionally not logged by default.
 */
final class RandomNumbersLog implements AutoCloseable {

    private static final Logger LOGGER = LoggerConfig.getLogger();

    private static final String TRUE_LOG_FILE_CONFIG_KEY = "random.log.true.file.name";
    private static final String PSEUDO_LOG_FILE_CONFIG_KEY = "random.log.pseudo.file.name";
    private static final String PSEUDO_LOG_ENABLED_CONFIG_KEY = "random.log.pseudo.enabled";
    private static final String FLUSH_EVERY_VALUES_CONFIG_KEY = "random.log.flush.every.values";

    private static final String DEFAULT_TRUE_LOG_FILE = "logs/rnds-true.log";
    private static final String DEFAULT_PSEUDO_LOG_FILE = "logs/rnds-pseudo.log";
    private static final int DEFAULT_FLUSH_EVERY_VALUES = 256;
    private static final int MIN_FLUSH_EVERY_VALUES = 1;

    private static final String VALUE_SEPARATOR = ",";
    private static final String BATCH_SEPARATOR = System.lineSeparator() + System.lineSeparator();

    private final NumberFileWriter trueNumbersWriter;
    private final NumberFileWriter pseudoNumbersWriter;

    static RandomNumbersLog disabled() {
        return new RandomNumbersLog(
                NumberFileWriter.disabled("TRUE random numbers logging disabled"),
                NumberFileWriter.disabled("PSEUDO random numbers logging disabled")
        );
    }

    RandomNumbersLog() {
        this(
                resolvePath(TRUE_LOG_FILE_CONFIG_KEY, DEFAULT_TRUE_LOG_FILE),
                resolvePath(PSEUDO_LOG_FILE_CONFIG_KEY, DEFAULT_PSEUDO_LOG_FILE),
                isPseudoLoggingEnabled(),
                resolveFlushEveryValues()
        );
    }

    RandomNumbersLog(Path trueLogPath, Path pseudoLogPath, boolean logPseudoNumbers) {
        this(trueLogPath, pseudoLogPath, logPseudoNumbers, DEFAULT_FLUSH_EVERY_VALUES);
    }

    RandomNumbersLog(Path trueLogPath, Path pseudoLogPath, boolean logPseudoNumbers, int flushEveryValues) {
        int safeFlushEveryValues = Math.max(MIN_FLUSH_EVERY_VALUES, flushEveryValues);

        this.trueNumbersWriter = NumberFileWriter.open("TRUE random numbers", trueLogPath, safeFlushEveryValues);
        this.pseudoNumbersWriter = logPseudoNumbers
                ? NumberFileWriter.open("PSEUDO random numbers", pseudoLogPath, safeFlushEveryValues)
                : NumberFileWriter.disabled("PSEUDO random numbers logging disabled");
    }

    private RandomNumbersLog(NumberFileWriter trueNumbersWriter, NumberFileWriter pseudoNumbersWriter) {
        this.trueNumbersWriter = trueNumbersWriter;
        this.pseudoNumbersWriter = pseudoNumbersWriter;
    }

    private static Path resolvePath(String configKey, String defaultPath) {
        String configuredPath = Config.getString(configKey);
        if (configuredPath == null || configuredPath.isBlank()) {
            configuredPath = defaultPath;
        }
        return Paths.get(configuredPath);
    }

    private static boolean isPseudoLoggingEnabled() {
        String configuredValue = Config.getString(PSEUDO_LOG_ENABLED_CONFIG_KEY);
        return configuredValue != null && Boolean.parseBoolean(configuredValue.trim());
    }

    private static int resolveFlushEveryValues() {
        String configuredValue = Config.getString(FLUSH_EVERY_VALUES_CONFIG_KEY);
        if (configuredValue == null || configuredValue.isBlank()) {
            return DEFAULT_FLUSH_EVERY_VALUES;
        }

        try {
            return Math.max(MIN_FLUSH_EVERY_VALUES, Integer.parseInt(configuredValue.trim()));
        } catch (NumberFormatException e) {
            LOGGER.warning("Invalid " + FLUSH_EVERY_VALUES_CONFIG_KEY
                    + " value: " + configuredValue
                    + ". Using default: " + DEFAULT_FLUSH_EVERY_VALUES);
            return DEFAULT_FLUSH_EVERY_VALUES;
        }
    }

    void writeTrueNumber(long value) {
        trueNumbersWriter.writeNumber(value);
    }

    void writePseudoNumber(long value) {
        pseudoNumbersWriter.writeNumber(value);
    }

    void finishBatch() {
        trueNumbersWriter.finishBatch();
        pseudoNumbersWriter.finishBatch();
    }

    @Override
    public void close() {
        trueNumbersWriter.close();
        pseudoNumbersWriter.close();
    }

    private static final class NumberFileWriter implements AutoCloseable {
        private final Object lock = new Object();
        private final String label;
        private final BufferedWriter writer;
        private final int flushEveryValues;

        private boolean firstNumberInBatch = true;
        private boolean closed = false;
        private int valuesSinceFlush = 0;

        private NumberFileWriter(String label, BufferedWriter writer, int flushEveryValues) {
            this.label = label;
            this.writer = writer;
            this.flushEveryValues = flushEveryValues;
        }

        static NumberFileWriter open(String label, Path logPath, int flushEveryValues) {
            try {
                Path parent = logPath.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }

                BufferedWriter writer = Files.newBufferedWriter(
                        logPath,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND
                );

                LOGGER.info(label + " log initialized: " + logPath.toAbsolutePath());
                return new NumberFileWriter(label, writer, flushEveryValues);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Could not initialize " + label + " log: " + logPath, e);
                return disabled(label + " log unavailable");
            }
        }

        static NumberFileWriter disabled(String label) {
            return new NumberFileWriter(label, null, DEFAULT_FLUSH_EVERY_VALUES);
        }

        void writeNumber(long value) {
            synchronized (lock) {
                if (closed || writer == null) {
                    return;
                }

                try {
                    boolean isFirstWrittenNumber = firstNumberInBatch;

                    if (!firstNumberInBatch) {
                        writer.write(VALUE_SEPARATOR);
                    }
                    writer.write(Long.toString(value));
                    firstNumberInBatch = false;
                    valuesSinceFlush++;

                    if (isFirstWrittenNumber || valuesSinceFlush >= flushEveryValues) {
                        writer.flush();
                        valuesSinceFlush = 0;
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Failed to write to " + label + " log", e);
                }
            }
        }

        void finishBatch() {
            synchronized (lock) {
                if (closed || writer == null || firstNumberInBatch) {
                    return;
                }

                try {
                    writer.write(BATCH_SEPARATOR);
                    writer.flush();
                    firstNumberInBatch = true;
                    valuesSinceFlush = 0;
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Failed to finish " + label + " log batch", e);
                }
            }
        }

        @Override
        public void close() {
            synchronized (lock) {
                if (closed) {
                    return;
                }

                try {
                    finishBatch();
                    if (writer != null) {
                        writer.close();
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Failed to close " + label + " log", e);
                } finally {
                    closed = true;
                }
            }
        }
    }
}
