# Fourier Series Spiral visualization patch

## Purpose

Adds a new visualization mode inspired by the uploaded spiral/epicycle video.
The algorithm treats the shape as a complex parametric Fourier spiral:

```text
z(theta) = r(theta) * exp(i theta) + sum(c_k * exp(i k theta))
```

The first term is an expanding Archimedean-like spiral. The small harmonic terms
are drawn as an epicycle chain. The chain endpoint leaves a luminous trail, while
QRNG/PSEUDO values add small phase, branch and color perturbations.

## Files

- `src/main/java/org/ThreeDotsSierpinski/mode/physics/FourierSeriesSpiralMode.java`
- `src/main/java/org/ThreeDotsSierpinski/mode/VisualizationModes.java`
- `src/test/java/org/ThreeDotsSierpinski/mode/physics/FourierSeriesSpiralModeTest.java`
- `src/test/java/org/ThreeDotsSierpinski/mode/VisualizationModeRegistryTest.java`

## Apply

From repository root:

```bash
git apply --check --whitespace=nowarn qrng_fourier_series_spiral_patch.patch
git apply --whitespace=nowarn qrng_fourier_series_spiral_patch.patch
```

## Verify

```bash
mvn -Dtest=FourierSeriesSpiralModeTest,VisualizationModeRegistryTest test
mvn test
```

## Commit message

```text
feat: add Fourier series spiral visualization mode
```
