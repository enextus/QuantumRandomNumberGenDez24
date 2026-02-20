package org.ThreeDotsSierpinski;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit тесты для класса Config.
 */
@DisplayName("Config - тесты конфигурации")
class ConfigTest {

    @Test
    @DisplayName("getString() - возвращает строковое значение")
    void testGetString() {
        String apiUrl = Config.getString("api.url");
        assertNotNull(apiUrl, "api.url не должен быть null");
        assertTrue(apiUrl.startsWith("https://"), "api.url должен начинаться с https://");
    }

    @Test
    @DisplayName("getInt() - возвращает целочисленное значение")
    void testGetInt() {
        int panelWidth = Config.getInt("panel.size.width");
        assertTrue(panelWidth > 0, "panel.size.width должен быть положительным");
        assertEquals(600, panelWidth, "Ожидается panel.size.width = 600");
    }

    @Test
    @DisplayName("getLong() - возвращает long значение")
    void testGetLong() {
        long minValue = Config.getLong("random.min.value");
        long maxValue = Config.getLong("random.max.value");
        
        assertEquals(0L, minValue, "random.min.value должен быть 0");
        assertEquals(65535L, maxValue, "random.max.value должен быть 65535 для uint16");
        assertTrue(maxValue > minValue, "max должен быть больше min");
    }

    @Test
    @DisplayName("getDouble() - возвращает double значение")
    void testGetDouble() {
        double scaleWidth = Config.getDouble("window.scale.width");
        double scaleHeight = Config.getDouble("window.scale.height");
        
        assertTrue(scaleWidth > 0, "window.scale.width должен быть положительным");
        assertTrue(scaleHeight > 0, "window.scale.height должен быть положительным");
    }

    @Test
    @DisplayName("getLogLevel() - возвращает уровень логирования")
    void testGetLogLevel() {
        Level logLevel = Config.getLogLevel();
        assertNotNull(logLevel, "log.level не должен быть null");
    }

    @Test
    @DisplayName("API параметры корректно загружены")
    void testApiConfiguration() {
        String apiKey = Config.getString("api.key");
        String dataType = Config.getString("api.data.type");
        int arrayLength = Config.getInt("api.array.length");
        int connectTimeout = Config.getInt("api.connect.timeout");
        
        assertNotNull(apiKey, "api.key не должен быть null");
        assertFalse(apiKey.isEmpty(), "api.key не должен быть пустым");
        
        assertTrue(
            dataType.equals("uint8") || dataType.equals("uint16") || dataType.equals("hex16"),
            "api.data.type должен быть uint8, uint16 или hex16"
        );
        
        assertTrue(arrayLength > 0 && arrayLength <= 1024, 
            "api.array.length должен быть от 1 до 1024");
        
        assertTrue(connectTimeout > 0, "api.connect.timeout должен быть положительным");
    }

    @Test
    @DisplayName("Panel параметры корректны")
    void testPanelConfiguration() {
        int width = Config.getInt("panel.size.width");
        int height = Config.getInt("panel.size.height");
        int dotSize = Config.getInt("dot.size");
        
        assertTrue(width > 0, "panel.size.width должен быть положительным");
        assertTrue(height > 0, "panel.size.height должен быть положительным");
        assertTrue(dotSize > 0, "dot.size должен быть положительным");
    }

    @Test
    @DisplayName("Timer параметры корректны")
    void testTimerConfiguration() {
        int timerDelay = Config.getInt("timer.delay");
        int dotsPerUpdate = Config.getInt("dots.per.update");
        
        assertTrue(timerDelay > 0, "timer.delay должен быть положительным");
        assertTrue(dotsPerUpdate > 0, "dots.per.update должен быть положительным");
    }

    @Test
    @DisplayName("getInt() выбрасывает исключение для несуществующего ключа")
    void testGetIntThrowsForMissingKey() {
        assertThrows(NumberFormatException.class, () -> {
            Config.getInt("non.existent.key");
        });
    }

    @Test
    @DisplayName("getString() возвращает null для несуществующего ключа")
    void testGetStringReturnsNullForMissingKey() {
        String value = Config.getString("non.existent.key");
        assertNull(value, "Несуществующий ключ должен возвращать null");
    }

}
