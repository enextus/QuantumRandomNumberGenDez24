package org.ThreeDotsSierpinski.mode.physics;

/**
 * Aizawa attractor: a toroidal chaotic flow with a bright central cavity.
 */
public class AizawaAttractorMode extends AbstractStrangeAttractorMode {

    @Override
    public String getId() {
        return "aizawa-attractor";
    }

    @Override
    public String getName() {
        return "Aizawa Attractor";
    }

    @Override
    public String getDescription() {
        return "Тороидальный странный аттрактор: QRNG слегка подталкивает параметры,\n"
                + "а траектория наматывается вокруг светящейся хаотической полости.";
    }

    @Override
    public String getIcon() {
        return "◎";
    }

    @Override
    protected double[] baseParameters() {
        return new double[]{0.95, 0.70, 0.60, 3.50, 0.25, 0.10};
    }

    @Override
    protected double[] jitterAmplitudes() {
        return new double[]{0.010, 0.006, 0.006, 0.018, 0.004, 0.003};
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
        double d = p[3];
        double e = p[4];
        double f = p[5];
        double radiusSquared = x * x + y * y;

        out[0] = (z - b) * x - d * y;
        out[1] = d * x + (z - b) * y;
        out[2] = c + a * z - (z * z * z) / 3.0 - radiusSquared * (1.0 + e * z) + f * z * x * x * x;
    }

    @Override
    protected String formulaLabel() {
        return "Aizawa: dx=(z-b)x-dy, dy=dx+(z-b)y";
    }

    @Override
    protected double dt() {
        return 0.0045;
    }

    @Override
    protected double scale() {
        return 145.0;
    }

    @Override
    protected int stepsPerFrame() {
        return 1_250;
    }
}
