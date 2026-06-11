# Tests — rep-qrng-chaos-game

Этот README описывает актуальный ISTZUSTAND тестового набора проекта.

---

## Current status

| Метрика | Значение |
|---|---:|
| Test framework | JUnit 5.13.4 |
| Test Java files | 21 |
| Last locally verified tests | 250 |
| Last locally verified failures/errors/skipped | 0 / 0 / 0 |
| Last locally verified result | `BUILD SUCCESS` |
| JaCoCo | enabled |

Последний подтверждённый локальный прогон пользователя:

```text
Tests run: 250, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-11T09:34:07+02:00
```

---

## Test tags

В проекте используются JUnit tags:

- `fast` — быстрые unit/component/smoke tests;
- `integration` — тесты с локальным HTTP-сервером для `RNProvider`;
- `slow` — более тяжёлые статистические/алгоритмические проверки.

Запуск:

```bash
mvn test
mvn test -Dgroups=fast
mvn test -Dgroups=integration
mvn test -DexcludedGroups=slow
```

---

## Актуальная структура тестов

```text
src/test/java/org/ThreeDotsSierpinski
├── README.md
├── config
│   ├── ConfigTest.java
│   └── LoggerConfigTest.java
├── math
│   └── SierpinskiAlgorithmTest.java
├── mode
│   ├── VisualizationModeRegistryTest.java
│   ├── VisualizationModesSmokeTest.java
│   ├── montecarlo
│   │   ├── MonteCarloMandelbrotAreaModeTest.java
│   │   └── MonteCarloMandelbrot3DAreaModeTest.java
│   └── physics
│       └── LorenzAttractor3DModeTest.java
├── model
│   └── DotTest.java
├── rng
│   ├── RandomNumberProcessorTest.java
│   ├── RandomNumbersLogTest.java
│   └── RNProviderIntegrationTest.java
└── stats
    ├── ChiSquareUniformityTestTest.java
    ├── FrequencyBitTestTest.java
    ├── KolmogorovSmirnovTestUnitTest.java
    ├── NISTRandomnessTest.java
    ├── NISTRandomnessTestUnitTest.java
    ├── RandomnessTestSuiteTest.java
    ├── RunsBitTestTest.java
    ├── StatisticalRandomnessTest.java
    └── TestResultTest.java
```

---

## Что покрыто

### Config / logging

- загрузка параметров из `config.properties`;
- приоритет environment / `.env` / classpath config;
- преобразование `dot.notation` ключей в `QRNG_UPPER_SNAKE_CASE`;
- обработка отсутствующих и некорректных значений;
- `LoggerConfig` initialization.

### RNG

- `RandomNumberProcessor`: HEX parsing, диапазоны, uint16 semantics;
- `RNProviderIntegrationTest`: локальный mock HTTP-сервер, успешные загрузки, HTTP request details, retry/backoff, error handling, API key validation, buffer behavior, listener callbacks, shutdown/thread-safety;
- `RandomNumbersLog`: TRUE/PSEUDO logging policy, batch separation, flushing/closing behavior.

### Statistics

- `KolmogorovSmirnovTest`;
- `FrequencyBitTest`;
- `ChiSquareUniformityTest`;
- `RunsBitTest`;
- `RandomnessTestSuite`;
- `TestResult` quality semantics;
- `NISTRandomnessTest` utility;
- более широкие statistical checks в `StatisticalRandomnessTest`.

### Math / model

- immutable `Dot` record;
- `SierpinskiAlgorithm`: выбор вершин, движение точки, свойства фрактала, воспроизводимость, центральная пустая область.

### Visualization modes

- `VisualizationModeRegistryTest`: наличие ключевых режимов и уникальность ids;
- `VisualizationModesSmokeTest`: smoke-проверки основных режимов без Mockito/ByteBuddy;
- dedicated tests для `LorenzAttractor3DMode`;
- dedicated tests для `MonteCarloMandelbrotAreaMode` и `MonteCarloMandelbrot3DAreaMode`.

---

## Важное про Mockito / Java 25

Mockito остаётся в `pom.xml` как test dependency:

```xml
<mockito.version>5.23.0</mockito.version>
```

Но mode smoke tests сейчас используют lightweight test doubles (`TestRNProvider`, `CountingDotController`) вместо Mockito/ByteBuddy. Это сделано, чтобы избежать проблем inline mock maker / ByteBuddy на Java 25.

---

## Запуск конкретных тестов

```bash
mvn -Dtest=ConfigTest test
mvn -Dtest=RNProviderIntegrationTest test
mvn -Dtest=VisualizationModeRegistryTest test
mvn -Dtest=VisualizationModesSmokeTest test
mvn -Dtest=LorenzAttractor3DModeTest test
mvn -Dtest=MonteCarloMandelbrotAreaModeTest test
mvn -Dtest=MonteCarloMandelbrot3DAreaModeTest test
```

---

## Coverage

JaCoCo включён в `pom.xml`:

- `prepare-agent` перед тестами;
- `report` на фазе `test`.

Локальный Maven output последнего подтверждённого запуска сообщил:

```text
Analyzed bundle 'rep-qrng-chaos-game' with 67 classes
```
