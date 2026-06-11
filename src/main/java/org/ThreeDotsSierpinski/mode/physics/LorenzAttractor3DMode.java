package org.ThreeDotsSierpinski.mode.physics;

import org.ThreeDotsSierpinski.mode.*;
import org.ThreeDotsSierpinski.mode.chaos.*;
import org.ThreeDotsSierpinski.mode.montecarlo.*;
import org.ThreeDotsSierpinski.mode.physics.*;
import org.ThreeDotsSierpinski.mode.stochastic.*;

import org.ThreeDotsSierpinski.app.*;
import org.ThreeDotsSierpinski.config.*;
import org.ThreeDotsSierpinski.math.*;
import org.ThreeDotsSierpinski.model.*;
import org.ThreeDotsSierpinski.rng.*;
import org.ThreeDotsSierpinski.stats.*;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;

/**
 * 3D Lorenz strange attractor visualization.
 * <p>
 * This mode renders an ensemble of nearby Lorenz trajectories. The particles
 * start almost at the same point, then diverge while still forming the iconic
 * butterfly-shaped attractor. A small QRNG-driven jitter is applied to the
 * Lorenz parameters around their classical base values each frame.
 * <p>
 * Rendering uses the same high-performance idea as the 3D Mandelbrot mode:
 * model-space trail buffers are reprojected by a rotating camera into an
 * {@code int[]} accumulation buffer and tone-mapped into a glowing image.
 */
@SuppressWarnings("DuplicatedCode")
public class LorenzAttractor3DMode implements VisualizationMode {

    private static final String ID = "lorenz-attractor-3d";
    private static final String NAME = "Lorenz Attractor 3D";
    private static final String DESCRIPTION =
            "Несколько почти совпадающих траекторий расходятся и образуют бабочку Лоренца.\n"
                    + "QRNG добавляет микроджиттер параметров σ/ρ/β — хаос остаётся живым.";
    private static final String ICON = "∞";

    // --- Random source mapping ------------------------------------------------
    private static final double RANDOM_MAX = 65_535.0;
    private static final int RANDOM_RANGE = 65_536;

    // --- Lorenz system --------------------------------------------------------
    private static final double BASE_SIGMA = 10.0;
    private static final double BASE_RHO = 28.0;
    private static final double BASE_BETA = 8.0 / 3.0;

    private static final double MAX_SIGMA_JITTER = 0.070;
    private static final double MAX_RHO_JITTER = 0.260;
    private static final double MAX_BETA_JITTER = 0.025;

    private static final int DEFAULT_JITTER_SLIDER = 45;
    private static final int MIN_JITTER_SLIDER = 0;
    private static final int MAX_JITTER_SLIDER = 100;

    private static final double DT = 0.0065;
    private static final int RK4_STEPS_PER_FRAME = 5;
    private static final int WARMUP_STEPS = 900;

    // --- Particle ensemble / trails -----------------------------------------
    private static final int PARTICLE_COUNT = 20;
    private static final int TRAIL_CAPACITY = 2_200;
    private static final double INITIAL_X = 0.1;
    private static final double INITIAL_Y = 0.0;
    private static final double INITIAL_Z = 0.0;
    private static final double INITIAL_SPREAD = 0.00035;

    // --- Model normalization and camera --------------------------------------
    private static final double MODEL_XY_SCALE = 30.0;
    private static final double MODEL_Z_CENTER = 25.0;
    private static final double MODEL_Z_SCALE = 30.0;
    private static final double CAM_DISTANCE = 3.25;
    private static final double PIXEL_SCALE_FACTOR = 0.66;
    private static final double YAW_SPEED = 0.42;
    private static final double DEFAULT_PITCH_DEG = 58.0;
    private static final int MIN_PITCH_DEG = 18;
    private static final int MAX_PITCH_DEG = 82;

    // --- Additive rendering ---------------------------------------------------
    private static final int TONE_MAX = 4_200;
    private static final double TONE_EXPOSURE = 155.0;
    private static final double LINE_WEIGHT = 0.36;
    private static final int BASE_R = 3;
    private static final int BASE_G = 7;
    private static final int BASE_B = 15;
    private static final int COLOR_LUT_SIZE = 256;
    private static final int SPREAD_DECIMAL_DIGITS = 4;
    private static final String SPREAD_FORMAT = "%." + SPREAD_DECIMAL_DIGITS + "f";
    private static final double RK4_HALF_DT = DT * 0.5;
    private static final double RK4_DT_SIXTH = DT / 6.0;

