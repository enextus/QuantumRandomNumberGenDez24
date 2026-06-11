package org.ThreeDotsSierpinski.mode.physics;

import org.ThreeDotsSierpinski.rng.RNProvider;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.OptionalInt;

/**
 * QRNG nudges a logistic map; the chaotic state modulates a Lissajous figure.
 */
public class ChaosLissajousMode extends AbstractLissajousMode {

    private static final String ID = "chaos-lissajous";
    private static final String NAME = "Chaos Lissajous";
    private static final String ICON = "λ";
    private static final String DESCRIPTION = "QRNG подталкивает логистическое отображение, а его состояние управляет Лиссажу.\n"
            + "Связка QRNG → Logistic Map → Lissajous показывает переход от порядка к хаотическому рисунку.";

    private static final int SAMPLE_COUNT = 520;
    private static final double BASE_R = 3.72;
    private static final double R_JITTER = 0.26;
    private static final double BASE_A = 2.0;
    private static final double BASE_B = 3.0;

    private double logisticX = 0.41;
    private double r = BASE_R;
    private double phase;

    private static String fmt(double value) {
        return String.format(java.util.Locale.US, "%.3f", value);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public String getIcon() {
        return ICON;
    }

    @Override
    protected String subtitle() {
        return "QRNG → r jitter → xₙ₊₁=r·xₙ·(1-xₙ) → Lissajous modulation";
    }

    @Override
    public void initialize(BufferedImage canvas, int width, int height) {
        super.initialize(canvas, width, height);
        logisticX = 0.41;
        r = BASE_R;
        phase = 0.0;
    }

    @Override
    protected List<Point> stepLissajous(RNProvider provider, BufferedImage canvas, int dotSize) {
        OptionalInt rndR = nextRandom(provider);
        OptionalInt rndPhase = nextRandom(provider);
        if (rndR.isEmpty() || rndPhase.isEmpty()) {
            return List.of();
        }

        r = Math.clamp(BASE_R + signedUnit(rndR.getAsInt()) * R_JITTER, 3.25, 4.0);
        logisticX = Math.clamp(r * logisticX * (1.0 - logisticX), 0.0001, 0.9999);
        phase = (phase + 0.018 + signedUnit(rndPhase.getAsInt()) * 0.12) % TWO_PI;

        double a = BASE_A + logisticX * 7.0;
        double b = BASE_B + (1.0 - logisticX) * 7.0;

        fadeCanvas(canvas, 26);
        drawGrid(canvas, getName(), subtitle());

        Graphics2D g2d = canvas.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int[] xs = new int[SAMPLE_COUNT];
            int[] ys = new int[SAMPLE_COUNT];
            for (int i = 0; i < SAMPLE_COUNT; i++) {
                double t = TWO_PI * i / (SAMPLE_COUNT - 1.0);
                double envelope = 0.78 + 0.22 * Math.sin(logisticX * TWO_PI * 8.0 + t * 0.5);
                xs[i] = mapX(envelope * Math.sin(a * t + phase));
                ys[i] = mapY(envelope * Math.sin(b * t + logisticX * TWO_PI));
            }

            Color color = Color.getHSBColor((float) logisticX, 0.72f, 1.0f);
            drawPolyline(g2d, xs, ys, SAMPLE_COUNT, color);
            drawStatus(g2d, a, b);
        } finally {
            g2d.dispose();
        }

        frame++;
        return List.of();
    }

    private void drawStatus(Graphics2D g2d, double a, double b) {
        g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2d.setColor(TEXT);
        g2d.drawString("r=" + fmt(r) + "   x=" + fmt(logisticX) + "   a:b=" + fmt(a) + ":" + fmt(b), 16, 60);
    }
}
