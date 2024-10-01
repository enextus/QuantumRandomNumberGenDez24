package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Контроллер для управления и отображения точек фрактала Серпинского.
 *
 * The controller for managing and displaying points of the Sierpinski fractal.
 */
public class DotController extends JPanel {
    private static final int SIZE = 900; // Размер панели
    // Panel size
    private static final int DOT_SIZE = 2; // Размер точки
    // Dot size
    private final List<Dot> dots; // Список точек
    // List of dots
    private final RandomNumberProvider randomNumberProvider; // Провайдер случайных чисел
    // Random number provider
    private int dotCounter; // Счетчик точек
    // Dot counter
    private String errorMessage; // Сообщение об ошибке
    // Error message
    private Point currentPoint; // Текущая позиция точки
    // Current point position
    private final BufferedImage offscreenImage; // Буфер для отрисовки
    // Offscreen image buffer for drawing

    /**
     * Конструктор, принимающий RandomNumberProvider.
     *
     * Constructor that takes a RandomNumberProvider.
     *
     * @param randomNumberProvider Провайдер случайных чисел
     *                             Random number provider
     */
    public DotController(RandomNumberProvider randomNumberProvider) {
        currentPoint = new Point(SIZE / 2, SIZE / 2); // Начальная точка в центре
        // Initial point in the center
        setPreferredSize(new Dimension(SIZE, SIZE)); // Устанавливаем размер панели
        // Setting the panel size
        setBackground(Color.WHITE); // Белый фон для лучшей видимости
        // White background for better visibility
        dots = new CopyOnWriteArrayList<>(); // Инициализируем потокобезопасный список точек
        // Initializing a thread-safe list of dots
        this.randomNumberProvider = randomNumberProvider; // Принимаем провайдер случайных чисел
        // Assigning the random number provider
        dotCounter = 0; // Инициализируем счетчик точек
        // Initializing the dot counter
        errorMessage = null; // Изначально ошибки нет
        // Initially, there is no error

        // Инициализируем буферизованное изображение
        // Initializing the offscreen image buffer
        offscreenImage = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
    }

    /**
     * Получает сообщение об ошибке.
     *
     * Gets the error message.
     *
     * @return Сообщение об ошибке или null, если ошибки нет.
     *         The error message or null if there is no error.
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Метод для перемещения точки на новую позицию.
     * Запускает фоновую задачу для обновления позиции точки и добавления новых точек.
     *
     * Method to move the dot to a new position.
     * Starts a background task to update the dot's position and add new dots.
     */
    public void moveDot() {
        // Если уже есть сообщение об ошибке, не продолжаем
        // If there is already an error message, do not continue
        if (errorMessage != null) {
            return;
        }

        new SwingWorker<Void, Dot>() {
            @Override
            protected Void doInBackground() {
                long MinValue = -99999999L; // Минимальное значение для диапазона случайных чисел
                // Minimum value for the random number range
                long MaxValue = 100000000L; // Максимальное значение для диапазона случайных чисел
                // Maximum value for the random number range

                for (int i = 0; i < 10000; i++) { // Цикл для добавления 10,000 точек
                    // Loop to add 10,000 dots
                    try {
                        // Получаем следующее случайное число в заданном диапазоне
                        // Getting the next random number in the specified range
                        long randomValue = randomNumberProvider.getNextRandomNumberInRange(MinValue, MaxValue);
                        // Вычисляем новую позицию точки на основе случайного числа
                        // Calculating the new dot position based on the random number
                        currentPoint = calculateNewDotPosition(currentPoint, randomValue);
                        // Создаем новую точку
                        // Creating a new dot
                        Dot newDot = new Dot(new Point(currentPoint));
                        dotCounter++; // Увеличиваем счетчик точек
                        // Incrementing the dot counter
                        publish(newDot); // Передаем точку для отрисовки
                        // Publishing the dot for drawing
                    } catch (NoSuchElementException e) {
                        // Если возникла ошибка из-за отсутствия случайных чисел или достижения лимита
                        // If an error occurred due to lack of random numbers or reaching the limit
                        if (errorMessage == null) { // Устанавливаем сообщение об ошибке только один раз
                            // Setting the error message only once
                            errorMessage = e.getMessage();
                        }
                        break; // Выходим из цикла
                        // Exiting the loop
                    }
                }
                return null;
            }

            @Override
            protected void process(List<Dot> chunks) {
                dots.addAll(chunks); // Добавляем новые точки в список
                // Adding new dots to the list
                drawDots(chunks); // Рисуем новые точки на буфере
                // Drawing new dots on the buffer
                repaint(); // Перерисовываем панель
                // Repainting the panel
            }

            @Override
            protected void done() {
                if (errorMessage != null) {
                    repaint(); // Перерисовываем панель, чтобы отобразить сообщение об ошибке
                    // Repainting the panel to display the error message
                }
            }
        }.execute(); // Запускаем SwingWorker
        // Starting the SwingWorker
    }

