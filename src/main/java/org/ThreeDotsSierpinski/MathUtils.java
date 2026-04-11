package org.ThreeDotsSierpinski;

/**
 * Математические утилиты для статистических тестов случайности.
 *
 * Содержит общие функции, используемые несколькими тестами
 * (FrequencyBitTest, RunsBitTest и др.).
 */
final class MathUtils {

    private MathUtils() {
        // Utility class — не инстанцируется
    }

    /**
     * Complementary error function: erfc(x) = 1 - erf(x).
     *
     * @param x аргумент
     * @return значение erfc(x)
     */
    static double erfc(double x) {
        return 1.0 - erf(x);
    }

    /**
     * Аппроксимация error function по формуле Абрамовица и Стегана (7.1.26).
     *
     * @param x аргумент
     * @return значение erf(x)
     * @see <a href="https://en.wikipedia.org/wiki/Error_function#Numerical_approximations">Wikipedia: Error function</a>
     */
    static double erf(double x) {
        double t = 1.0 / (1.0 + 0.5 * Math.abs(x));
        double tau = t * Math.exp(-x * x - 1.26551223
                + 1.00002368 * t
                + 0.37409196 * t * t
                + 0.09678418 * t * t * t
                - 0.18628806 * t * t * t * t
                + 0.27886807 * t * t * t * t * t
                - 1.13520398 * t * t * t * t * t * t
                + 1.48851587 * t * t * t * t * t * t * t
                - 0.82215223 * t * t * t * t * t * t * t * t
                + 0.17087277 * t * t * t * t * t * t * t * t * t);
        return x >= 0 ? 1 - tau : tau - 1;
    }
}
