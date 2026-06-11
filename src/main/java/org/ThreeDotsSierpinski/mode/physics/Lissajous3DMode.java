package org.ThreeDotsSierpinski.mode.physics;

import org.ThreeDotsSierpinski.rng.RNProvider;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.OptionalInt;

/**
 * 3D Lissajous knot projected to 2D with a slowly rotating camera.
 */
public class Lissajous3DMode extends AbstractLissajousMode {

    private static final String ID = "lissajous-3d";
    private static final String NAME = "Lissajous 3D Knot";
    private static final String ICON = "⌘";
    private static final String DESCRIPTION = "3D-фигура Лиссажу: x/y/z — три синусоидальных колебания.\n"
            + "QRNG выбирает частоты a/b/c и фазу, а камера медленно вращает узел.";

    private static final int SAMPLE_COUNT = 900;
    private static final int MIN_FREQUENCY = 1;
    private static final int FREQUENCY_SPAN = 9;
    private static final double CAMERA_DISTANCE = 3.2;
    private static final double FOV_SCALE = 1.25;

    private int a = 3;
    private int b = 4;
    private int c = 5;
    private double phase = 0.7;

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
        return "x=sin(a·t+δ), y=sin(b·t), z=sin(c·t+δ/2)";
    }

    @Override
    protected List<Point> stepLissajous(RNProvider provider, BufferedImage canvas, int dotSize) {
        OptionalInt rndA = nextRandom(provider);
        OptionalInt rndB = nextRandom(provider);
        OptionalInt rndC = nextRandom(provider);
        OptionalInt rndPhase = nextRandom(provider);
        if (rndA.isEmpty() || rndB.isEmpty() || rndC.isEmpty() || rndPhase.isEmpty()) {
            return List.of();
        }

        a = MIN_FREQUENCY + Math.floorMod(rndA.getAsInt(), FREQUENCY_SPAN);
        b = MIN_FREQUENCY + Math.floorMod(rndB.getAsInt(), FREQUENCY_SPAN);
        c = MIN_FREQUENCY + Math.floorMod(rndC.getAsInt(), FREQUENCY_SPAN);
        phase = unit(rndPhase.getAsInt()) * TWO_PI;

        fadeCanvas(canvas, 28);
        drawGrid(canvas, getName(), subtitle());

        Graphics2D g2d = canvas.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int[] xs = new int[SAMPLE_COUNT];
            int[] ys = new int[SAMPLE_COUNT];
            double yaw = frame * 0.025;
            double pitch = 0.62 + Math.sin(frame * 0.012) * 0.18;

            for (int i = 0; i < SAMPLE_COUNT; i++) {
                double t = TWO_PI * i / (SAMPLE_COUNT - 1.0);
                double x = Math.sin(a * t + phase);
                double y = Math.sin(b * t);
                double z = Math.sin(c * t + phase * 0.5);

                double x1 = x * Math.cos(yaw) + z * Math.sin(yaw);
                double z1 = -x * Math.sin(yaw) + z * Math.cos(yaw);
                double y1 = y * Math.cos(pitch) - z1 * Math.sin(pitch);
                double z2 = y * Math.sin(pitch) + z1 * Math.cos(pitch);

                double perspective = FOV_SCALE / (CAMERA_DISTANCE - z2 * 0.75);
                xs[i] = mapX(x1 * perspective);
                ys[i] = mapY(y1 * perspective);
            }

            drawPolyline(g2d, xs, ys, SAMPLE_COUNT, GREEN);
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
        g2d.drawString("a:b:c = " + a + ":" + b + ":" + c + "   camera spin", 16, 60);
    }
}
