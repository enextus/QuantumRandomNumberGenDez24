package org.ThreeDotsSierpinski.mode.physics;

/**
 * Dadras attractor: a compact butterfly-like chaotic oscillator.
 */
public class DadrasAttractorMode extends AbstractStrangeAttractorMode {

    @Override
    public String getId() {
        return "dadras-attractor";
    }

    @Override
    public String getName() {
        return "Dadras Attractor";
    }

    @Override
    public String getDescription() {
        return "Dadras attractor: компактная бабочка со смешанными xy/yz/xz\n"
                + "связями и живым QRNG-джиттером параметров.";
    }

    @Override
    public String getIcon() {
        return "⋈";
    }

    @Override
    protected double[] baseParameters() {
        return new double[]{3.00, 2.70, 1.70, 2.00, 9.00};
    }

    @Override
    protected double[] jitterAmplitudes() {
        return new double[]{0.012, 0.010, 0.008, 0.008, 0.020};
    }

    @Override
    protected double[] initialState() {
        return new double[]{0.10, 0.10, 0.10};
    }

    @Override
    protected void derivative(double x, double y, double z, double[] p, double[] out) {
        double a = p[0];
        double b = p[1];
        double c = p[2];
        double d = p[3];
        double e = p[4];

        out[0] = y - a * x + b * y * z;
        out[1] = c * y - x * z + z;
        out[2] = d * x * y - e * z;
    }

    @Override
    protected String formulaLabel() {
        return "Dadras: dx=y-a·x+b·y·z, dy=c·y-x·z+z, dz=d·x·y-e·z";
    }

    @Override
    protected double dt() {
        return 0.0035;
    }

    @Override
    protected double scale() {
        return 18.5;
    }

    @Override
    protected int stepsPerFrame() {
        return 1_300;
    }
}