    // --- Dashboard layout -----------------------------------------------------
    private static final int HEADER_HEIGHT = 96;
    private static final int OUTER_PADDING = 18;
    private static final int PANEL_INSET = 16;
    private static final int PANEL_RADIUS = 22;
    private static final int STAT_CARD_HEIGHT = 74;
    private static final int STAT_CARD_GAP = 10;
    private static final int STAT_CARD_COUNT = 6;
    private static final int CONTROL_HEIGHT = 28;
    private static final int RESET_BUTTON_WIDTH = 82;
    private static final int TILT_SLIDER_WIDTH = 140;
    private static final int JITTER_SLIDER_WIDTH = 130;

    private static final String RESET_TEXT = "Reset";
    private static final String SPIN_TEXT = "Spin";
    private static final String JITTER_TEXT = "QRNG jitter";
    private static final String JITTER_LABEL_TEXT = "Jitter";
    private static final String TILT_LABEL_TEXT = "Tilt";

    private static final Color BACKGROUND = new Color(2, 7, 14);
    private static final Color PANEL_BACKGROUND = new Color(8, 16, 30);
    private static final Color PANEL_BORDER = new Color(55, 90, 130, 180);
    private static final Color BOX_COLOR = new Color(90, 130, 175, 120);

    private static final Color TITLE_COLOR = new Color(238, 244, 252);
    private static final Color TEXT_PRIMARY = new Color(224, 235, 248);
    private static final Color TEXT_MUTED = new Color(140, 160, 190);
    private static final Color TEXT_DIM = new Color(90, 110, 136);

