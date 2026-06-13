# Tests

This project uses **JUnit 5** tests for configuration, RNG processing, statistical checks, visualization mode registry/smoke coverage, and selected integration scenarios.

The exact number of tests is intentionally not documented as a fixed expected value. It changes frequently when visualization modes, smoke tests, and integration tests are added. The source of truth is the local Maven output.

## Main command

```bash
mvn test
```

## Useful targeted commands

```bash
mvn test -Dtest=VisualizationModeRegistryTest
mvn test -Dtest=VisualizationModesSmokeTest
mvn test -Dtest=RNProviderIntegrationTest
mvn test -Dtest=RngStepBudgetTest
mvn test -Dtest=LorenzAttractor3DModeTest
mvn test -Dtest=MonteCarloMandelbrotAreaModeTest
mvn test -Dtest=MonteCarloMandelbrot3DAreaModeTest
```

## Test groups by purpose

### Configuration and infrastructure

- `ConfigTest` — configuration loading and typed accessors.
- `LoggerConfigTest` — logger setup behavior.
- `RandomNumbersLogTest` — random-number log file behavior.

### RNG and number processing

- `RandomNumberProcessorTest` — HEX/uint processing and range mapping.
- `RNProviderIntegrationTest` — `RNProvider` behavior with a local mock HTTP server.
- `RngStepBudgetTest` — adaptive RNG step-budget policy for PSEUDO/QUANTUM/forced-pseudo providers.
- `RngSamplerTest` — mode-local RNG sampling counter and uint16 normalization helpers.

`RNProviderIntegrationTest` does not require the real ANU API. It starts a local `com.sun.net.httpserver.HttpServer`, verifies request headers such as `x-api-key`, simulates success/error/rate-limit responses, and checks fallback behavior.

### Statistical tests

- `KolmogorovSmirnovTestUnitTest`
- `ChiSquareUniformityTestTest`
- `FrequencyBitTestTest`
- `RunsBitTestTest`
- `RandomnessTestSuiteTest`
- `TestResultTest`
- `NISTRandomnessTestUnitTest`
- `StatisticalRandomnessTest`

These tests check implementation behavior and statistical sanity logic. They are not a cryptographic certification of a random source.

### Model and algorithm tests

- `DotTest` — immutable `Dot` record behavior.
- `SierpinskiAlgorithmTest` — Chaos Game / Sierpinski movement and invariants.

### Visualization tests

- `VisualizationModeRegistryTest` — registered mode ids, ordering, and category mapping.
- `VisualizationModesSmokeTest` — smoke coverage for representative visualization modes with lightweight test doubles.
- `LorenzAttractor3DModeTest`
- `MonteCarloMandelbrotAreaModeTest`
- `MonteCarloMandelbrot3DAreaModeTest`

Visualization smoke tests avoid Mockito/ByteBuddy and use small local test doubles so they remain stable on newer Java versions.

## Notes

- The project currently targets Java 25 via Maven compiler `<release>25</release>`.
- Run tests from the project root where `pom.xml` is located.
- Maven/IDE warnings from dependencies such as Jansi/Guice about future Java restrictions are external tooling warnings, not project test failures.
- If IntelliJ marks Maven dependencies as missing while CLI Maven works, reload the Maven project or use `Add as Maven Project` on `pom.xml`.
