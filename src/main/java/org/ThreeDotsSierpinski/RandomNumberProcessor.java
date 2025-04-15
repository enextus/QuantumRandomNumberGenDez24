package org.ThreeDotsSierpinski;

import java.util.ArrayList;
import java.util.List;

/**
 * Класс для обработки случайных чисел из HEX-формата и генерации чисел в заданном диапазоне.
 */
public class RandomNumberProcessor {

    /**
     * Преобразует HEX-строку в список чисел в диапазоне [0, 65535].
     *
     * @param hexData HEX-строка от API.
     * @return Список чисел (16-битные значения).
     * @throws IllegalArgumentException Если HEX-строка некорректна.
     */
    public List<Integer> processHexToNumbers(String hexData) {
        byte[] bytes = hexStringToByteArray(hexData);
        List<Integer> numbers = new ArrayList<>();

        for (int i = 0; i < bytes.length - 1; i += 2) {
            int high = bytes[i] & 0xFF;
            int low = bytes[i + 1] & 0xFF;
            int combined = (high << 8) | low; // 0–65535
            numbers.add(combined);
        }

        return numbers;
    }

    /**
     * Генерирует число в заданном диапазоне [min, max] из 16-битного числа.
     *
     * @param number 16-битное число (0–65535).
     * @param min Минимальное значение диапазона.
     * @param max Максимальное значение диапазона.
     * @return Число в диапазоне [min, max].
     */
    public long generateNumberInRange(int number, long min, long max) {
        double normalized = number / 65535.0; // Нормализация в [0.0, 1.0]
        long range = max - min;
        return min + (long) (normalized * range);
    }

    /**
     * Преобразует HEX-строку в массив байтов.
     *
     * @param s HEX-строка.
     * @return Массив байтов.
     * @throws IllegalArgumentException Если строка некорректна.
     */
    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Invalid HEX string length.");
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int high = Character.digit(s.charAt(i), 16);
            int low = Character.digit(s.charAt(i + 1), 16);
            if (high == -1 || low == -1) {
                throw new IllegalArgumentException("Invalid character in HEX string.");
            }
            data[i / 2] = (byte) ((high << 4) + low);
        }
        return data;
    }

}
