package org.ThreeDotsSierpinski.mode;

import org.ThreeDotsSierpinski.mode.chaos.BarnsleyFernMode;
import org.ThreeDotsSierpinski.mode.chaos.ChaosGameRepresentationMode;
import org.ThreeDotsSierpinski.mode.chaos.FractalFlameMode;
import org.ThreeDotsSierpinski.mode.chaos.SierpinskiMode;
import org.ThreeDotsSierpinski.mode.montecarlo.BuddhabrotMode;
import org.ThreeDotsSierpinski.mode.montecarlo.MonteCarloMandelbrot3DAreaMode;
import org.ThreeDotsSierpinski.mode.montecarlo.MonteCarloMandelbrotAreaMode;
import org.ThreeDotsSierpinski.mode.montecarlo.MonteCarloPiMode;
import org.ThreeDotsSierpinski.mode.physics.AbelianSandpileMode;
import org.ThreeDotsSierpinski.mode.physics.AizawaAttractorMode;
import org.ThreeDotsSierpinski.mode.physics.BifurcationDiagramMode;
import org.ThreeDotsSierpinski.mode.physics.ChaosLissajousMode;
import org.ThreeDotsSierpinski.mode.physics.ChirikovStandardMapMode;
import org.ThreeDotsSierpinski.mode.physics.DadrasAttractorMode;
import org.ThreeDotsSierpinski.mode.physics.ForestFireMode;
import org.ThreeDotsSierpinski.mode.physics.FourierSeriesSpiralMode;
import org.ThreeDotsSierpinski.mode.physics.HalvorsenAttractorMode;
import org.ThreeDotsSierpinski.mode.physics.Lissajous3DMode;
import org.ThreeDotsSierpinski.mode.physics.LissajousFrequencyMode;
import org.ThreeDotsSierpinski.mode.physics.LissajousOscilloscopeMode;
import org.ThreeDotsSierpinski.mode.physics.LissajousQuantumVsPseudoMode;
import org.ThreeDotsSierpinski.mode.physics.LissajousSpectralAnalyzerMode;
import org.ThreeDotsSierpinski.mode.physics.LorenzAttractor3DMode;
import org.ThreeDotsSierpinski.mode.physics.PercolationMode;
import org.ThreeDotsSierpinski.mode.physics.RosslerAttractorMode;
import org.ThreeDotsSierpinski.mode.physics.ThomasAttractorMode;
import org.ThreeDotsSierpinski.mode.stochastic.DLAMode;
import org.ThreeDotsSierpinski.mode.stochastic.GaltonBoardMode;
import org.ThreeDotsSierpinski.mode.stochastic.RandomWalkHeatmapMode;
import org.ThreeDotsSierpinski.mode.stochastic.Rule30AutomatonMode;
import org.ThreeDotsSierpinski.mode.stochastic.SpectralPlotMode;
import org.ThreeDotsSierpinski.mode.stochastic.VoronoiMode;

/**
 * Реестр всех доступных режимов визуализации.
 *
 * <p>VisualizationMode остаётся только контрактом режима, а concrete mode registry
 * живёт здесь, чтобы интерфейс не зависел от всех реализаций.</p>
 */
public final class VisualizationModes {

    private VisualizationModes() {
    }

    /**
     * Все доступные режимы в порядке отображения в диалоге выбора.
     * Для добавления нового режима — добавить его в этот массив.
     */
    public static VisualizationMode[] all() {
        return new VisualizationMode[] {
                new SierpinskiMode(),
                new VoronoiMode(),
                new BarnsleyFernMode(),
                new FractalFlameMode(),
                new RandomWalkHeatmapMode(),
                new Rule30AutomatonMode(),
                new BuddhabrotMode(),
                new MonteCarloMandelbrotAreaMode(),
                new MonteCarloMandelbrot3DAreaMode(),
                new LorenzAttractor3DMode(),
                new ChirikovStandardMapMode(),
                new AizawaAttractorMode(),
                new ThomasAttractorMode(),
                new RosslerAttractorMode(),
                new HalvorsenAttractorMode(),
                new DadrasAttractorMode(),
                new MonteCarloPiMode(),
                new GaltonBoardMode(),
                new PercolationMode(),
                new ForestFireMode(),
                new AbelianSandpileMode(),
                new SpectralPlotMode(),
                new ChaosGameRepresentationMode(),
                new DLAMode(),
                new BifurcationDiagramMode(),
                new LissajousFrequencyMode(),
                new LissajousOscilloscopeMode(),
                new LissajousQuantumVsPseudoMode(),
                new Lissajous3DMode(),
                new LissajousSpectralAnalyzerMode(),
                new ChaosLissajousMode(),
                new FourierSeriesSpiralMode(),
        };
    }
}