    private static final Color SPREAD_COLOR = new Color(120, 255, 205);
    private static final Color JITTER_COLOR = new Color(255, 215, 110);
    private static final Color PARAM_COLOR = new Color(120, 180, 255);

    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 29);
    private static final Font SUBTITLE_FONT = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font PANEL_TITLE_FONT = new Font("SansSerif", Font.BOLD, 18);
    private static final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font VALUE_FONT = new Font("SansSerif", Font.BOLD, 18);
    private static final Font SMALL_FONT = new Font("SansSerif", Font.PLAIN, 11);

    // --- Mutable application state -------------------------------------------
    private int width;
    private int height;

    private int pointCount;
    private int randomNumbersUsed;

    private final double[] x = new double[PARTICLE_COUNT];
    private final double[] y = new double[PARTICLE_COUNT];
    private final double[] z = new double[PARTICLE_COUNT];

    private float[] trailX;
    private float[] trailY;
    private float[] trailZ;
    private int[] trailColor;
    private int[] trailWritePos;
    private int[] trailCount;

    private boolean spinning = true;
    private boolean jitterEnabled = true;
    private double yaw;
    private double pitch = Math.toRadians(DEFAULT_PITCH_DEG);
    private long lastNanos;
    private double jitterScale = DEFAULT_JITTER_SLIDER / 100.0;

    private double currentSigma = BASE_SIGMA;
    private double currentRho = BASE_RHO;
    private double currentBeta = BASE_BETA;
    private double spread;

    private int[] colorLut;
    private int[] toneLut;

    private BufferedImage plotImage;
    private int[] plotPixels;
    private int[] accumR;
    private int[] accumG;
    private int[] accumB;
    private int plotW;
    private int plotH;

    private BufferedImage chromeLayer;

    private Rectangle plotBounds = new Rectangle();
    private Rectangle panelBounds = new Rectangle();
    private final Rectangle[] statCards = new Rectangle[STAT_CARD_COUNT];

    private DotController controller;

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
    public boolean usesRecolorAnimation() {
        return false;
    }

    @Override
    public boolean usesDarkBackground() {
        return true;
    }

    @Override
    public List<JComponent> createModeControls(DotController controller) {
        this.controller = controller;

        JButton resetButton = new JButton(RESET_TEXT);
        resetButton.setPreferredSize(new Dimension(RESET_BUTTON_WIDTH, CONTROL_HEIGHT));
        resetButton.addActionListener(ignored -> restart());

        JCheckBox spinCheckBox = new JCheckBox(SPIN_TEXT, spinning);
        spinCheckBox.addActionListener(ignored -> {
            spinning = spinCheckBox.isSelected();
            lastNanos = System.nanoTime();
        });

        JCheckBox jitterCheckBox = new JCheckBox(JITTER_TEXT, jitterEnabled);
        jitterCheckBox.addActionListener(ignored -> jitterEnabled = jitterCheckBox.isSelected());

        JLabel jitterLabel = new JLabel(JITTER_LABEL_TEXT);
        JSlider jitterSlider = new JSlider(MIN_JITTER_SLIDER, MAX_JITTER_SLIDER, DEFAULT_JITTER_SLIDER);
        jitterSlider.setPreferredSize(new Dimension(JITTER_SLIDER_WIDTH, CONTROL_HEIGHT));
        jitterSlider.addChangeListener(ignored -> jitterScale = jitterSlider.getValue() / 100.0);

        JLabel tiltLabel = new JLabel(TILT_LABEL_TEXT);
        JSlider tiltSlider = new JSlider(MIN_PITCH_DEG, MAX_PITCH_DEG, (int) Math.round(DEFAULT_PITCH_DEG));
        tiltSlider.setPreferredSize(new Dimension(TILT_SLIDER_WIDTH, CONTROL_HEIGHT));
        tiltSlider.addChangeListener(ignored -> {
            pitch = Math.toRadians(tiltSlider.getValue());
            refreshController();
        });

        return List.of(resetButton, spinCheckBox, jitterCheckBox, jitterLabel, jitterSlider, tiltLabel, tiltSlider);
    }

    private void restart() {
        resetState();
        layoutDashboard();
        refreshController();
    }

    @Override
    public void initialize(BufferedImage canvas, int width, int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);

        layoutDashboard();
        resetState();
        render(canvas);
    }

    @Override
    public List<Point> step(RNProvider provider, BufferedImage canvas, int dotSize) {
        ensureInitialized(canvas);

        updateJitteredParameters(provider);
        for (int step = 0; step < RK4_STEPS_PER_FRAME; step++) {
            for (int particle = 0; particle < PARTICLE_COUNT; particle++) {
                rk4Step(particle, currentSigma, currentRho, currentBeta);
                storeTrailPoint(particle);
                pointCount++;
            }
        }
        spread = computeSpread();

        render(canvas);
        return List.of();
    }

    @Override
    public void redraw(BufferedImage canvas, int width, int height, int dotSize) {
        if (canvas == null) {
            return;
        }

        if (this.width != width || this.height != height || plotImage == null) {
            this.width = Math.max(1, width);
            this.height = Math.max(1, height);
            layoutDashboard();
            ensureTrailBuffers();
            ensureLuts();
        }

        render(canvas);
    }

    @Override
    public int getPointCount() {
        return pointCount;
    }

    @Override
    public int getRandomNumbersUsed() {
        return randomNumbersUsed;
    }

    private void refreshController() {
        if (controller != null) {
            controller.refreshVisualization();
        }
    }

    private void ensureInitialized(BufferedImage canvas) {
        if (plotImage == null || width != canvas.getWidth() || height != canvas.getHeight()) {
            initialize(canvas, canvas.getWidth(), canvas.getHeight());
        }
    }

    private void resetState() {
        pointCount = 0;
        randomNumbersUsed = 0;
        yaw = 0.0;
        spread = 0.0;
        currentSigma = BASE_SIGMA;
        currentRho = BASE_RHO;
        currentBeta = BASE_BETA;
        lastNanos = System.nanoTime();

        ensureTrailBuffers();
        ensureLuts();
        Arrays.fill(trailWritePos, 0);
        Arrays.fill(trailCount, 0);

        seedParticles();
        warmUpParticles();
    }

    private void ensureTrailBuffers() {
        int totalTrailPoints = PARTICLE_COUNT * TRAIL_CAPACITY;
        if (trailX == null || trailX.length != totalTrailPoints) {
            trailX = new float[totalTrailPoints];
            trailY = new float[totalTrailPoints];
            trailZ = new float[totalTrailPoints];
            trailColor = new int[totalTrailPoints];
            trailWritePos = new int[PARTICLE_COUNT];
            trailCount = new int[PARTICLE_COUNT];
        }
    }

    private void ensureLuts() {
        if (colorLut == null) {
            buildParticleColorLut();
        }
        if (toneLut == null) {
            buildToneLut();
        }
    }

    private void seedParticles() {
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            double angle = 2.0 * Math.PI * i / PARTICLE_COUNT;
            double radius = INITIAL_SPREAD * (1.0 + (i % 5) * 0.18);
            x[i] = INITIAL_X + Math.cos(angle) * radius;
            y[i] = INITIAL_Y + Math.sin(angle) * radius;
            z[i] = INITIAL_Z + (i - PARTICLE_COUNT / 2.0) * INITIAL_SPREAD * 0.25;
        }
    }

    private void warmUpParticles() {
        for (int step = 0; step < WARMUP_STEPS; step++) {
            for (int particle = 0; particle < PARTICLE_COUNT; particle++) {
                rk4Step(particle, BASE_SIGMA, BASE_RHO, BASE_BETA);
            }
        }

        for (int particle = 0; particle < PARTICLE_COUNT; particle++) {
            storeTrailPoint(particle);
        }
        spread = computeSpread();
    }

    private void updateJitteredParameters(RNProvider provider) {
        double sigmaJitter = 0.0;
        double rhoJitter = 0.0;
        double betaJitter = 0.0;

        if (jitterEnabled && provider != null) {
            sigmaJitter = nextSignedUnit(provider) * MAX_SIGMA_JITTER * jitterScale;
            rhoJitter = nextSignedUnit(provider) * MAX_RHO_JITTER * jitterScale;
            betaJitter = nextSignedUnit(provider) * MAX_BETA_JITTER * jitterScale;
        }

        currentSigma = BASE_SIGMA + sigmaJitter;
        currentRho = BASE_RHO + rhoJitter;
        currentBeta = BASE_BETA + betaJitter;
    }

    private double nextSignedUnit(RNProvider provider) {
        OptionalInt value = provider.getNextRandomNumber();
        if (value.isEmpty()) {
            return 0.0;
        }
        randomNumbersUsed++;
        return normalize(value.getAsInt()) * 2.0 - 1.0;
    }

    private void rk4Step(int particle, double sigma, double rho, double beta) {
        double px = x[particle];
        double py = y[particle];
        double pz = z[particle];

        double k1x = dx(px, py, sigma);
        double k1y = dy(px, py, pz, rho);
        double k1z = dz(px, py, pz, beta);

        double x2 = px + RK4_HALF_DT * k1x;
        double y2 = py + RK4_HALF_DT * k1y;
        double z2 = pz + RK4_HALF_DT * k1z;
        double k2x = dx(x2, y2, sigma);
        double k2y = dy(x2, y2, z2, rho);
        double k2z = dz(x2, y2, z2, beta);

        double x3 = px + RK4_HALF_DT * k2x;
        double y3 = py + RK4_HALF_DT * k2y;
        double z3 = pz + RK4_HALF_DT * k2z;
        double k3x = dx(x3, y3, sigma);
        double k3y = dy(x3, y3, z3, rho);
        double k3z = dz(x3, y3, z3, beta);

        double x4 = px + DT * k3x;
        double y4 = py + DT * k3y;
        double z4 = pz + DT * k3z;
        double k4x = dx(x4, y4, sigma);
        double k4y = dy(x4, y4, z4, rho);
        double k4z = dz(x4, y4, z4, beta);

        x[particle] = px + RK4_DT_SIXTH * (k1x + 2.0 * k2x + 2.0 * k3x + k4x);
        y[particle] = py + RK4_DT_SIXTH * (k1y + 2.0 * k2y + 2.0 * k3y + k4y);
        z[particle] = pz + RK4_DT_SIXTH * (k1z + 2.0 * k2z + 2.0 * k3z + k4z);
    }

    private static double dx(double x, double y, double sigma) {
        return sigma * (y - x);
    }

    private static double dy(double x, double y, double z, double rho) {
        return x * (rho - z) - y;
    }

    private static double dz(double x, double y, double z, double beta) {
        return x * y - beta * z;
    }

    private void storeTrailPoint(int particle) {
        int offset = particle * TRAIL_CAPACITY;
        int write = trailWritePos[particle];
        int index = offset + write;

        trailX[index] = (float) x[particle];
        trailY[index] = (float) y[particle];
        trailZ[index] = (float) z[particle];
        trailColor[index] = colorLut[particleColorIndex(particle)];

        trailWritePos[particle] = (write + 1) % TRAIL_CAPACITY;
        if (trailCount[particle] < TRAIL_CAPACITY) {
            trailCount[particle]++;
        }
    }

    private int particleColorIndex(int particle) {
        if (PARTICLE_COUNT <= 1) {
            return 0;
        }
        int scaledIndex = Math.round(particle * (COLOR_LUT_SIZE - 1f) / (PARTICLE_COUNT - 1f));
        return Math.clamp(scaledIndex, 0, COLOR_LUT_SIZE - 1);
    }

    private double computeSpread() {
        double cx = 0.0;
        double cy = 0.0;
        double cz = 0.0;
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            cx += x[i];
            cy += y[i];
            cz += z[i];
        }
        cx /= PARTICLE_COUNT;
        cy /= PARTICLE_COUNT;
        cz /= PARTICLE_COUNT;

        double sum = 0.0;
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            double dx = x[i] - cx;
            double dy = y[i] - cy;
            double dz = z[i] - cz;
            sum += dx * dx + dy * dy + dz * dz;
        }
        return Math.sqrt(sum / PARTICLE_COUNT);
    }

    private void buildParticleColorLut() {
        colorLut = new int[COLOR_LUT_SIZE];
        for (int i = 0; i < COLOR_LUT_SIZE; i++) {
            float t = i / (float) (COLOR_LUT_SIZE - 1);
            float hue = 0.55f + 0.30f * t;
            if (hue > 1.0f) {
                hue -= 1.0f;
            }
            float saturation = 0.72f;
            float brightness = 0.82f + 0.16f * (float) Math.sin(Math.PI * t);
            colorLut[i] = Color.HSBtoRGB(hue, saturation, brightness) & 0xFFFFFF;
        }
    }

    private void buildToneLut() {
        toneLut = new int[TONE_MAX + 1];
        for (int i = 0; i <= TONE_MAX; i++) {
            double mapped = 255.0 * (1.0 - Math.exp(-i / TONE_EXPOSURE));
            toneLut[i] = Math.clamp((int) Math.round(mapped), 0, 255);
        }
    }

    private void layoutDashboard() {
        int cardsY = height - OUTER_PADDING - STAT_CARD_HEIGHT;
        int mainTop = HEADER_HEIGHT + OUTER_PADDING;
        int mainHeight = Math.max(120, cardsY - mainTop - OUTER_PADDING);

        panelBounds = new Rectangle(
                OUTER_PADDING,
                mainTop,
                width - OUTER_PADDING * 2,
                mainHeight
        );

        int availablePlotSize = Math.min(
                panelBounds.width - PANEL_INSET * 2,
                panelBounds.height - PANEL_INSET * 2 - 34
        );
        int plotSize = Math.max(80, availablePlotSize);

        int plotX = panelBounds.x + (panelBounds.width - plotSize) / 2;
        int plotY = panelBounds.y + PANEL_INSET + 34;
        plotBounds = new Rectangle(plotX, plotY, plotSize, plotSize);

        int availableCardsWidth = width - OUTER_PADDING * 2 - STAT_CARD_GAP * (STAT_CARD_COUNT - 1);
        int cardWidth = Math.max(120, availableCardsWidth / STAT_CARD_COUNT);
        for (int i = 0; i < STAT_CARD_COUNT; i++) {
            int x = OUTER_PADDING + i * (cardWidth + STAT_CARD_GAP);
            statCards[i] = new Rectangle(x, cardsY, cardWidth, STAT_CARD_HEIGHT);
        }

        allocatePlotBuffers(plotSize, plotSize);
        buildChrome();
    }

    private void allocatePlotBuffers(int w, int h) {
        plotW = Math.max(1, w);
        plotH = Math.max(1, h);
        plotImage = new BufferedImage(plotW, plotH, BufferedImage.TYPE_INT_RGB);
        plotPixels = ((DataBufferInt) plotImage.getRaster().getDataBuffer()).getData();
        int pixels = plotW * plotH;
        accumR = new int[pixels];
        accumG = new int[pixels];
        accumB = new int[pixels];
    }

    private void buildChrome() {
        chromeLayer = new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = chromeLayer.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g.setColor(BACKGROUND);
            g.fillRect(0, 0, width, height);

            drawHeader(g);

            drawPanelBox(g, panelBounds);
            g.setFont(PANEL_TITLE_FONT);
            g.setColor(TEXT_PRIMARY);
            g.drawString("LORENZ STRANGE ATTRACTOR — ENSEMBLE TRAILS", panelBounds.x + PANEL_INSET, panelBounds.y + 24);
        } finally {
            g.dispose();
        }
    }

    private void render(BufferedImage canvas) {
        advanceYaw();
        renderTrails();

        Graphics2D g = canvas.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g.drawImage(chromeLayer, 0, 0, null);
            g.drawImage(plotImage, plotBounds.x, plotBounds.y, null);

            drawBoundingBox(g);

            g.setColor(PANEL_BORDER);
            g.drawRect(plotBounds.x, plotBounds.y, plotBounds.width, plotBounds.height);

            drawLegend(g);
            drawStats(g);
        } finally {
            g.dispose();
        }
    }

    private void advanceYaw() {
        long now = System.nanoTime();
        if (spinning) {
            yaw += (now - lastNanos) * 1.0e-9 * YAW_SPEED;
            if (yaw > Math.PI * 2.0) {
                yaw -= Math.PI * 2.0;
            }
        }
        lastNanos = now;
    }

    private void renderTrails() {
        Arrays.fill(accumR, 0);
        Arrays.fill(accumG, 0);
        Arrays.fill(accumB, 0);

        for (int particle = 0; particle < PARTICLE_COUNT; particle++) {
            renderParticleTrail(particle);
        }

        toneMap();
    }

    private void renderParticleTrail(int particle) {
        int count = trailCount[particle];
        if (count < 2) {
            return;
        }

        int offset = particle * TRAIL_CAPACITY;
        int start = (trailWritePos[particle] - count + TRAIL_CAPACITY) % TRAIL_CAPACITY;

        ProjectedPoint previous = null;
        for (int age = 0; age < count; age++) {
            int ringIndex = (start + age) % TRAIL_CAPACITY;
            int idx = offset + ringIndex;
            ProjectedPoint current = projectModel(
                    normalizeX(trailX[idx]),
                    normalizeY(trailY[idx]),
                    normalizeZ(trailZ[idx])
            );

            if (previous != null && current.visible && previous.visible) {
                double t = age / (double) Math.max(1, count - 1);
                double intensity = (0.05 + 0.95 * Math.pow(t, 1.8)) * LINE_WEIGHT;
                drawAdditiveLine(previous.x, previous.y, current.x, current.y, trailColor[idx], intensity);
            }
            previous = current;
        }
    }

    private ProjectedPoint projectModel(double mx, double my, double mz) {
        double cosY = Math.cos(yaw);
        double sinY = Math.sin(yaw);
        double cosP = Math.cos(pitch);
        double sinP = Math.sin(pitch);

        double x1 = mx * cosY - my * sinY;
        double y1 = mx * sinY + my * cosY;

        double yRot = y1 * cosP - mz * sinP;
        double depth = y1 * sinP + mz * cosP + CAM_DISTANCE;
        if (depth <= 0.05) {
            return new ProjectedPoint(0, 0, false);
        }

        double persp = CAM_DISTANCE / depth;
        double cx = plotW * 0.5;
        double cy = plotH * 0.5;
        double pixelScale = Math.min(plotW, plotH) * 0.5 * PIXEL_SCALE_FACTOR;

        int px = (int) Math.round(cx + x1 * persp * pixelScale);
        int py = (int) Math.round(cy + yRot * persp * pixelScale);
        boolean visible = px >= 1 && px < plotW - 1 && py >= 1 && py < plotH - 1;
        return new ProjectedPoint(px, py, visible);
    }

    private void drawAdditiveLine(int x0, int y0, int x1, int y1, int color, double intensity) {
        int dx = x1 - x0;
        int dy = y1 - y0;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0) {
            splatPoint(x0, y0, color, intensity);
            return;
        }

        double sx = dx / (double) steps;
        double sy = dy / (double) steps;
        double xPos = x0;
        double yPos = y0;
        for (int i = 0; i <= steps; i++) {
            splatPoint((int) Math.round(xPos), (int) Math.round(yPos), color, intensity);
            xPos += sx;
            yPos += sy;
        }
    }

    private void splatPoint(int px, int py, int color, double intensity) {
        if (px < 1 || px >= plotW - 1 || py < 1 || py >= plotH - 1) {
            return;
        }

        int r = (int) (((color >> 16) & 0xFF) * intensity);
        int g = (int) (((color >> 8) & 0xFF) * intensity);
        int b = (int) ((color & 0xFF) * intensity);

        int idx = py * plotW + px;
        accumR[idx] += r;
        accumG[idx] += g;
        accumB[idx] += b;

        int nr = r / 4;
        int ng = g / 4;
        int nb = b / 4;
        addAccum(idx - 1, nr, ng, nb);
        addAccum(idx + 1, nr, ng, nb);
        addAccum(idx - plotW, nr, ng, nb);
        addAccum(idx + plotW, nr, ng, nb);
    }

    private void addAccum(int idx, int r, int g, int b) {
        accumR[idx] += r;
        accumG[idx] += g;
        accumB[idx] += b;
    }

    private void toneMap() {
        int[] tone = toneLut;
        int[] pixels = plotPixels;
        int n = pixels.length;
        for (int i = 0; i < n; i++) {
            int vr = accumR[i];
            int vg = accumG[i];
            int vb = accumB[i];
            if (vr > TONE_MAX) vr = TONE_MAX;
            if (vg > TONE_MAX) vg = TONE_MAX;
            if (vb > TONE_MAX) vb = TONE_MAX;

            int r = BASE_R + tone[vr];
            int g = BASE_G + tone[vg];
            int b = BASE_B + tone[vb];
            if (r > 255) r = 255;
            if (g > 255) g = 255;
            if (b > 255) b = 255;

            pixels[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
        }
    }

    private void drawBoundingBox(Graphics2D g) {
        Shape oldClip = g.getClip();
        g.setClip(plotBounds.x, plotBounds.y, plotBounds.width, plotBounds.height);
        g.setColor(BOX_COLOR);

        double[][] corners = {
                {-1.15, -1.15, -0.95}, {1.15, -1.15, -0.95}, {1.15, 1.15, -0.95}, {-1.15, 1.15, -0.95},
                {-1.15, -1.15, 1.15}, {1.15, -1.15, 1.15}, {1.15, 1.15, 1.15}, {-1.15, 1.15, 1.15}
        };

        ProjectedPoint[] projected = new ProjectedPoint[corners.length];
        for (int i = 0; i < corners.length; i++) {
            projected[i] = projectModel(corners[i][0], corners[i][1], corners[i][2]);
        }

        for (int base = 0; base <= 4; base += 4) {
            for (int k = 0; k < 4; k++) {
                drawEdge(g, projected[base + k], projected[base + (k + 1) % 4]);
            }
        }
        for (int k = 0; k < 4; k++) {
            drawEdge(g, projected[k], projected[k + 4]);
        }

        g.setClip(oldClip);
    }

    private void drawEdge(Graphics2D g, ProjectedPoint a, ProjectedPoint b) {
        if (!a.visible || !b.visible) {
            return;
        }
        g.drawLine(plotBounds.x + a.x, plotBounds.y + a.y, plotBounds.x + b.x, plotBounds.y + b.y);
    }

    private void drawHeader(Graphics2D g) {
        g.setFont(TITLE_FONT);
        g.setColor(TITLE_COLOR);
        g.drawString("LORENZ ATTRACTOR — 3D CHAOS BUTTERFLY", OUTER_PADDING, 44);

        g.setFont(SUBTITLE_FONT);
        g.setColor(TEXT_MUTED);
        g.drawString(
                "Nearby trajectories diverge, yet settle into one iconic strange attractor.",
                OUTER_PADDING,
                68
        );

        g.setFont(SMALL_FONT);
        g.setColor(TEXT_DIM);
        g.drawString(
                "RK4 integration; QRNG jitter modulates σ/ρ/β around classical Lorenz parameters.",
                OUTER_PADDING,
                86
        );
    }

    private void drawLegend(Graphics2D g) {
        int x = panelBounds.x + panelBounds.width - 410;
        int y = panelBounds.y + 20;
        g.setFont(LABEL_FONT);

        g.setColor(SPREAD_COLOR);
        g.fillOval(x, y - 8, 8, 8);
        g.drawString("nearby trajectories", x + 14, y);

        g.setColor(JITTER_COLOR);
        g.fillOval(x + 160, y - 8, 8, 8);
        g.drawString("QRNG parameter jitter", x + 174, y);

        g.setColor(PARAM_COLOR);
        g.fillOval(x + 330, y - 8, 8, 8);
        g.drawString("RK4 trail", x + 344, y);
    }

    private void drawStats(Graphics2D g) {
        drawStatCard(g, statCards[0], "TRACE POINTS", Integer.toString(pointCount), "stored as glowing trail segments", TITLE_COLOR);
        drawStatCard(g, statCards[1], "PARTICLES", Integer.toString(PARTICLE_COUNT), "near-identical initial states", PARAM_COLOR);
        drawStatCard(g, statCards[2], "SPREAD", formatSpread(spread), "RMS distance between particles", SPREAD_COLOR);
        drawStatCard(g, statCards[3], "PARAMETERS", formatParams(), "σ / ρ / β with tiny jitter", TITLE_COLOR);
        drawStatCard(g, statCards[4], "JITTER", formatPercent(jitterScale), jitterEnabled ? "QRNG jitter enabled" : "jitter disabled", JITTER_COLOR);
        drawStatCard(g, statCards[5], "TRAIL", Integer.toString(TRAIL_CAPACITY), "points per particle ring buffer", TEXT_PRIMARY);
    }

    private void drawStatCard(
            Graphics2D g,
            Rectangle bounds,
            String label,
            String value,
            String smallText,
            Color valueColor
    ) {
        if (bounds == null) {
            return;
        }

        drawPanelBox(g, bounds);

        g.setFont(LABEL_FONT);
        g.setColor(TEXT_MUTED);
        g.drawString(label, bounds.x + 14, bounds.y + 22);

        g.setFont(VALUE_FONT);
        g.setColor(valueColor);
        g.drawString(value, bounds.x + 14, bounds.y + 47);

        g.setFont(SMALL_FONT);
        g.setColor(TEXT_DIM);
        g.drawString(smallText, bounds.x + 14, bounds.y + 64);
    }

    private void drawPanelBox(Graphics2D g, Rectangle bounds) {
        g.setColor(PANEL_BACKGROUND);
        g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, PANEL_RADIUS, PANEL_RADIUS);
        g.setColor(PANEL_BORDER);
        g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, PANEL_RADIUS, PANEL_RADIUS);
    }

    private static double normalizeX(double value) {
        return value / MODEL_XY_SCALE;
    }

    private static double normalizeY(double value) {
        return value / MODEL_XY_SCALE;
    }

    private static double normalizeZ(double value) {
        return (value - MODEL_Z_CENTER) / MODEL_Z_SCALE;
    }

    private static double normalize(int value) {
        return Math.floorMod(value, RANDOM_RANGE) / RANDOM_MAX;
    }

    private static String formatSpread(double value) {
        return String.format(java.util.Locale.US, SPREAD_FORMAT, value);
    }

    private String formatParams() {
        return String.format(java.util.Locale.US, "%.2f / %.2f / %.3f", currentSigma, currentRho, currentBeta);
    }

    private static String formatPercent(double value) {
        return String.format(java.util.Locale.US, "%.0f%%", value * 100.0);
    }


    private record ProjectedPoint(int x, int y, boolean visible) { }
}