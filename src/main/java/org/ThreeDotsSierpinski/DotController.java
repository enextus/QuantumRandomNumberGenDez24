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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Контроллер для управления и отображения точек фрактала Серпинского.
 */
public class DotController extends JPanel {
    private static final int SIZE = 1000; // Размер панели
    private static final int DOT_SIZE = 3; // Размер точки
    private static final int TIMER_DELAY = 1; // Интервал между обновлениями в миллисекундах
    private static final int DOTS_PER_UPDATE = 1; // Количество точек, добавляемых за одно обновление
    private static final long MIN_RANDOM_VALUE = -99999999L; // Минимальное значение диапазона случайных чисел
    private static final long MAX_RANDOM_VALUE = 100000000L; // Максимальное значение диапазона случайных чисел

    private final List<Dot> dots; // Список точек
    private final RandomNumberProvider randomNumberProvider; // Провайдер случайных чисел
    private int dotCounter; // Счетчик точек
    private volatile String errorMessage; // Сообщение об ошибке
    private Point currentPoint; // Текущее положение точки
    private final BufferedImage offscreenImage; // Буфер офф-скрина для рисования
    private final ScheduledExecutorService scheduler; // Планировщик для смены цвета точки

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
        scheduler = Executors.newScheduledThreadPool(1); // Инициализация планировщика
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
     * Запускает обновление точек с использованием Timer.
     */
    public void startDotMovement() {
        Timer timer = new Timer(TIMER_DELAY, e -> {
            if (errorMessage == null) {
                List<Dot> newDots = new ArrayList<>();

                for (int i = 0; i < DOTS_PER_UPDATE; i++) { // Цикл добавления точек
                    try {
                        // Получение следующего случайного числа в указанном диапазоне
                        long randomValue = randomNumberProvider.getNextRandomNumberInRange(MIN_RANDOM_VALUE, MAX_RANDOM_VALUE);
                        // Вычисление нового положения точки на основе случайного числа
                        currentPoint = calculateNewDotPosition(currentPoint, randomValue);
                        // Создание новой точки
                        Dot newDot = new Dot(new Point(currentPoint));
                        dotCounter++; // Увеличение счетчика точек
                        newDots.add(newDot); // Добавление точки в список для публикации
                    } catch (NoSuchElementException ex) {
                        // Установка сообщения об ошибке только один раз
                        if (errorMessage == null) {
                            errorMessage = ex.getMessage();
                            LOGGER.log(Level.WARNING, "Больше нет доступных случайных чисел: " + ex.getMessage());
                        }
                        ((Timer) e.getSource()).stop();
                        break; // Выход из цикла
                    }
                }

                dots.addAll(newDots); // Добавление всех точек в список
                drawDots(newDots, Color.RED); // Рисование новых точек красным цветом
                repaint(); // Перерисовка панели после добавления всех точек
                LOGGER.fine("Обработано " + newDots.size() + " новых точек.");

                // Планируем смену цвета точки на черный через 1 секунду
                scheduler.schedule(() -> {
                    drawDots(newDots, Color.BLACK); // Смена цвета на черный
                    repaint();
                }, 1, TimeUnit.SECONDS);
            } else {
                ((Timer) e.getSource()).stop();
                repaint(); // Перерисовка панели для отображения сообщения об ошибке
                LOGGER.severe("Обнаружена ошибка при перемещении точек: " + errorMessage);
            }
        });
        timer.start();
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
     * @param color   Цвет для рисования точек
     */
    private void drawDots(List<Dot> newDots, Color color) {
        Graphics2D g2d = offscreenImage.createGraphics(); // Получение контекста графики буфера
        g2d.setColor(color); // Установка цвета для рисования точек
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
            // Проверка на пустоту списка dots
            if (dots.isEmpty()) {
                LOGGER.warning("Экспорт отменен: список точек пуст.");
                return; // Если список пуст, выходим из метода
            }

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