package org.ThreeDotsSierpinski.mode.physics;

/**
 * Thomas cyclically symmetric attractor.
 */
public class ThomasAttractorMode extends AbstractStrangeAttractorMode {

    @Override
    public String getId() {
        return "thomas-attractor";
    }

    @Override
    public String getName() {
        return "Thomas Attractor";
    }

    @Override
    public String getDescription() {
        return "Циклически симметричный странный аттрактор Томаса: sin-связи\n"
                + "между осями создают мягкий объёмный хаос.";
    }

    @Override
    public String getIcon() {
        return "✶";
    }

    @Override
    protected double[] baseParameters() {
        return new double[]{0.208186};
    }

    @Override
    protected double[] jitterAmplitudes() {
        return new double[]{0.0018};
    }

    @Override
    protected double[] initialState() {
        return new double[]{0.10, 0.00, -0.10};
    }

    @Override
    protected void derivative(double x, double y, double z, double[] p, double[] out) {
        double b = p[0];
        out[0] = Math.sin(y) - b * x;
        out[1] = Math.sin(z) - b * y;
        out[2] = Math.sin(x) - b * z;
    }

    @Override
    protected String formulaLabel() {
        return "Thomas: dx=sin(y)-b·x, dy=sin(z)-b·y, dz=sin(x)-b·z";
    }

    @Override
    protected double dt() {
        return 0.045;
    }

    @Override
    protected double scale() {
        return 82.0;
    }

    @Override
    protected int stepsPerFrame() {
        return 900;
    }

    @Override
    protected double maxAbsCoordinate() {
        return 12.0;
    }
}
