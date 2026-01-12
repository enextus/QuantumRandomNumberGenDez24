package org.ThreeDotsSierpinski;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;

/**
 * Класс для загрузки и предоставления конфигурационных параметров из файла config.properties.
 */
public class Config {




    private static final Properties properties = new Properties();

    static {
        try (InputStream input = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("Файл конфигурации config.properties не найден в classpath.");
            }
            properties.load(input);
        } catch (IOException ex) {
            throw new RuntimeException("Не удалось загрузить файл конфигурации config.properties.", ex);
        }
    }

    /**
     * Получает строковое значение параметра.
     *
     * @param key Ключ параметра
     * @return Значение параметра
     */
    public static String getString(String key) {
        return properties.getProperty(key);
    }

    /**
     * Получает целочисленное значение параметра.
     *
     * @param key Ключ параметра
     * @return Целочисленное значение параметра
     */
    public static int getInt(String key) {
        return Integer.parseInt(properties.getProperty(key));
    }

    /**
     * Получает длинное (long) значение параметра.
     *
     * @param key Ключ параметра
     * @return Длинное значение параметра
     */
    public static long getLong(String key) {
        return Long.parseLong(properties.getProperty(key));
    }

    /**
     * Получает значение параметра в виде double.
     *
     * @param key Ключ параметра
     * @return Значение параметра как double
     */
    public static double getDouble(String key) {
        return Double.parseDouble(properties.getProperty(key));
    }




    /**
     * Получает уровень логирования.
     *
     * @return Уровень логирования
     */
    public static Level getLogLevel() {
        return Level.parse(properties.getProperty("log.level", "INFO"));
    }

}
