package org.ThreeDotsSierpinski;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit тесты для класса LoggerConfig.
 */
@DisplayName("LoggerConfig - конфигурация логгера")
class LoggerConfigTest {

    @Test
    @DisplayName("getLogger() возвращает не-null логгер")
    void testGetLoggerReturnsNonNull() {
        Logger logger = LoggerConfig.getLogger();
        assertNotNull(logger, "Logger не должен быть null");
    }

    @Test
    @DisplayName("getLogger() возвращает один и тот же экземпляр")
    void testGetLoggerReturnsSameInstance() {
        Logger logger1 = LoggerConfig.getLogger();
        Logger logger2 = LoggerConfig.getLogger();
        
        assertSame(logger1, logger2, "Должен возвращаться один и тот же логгер");
    }

    @Test
    @DisplayName("initializeLogger() может вызываться многократно без ошибок")
    void testMultipleInitializationCalls() {
        assertDoesNotThrow(() -> {
            LoggerConfig.initializeLogger();
            LoggerConfig.initializeLogger();
            LoggerConfig.initializeLogger();
        }, "Многократные вызовы initializeLogger() не должны вызывать ошибок");
    }

    @Test
    @DisplayName("Логгер может записывать сообщения")
    void testLoggerCanLog() {
        Logger logger = LoggerConfig.getLogger();
        
        assertDoesNotThrow(() -> {
            logger.info("Test info message");
            logger.warning("Test warning message");
            logger.fine("Test fine message");
        }, "Логгер должен уметь записывать сообщения");
    }

    @Test
    @DisplayName("Логгер thread-safe при параллельных вызовах")
    void testLoggerThreadSafety() throws InterruptedException {
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                Logger logger = LoggerConfig.getLogger();
                assertNotNull(logger);
                logger.info("Message from thread " + Thread.currentThread().getName());
            });
        }
        
        for (Thread thread : threads) {
            thread.start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
    }
}
