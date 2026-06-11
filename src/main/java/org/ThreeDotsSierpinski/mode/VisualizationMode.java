package org.ThreeDotsSierpinski.mode;

import org.ThreeDotsSierpinski.app.*;
import org.ThreeDotsSierpinski.config.*;
import org.ThreeDotsSierpinski.math.*;
import org.ThreeDotsSierpinski.mode.*;
import org.ThreeDotsSierpinski.model.*;
import org.ThreeDotsSierpinski.rng.*;
import org.ThreeDotsSierpinski.stats.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.*;

/**
 * Интерфейс для режимов визуализации случайных чисел.
 * Каждый режим:
 * - Получает случайные числа из RNProvider
 * - Рисует на BufferedImage
 * - Возвращает список нарисованных точек (для анимации RED→BLACK)
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
     * @return список точек, нарисованных свежим цветом
     */
    List<Point> step(RNProvider provider, BufferedImage canvas, int dotSize);

    /** Количество нарисованных точек с момента initialize() */
    int getPointCount();

    /** Количество потреблённых случайных чисел */
    int getRandomNumbersUsed();

    /**
     * Полная перерисовка текущего состояния без потребления новых случайных чисел.
     * Используется mode-specific UI controls, например для скрытия/показа
     * дополнительных визуальных слоёв.
     */
    default void redraw(BufferedImage canvas, int width, int height, int dotSize) {
        // Default no-op: режимы без собственного состояния не обязаны поддерживать redraw.
    }

    /**
     * Дополнительные UI-контролы, специфичные для конкретного режима визуализации.
     * Например, VoronoiMode может добавить переключатель показа меток центров.
     */
    default List<JComponent> createModeControls(DotController controller) {
        return List.of();
    }


    /**
     * Прямоугольные области внутри canvas, которые режим не должен
     * использовать для рисования собственных точек/объектов.
     * Координаты передаются в системе координат текущего canvas режима,
     * а не всего JPanel. По умолчанию режимы игнорируют reserved areas.
     */
    default void setReservedDrawingAreas(List<Rectangle> reservedAreas) {
        // Default no-op.
    }

    /**
     * Обработка кликов мыши по области визуализации.
     * Режимы, которым нужны canvas-hit areas или help overlays, могут переопределить этот hook.
     */
    default void handleMouseClicked(Point point, Component parent) {
        // Default no-op.
    }

    enum PointCounterOverlayPlacement {
        LEFT,
        RIGHT,
        TOP_CENTER
    }

    default boolean usesLeftPointCounterOverlay() {
        return false;
    }

    default PointCounterOverlayPlacement getPointCounterOverlayPlacement() {
        return usesLeftPointCounterOverlay()
                ? PointCounterOverlayPlacement.LEFT
                : PointCounterOverlayPlacement.RIGHT;
    }

    /**
     * Текущий визуальный стиль режима. DEFAULT сохраняет существующий UI.
     */
    default VisualizationStyle getVisualizationStyle() {
        return VisualizationStyle.DEFAULT;
    }

    /**
     * Нужна ли анимация свежих точек в stable-color.
     * True = Sierpinski-style; False = режим сам управляет цветами.
     */
    default boolean usesRecolorAnimation() { return true; }

    /**
     * Цвет, в который DotController перекрашивает свежие точки.
     */
    default Color getRecolorAnimationTargetColor() {
        return usesDarkBackground() ? Color.WHITE : Color.BLACK;
    }

    /**
     * Нужен ли чёрный/тёмный фон.
     */
    default boolean usesDarkBackground() { return false; }

    /**
     * Нужно ли показывать overlay таблицы потреблённых random numbers.
     */
    default boolean usesRandomNumbersStackOverlay() {
        return !usesDarkBackground();
    }

    /**
     * Реестр всех доступных режимов.
     * Для добавления нового — просто добавить в массив.
     */
    static VisualizationMode[] allModes() {
        return new VisualizationMode[] {
                new SierpinskiMode(),
                new VoronoiMode(),
                new BarnsleyFernMode(),
                new RandomWalkHeatmapMode(),
                new MonteCarloMandelbrotAreaMode(),
                new MonteCarloMandelbrot3DAreaMode(),
                new LorenzAttractor3DMode(),
                new MonteCarloPiMode(),
                new GaltonBoardMode(),
                new PercolationMode(),
                new ForestFireMode(),
                new SpectralPlotMode(),
                new ChaosGameRepresentationMode(),
                new DLAMode(),
                new BifurcationDiagramMode(),
        };
    }
}