package org.ThreeDotsSierpinski;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.Level;

public class DotController extends JPanel {
    // Параметры панели
    private static final int SIZE_WIDTH = Config.getInt("panel.size.width");
    private static final int SIZE_HEIGHT = Config.getInt("panel.size.height");

    // Параметры точек и таймера
    private static final int DOT_SIZE = Config.getInt("dot.size");
    private static final int TIMER_DELAY = Config.getInt("timer.delay");
    private static final int DOTS_PER_UPDATE = Config.getInt("dots.per.update");

    // Диапазон случайных чисел
    private static final long MIN_RANDOM_VALUE = Config.getLong("random.min.value");
    private static final long MAX_RANDOM_VALUE = Config.getLong("random.max.value");

    // Константы для сообщений и логирования
    private static final String ERROR_NO_RANDOM_NUMBERS = "Больше нет доступных случайных чисел: ";
    private static final String LOG_DOTS_PROCESSED = "Обработано %d новых точек.";
    private static final String LOG_ERROR_MOVEMENT = "Обнаружена ошибка при перемещении точек: %s";

    // Параметры визуализации стека чисел
    private static final int COLUMN_WIDTH = Config.getInt("column.width");
    private static final int ROW_HEIGHT = Config.getInt("row.height");
    private static final int COLUMN_SPACING = Config.getInt("column.spacing");
    private static final int MAX_COLUMNS = Config.getInt("max.columns");

    private final List<Dot> dots; // Список точек
    private final List<Long> usedRandomNumbers; // Список использованных случайных чисел для визуализации
    private final RandomNumberProvider randomNumberProvider; // Провайдер случайных чисел
    private volatile String errorMessage; // Сообщение об ошибке
    private Point currentPoint; // Текущее положение точки
    private final BufferedImage offscreenImage; // Буфер оффскрина для рисования
    private final ScheduledExecutorService scheduler; // Планировщик для смены цвета точки

    private int currentRandomValueIndex = 0; // Порядковый номер текущего случайного числа
    private Long currentRandomValue; // Текущее случайное число

    private static final Logger LOGGER = LoggerConfig.getLogger();

    /**
     * Конструктор, принимающий RandomNumberProvider.
     *
     * @param randomNumberProvider Провайдер случайных чисел
     */
    public DotController(RandomNumberProvider randomNumberProvider) {
        currentPoint = new Point(SIZE_WIDTH / 2, SIZE_HEIGHT / 2); // Начальная точка в центре
        setPreferredSize(new Dimension(SIZE_WIDTH + 300, SIZE_HEIGHT)); // Увеличиваем ширину панели для отображения стека чисел
        setBackground(Color.WHITE); // Белый фон для лучшей видимости
        dots = Collections.synchronizedList(new ArrayList<>()); // Инициализация синхронизированного списка точек
        usedRandomNumbers = new ArrayList<>(); // Инициализация списка использованных случайных чисел
        this.randomNumberProvider = randomNumberProvider; // Назначение провайдера случайных чисел
        errorMessage = null; // Изначально отсутствует ошибка

        // Инициализация буфера оффскрина
        offscreenImage = new BufferedImage(SIZE_WIDTH, SIZE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        scheduler = Executors.newScheduledThreadPool(1); // Инициализация планировщика
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
                        currentRandomValueIndex++;

                        // Получение следующего случайного числа в указанном диапазоне
                        long randomValue = randomNumberProvider.getNextRandomNumberInRange(MIN_RANDOM_VALUE, MAX_RANDOM_VALUE);

                        // Сохранение текущего случайного числа и добавление его в список использованных чисел
                        currentRandomValue = randomValue;
                        usedRandomNumbers.add(currentRandomValue);

                        // Вычисление нового положения точки на основе случайного числа
                        currentPoint = calculateNewDotPosition(currentPoint, randomValue);

                        // Создание новой точки
                        Dot newDot = new Dot(new Point(currentPoint));
                        newDots.add(newDot);
                    } catch (NoSuchElementException ex) {
                        if (errorMessage == null) {
                            errorMessage = ex.getMessage();
                            LOGGER.log(Level.WARNING, ERROR_NO_RANDOM_NUMBERS + ex.getMessage());
                        }
                        ((Timer) e.getSource()).stop();
                        break; // Выход из цикла
                    }
                }

