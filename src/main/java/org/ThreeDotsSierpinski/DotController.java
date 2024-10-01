package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class DotController extends JPanel {
    private static final int SIZE = 900; // Размер панели
    private static final int DOTWIDTH = 2; // Ширина точки
    private static final int DOTHEIGHT = 2; // Высота точки
    private final List<Dot> dots; // Список точек
    private final RandomNumberProvider randomNumberProvider; // Провайдер случайных чисел
    private int dotCounter; // Счетчик точек
    private String errorMessage; // Сообщение об ошибке
    private Point currentPoint;

    // Конструктор, принимающий RandomNumberProvider
    public DotController(RandomNumberProvider randomNumberProvider) {
        currentPoint = new Point(SIZE / 2, SIZE / 2); // Начальная точка в центре
        setPreferredSize(new Dimension(SIZE, SIZE)); // Устанавливаем размер панели
        setBackground(new Color(255, 255, 255)); // Белый фон для лучшей видимости
        dots = new ArrayList<>(); // Инициализируем список точек
        this.randomNumberProvider = randomNumberProvider; // Принимаем провайдер случайных чисел
        dotCounter = 0; // Инициализируем счетчик точек
        errorMessage = null; // Изначально ошибки нет
    }

    // Метод для перемещения точки на новую позицию
    public void moveDot() {
        new SwingWorker<Void, Dot>() {
            @Override
            protected Void doInBackground() throws Exception {
                for (int i = 0; i < 1000; i++) {
                    int randomValue = randomNumberProvider.getNextRandomNumber();
                    currentPoint = calculateNewDotPosition(currentPoint, randomValue); // Обновляем текущую точку
                    Dot newDot = new Dot(new Point(currentPoint)); // Создаем новую точку
                    dotCounter++;
                    publish(newDot); // Передаем точку для отрисовки
                }
                return null;
            }

            @Override
            protected void process(List<Dot> chunks) {
                dots.addAll(chunks); // Добавляем новые точки в список
                repaint(); // Перерисовываем панель
            }

            @Override
            protected void done() {
                // Дополнительные действия после завершения (если необходимо)
            }
        }.execute();
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
            g.setColor(Color.BLACK); // Цвет точек
            for (Dot dot : dots) {
                g.fillRect(dot.getPoint().x, dot.getPoint().y, DOTWIDTH, DOTHEIGHT); // Рисуем точку
            }
        }
    }

    // Метод для получения количества созданных точек
    public int getDotCounter() {
        return dotCounter;
    }
}
