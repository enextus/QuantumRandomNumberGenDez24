package org.ThreeDotsSierpinski.mode.physics;

/**
 * Halvorsen attractor: a three-wing chaotic flow with quadratic coupling.
 */
public class HalvorsenAttractorMode extends AbstractStrangeAttractorMode {

    @Override
    public String getId() {
        return "halvorsen-attractor";
    }

    @Override
    public String getName() {
        return "Halvorsen Attractor";
    }

    @Override
    public String getDescription() {
        return "Трёхкрылый Halvorsen attractor: квадратичные связи рождают\n"
                + "агрессивную симметричную хаотическую структуру.";
    }

    @Override
    public String getIcon() {
        return "✣";
    }

    @Override
    protected double[] baseParameters() {
        return new double[]{1.40};
    }

    @Override
    protected double[] jitterAmplitudes() {
        return new double[]{0.006};
    }

    @Override
    protected double[] initialState() {
        return new double[]{0.10, 0.00, 0.00};
    }

    @Override
    protected void derivative(double x, double y, double z, double[] p, double[] out) {
        double a = p[0];
        out[0] = -a * x - 4.0 * y - 4.0 * z - y * y;
        out[1] = -a * y - 4.0 * z - 4.0 * x - z * z;
        out[2] = -a * z - 4.0 * x - 4.0 * y - x * x;
    }

    @Override
    protected String formulaLabel() {
        return "Halvorsen: dx=-a·x-4y-4z-y², cyclic permutations";
    }

    @Override
    protected double dt() {
        return 0.0038;
    }

    @Override
    protected double scale() {
        return 32.0;
    }

    @Override
    protected int stepsPerFrame() {
        return 1_350;
    }

    @Override
    protected double maxAbsCoordinate() {
        return 40.0;
    }
}