                dots.addAll(newDots); // Добавление всех точек в список
                drawDots(newDots, Color.RED); // Рисование новых точек красным цветом
                repaint(); // Перерисовка панели после добавления всех точек
                LOGGER.fine(String.format(LOG_DOTS_PROCESSED, newDots.size()));

                // Планируем смену цвета точки на черный через 1 секунду
                scheduler.schedule(() -> {
                    drawDots(newDots, Color.BLACK);
                    repaint();
                }, 1, TimeUnit.SECONDS);
            } else {
                ((Timer) e.getSource()).stop();
                repaint();
                LOGGER.severe(String.format(LOG_ERROR_MOVEMENT, errorMessage));
            }
        });
        timer.start();
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

        // Отображение порядкового номера случайного числа
        g.setColor(Color.BLUE);
        g.drawString("Порядковый номер выборки: " + currentRandomValueIndex, 10, 20);

        // Отображение текущего случайного числа
        if (currentRandomValue != null) {
            g.setColor(Color.BLACK);
            g.drawString("Текущее случайное число: " + currentRandomValue, 10, 40);
        }

        // Отображение сообщения об ошибке, если есть
        if (errorMessage != null) {
            g.setColor(Color.RED);
            g.drawString(errorMessage, 10, 60);
        }

        // Отрисовка стека использованных случайных чисел справа от треугольника
        drawRandomNumbersStack(g);
    }

    /**
     * Вычисляет новое положение точки на основе случайного числа.
     *
     * @param currentPoint Текущее положение точки
     * @param randomValue  Случайное число для определения направления движения
     * @return Новое положение точки
     */
    private Point calculateNewDotPosition(Point currentPoint, long randomValue) {
        long MinValue = MIN_RANDOM_VALUE;

        // Фиксированные вершины треугольника
        Point A = new Point(SIZE_WIDTH / 2, 0); // Верхняя вершина
        Point B = new Point(0, SIZE_HEIGHT); // Левый нижний угол
        Point C = new Point(SIZE_WIDTH, SIZE_HEIGHT); // Правый нижний угол

        long rangePart = (MAX_RANDOM_VALUE - MinValue) / 3; // Разделение диапазона на три части

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
     * Отрисовывает стек использованных случайных чисел справа от треугольника.
     *
     * @param g Объект Graphics для рисования
     */
    private void drawRandomNumbersStack(Graphics g) {
        g.setColor(Color.BLACK);

        // Получение параметров из конфигурации
        int maxColumns = MAX_COLUMNS;
        int maxRowsPerColumn = SIZE_HEIGHT / ROW_HEIGHT;

        // Определение начальной позиции для рисования чисел
        int startX = SIZE_WIDTH + 20; // Начальная позиция по оси X, справа от треугольника
        int startY = 20; // Начальная позиция по оси Y, сверху панели

        int column = maxColumns - 1; // Начинаем с самой правой колонки
        int row = 0; // Стартуем с самой верхней строки

        // Перебор использованных случайных чисел в обратном порядке для заполнения справа налево
        for (int i = usedRandomNumbers.size() - 1; i >= 0; i--) {
            Long randomValue = usedRandomNumbers.get(i);

            // Определение позиции для текущего числа
            int x = startX + column * (COLUMN_WIDTH + COLUMN_SPACING);
            int y = startY + row * ROW_HEIGHT;

            // Отрисовка числа
            g.drawString(randomValue.toString(), x, y);

            // Переход на следующую строку
            row++;

            // Если достигли конца текущей колонки, переходим к следующей колонке слева
            if (row >= maxRowsPerColumn) {
                row = 0;
                column--;

                // Если колонок больше не осталось, прекращаем отображение чисел
                if (column < 0) {
                    break;
                }
            }
        }
    }
}
