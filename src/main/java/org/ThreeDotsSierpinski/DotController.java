package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class DotController extends JPanel {
    private static final int SIZE = 900; // Размер панели
    private static final int DOTWIDTH = 7; // Ширина точки
    private static final int DOTHEIGHT = 7; // Высота точки
    private final List<Dot> dots; // Список точек
    private final RandomNumberProvider randomNumberProvider; // Провайдер случайных чисел
    private int dotCounter; // Счетчик точек
    private String errorMessage; // Сообщение об ошибке

    // Конструктор, инициализирующий панель и параметры
    public DotController() {
        setPreferredSize(new Dimension(SIZE, SIZE)); // Устанавливаем размер панели
        setBackground(new Color(176, 224, 230)); // Задаем цвет фона
        dots = new ArrayList<>(); // Инициализируем список точек
        randomNumberProvider = new RandomNumberProvider(); // Инициализируем провайдер случайных чисел
        dotCounter = 0; // Инициализируем счетчик точек
        errorMessage = null; // Изначально ошибки нет
    }

    // Метод для перемещения точки на новую позицию
    public void moveDot() {
        try {
            int randomValue = randomNumberProvider.getNextRandomNumber(); // Получаем случайное число
            Point newPoint = calculateNewDotPosition(randomValue); // Вычисляем новую позицию точки
            Dot newDot = new Dot(newPoint); // Создаем новую точку
            dots.add(newDot); // Добавляем точку в список
            dotCounter++; // Увеличиваем счетчик точек
            repaint(); // Перерисовываем панель для отображения новой точки

            System.out.println("Перерисовано!");

        } catch (Exception e) {
            errorMessage = "Error: Cannot connect to Random Number Provider."; // Устанавливаем сообщение об ошибке
            repaint(); // Перерисовываем панель для отображения ошибки
        }
    }

    // Метод для вычисления новой позиции точки на основе случайного числа
    private Point calculateNewDotPosition(int randomValue) {

        int MinValue = -99999999;
        int MaxValue = 100000000;

        System.out.println("RandomValue => " + randomValue + ";");

        // Текущие координаты центра панели
        int x = SIZE / 2;
        int y = SIZE / 2;

        // Вычисляем трети диапазона
        int rangePart = (MaxValue - MinValue) / 3;

        // Пример логики для вычисления новой позиции
        if (randomValue <= (MinValue + rangePart)) {
            // Двигаемся к точке A (например, половина расстояния)
            x = x / 2;
            y = y / 2;
        } else if (randomValue <= (MinValue + 2 * rangePart)) {
            // Двигаемся к точке B (половина расстояния к другой вершине)
            x = SIZE / 2 + x / 2;
            y = y / 2;
        } else {
            // Двигаемся к точке C (третья вершина)
            x = x / 2;
            y = SIZE / 2 + y / 2;
        }

        // Возвращаем новую позицию точки
        return new Point(x, y);
    }


    // Метод для отрисовки точек и ошибок
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (errorMessage != null) {
            g.setColor(Color.RED); // Цвет ошибки
            g.drawString(errorMessage, 10, 20); // Отрисовываем сообщение об ошибке
        } else {
            for (Dot dot : dots) {
                g.setColor(Color.BLACK); // Цвет точки
                g.fillOval(dot.getPoint().x, dot.getPoint().y, DOTWIDTH, DOTHEIGHT); // Рисуем точку
            }
        }
    }

    // Метод для получения количества созданных точек
    public int getDotCounter() {
        return dotCounter;
    }
}
