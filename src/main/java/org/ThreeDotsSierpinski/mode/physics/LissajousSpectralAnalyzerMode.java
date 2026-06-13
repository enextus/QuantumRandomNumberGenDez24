package org.ThreeDotsSierpinski.mode.physics;

import org.ThreeDotsSierpinski.mode.RngStepBudget;
import org.ThreeDotsSierpinski.rng.RNProvider;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.OptionalInt;

/**
 * Spectral-style Lissajous analyzer: consecutive random values perturb x/y phases.
 */
public class LissajousSpectralAnalyzerMode extends AbstractLissajousMode {

    private static final String ID = "lissajous-spectral-analyzer";
    private static final String NAME = "Lissajous Spectral Analyzer";
    private static final String ICON = "≋";
    private static final String DESCRIPTION = "Поток random numbers используется как фазовый шум для пары синусоид.\n"
            + "Плохие генераторы дают устойчивые полосы и повторяемые мотивы, хороший поток — ровное заполнение.";

    private static final int POINTS_PER_STEP = 1_300;
    private static final int QUANTUM_POINTS_PER_STEP = 16;
    private static final double BASE_A = 7.0;
    private static final double BASE_B = 11.0;
    private static final double PHASE_NOISE = Math.PI;

    private double t;

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
        return "Consecutive random values perturb x/y phase in a Lissajous scan";
    }

    @Override
    public void initialize(BufferedImage canvas, int width, int height) {
        super.initialize(canvas, width, height);
        t = 0.0;
    }

    @Override
    protected List<Point> stepLissajous(RNProvider provider, BufferedImage canvas, int dotSize) {
        fadeCanvas(canvas, 12);
        drawGrid(canvas, getName(), subtitle());

        Graphics2D g2d = canvas.createGraphics();
        try {
            int pointsThisStep = RngStepBudget.forProvider(provider, POINTS_PER_STEP, QUANTUM_POINTS_PER_STEP);

            for (int i = 0; i < pointsThisStep; i++) {
                OptionalInt rndX = nextRandom(provider);
                OptionalInt rndY = nextRandom(provider);
                if (rndX.isEmpty() || rndY.isEmpty()) {
                    break;
                }

                double xNoise = signedUnit(rndX.getAsInt()) * PHASE_NOISE;
                double yNoise = signedUnit(rndY.getAsInt()) * PHASE_NOISE;
                double x = Math.sin(BASE_A * t + xNoise);
                double y = Math.sin(BASE_B * t + yNoise);

                int px = mapX(x);
                int py = mapY(y);
                Color color = Color.getHSBColor((float) ((unit(rndX.getAsInt()) + unit(rndY.getAsInt())) * 0.5), 0.55f, 1.0f);
                drawDot(g2d, px, py, Math.max(1, dotSize), color);

                t += 0.010;
            }

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
        g2d.drawString("x=sin(7t+rndₙ), y=sin(11t+rndₙ₊₁)", 16, 60);
    }
}