    /**
     * Метод для вычисления новой позиции точки на основе случайного числа.
     *
     * Method to calculate the new dot position based on a random number.
     *
     * @param currentPoint Текущая позиция точки
     *                     Current point position
     * @param randomValue  Случайное число для определения направления движения
     *                     Random number to determine the movement direction
     * @return Новая позиция точки
     *         New dot position
     */
    private Point calculateNewDotPosition(Point currentPoint, long randomValue) {
        long MinValue = -99999999L; // Минимальное значение диапазона
        // Minimum range value
        long MaxValue = 100000000L; // Максимальное значение диапазона
        // Maximum range value

        // Фиксированные вершины треугольника
        // Fixed vertices of the triangle
        Point A = new Point(SIZE / 2, 0); // Вершина сверху
        // Top vertex
        Point B = new Point(0, SIZE); // Левая нижняя вершина
        // Bottom-left vertex
        Point C = new Point(SIZE, SIZE); // Правая нижняя вершина
        // Bottom-right vertex

        long rangePart = (MaxValue - MinValue) / 3; // Разделение диапазона на три части
        // Dividing the range into three parts

        int x = currentPoint.x;
        int y = currentPoint.y;

        if (randomValue <= MinValue + rangePart) {
            // Двигаемся к точке A
            // Moving towards point A
            x = (x + A.x) / 2;
            y = (y + A.y) / 2;
        } else if (randomValue <= MinValue + 2 * rangePart) {
            // Двигаемся к точке B
            // Moving towards point B
            x = (x + B.x) / 2;
            y = (y + B.y) / 2;
        } else {
            // Двигаемся к точке C
            // Moving towards point C
            x = (x + C.x) / 2;
            y = (y + C.y) / 2;
        }

        return new Point(x, y); // Возвращаем новую позицию точки
        // Returning the new dot position
    }

    /**
     * Метод для рисования новых точек на буфере.
     *
     * Method to draw new dots on the buffer.
     *
     * @param newDots Список новых точек для отрисовки
     *                List of new dots to draw
     */
    private void drawDots(List<Dot> newDots) {
        Graphics2D g2d = offscreenImage.createGraphics(); // Получаем графический контекст буфера
        // Obtaining the graphics context of the buffer
        g2d.setColor(Color.BLACK); // Устанавливаем цвет для отрисовки точек
        // Setting the color for drawing dots
        for (Dot dot : newDots) {
            g2d.fillRect(dot.point().x, dot.point().y, DOT_SIZE, DOT_SIZE); // Рисуем точку
            // Drawing the dot
        }
        g2d.dispose(); // Освобождаем графический контекст
        // Disposing of the graphics context
    }

    /**
     * Метод для отрисовки панели.
     *
     * Method for painting the panel.
     *
     * @param g Объект Graphics для рисования
     *          Graphics object for drawing
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // Вызываем метод родительского класса для базовой отрисовки
        // Calling the superclass method for basic painting
        g.drawImage(offscreenImage, 0, 0, null); // Рисуем буферизованное изображение
        // Drawing the offscreen image
        if (errorMessage != null) {
            g.setColor(Color.RED); // Устанавливаем красный цвет для текста ошибки
            // Setting red color for the error text
            g.drawString(errorMessage, 10, 20); // Отрисовываем сообщение об ошибке
            // Drawing the error message
        }
    }

    /**
     * Метод для получения количества созданных точек.
     *
     * Method to get the number of created dots.
     *
     * @return Количество точек
     *         Number of dots
     */
    public int getDotCounter() {
        return dotCounter;
    }
}
