package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class RandomnessTester extends JFrame {
    private JTextArea inputArea;  // Поле для ввода последовательностей
    private JTextArea outputArea; // Поле для вывода результатов

    public RandomnessTester() {
        // Настройка окна
        setTitle("Тестер случайности");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Текстовое поле для ввода
        inputArea = new JTextArea(10, 50);
        inputArea.setLineWrap(true);
        JScrollPane inputScroll = new JScrollPane(inputArea);

        // Кнопка для запуска тестов
        JButton testButton = new JButton("Провести тесты");
        testButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                processInput();
            }
        });

        // Текстовое поле для вывода результатов
        outputArea = new JTextArea(10, 50);
        outputArea.setEditable(false);
        JScrollPane outputScroll = new JScrollPane(outputArea);

        // Расположение элементов в окне
        add(inputScroll, BorderLayout.NORTH);
        add(testButton, BorderLayout.CENTER);
        add(outputScroll, BorderLayout.SOUTH);
    }

    // Обработка введённых данных
    private void processInput() {
        String input = inputArea.getText();
        String[] sequences = input.split("\n");
        StringBuilder results = new StringBuilder();

        for (String seq : sequences) {
            if (!seq.trim().isEmpty()) {
                String binary = hexToBinary(seq.trim());
                results.append("Последовательность: ").append(seq.substring(0, Math.min(10, seq.length()))).append("...\n");
                results.append("Частотный тест: ").append(monobitTest(binary) ? "Пройден" : "Не пройден").append("\n");
                results.append("Тест на последовательности: ").append(runsTest(binary) ? "Пройден" : "Не пройден").append("\n");
                results.append("Тест на автокорреляцию (d=1): ").append(autocorrelationTest(binary, 1) ? "Пройден" : "Не пройден").append("\n");
                results.append("Тест на аппроксимационную энтропию (m=2): ").append(approximateEntropyTest(binary, 2) ? "Пройден" : "Не пройден").append("\n");
                results.append("Тест на апериодичность: ").append(aperiodicTest(binary) ? "Пройден" : "Не пройден").append("\n\n");
            }
        }
        outputArea.setText(results.toString());
    }

    // Преобразование шестнадцатеричной строки в бинарную
    private String hexToBinary(String hex) {
        return new BigInteger(hex, 16).toString(2);
    }

    // Частотный тест (Monobit Test)
    private boolean monobitTest(String binary) {
        int n = binary.length();
        int n1 = 0; // Количество единиц
        for (char c : binary.toCharArray()) {
            if (c == '1') n1++;
        }
        int n0 = n - n1; // Количество нулей
        double S = Math.abs(n1 - n0) / Math.sqrt(n);
        return S < 1.96; // Уровень значимости 0.05
    }

    // Тест на последовательности (Runs Test)
    private boolean runsTest(String binary) {
        int n = binary.length();
        int n1 = 0, n0 = 0;
        for (char c : binary.toCharArray()) {
            if (c == '1') n1++;
            else n0++;
        }
        if (n1 == 0 || n0 == 0) return false; // Если все биты одинаковы, тест не пройден

        int r = 1; // Начальное количество серий
        for (int i = 1; i < n; i++) {
            if (binary.charAt(i) != binary.charAt(i - 1)) r++;
        }

        double Er = (2.0 * n1 * n0 / n) + 1; // Ожидаемое количество серий
        double sigma2 = (2.0 * n1 * n0 * (2.0 * n1 * n0 - n)) / (n * n * (n - 1)); // Дисперсия
        double Z = (r - Er) / Math.sqrt(sigma2);
        return Math.abs(Z) < 1.96; // Уровень значимости 0.05
    }

    // Тест на автокорреляцию (Autocorrelation Test)
    private boolean autocorrelationTest(String binary, int d) {
        int n = binary.length();
        int A = 0; // Количество различий между битами со сдвигом d
        for (int i = 0; i < n - d; i++) {
            if (binary.charAt(i) != binary.charAt(i + d)) A++;
        }
        double mean = (n - d) / 2.0; // Ожидаемое значение
        double variance = (n - d) / 4.0; // Дисперсия
        double Z = (A - mean) / Math.sqrt(variance);
        return Math.abs(Z) < 1.96; // Уровень значимости 0.05
    }

    // Тест на аппроксимационную энтропию (Approximate Entropy Test)
    private boolean approximateEntropyTest(String binary, int m) {
        int n = binary.length();
        if (n < m) return false; // Последовательность слишком коротка для теста

        // Подсчёт частоты m-битных и (m+1)-битных шаблонов
        Map<String, Integer> countM = new HashMap<>();
        Map<String, Integer> countM1 = new HashMap<>();

        for (int i = 0; i <= n - m; i++) {
            String sub = binary.substring(i, i + m);
            countM.put(sub, countM.getOrDefault(sub, 0) + 1);
            if (i <= n - m - 1) {
                String sub1 = binary.substring(i, i + m + 1);
                countM1.put(sub1, countM1.getOrDefault(sub1, 0) + 1);
            }
        }

        // Вычисление φ(m) и φ(m+1)
        double phiM = 0.0;
        for (int c : countM.values()) {
            double p = (double) c / (n - m + 1);
            phiM += p * Math.log(p);
        }
        phiM = -phiM;

        double phiM1 = 0.0;
        for (int c : countM1.values()) {
            double p = (double) c / (n - m);
            phiM1 += p * Math.log(p);
        }
        phiM1 = -phiM1;

        // Вычисление аппроксимационной энтропии
        double apEn = phiM - phiM1;

        // Ожидаемое значение для случайной последовательности
        double expectedApEn = Math.log(2) - (Math.log(2) / 2); // Примерно 0.3465

        // Проверка: тест пройден, если apEn близко к expectedApEn (порог 0.01)
        return Math.abs(apEn - expectedApEn) < 0.01;
    }

    // Тест на апериодичность (Aperiodic Test)
    private boolean aperiodicTest(String binary) {
        int n = binary.length();
        if (n < 20) return false; // Минимальная длина для теста

        int maxD = Math.min(10, n / 2); // Ограничиваем количество сдвигов
        double threshold = 1.96 * Math.sqrt(n); // Пороговое значение для уровня значимости 0.05

        for (int d = 1; d <= maxD; d++) {
            int R = 0;
            for (int i = 0; i < n - d; i++) {
                int xi = binary.charAt(i) == '1' ? 1 : 0;
                int xi_d = binary.charAt(i + d) == '1' ? 1 : 0;
                R += (2 * xi - 1) * (2 * xi_d - 1);
            }
            if (Math.abs(R) >= threshold) {
                return false; // Обнаружена значимая корреляция
            }
        }
        return true; // Все корреляции в пределах нормы
    }

    // Запуск программы
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RandomnessTester().setVisible(true));
    }
}

