package org.ThreeDotsSierpinski;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Интерфейс для режимов визуализации случайных чисел.
 *
 * Каждый режим:
 * - Получает случайные числа из RNProvider
 * - Рисует на BufferedImage
 * - Возвращает список нарисованных точек (для анимации RED→BLACK)
 *
 * Для добавления нового режима:
 * 1. Создать класс, реализующий этот интерфейс
 * 2. Зарегистрировать в {@link VisualizationMode#allModes()}
 */
public interface VisualizationMode {

    /** Уникальный идентификатор режима (для конфига) */
    String getId();

    /** Человекочитаемое название */
    String getName();

    /** Краткое описание (1-2 строки) */
    String getDescription();

    /** Эмодзи или символ для карточки выбора */
    String getIcon();

    /**
     * Инициализация. Вызывается один раз перед началом анимации.
     *
     * @param canvas    изображение для рисования
     * @param width     ширина области
     * @param height    высота области
     */
    void initialize(BufferedImage canvas, int width, int height);

    /**
     * Один шаг анимации. Потребляет случайные числа, рисует на canvas.
     * Вызывается из EDT (Swing Timer) — безопасен для Swing.
     *
     * @param provider источник случайных чисел
     * @param canvas   изображение для рисования
     * @param dotSize  размер точки (из конфига)
     * @return список точек, нарисованных красным (для последующей перекраски в чёрный)
     */
    List<Point> step(RNProvider provider, BufferedImage canvas, int dotSize);

    /** Количество нарисованных точек с момента initialize() */
    int getPointCount();

    /** Количество потреблённых случайных чисел */
    int getRandomNumbersUsed();

    /**
     * Реестр всех доступных режимов.
     * Для добавления нового — просто добавить в массив.
     */
    static VisualizationMode[] allModes() {
        return new VisualizationMode[] {
                new SierpinskiMode(),
                new DLAMode(),
                // Добавьте новые режимы здесь:
                // new PercolationMode(),
                // new BlueNoiseMode(),
                // new VoronoiMode(),
        };
    }
}
