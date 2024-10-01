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
    private Point currentPoint;

    // Конструктор, инициализирующий панель и параметры
    public DotController() {
        currentPoint = new Point(SIZE / 2, SIZE / 2); // Начальная точка в центре
        setPreferredSize(new Dimension(SIZE, SIZE)); // Устанавливаем размер панели
        setBackground(new Color(176, 224, 230)); // Задаем цвет фона
        dots = new ArrayList<>(); // Инициализируем список точек
        randomNumberProvider = new RandomNumberProvider(); // Инициализируем провайдер случайных чисел
        dotCounter = 0; // Инициализируем счетчик точек
        errorMessage = null; // Изначально ошибки нет
    }

    // Метод для перемещения точки на новую позицию
// Метод для перемещения точки на новую позицию
    public void moveDot() {
        try {
            for (int i = 0; i < 1000; i++) {
                int randomValue = randomNumberProvider.getNextRandomNumber();
                currentPoint = calculateNewDotPosition(currentPoint, randomValue); // Обновляем текущую точку
                Dot newDot = new Dot(new Point(currentPoint)); // Создаем новую точку
                dots.add(newDot);
                dotCounter++;
                System.out.println("RandomValue: " + randomValue + ", New Point: " + currentPoint);
            }
            repaint();
            System.out.println("Перерисовано!");
        } catch (Exception e) {
            errorMessage = "Error: Cannot connect to Random Number Provider.";
            repaint();
        }
    }

    // Метод для вычисления новой позиции точки на основе случайного числа
    private Point calculateNewDotPosition(Point currentPoint, int randomValue) {
        int MinValue = -99999999;
        int MaxValue = 100000000;

        // Фиксированные вершины треугольника
        Point A = new Point(SIZE / 2, 0); // Вершина сверху
        Point B = new Point(0, SIZE); // Левая нижняя вершина
        Point C = new Point(SIZE, SIZE); // Правая нижняя вершина

        int rangePart = (MaxValue - MinValue) / 3;

        int x = currentPoint.x;
        int y = currentPoint.y;

        if (randomValue <= MinValue + rangePart) {
            // Двигаемся к точке A
            x = (x + A.x) / 2;
            y = (y + A.y) / 2;
        } else if (randomValue <= MinValue + 2 * rangePart) {
            // Двигаемся к точке B
            x = (x + B.x) / 2;
            y = (y + B.y) / 2;
        } else {
            // Двигаемся к точке C
            x = (x + C.x) / 2;
            y = (y + C.y) / 2;
        }

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
