package org.ThreeDotsSierpinski.mode.physics;

/**
 * Rössler attractor: a classic folded spiral chaotic flow.
 */
public class RosslerAttractorMode extends AbstractStrangeAttractorMode {

    @Override
    public String getId() {
        return "rossler-attractor";
    }

    @Override
    public String getName() {
        return "Rössler Attractor";
    }

    @Override
    public String getDescription() {
        return "Классический Rössler attractor: траектория вращается почти как диск,\n"
                + "а затем складывается в хаотическую ленту.";
    }

    @Override
    public String getIcon() {
        return "↻";
    }

    @Override
    protected double[] baseParameters() {
        return new double[]{0.20, 0.20, 5.70};
    }

    @Override
    protected double[] jitterAmplitudes() {
        return new double[]{0.003, 0.003, 0.020};
    }

    @Override
    protected double[] initialState() {
        return new double[]{0.10, 0.00, 0.00};
    }

    @Override
    protected void derivative(double x, double y, double z, double[] p, double[] out) {
        double a = p[0];
        double b = p[1];
        double c = p[2];

        out[0] = -y - z;
        out[1] = x + a * y;
        out[2] = b + z * (x - c);
    }

    @Override
    protected String formulaLabel() {
        return "Rössler: dx=-y-z, dy=x+a·y, dz=b+z(x-c)";
    }

    @Override
    protected double dt() {
        return 0.010;
    }

    @Override
    protected double scale() {
        return 18.0;
    }

    @Override
    protected int stepsPerFrame() {
        return 1_200;
    }

    @Override
    protected double initialTilt() {
        return 0.82;
    }
}
