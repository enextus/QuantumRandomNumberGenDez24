package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Collections;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Контроллер для управления и отображения точек фрактала Серпинского.
 */
public class DotController extends JPanel {
    private static final int SIZE = 1000; // Размер панели
    private static final int DOT_SIZE = 2; // Размер точки
    private final List<Dot> dots; // Список точек
    private final RandomNumberProvider randomNumberProvider; // Провайдер случайных чисел
    private int dotCounter; // Счетчик точек
    private volatile String errorMessage; // Сообщение об ошибке
    private Point currentPoint; // Текущее положение точки
    private final BufferedImage offscreenImage; // Буфер оффскрина для рисования

    private static final Logger LOGGER = LoggerConfig.getLogger();

    /**
     * Конструктор, принимающий RandomNumberProvider.
     *
     * @param randomNumberProvider Провайдер случайных чисел
     */
    public DotController(RandomNumberProvider randomNumberProvider) {
        currentPoint = new Point(SIZE / 2, SIZE / 2); // Начальная точка в центре
        setPreferredSize(new Dimension(SIZE, SIZE)); // Установка размера панели
        setBackground(Color.WHITE); // Белый фон для лучшей видимости
        dots = Collections.synchronizedList(new ArrayList<>()); // Инициализация синхронизированного списка точек
        this.randomNumberProvider = randomNumberProvider; // Назначение провайдера случайных чисел
        dotCounter = 0; // Инициализация счетчика точек
        errorMessage = null; // Изначально отсутствует ошибка

        // Инициализация буфера оффскрина
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

    /**
     * Перемещает точку в новое положение.
     * Запускает фоновую задачу для обновления положения точки и добавления новых точек.
     */
    public void moveDot() {
        // Если уже есть сообщение об ошибке, не продолжаем
        if (errorMessage != null) {
            return;
        }

        new SwingWorker<Void, Dot>() {
            @Override
            protected Void doInBackground() {
                long MinValue = -99999999L; // Минимальное значение диапазона случайных чисел
                long MaxValue = 100000000L; // Максимальное значение диапазона случайных чисел

                for (int i = 0; i < 10000; i++) { // Цикл добавления 10,000 точек
                    try {
                        // Получение следующего случайного числа в указанном диапазоне
                        long randomValue = randomNumberProvider.getNextRandomNumberInRange(MinValue, MaxValue);
                        // Вычисление нового положения точки на основе случайного числа
                        currentPoint = calculateNewDotPosition(currentPoint, randomValue);
                        // Создание новой точки
                        Dot newDot = new Dot(new Point(currentPoint));
                        dotCounter++; // Увеличение счетчика точек
                        publish(newDot); // Публикация точки для рисования
                    } catch (NoSuchElementException e) {
                        // Установка сообщения об ошибке только один раз
                        if (errorMessage == null) {
                            errorMessage = e.getMessage();
                            LOGGER.log(Level.WARNING, "Больше нет доступных случайных чисел: " + e.getMessage());
                        }
                        break; // Выход из цикла
                    }
                }
                return null;
            }

            @Override
            protected void process(List<Dot> chunks) {
                dots.addAll(chunks); // Добавление новых точек в список
                drawDots(chunks); // Рисование новых точек на буфере
                repaint(); // Перерисовка панели
                LOGGER.fine("Обработано " + chunks.size() + " новых точек.");
            }

            @Override
            protected void done() {
                if (errorMessage != null) {
                    repaint(); // Перерисовка панели для отображения сообщения об ошибке
                    LOGGER.severe("Обнаружена ошибка при перемещении точек: " + errorMessage);
                }
            }
        }.execute(); // Запуск SwingWorker
    }

    /**
     * Вычисляет новое положение точки на основе случайного числа.
     *
     * @param currentPoint Текущее положение точки
     * @param randomValue  Случайное число для определения направления движения
     * @return Новое положение точки
     */
    private Point calculateNewDotPosition(Point currentPoint, long randomValue) {
        long MinValue = -99999999L; // Минимальное значение диапазона
        long MaxValue = 100000000L; // Максимальное значение диапазона

        // Фиксированные вершины треугольника
        Point A = new Point(SIZE / 2, 0); // Верхняя вершина
        Point B = new Point(0, SIZE); // Левый нижний угол
        Point C = new Point(SIZE, SIZE); // Правый нижний угол

        long rangePart = (MaxValue - MinValue) / 3; // Разделение диапазона на три части

        int x = currentPoint.x;
        int y = currentPoint.y;

        if (randomValue <= MinValue + rangePart) {
            // Движение к вершине A
            x = (x + A.x) / 2;
            y = (y + A.y) / 2;
        } else if (randomValue <= MinValue + 2 * rangePart) {
            // Движение к вершине B
            x = (x + B.x) / 2;
            y = (y + B.y) / 2;
        } else {
            // Движение к вершине C
            x = (x + C.x) / 2;
            y = (y + C.y) / 2;
        }

        return new Point(x, y); // Возвращение нового положения точки
    }

    /**
     * Рисует новые точки на буфере.
     *
     * @param newDots Список новых точек для рисования
     */
    private void drawDots(List<Dot> newDots) {
        Graphics2D g2d = offscreenImage.createGraphics(); // Получение контекста графики буфера
        g2d.setColor(Color.BLACK); // Установка цвета для рисования точек
        for (Dot dot : newDots) {
            g2d.fillRect(dot.point().x, dot.point().y, DOT_SIZE, DOT_SIZE); // Рисование точки
        }
        g2d.dispose(); // Освобождение контекста графики
    }

    /**
     * Отрисовывает панель.
     *
     * @param g Объект Graphics для рисования
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // Вызов метода суперкласса для базовой отрисовки
        g.drawImage(offscreenImage, 0, 0, null); // Отрисовка буферного изображения
        if (errorMessage != null) {
            g.setColor(Color.RED); // Установка красного цвета для текста ошибки
            g.drawString(errorMessage, 10, 20); // Отрисовка сообщения об ошибке
        }
    }

    /**
     * Получает количество созданных точек.
     *
     * @return Количество точек
     */
    public int getDotCounter() {
        return dotCounter;
    }

    /**
     * Экспортирует список точек в указанный текстовый файл.
     *
     * @param filename Имя файла для экспорта точек.
     * @throws IOException Если произошла ошибка ввода-вывода.
     */
    public void exportDotsToFile(String filename) throws IOException {
        synchronized (dots) { // Синхронизация доступа к списку точек
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
                for (Dot dot : dots) {
                    Point p = dot.point();
                    writer.write(p.x + "," + p.y);
                    writer.newLine();
                }
                LOGGER.info("Точки успешно экспортированы в " + filename);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Не удалось экспортировать точки в файл: " + filename, e);
                throw e; // Проброс исключения для обработки на более высоком уровне
            }
        }
    }
}
