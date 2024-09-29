package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.awt.*;

public class NumberDisplayWindow extends JFrame {
    private JTextArea textArea; // Поле для отображения чисел

    // Конструктор для инициализации окна
    public NumberDisplayWindow() {
        setTitle("Random Numbers"); // Устанавливаем заголовок окна
        setSize(300, 400); // Устанавливаем размер окна
        setLocationRelativeTo(null);  // Центрируем окно на экране
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);  // Закрываем только это окно, при нажатии "закрыть"
        initUI(); // Инициализируем компоненты
    }

    // Инициализация интерфейса
    private void initUI() {
        textArea = new JTextArea(); // Создаем текстовое поле
        textArea.setEditable(false);  // Делаем текстовое поле нередактируемым
        JScrollPane scrollPane = new JScrollPane(textArea); // Добавляем возможность прокрутки текста
        add(scrollPane, BorderLayout.CENTER); // Размещаем текстовое поле с прокруткой в центре окна
    }

    // Метод для добавления числа в текстовое поле
    public void addNumber(int number) {
        SwingUtilities.invokeLater(() -> {
            textArea.append(number + "\n"); // Добавляем новое число в текстовое поле
        });
    }
}
