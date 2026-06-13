package org.ThreeDotsSierpinski.mode.physics;

import org.ThreeDotsSierpinski.rng.RNProvider;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.OptionalInt;

/**
 * Side-by-side Lissajous comparison: QRNG-driven modulation versus a local LCG pseudo stream.
 */
public class LissajousQuantumVsPseudoMode extends AbstractLissajousMode {

    private static final String ID = "lissajous-quantum-vs-pseudo";
    private static final String NAME = "Lissajous Quantum vs Pseudo";
    private static final String ICON = "⇄";
    private static final String DESCRIPTION = "Сравнение двух Лиссажу-осциллографов: слева QRNG, справа локальный LCG pseudo stream.\n"
            + "Одинаковая модель получает разные источники микроджиттера и постепенно расходится.";

    private static final int SAMPLE_COUNT = 420;
    private static final double BASE_A = 3.0;
    private static final double BASE_B = 4.0;
    private static final double JITTER = 0.050;

    private long lcgState = 0x5DEECE66DL;
    private double qa = BASE_A;
    private double qb = BASE_B;
    private double qPhase = 0.4;
    private double pa = BASE_A;
    private double pb = BASE_B;
    private double pPhase = 0.4;

    private static void drawPanelFrame(Graphics2D g2d, Rectangle rect, String label) {
        g2d.setColor(GRID_BRIGHT);
        g2d.drawRect(rect.x, rect.y, rect.width, rect.height);
        g2d.setColor(MUTED_TEXT);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2d.drawString(label, rect.x + 8, rect.y + 16);
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
        return "Left: QRNG jitter   |   Right: deterministic LCG jitter";
    }

    @Override
    public void initialize(BufferedImage canvas, int width, int height) {
        super.initialize(canvas, width, height);
        lcgState = 0x5DEECE66DL;
        qa = BASE_A;
        qb = BASE_B;
        qPhase = 0.4;
        pa = BASE_A;
        pb = BASE_B;
        pPhase = 0.4;
    }

    @Override
    protected List<Point> stepLissajous(RNProvider provider, BufferedImage canvas, int dotSize) {
        OptionalInt rndA = nextRandom(provider);
        OptionalInt rndB = nextRandom(provider);
        OptionalInt rndPhase = nextRandom(provider);
        if (rndA.isEmpty() || rndB.isEmpty() || rndPhase.isEmpty()) {
            return List.of();
        }

        qa = Math.clamp(qa + signedUnit(rndA.getAsInt()) * JITTER, 1.0, 9.0);
        qb = Math.clamp(qb + signedUnit(rndB.getAsInt()) * JITTER, 1.0, 9.0);
        qPhase = (qPhase + 0.022 + signedUnit(rndPhase.getAsInt()) * JITTER) % TWO_PI;

        pa = Math.clamp(pa + pseudoSigned() * JITTER, 1.0, 9.0);
        pb = Math.clamp(pb + pseudoSigned() * JITTER, 1.0, 9.0);
        pPhase = (pPhase + 0.022 + pseudoSigned() * JITTER) % TWO_PI;

        fadeCanvas(canvas, 40);
        drawGrid(canvas, getName(), subtitle());

        Graphics2D g2d = canvas.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Rectangle left = new Rectangle(24, 58, Math.max(10, width / 2 - 36), Math.max(10, height - 96));
            Rectangle right = new Rectangle(width / 2 + 12, 58, Math.max(10, width / 2 - 36), Math.max(10, height - 96));
            drawPanelFrame(g2d, left, "QUANTUM");
            drawPanelFrame(g2d, right, "PSEUDO LCG");
            drawCurve(g2d, left, qa, qb, qPhase, CYAN);
            drawCurve(g2d, right, pa, pb, pPhase, AMBER);
        } finally {
            g2d.dispose();
        }

        frame++;
        return List.of();
    }

    private void drawCurve(Graphics2D g2d, Rectangle rect, double a, double b, double phase, Color color) {
        int[] xs = new int[SAMPLE_COUNT];
        int[] ys = new int[SAMPLE_COUNT];
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            double t = TWO_PI * i / (SAMPLE_COUNT - 1.0);
            xs[i] = mapXInRect(rect, Math.sin(a * t + phase));
            ys[i] = mapYInRect(rect, Math.sin(b * t));
        }
        drawPolyline(g2d, xs, ys, SAMPLE_COUNT, color);
    }

    private double pseudoSigned() {
        lcgState = (lcgState * 25214903917L + 11L) & ((1L << 48) - 1L);
        int value = (int) ((lcgState >>> 16) & 0xFFFF);
        return org.ThreeDotsSierpinski.mode.RngSampler.toSignedUnit(value);
    }
}
