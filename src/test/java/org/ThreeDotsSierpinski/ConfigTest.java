package org.ThreeDotsSierpinski;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit тесты для класса Config.
 *
 * Покрывает:
 * - Базовые геттеры (getString, getInt, getLong, getDouble, getLogLevel)
 * - Преобразование ключей в имена переменных окружения (toEnvVarName)
 * - Валидацию параметров из config.properties
 * - Обработку отсутствующих ключей
 * - Логику приоритетов загрузки
 */
@DisplayName("Config — тесты конфигурации")
class ConfigTest {

    // ========================================================================
    // Базовые геттеры
    // ========================================================================

    @Nested
    @DisplayName("Базовые геттеры")
    class BasicGetterTests {

        @Test
        @DisplayName("getString() — возвращает строковое значение")
        void testGetString() {
            String apiUrl = Config.getString("api.url");
            assertNotNull(apiUrl, "api.url не должен быть null");
            assertTrue(apiUrl.startsWith("https://"), "api.url должен начинаться с https://");
        }

        @Test
        @DisplayName("getInt() — возвращает целочисленное значение")
        void testGetInt() {
            int panelWidth = Config.getInt("panel.size.width");
            assertTrue(panelWidth > 0, "panel.size.width должен быть положительным");
            assertEquals(600, panelWidth, "Ожидается panel.size.width = 600");
        }

        @Test
        @DisplayName("getLong() — возвращает long значение")
        void testGetLong() {
            long minValue = Config.getLong("random.min.value");
            long maxValue = Config.getLong("random.max.value");

            assertEquals(0L, minValue, "random.min.value должен быть 0");
            assertEquals(65535L, maxValue, "random.max.value должен быть 65535 для uint16");
            assertTrue(maxValue > minValue, "max должен быть больше min");
        }

        @Test
        @DisplayName("getDouble() — возвращает double значение")
        void testGetDouble() {
            double scaleWidth = Config.getDouble("window.scale.width");
            double scaleHeight = Config.getDouble("window.scale.height");

            assertTrue(scaleWidth > 0, "window.scale.width должен быть положительным");
            assertTrue(scaleHeight > 0, "window.scale.height должен быть положительным");
        }

        @Test
        @DisplayName("getLogLevel() — возвращает уровень логирования")
        void testGetLogLevel() {
            Level logLevel = Config.getLogLevel();
            assertNotNull(logLevel, "log.level не должен быть null");
        }
    }

    // ========================================================================
    // Преобразование ключей в имена переменных окружения
    // ========================================================================

    @Nested
    @DisplayName("toEnvVarName() — преобразование ключей")
    class EnvVarNameTests {

        @Test
        @DisplayName("Простой ключ с одной точкой")
        void testSimpleKey() {
            assertEquals("QRNG_API_KEY", Config.toEnvVarName("api.key"));
        }

        @Test
        @DisplayName("Ключ с несколькими точками")
        void testMultiDotKey() {
            assertEquals("QRNG_PANEL_SIZE_WIDTH", Config.toEnvVarName("panel.size.width"));
        }

        @Test
        @DisplayName("Ключ без точек")
        void testNoDotKey() {
            assertEquals("QRNG_STANDALONE", Config.toEnvVarName("standalone"));
        }

        @Test
        @DisplayName("Ключ в нижнем регистре преобразуется в верхний")
        void testLowerCaseKey() {
            assertEquals("QRNG_MY_PARAM", Config.toEnvVarName("my.param"));
        }

        @Test
        @DisplayName("Все типовые ключи проекта корректно преобразуются")
        void testProjectKeys() {
            assertEquals("QRNG_API_URL", Config.toEnvVarName("api.url"));
            assertEquals("QRNG_API_DATA_TYPE", Config.toEnvVarName("api.data.type"));
            assertEquals("QRNG_API_ARRAY_LENGTH", Config.toEnvVarName("api.array.length"));
            assertEquals("QRNG_LOG_LEVEL", Config.toEnvVarName("log.level"));
            assertEquals("QRNG_RANDOM_MIN_VALUE", Config.toEnvVarName("random.min.value"));
        }
    }

