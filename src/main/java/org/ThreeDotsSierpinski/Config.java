package org.ThreeDotsSierpinski;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;

/**
 * Класс для загрузки и предоставления конфигурационных параметров.
 *
 * Приоритет загрузки значений (от высшего к низшему):
 * <ol>
 *   <li>Переменные окружения (например, QRNG_API_KEY)</li>
 *   <li>Файл .env в корне проекта</li>
 *   <li>Файл config.properties из classpath</li>
 * </ol>
 *
 * Конвенция именования переменных окружения:
 * Ключ из properties преобразуется в UPPER_SNAKE_CASE с префиксом QRNG_.
 * Например: api.key → QRNG_API_KEY, api.url → QRNG_API_URL
 */
public class Config {

    private static final String ENV_PREFIX = "QRNG_";
    private static final Properties configProperties = new Properties();
    private static final Properties envFileProperties = new Properties();

    static {
        loadConfigProperties();
        loadEnvFile();
    }

    /**
     * Загружает config.properties из classpath.
     */
    private static void loadConfigProperties() {
        try (InputStream input = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("Файл конфигурации config.properties не найден в classpath.");
            }
            configProperties.load(input);
        } catch (IOException ex) {
            throw new RuntimeException("Не удалось загрузить файл конфигурации config.properties.", ex);
        }
    }

    /**
     * Загружает .env файл из корня проекта, если он существует.
     * Формат файла: KEY=VALUE (по одной паре на строку).
     * Строки, начинающиеся с #, игнорируются как комментарии.
     * Пустые строки пропускаются.
     */
    private static void loadEnvFile() {
        Path envPath = Paths.get(".env");
        if (!Files.exists(envPath)) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(envPath.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Пропускаем пустые строки и комментарии
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                int separatorIndex = line.indexOf('=');
                if (separatorIndex > 0) {
                    String key = line.substring(0, separatorIndex).trim();
                    String value = line.substring(separatorIndex + 1).trim();

                    // Убираем кавычки, если значение обёрнуто в них
                    if (value.length() >= 2
                            && ((value.startsWith("\"") && value.endsWith("\""))
                            || (value.startsWith("'") && value.endsWith("'")))) {
                        value = value.substring(1, value.length() - 1);
                    }

                    envFileProperties.setProperty(key, value);
                }
            }
        } catch (IOException ex) {
            // .env файл опционален — при ошибке чтения просто пропускаем
            System.err.println("Warning: Could not read .env file: " + ex.getMessage());
        }
    }

    /**
     * Преобразует ключ из dot.notation в UPPER_SNAKE_CASE с префиксом QRNG_.
     * Например: api.key → QRNG_API_KEY
     *
     * @param propertyKey Ключ в формате dot.notation
     * @return Ключ в формате QRNG_UPPER_SNAKE_CASE
     */
    static String toEnvVarName(String propertyKey) {
        return ENV_PREFIX + propertyKey.replace('.', '_').toUpperCase();
    }

    /**
     * Получает строковое значение параметра с учётом приоритетов:
     * 1. Переменная окружения (QRNG_UPPER_SNAKE_CASE)
     * 2. Значение из .env файла (QRNG_UPPER_SNAKE_CASE)
     * 3. Значение из config.properties (dot.notation)
     *
     * @param key Ключ параметра в формате dot.notation
     * @return Значение параметра или null, если не найдено
     */
    public static String getString(String key) {
        String envVarName = toEnvVarName(key);

        // Приоритет 1: Переменная окружения
        String envValue = System.getenv(envVarName);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }

        // Приоритет 2: .env файл
        String envFileValue = envFileProperties.getProperty(envVarName);
        if (envFileValue != null && !envFileValue.isEmpty()) {
            return envFileValue;
        }

        // Приоритет 3: config.properties
        return configProperties.getProperty(key);
    }

    /**
     * Получает целочисленное значение параметра.
     *
     * @param key Ключ параметра
     * @return Целочисленное значение параметра
     * @throws NumberFormatException если значение не является числом
     * @throws RuntimeException если параметр не найден
     */
    public static int getInt(String key) {
        String value = getString(key);
        if (value == null) {
            throw new RuntimeException("Параметр не найден: " + key);
        }
        return Integer.parseInt(value);
    }

    /**
     * Получает длинное (long) значение параметра.
     *
     * @param key Ключ параметра
     * @return Длинное значение параметра
     * @throws NumberFormatException если значение не является числом
     * @throws RuntimeException если параметр не найден
     */
    public static long getLong(String key) {
        String value = getString(key);
        if (value == null) {
            throw new RuntimeException("Параметр не найден: " + key);
        }
        return Long.parseLong(value);
    }

    /**
     * Получает значение параметра в виде double.
     *
     * @param key Ключ параметра
     * @return Значение параметра как double
     * @throws NumberFormatException если значение не является числом
     * @throws RuntimeException если параметр не найден
     */
    public static double getDouble(String key) {
        String value = getString(key);
        if (value == null) {
            throw new RuntimeException("Параметр не найден: " + key);
        }
        return Double.parseDouble(value);
    }

    /**
     * Получает уровень логирования.
     *
     * @return Уровень логирования
     */
    public static Level getLogLevel() {
        String value = getString("log.level");
        return Level.parse(value != null ? value : "INFO");
    }

}
