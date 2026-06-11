package org.ThreeDotsSierpinski.mode.physics;

import org.ThreeDotsSierpinski.rng.RNProvider;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.OptionalInt;

/**
 * QRNG picks the Lissajous frequency pair and phase shift for each new figure.
 */
public class LissajousFrequencyMode extends AbstractLissajousMode {

    private static final String ID = "lissajous-frequency";
    private static final String NAME = "Lissajous Frequencies";
    private static final String ICON = "∞";
    private static final String DESCRIPTION = "QRNG выбирает частоты a/b и фазу δ для классической фигуры Лиссажу.\n"
            + "Каждый новый batch показывает другую связь двух гармонических колебаний.";

    private static final int MIN_FREQUENCY = 1;
    private static final int FREQUENCY_SPAN = 20;
    private static final int SAMPLE_COUNT = 720;
    private static final int CURVES_PER_STEP = 1;

    private static void drawStatus(Graphics2D g2d, int a, int b, double phase) {
        g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2d.setColor(TEXT);
        g2d.drawString("a:b = " + a + ":" + b + "   δ = " + Math.round(Math.toDegrees(phase)) + "°", 16, 60);
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
        return "QRNG → frequency ratio a:b and phase δ";
    }

    @Override
    protected List<Point> stepLissajous(RNProvider provider, BufferedImage canvas, int dotSize) {
        Graphics2D g2d = canvas.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            fadeCanvas(canvas, 22);
            drawGrid(canvas, getName(), subtitle());

            for (int curve = 0; curve < CURVES_PER_STEP; curve++) {
                OptionalInt rndA = nextRandom(provider);
                OptionalInt rndB = nextRandom(provider);
                OptionalInt rndPhase = nextRandom(provider);
                if (rndA.isEmpty() || rndB.isEmpty() || rndPhase.isEmpty()) {
                    return List.of();
                }

                int a = MIN_FREQUENCY + Math.floorMod(rndA.getAsInt(), FREQUENCY_SPAN);
                int b = MIN_FREQUENCY + Math.floorMod(rndB.getAsInt(), FREQUENCY_SPAN);
                double phase = unit(rndPhase.getAsInt()) * TWO_PI;

                int[] xs = new int[SAMPLE_COUNT];
                int[] ys = new int[SAMPLE_COUNT];
                for (int i = 0; i < SAMPLE_COUNT; i++) {
                    double t = TWO_PI * i / (SAMPLE_COUNT - 1.0);
                    xs[i] = mapX(Math.sin(a * t + phase));
                    ys[i] = mapY(Math.sin(b * t));
                }

                float hue = (float) ((a * 0.037 + b * 0.021 + phase / TWO_PI) % 1.0);
                drawPolyline(g2d, xs, ys, SAMPLE_COUNT, Color.getHSBColor(hue, 0.58f, 1.0f));
                drawStatus(g2d, a, b, phase);
            }
        } finally {
            g2d.dispose();
        }

        frame++;
        return List.of();
    }
}
