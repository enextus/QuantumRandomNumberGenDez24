package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;

public class DotController extends JPanel {
    private static final int SIZE = 900; // Размер панели
    private static final int DOT_SIZE = 2; // Размер точки
    private final List<Dot> dots; // Список точек
    private final RandomNumberProvider randomNumberProvider; // Провайдер случайных чисел
    private int dotCounter; // Счетчик точек
    private String errorMessage; // Сообщение об ошибке
    private Point currentPoint;
    private final BufferedImage offscreenImage; // Буфер для отрисовки

    // Конструктор, принимающий RandomNumberProvider
    public DotController(RandomNumberProvider randomNumberProvider) {
        currentPoint = new Point(SIZE / 2, SIZE / 2); // Начальная точка в центре
        setPreferredSize(new Dimension(SIZE, SIZE)); // Устанавливаем размер панели
        setBackground(Color.WHITE); // Белый фон для лучшей видимости
        dots = new CopyOnWriteArrayList<>(); // Инициализируем потокобезопасный список точек
        this.randomNumberProvider = randomNumberProvider; // Принимаем провайдер случайных чисел
        dotCounter = 0; // Инициализируем счетчик точек
        errorMessage = null; // Изначально ошибки нет

        // Инициализируем буферизованное изображение
        offscreenImage = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
    }

    /**
     * Получает сообщение об ошибке.
     *
     * @return Сообщение об ошибке или null, если ошибки нет.
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    // Метод для перемещения точки на новую позицию
    public void moveDot() {
        // Если уже есть сообщение об ошибке, не продолжаем
        if (errorMessage != null) {
            return;
        }

        new SwingWorker<Void, Dot>() {
            @Override
            protected Void doInBackground() {
                long MinValue = -99999999L;
                long MaxValue = 100000000L;

                for (int i = 0; i < 10000; i++) {
                    try {
                        long randomValue = randomNumberProvider.getNextRandomNumberInRange(MinValue, MaxValue);
                        currentPoint = calculateNewDotPosition(currentPoint, randomValue); // Обновляем текущую точку
                        Dot newDot = new Dot(new Point(currentPoint)); // Создаем новую точку
                        dotCounter++;
                        publish(newDot); // Передаем точку для отрисовки
                    } catch (NoSuchElementException e) {
                        if (errorMessage == null) { // Устанавливаем сообщение об ошибке только один раз
                            errorMessage = e.getMessage();
                        }
                        break; // Выходим из цикла
                    }
                }
                return null;
            }

            @Override
            protected void process(List<Dot> chunks) {
                dots.addAll(chunks); // Добавляем новые точки в список
                drawDots(chunks); // Рисуем новые точки на буфере
                repaint(); // Перерисовываем панель
            }

            @Override
            protected void done() {
                if (errorMessage != null) {
                    repaint(); // Перерисовываем панель, чтобы отобразить сообщение об ошибке
                }
            }
        }.execute();
    }

    // Метод для вычисления новой позиции точки на основе случайного числа
    private Point calculateNewDotPosition(Point currentPoint, long randomValue) {
        long MinValue = -99999999L;
        long MaxValue = 100000000L;

        // Фиксированные вершины треугольника
        Point A = new Point(SIZE / 2, 0); // Вершина сверху
        Point B = new Point(0, SIZE); // Левая нижняя вершина
        Point C = new Point(SIZE, SIZE); // Правая нижняя вершина

        long rangePart = (MaxValue - MinValue) / 3;

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

    // Метод для рисования новых точек на буфере
    private void drawDots(List<Dot> newDots) {
        Graphics2D g2d = offscreenImage.createGraphics();
        g2d.setColor(Color.BLACK);
        for (Dot dot : newDots) {
            g2d.fillRect(dot.point().x, dot.point().y, DOT_SIZE, DOT_SIZE);
        }
        g2d.dispose();
    }

    // Метод для отрисовки панели
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(offscreenImage, 0, 0, null); // Рисуем буферизованное изображение
        if (errorMessage != null) {
            g.setColor(Color.RED); // Цвет ошибки
            g.drawString(errorMessage, 10, 20); // Отрисовываем сообщение об ошибке
        }
    }

    // Метод для получения количества созданных точек
    public int getDotCounter() {
        return dotCounter;
    }
}
