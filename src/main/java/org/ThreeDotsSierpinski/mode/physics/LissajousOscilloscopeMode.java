package org.ThreeDotsSierpinski.mode.physics;

import org.ThreeDotsSierpinski.rng.RNProvider;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.OptionalInt;

/**
 * Live oscilloscope mode: QRNG gently modulates the frequencies and phase.
 */
public class LissajousOscilloscopeMode extends AbstractLissajousMode {

    private static final String ID = "lissajous-oscilloscope";
    private static final String NAME = "Lissajous Oscilloscope";
    private static final String ICON = "◌";
    private static final String DESCRIPTION = "Живой осциллограф Лиссажу: QRNG слегка меняет частоты и фазу.\n"
            + "Фигура не пересоздаётся, а дышит и дрейфует во времени.";

    private static final int SAMPLE_COUNT = 540;
    private static final double BASE_A = 3.0;
    private static final double BASE_B = 2.0;
    private static final double MAX_JITTER = 0.060;
    private static final double PHASE_SPEED = 0.030;

    private double a = BASE_A;
    private double b = BASE_B;
    private double phase = Math.PI / 3.0;

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
        return "Live QRNG micro-jitter applied to a, b and δ";
    }

    @Override
    public void initialize(BufferedImage canvas, int width, int height) {
        super.initialize(canvas, width, height);
        a = BASE_A;
        b = BASE_B;
        phase = Math.PI / 3.0;
    }

    @Override
    protected List<Point> stepLissajous(RNProvider provider, BufferedImage canvas, int dotSize) {
        OptionalInt rndA = nextRandom(provider);
        OptionalInt rndB = nextRandom(provider);
        OptionalInt rndPhase = nextRandom(provider);
        if (rndA.isEmpty() || rndB.isEmpty() || rndPhase.isEmpty()) {
            return List.of();
        }

        a = Math.clamp(a + signedUnit(rndA.getAsInt()) * MAX_JITTER, 1.0, 8.0);
        b = Math.clamp(b + signedUnit(rndB.getAsInt()) * MAX_JITTER, 1.0, 8.0);
        phase = (phase + PHASE_SPEED + signedUnit(rndPhase.getAsInt()) * MAX_JITTER) % TWO_PI;

        fadeCanvas(canvas, 34);
        drawGrid(canvas, getName(), subtitle());

        Graphics2D g2d = canvas.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int[] xs = new int[SAMPLE_COUNT];
            int[] ys = new int[SAMPLE_COUNT];
            double localTime = frame * 0.010;
            for (int i = 0; i < SAMPLE_COUNT; i++) {
                double t = TWO_PI * i / (SAMPLE_COUNT - 1.0);
                xs[i] = mapX(Math.sin(a * t + phase + localTime));
                ys[i] = mapY(Math.sin(b * t));
            }
            drawPolyline(g2d, xs, ys, SAMPLE_COUNT, CYAN);
            drawStatus(g2d);
        } finally {
            g2d.dispose();
        }

        frame++;
        return List.of();
    }

    private void drawStatus(Graphics2D g2d) {
        g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2d.setColor(TEXT);
        g2d.drawString("a=" + format(a) + "   b=" + format(b) + "   δ=" + format(Math.toDegrees(phase)) + "°", 16, 60);
    }

    private static String format(double value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }
}