    // ========================================================================
    // Валидация параметров из config.properties
    // ========================================================================

    @Nested
    @DisplayName("Валидация параметров конфигурации")
    class ConfigValidationTests {

        @Test
        @DisplayName("api.key — присутствует (может быть placeholder)")
        void testApiKeyPresent() {
            String apiKey = Config.getString("api.key");
            assertNotNull(apiKey, "api.key не должен быть null");
            // Ключ может быть placeholder YOUR_API_KEY_HERE;
            // реальный ключ подставляется через env var или .env файл
        }

        @Test
        @DisplayName("api.data.type — допустимое значение")
        void testApiDataType() {
            String dataType = Config.getString("api.data.type");
            assertTrue(
                    "uint8".equals(dataType) || "uint16".equals(dataType) || "hex16".equals(dataType),
                    "api.data.type должен быть uint8, uint16 или hex16, получено: " + dataType
            );
        }

        @Test
        @DisplayName("api.array.length — в допустимом диапазоне")
        void testApiArrayLength() {
            int arrayLength = Config.getInt("api.array.length");
            assertTrue(arrayLength > 0 && arrayLength <= 1024,
                    "api.array.length должен быть от 1 до 1024");
        }

        @Test
        @DisplayName("Таймауты API — положительные значения")
        void testApiTimeouts() {
            int connectTimeout = Config.getInt("api.connect.timeout");
            int readTimeout = Config.getInt("api.read.timeout");

            assertTrue(connectTimeout > 0, "api.connect.timeout должен быть положительным");
            assertTrue(readTimeout > 0, "api.read.timeout должен быть положительным");
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
    }

    // ========================================================================
    // Обработка отсутствующих ключей
    // ========================================================================

    @Nested
    @DisplayName("Обработка отсутствующих ключей")
    class MissingKeyTests {

        @Test
        @DisplayName("getString() — возвращает null для несуществующего ключа")
        void testGetStringReturnsNullForMissingKey() {
            String value = Config.getString("non.existent.key");
            assertNull(value, "Несуществующий ключ должен возвращать null");
        }

        @Test
        @DisplayName("getInt() — выбрасывает RuntimeException для несуществующего ключа")
        void testGetIntThrowsForMissingKey() {
            assertThrows(RuntimeException.class, () -> Config.getInt("non.existent.key"));
        }

        @Test
        @DisplayName("getLong() — выбрасывает RuntimeException для несуществующего ключа")
        void testGetLongThrowsForMissingKey() {
            assertThrows(RuntimeException.class, () -> Config.getLong("non.existent.key"));
        }

        @Test
        @DisplayName("getDouble() — выбрасывает RuntimeException для несуществующего ключа")
        void testGetDoubleThrowsForMissingKey() {
            assertThrows(RuntimeException.class, () -> Config.getDouble("non.existent.key"));
        }
    }

    // ========================================================================
    // Приоритеты загрузки (интеграционная проверка)
    // ========================================================================

    @Nested
    @DisplayName("Приоритеты загрузки значений")
    class PriorityTests {

        @Test
        @DisplayName("config.properties — значения загружаются как fallback")
        void testConfigPropertiesFallback() {
            // Эти ключи вряд ли будут перекрыты env var в тестовом окружении
            String apiUrl = Config.getString("api.url");
            assertNotNull(apiUrl, "Значение из config.properties должно быть доступно");
            assertTrue(apiUrl.contains("quantumnumbers.anu.edu.au"),
                    "Значение api.url должно содержать домен ANU API");
        }

        @Test
        @DisplayName("Переменная окружения имеет высший приоритет над config.properties")
        void testEnvVarOverridesProperties() {
            // Если переменная окружения QRNG_API_KEY задана,
            // getString("api.key") должна вернуть её, а не значение из config.properties
            String envValue = System.getenv("QRNG_API_KEY");
            String configValue = Config.getString("api.key");

            if (envValue != null && !envValue.isEmpty()) {
                assertEquals(envValue, configValue,
                        "Переменная окружения должна перекрывать config.properties");
            }
            // Если env var не задана — тест проходит (нечего проверять)
        }
    }
}
