# rep-qrng-chaos-game

Java 25 / Swing-приложение для визуализации потоков случайных чисел. Проект начинался как **Chaos Game / Sierpinski Triangle** на квантовых случайных числах, а сейчас является небольшой лабораторией генеративной случайности, фракталов, Monte Carlo-оценок, динамических систем, статистической физики, Lissajous-визуализаций и stochastic processes.

Проект использует два источника случайности:

- **QUANTUM** — истинные случайные числа из **ANU Quantum Random Numbers API**;
- **PSEUDO** — локальный fallback на `L128X256MixRandom`, если API недоступен, не задан ключ, исчерпан лимит или пользователь вручную выбрал локальный режим.

Важно: приложение **не доказывает** «истинную случайность» генератора. Оно визуализирует поток чисел и даёт прикладные **statistical sanity checks** по тем значениям, которые реально были использованы в текущей сессии.

---

## Current status / ISTZUSTAND

Актуальное состояние архива `rep-qrng-chaos-game_context_2026-06-11_09-36-19.zip`:

| Метрика | Значение |
|---|---:|
| Java target | 25 |
| Maven project | yes |
| Main Java files | 51 |
| Test Java files | 21 |
| Registered visualization modes | 26 |
| Visualization categories | 6 |
| Last locally verified tests | 250 |
| Last locally verified failures/errors/skipped | 0 / 0 / 0 |
| Last locally verified result | `BUILD SUCCESS` |
| JaCoCo | enabled, bundle analyzed with 67 classes |

Последний подтверждённый локальный прогон пользователя:

```text
Tests run: 250, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-11T09:34:07+02:00
```

Примечание по sandbox: в текущем ChatGPT sandbox Maven не установлен, а доступный JDK — 21, поэтому локальный `mvn test` здесь не запускался. Источником статуса тестов является предоставленный пользователем Maven output.

---

## Visualization categories

Окно выбора режимов сейчас двухуровневое: сначала выбирается категория, затем конкретный режим внутри неё. Категории задаются enum `VisualizationCategory`, а режимы регистрируются в `VisualizationMode.allModes()`.

| Category | Modes | Назначение |
|---|---:|---|
| `CHAOS_FRACTALS` / Chaos & Fractals | 4 | Chaos games, IFS fractals, CGR, aggregation structures |
| `MONTE_CARLO` / Monte Carlo | 3 | Random sampling для геометрии и численных оценок |
| `RANDOM_PROCESSES` / Random Processes | 4 | Walks, distributions, mosaics, sequence diagnostics |
| `STATISTICAL_PHYSICS` / Statistical Physics | 2 | Lattice/percolation/spreading-process simulations |
| `ATTRACTORS` / Attractors | 7 | Strange attractors и bifurcation dynamics |
| `LISSAJOUS` / Lissajous | 6 | Oscilloscope curves, phase systems, QRNG-driven harmonics |

---

## Зарегистрированные режимы визуализации

Текущий registry содержит 26 режимов:

### Chaos & Fractals

- **Sierpinski Triangle** (`Sierpinski`) — классический Chaos Game: случайное число выбирает вершину, точка прыгает на середину отрезка, постепенно возникает треугольник Серпинского.
- **Barnsley Fern** (`barnsley-fern`) — IFS-фрактал папоротника Барнсли; случайные числа выбирают affine transformations.
- **CGR Bit Stream** (`cgr-bitstream`) — Chaos Game Representation для битового потока через символы `00/01/10/11`; пустоты, полосы и симметрии показывают скрытую структуру последовательности.
- **DLA / Brownian Tree** (`dla`) — diffusion-limited aggregation: частицы случайно блуждают и прилипают к кластеру, создавая структуры типа кораллов, молний или кристаллов.

### Monte Carlo

- **Monte Carlo π Dashboard** (`monte-carlo-pi`) — оценка числа π через случайные точки в квадрате и долю точек внутри четверти окружности.
- **Monte Carlo Mandelbrot Area** (`monte-carlo-mandelbrot-area`) — оценка площади множества Мандельброта методом Monte Carlo по bounded/escaped точкам комплексной плоскости.
- **Monte Carlo Mandelbrot 3D Area** (`monte-carlo-mandelbrot-3d-area`) — 3D-рельеф escape-time над комплексной плоскостью; высота показывает скорость убегания.

### Random Processes

- **Random Walk Heatmap** (`random-walk-heatmap`) — тепловая карта посещений случайного блуждания; чем чаще область посещается, тем ярче она светится.
- **Galton Board** (`galton-board`) — доска Гальтона: из множества бинарных случайных развилок возникает биномиальное распределение.
- **Voronoi Mosaic** (`voronoi`) — случайные точки порождают ячейки Вороного; есть mode-specific toggle **Метки ВКЛ./ВЫКЛ.** для центров.
- **Spectral RNG Plot** (`spectral-plot`) — тройки чисел `(xₙ, xₙ₊₁, xₙ₊₂)` становятся 3D-точками; плохой генератор может проявляться плоскостями или полосами.

### Statistical Physics

- **Percolation** (`percolation`) — site percolation на квадратной решётке; около `p≈0.59` появляется путь через всю решётку.
- **Forest Fire** (`forest-fire`) — клеточная модель роста леса, молний, распространения огня и восстановления.

### Attractors

- **Lorenz Attractor 3D** (`lorenz-attractor-3d`) — ансамбль близких траекторий Лоренца с RK4-интеграцией и QRNG-jitter параметров `σ / ρ / β`.
- **Aizawa Attractor** (`aizawa-attractor`) — тороидальный странный аттрактор с QRNG-джиттером параметров.
- **Thomas Attractor** (`thomas-attractor`) — циклически симметричный странный аттрактор с `sin`-связями между осями.
- **Rössler Attractor** (`rossler-attractor`) — классический Rössler-flow: вращение почти как диск, затем хаотическая лента.
- **Halvorsen Attractor** (`halvorsen-attractor`) — трёхкрылый attractor с квадратичными связями.
- **Dadras Attractor** (`dadras-attractor`) — компактная «бабочка» со смешанными `xy/yz/xz` связями.
- **Bifurcation Diagram** (`bifurcation-diagram`) — логистическое отображение и путь от стабильности к каскаду бифуркаций и хаосу.

### Lissajous

- **Lissajous Frequencies** (`lissajous-frequency`) — QRNG выбирает частоты `a/b` и фазу `δ` для классической фигуры Лиссажу.
- **Lissajous Oscilloscope** (`lissajous-oscilloscope`) — «живой осциллограф»: QRNG слегка меняет частоты и фазу, фигура дышит и дрейфует.
- **Lissajous Quantum vs Pseudo** (`lissajous-quantum-vs-pseudo`) — сравнение двух Lissajous-осциллографов: слева QRNG, справа локальный pseudo stream.
- **Lissajous 3D Knot** (`lissajous-3d`) — 3D-фигура Лиссажу с тремя синусоидальными колебаниями и вращающейся камерой.
- **Lissajous Spectral Analyzer** (`lissajous-spectral-analyzer`) — поток random numbers используется как фазовый шум для пары синусоид.
- **Chaos Lissajous** (`chaos-lissajous`) — связка QRNG → Logistic Map → Lissajous; состояние логистического отображения управляет рисунком.

---

## Что умеет приложение

- показывает стартовое окно **Quantum Random Visualizer** с двухуровневым выбором категорий и режимов;
- поддерживает workflow loop: выбор режима → окно визуализации → **Выйти** → возврат к выбору режима;
- запоминает последнюю категорию, последний выбранный режим и visited mode ids в рамках текущего запуска;
- корректно работает с несколькими мониторами: новое окно открывается на том экране, где был выбор/последняя визуализация;
- рисует визуализации в реальном времени через Swing/AWT;
- получает числа из ANU QRNG API по HTTP с заголовком `x-api-key`;
- по умолчанию стартует в локальном `PSEUDO`, параллельно может загрузить QUANTUM batch при наличии ключа;
- автоматически переключается в `PSEUDO` при недоступности API, отсутствии ключа или исчерпании лимита;
- пытается вернуться в `QUANTUM` после восстановления внешнего источника, если это имеет смысл;
- поддерживает manual toggle **QUANTUM / PSEUDO**;
- поддерживает **Play / Stop** для паузы и продолжения анимации;
- отображает stack использованных чисел в режимах, где это не отключено тёмным canvas;
- хранит историю потреблённых чисел в ring buffer до `100_000` значений;
- запускает встроенные статистические тесты кнопкой **Test RNG**;
- показывает результаты тестов с уровнями качества `STRONG / MARGINAL / FAIL`;
- сохраняет текущую визуализацию как две PNG-картинки: transparent и white-background;
- поддерживает mode-specific controls, например reset/toggle controls у отдельных режимов;
- поддерживает визуальный стиль **AppleMac** для Sierpinski UI;
- пишет application logs в `logs/app.log`;
- пишет TRUE/QUANTUM stream в `logs/rnds-true.log`;
- инфраструктурно поддерживает `logs/rnds-pseudo.log`, но pseudo logging по умолчанию отключён.

---

## Visualization styles

### Default

Базовый стиль приложения: светлые и тёмные режимы, цветные canvas-визуализации, dashboard-панели и mode-specific controls.

### AppleMac

Монохромный retro UI в духе classic Macintosh / System 6/7 для режима **Sierpinski Triangle**.

Особенности:

- 4:3-пропорции окна визуализации;
- отдельная left plot area;
- отдельная right sidebar для таблицы использованных чисел;
- framed counter/status block слева;
- reserved UI area для counter block, куда точки не рисуются;
- bitmap-like fonts;
- Mac-like table grid;
- bevel-like bottom controls;
- monochrome palette.

---

## Как это работает

### 1. Источник случайных чисел

`RNProvider` загружает числа из ANU API. По умолчанию проект запрашивает:

- тип данных: `uint16`;
- длину массива: `1024`;
- минимальный локальный буфер: `100`;
- максимум API-запросов за сессию: `100`.

Стартовая логика сейчас осторожная: `RNProvider` начинает в локальном `PSEUDO` mode (`isForcedPseudo=true`), чтобы UI не блокировался, и при наличии API key параллельно запускает загрузку QUANTUM data. После успешной загрузки stale PSEUDO entries удаляются из очереди, а mode переключается в `QUANTUM`.

При временных ошибках используется retry с exponential backoff:

```text
1s → 2s → 4s → 8s → 16s → max 30s
```

Если загрузка не удалась, API key не задан или лимит запросов исчерпан, `RNProvider` переключается в `PSEUDO` и начинает выдавать числа из `L128X256MixRandom`.

---

### 2. Логгирование случайных чисел

Проект ведёт отдельное логгирование random stream:

```text
logs/rnds-true.log
logs/rnds-pseudo.log
```

Текущая политика:

- TRUE/QUANTUM numbers пишутся в `logs/rnds-true.log`;
- PSEUDO logging инфраструктурно поддержан, но по умолчанию отключён через `random.log.pseudo.enabled=false`;
- pseudo-числа не смешиваются с true-логом;
- каждый lifecycle `RNProvider` / запуск режима отделяется пустой строкой;
- flush выполняется после первого значения и далее каждые `random.log.flush.every.values` значений.

---

### 3. Абстракция режимов визуализации

Все режимы реализуют интерфейс `VisualizationMode`.

Режим отвечает за:

- уникальный id;
- имя;
- описание;
- иконку;
- high-level category;
- инициализацию canvas;
- один шаг анимации;
- количество нарисованных точек;
- количество использованных случайных чисел;
- dark/light background flag;
- point counter placement;
- mode-specific controls;
- redraw текущего состояния без потребления новых чисел;
- optional reserved drawing areas;
- optional mouse click handling для canvas overlays/help areas.

Реестр доступных режимов находится в:

```java
VisualizationMode.allModes()
```

---

### 4. Отрисовка

`DotController` работает как Swing-панель, которая:

- хранит `offscreenImage`;
- обновляет изображение через `javax.swing.Timer`;
- делегирует шаг визуализации выбранному `VisualizationMode`;
- выполняет работу с canvas на EDT;
- показывает счётчик точек;
- показывает текущий режим RNG;
- отображает stack использованных чисел;
- поддерживает `refreshVisualization()`;
- поддерживает AppleMac plot/sidebar separation;
- поддерживает reserved UI areas;
- поддерживает transparent и white-background PNG export;
- останавливает timers через `shutdown()`.

---

### 5. Workflow приложения

```text
App.main()
    ↓
ModeSelectionDialog: category selection
    ↓
ModeSelectionDialog: mode selection
    ↓
Visualization JFrame + DotController + RNProvider
    ↓
Выйти / closing window
    ↓
DotController.shutdown() + RNProvider.shutdown()
    ↓
ModeSelectionDialog
```

`App` управляет:

- стартом FlatLaf;
- выбором режима;
- созданием окна визуализации;
- status bar;
- кнопками управления;
- RNG toggle;
- запуском статистических тестов;
- PNG export;
- multi-monitor positioning;
- shutdown `DotController` и `RNProvider`.

---

## Архитектура проекта

### Package structure

```text
org.ThreeDotsSierpinski
├── app
│   ├── App
│   ├── ModeSelectionDialog
│   └── DotController
├── mode
│   ├── VisualizationMode
│   ├── VisualizationCategory
│   ├── VisualizationStyle
│   ├── chaos
│   │   ├── SierpinskiMode
│   │   ├── BarnsleyFernMode
│   │   └── ChaosGameRepresentationMode
│   ├── montecarlo
│   │   ├── MonteCarloPiMode
│   │   ├── MonteCarloMandelbrotAreaMode
│   │   └── MonteCarloMandelbrot3DAreaMode
│   ├── physics
│   │   ├── AbstractStrangeAttractorMode
│   │   ├── AbstractLissajousMode
│   │   ├── LorenzAttractor3DMode
│   │   ├── AizawaAttractorMode
│   │   ├── ThomasAttractorMode
│   │   ├── RosslerAttractorMode
│   │   ├── HalvorsenAttractorMode
│   │   ├── DadrasAttractorMode
│   │   ├── BifurcationDiagramMode
│   │   ├── PercolationMode
│   │   ├── ForestFireMode
│   │   ├── LissajousFrequencyMode
│   │   ├── LissajousOscilloscopeMode
│   │   ├── LissajousQuantumVsPseudoMode
│   │   ├── Lissajous3DMode
│   │   ├── LissajousSpectralAnalyzerMode
│   │   └── ChaosLissajousMode
│   └── stochastic
│       ├── GaltonBoardMode
│       ├── RandomWalkHeatmapMode
│       ├── VoronoiMode
│       ├── SpectralPlotMode
│       └── DLAMode
├── rng
│   ├── RNProvider
│   ├── RNLoadListener
│   ├── RNLoadListenerImpl
│   ├── RandomNumberProcessor
│   └── RandomNumbersLog
├── stats
│   ├── RandomnessTest
│   ├── RandomnessTestSuite
│   ├── TestResult
│   ├── ChiSquareUniformityTest
│   ├── FrequencyBitTest
│   ├── KolmogorovSmirnovTest
│   └── RunsBitTest
├── model
│   └── Dot
├── math
│   ├── SierpinskiAlgorithm
│   └── MathUtils
└── config
    ├── Config
    └── LoggerConfig
```

### Основные классы

- **`App`** — точка входа, GUI workflow loop, multi-monitor logic, кнопки управления, статистические тесты, PNG export.
- **`ModeSelectionDialog`** — двухуровневое окно выбора: categories → modes.
- **`DotController`** — центральная Swing-панель визуализации.
- **`VisualizationMode`** — общий контракт и registry всех режимов.
- **`VisualizationCategory`** — 6 high-level групп для меню выбора режимов.
- **`VisualizationStyle`** — стиль визуализации, включая `APPLE_MAC`.
- **`RNProvider`** — сетевой клиент, буфер случайных чисел, fallback `QUANTUM → PSEUDO → QUANTUM`, ring buffer истории.
- **`RandomNumbersLog`** — файловое логгирование TRUE/PSEUDO random streams.
- **`RandomnessTestSuite`** — runtime sanity checks.
- **`Config`** — загрузка настроек из environment, `.env`, `config.properties`.
- **`LoggerConfig`** — файловое и консольное логирование.

---

## Runtime-тесты случайности

`RandomnessTestSuite` запускает 4 runtime checks:

- **`KolmogorovSmirnovTest`** — отклонение эмпирического распределения от равномерного;
- **`FrequencyBitTest`** — баланс битов `0/1`;
- **`ChiSquareUniformityTest`** — χ²-проверка равномерности по корзинам;
- **`RunsBitTest`** — тест серий по битовой последовательности.

Кнопка **Test RNG** показывает диалог с уровнями качества:

- `STRONG`;
- `MARGINAL`;
- `FAIL`.

---

## Технологический стек

- **Java 25**
- **Swing / AWT**
- **Maven**
- **Jackson Databind 2.22.0** для JSON
- **FlatLaf 3.5.4** для Swing UI
- **JUnit 5.13.4** для тестов
- **Mockito 5.23.0** как test dependency
- **JaCoCo 0.8.15** для coverage report
- **Maven Surefire 3.5.5** для JUnit 5 / tags / parallel class execution
- **JetBrains annotations 26.1.0**
- встроенный `com.sun.net.httpserver.HttpServer` для integration-тестов `RNProvider`
- **ANU Quantum Random Numbers API** как внешний источник данных

Примечание: Mockito-зависимости остаются в `pom.xml`, но mode smoke tests используют lightweight test doubles вместо Mockito/ByteBuddy.

---

## Требования

- **JDK 25**
- **Maven 3.8+**
- API key для **ANU Quantum Random Numbers API** для режима `QUANTUM`
- доступ в интернет для живого источника квантовых чисел

В `pom.xml`:

```xml
<maven.compiler.source>25</maven.compiler.source>
<maven.compiler.target>25</maven.compiler.target>
```

Даже без API key приложение стартует и работает в `PSEUDO`.

---

## Настройка API-ключа

Проект ищет настройки в таком порядке:

1. переменные окружения;
2. `.env` из текущей рабочей директории;
3. `.env` рядом с запущенным JAR;
4. `.env` из домашней директории пользователя: `~/.rep-qrng-chaos-game/.env`;
5. `src/main/resources/config.properties`.

Правило преобразования имён:

```text
api.key → QRNG_API_KEY
api.url → QRNG_API_URL
panel.size.width → QRNG_PANEL_SIZE_WIDTH
```

Linux/macOS:

```bash
export QRNG_API_KEY=your_real_anu_api_key
mvn clean package
```

Windows PowerShell:

```powershell
$env:QRNG_API_KEY="your_real_anu_api_key"
mvn clean package
```

Пример `.env`:

```text
QRNG_API_KEY=your_real_anu_api_key
```

---

## Сборка и запуск

### Сборка

```bash
mvn clean package
```

После сборки Maven Assembly Plugin создаёт fat JAR:

```text
target/rep-qrng-chaos-game-1.0-SNAPSHOT-jar-with-dependencies.jar
```

### Запуск JAR

```bash
java -jar target/rep-qrng-chaos-game-1.0-SNAPSHOT-jar-with-dependencies.jar
```

### Запуск из IDE

Main class:

```text
org.ThreeDotsSierpinski.app.App
```

---

## Конфигурация по умолчанию

Основные значения из `src/main/resources/config.properties`:

| Параметр | Значение по умолчанию | Назначение |
|---|---:|---|
| `api.url` | `https://api.quantumnumbers.anu.edu.au` | Базовый URL API |
| `api.key` | `YOUR_API_KEY_HERE` | Placeholder, реальный ключ задаётся через env/.env |
| `api.data.type` | `uint16` | Тип случайных данных |
| `api.array.length` | `1024` | Число элементов в одном запросе |
| `api.block.size` | `2` | Размер логического блока |
| `api.max.requests` | `100` | Максимум API-запросов за сессию |
| `api.connect.timeout` | `10000` | Таймаут соединения, мс |
| `api.read.timeout` | `15000` | Таймаут чтения, мс |
| `random.queue.min.size` | `100` | Порог дозагрузки буфера |
| `random.min.value` | `0` | Нижняя граница диапазона |
| `random.max.value` | `65535` | Верхняя граница диапазона |
| `panel.size.width` | `680` | Базовая ширина области рисования |
| `panel.size.height` | `600` | Базовая высота области рисования |
| `dot.size` | `2` | Размер точки |
| `timer.delay` | `150` | Интервал Swing timer, мс |
| `dots.per.update` | `5` | Число новых точек за тик Sierpinski |
| `window.scale.width` | `1.5` | Масштаб окна по ширине |
| `window.scale.height` | `1.1` | Масштаб окна по высоте |
| `column.width` | `52` | Ширина колонки числового overlay |
| `row.height` | `14` | Высота строки числового overlay |
| `column.spacing` | `8` | Расстояние между колонками |
| `max.columns` | `5` | Максимум колонок числового overlay |
| `log.file.name` | `logs/app.log` | Application log |
| `log.level` | `INFO` | Уровень логирования |
| `random.log.true.file.name` | `logs/rnds-true.log` | TRUE/QUANTUM random stream log |
| `random.log.pseudo.file.name` | `logs/rnds-pseudo.log` | PSEUDO stream log path |
| `random.log.pseudo.enabled` | `false` | PSEUDO logging disabled by default |
| `random.log.flush.every.values` | `256` | Flush interval for random logs |

---

## Тесты

Проект содержит unit-, component-, smoke- и integration-тесты на **JUnit 5**.

### Что покрыто

- конфигурация и преобразование ключей;
- `.env` / environment priority;
- `LoggerConfig`;
- immutable `Dot` record;
- математика Sierpinski Chaos Game;
- обработка чисел и HEX;
- K-S, Frequency, Chi-Square и Runs tests;
- `NISTRandomnessTest` utility;
- `RandomnessTestSuite`;
- `TestResult` и quality-семантика;
- `RNProviderIntegrationTest` с локальным mock HTTP-сервером;
- registry режимов визуализации;
- smoke-тесты основных режимов визуализации без Mockito/ByteBuddy;
- dedicated tests для Lorenz и Mandelbrot modes;
- `RandomNumbersLog`.

### Tags

- `fast` — быстрые unit/component/smoke tests;
- `integration` — тесты с локальным HTTP-сервером для `RNProvider`;
- `slow` — более тяжёлые статистические/алгоритмические проверки.

### Запуск

```bash
mvn test
mvn test -Dgroups=fast
mvn test -Dgroups=integration
mvn test -DexcludedGroups=slow
```

### Запуск конкретных тестов

```bash
mvn -Dtest=VisualizationModeRegistryTest test
mvn -Dtest=VisualizationModesSmokeTest test
mvn -Dtest=RNProviderIntegrationTest test
mvn -Dtest=LorenzAttractor3DModeTest test
mvn -Dtest=MonteCarloMandelbrotAreaModeTest test
mvn -Dtest=MonteCarloMandelbrot3DAreaModeTest test
```

### Coverage

JaCoCo включён через `jacoco-maven-plugin`. Отчёт генерируется на фазе `test`.

---

## ZIP-контекст для ChatGPT

Для быстрой передачи актуального состояния проекта в ChatGPT используются скрипты:

```text
scripts/make-chatgpt-context-zip.ps1
scripts/make-chatgpt-context-zip.cmd
scripts/make-chatgpt-context-zip.sh
```

Они создают архив только с полезным проектным контекстом:

```text
pom.xml
Readme.md / README.md
src/
scripts/
```

В архив намеренно не попадают:

```text
target/
.git/
logs/
*.zip
```

Запуск из Git Bash:

```bash
./scripts/make-chatgpt-context-zip.sh
```

Запуск из Windows CMD:

```cmd
scripts\make-chatgpt-context-zip.cmd
```

Запуск через PowerShell:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\make-chatgpt-context-zip.ps1
```

---

## Ограничения и важные замечания

- для реального квантового режима нужен рабочий **ANU API key**;
- работа в `QUANTUM` зависит от сети и доступности внешнего API;
- лимиты ANU по запросам и битам влияют на длительность непрерывной сессии;
- fallback-режим сохраняет непрерывность приложения, но в этот момент используются псевдослучайные числа;
- статистические тесты встроены для практической оценки конкретной выборки, а не как строгая криптографическая сертификация;
- визуальные режимы показывают разные свойства случайности: фрактальность, блуждание, агрегацию, связность, распределение, spatial partitioning, chaos, bifurcation, strange attractors, Lissajous phase systems и Monte Carlo convergence;
- в репозитории не стоит хранить `target/`, `logs/`, временные build artifacts и generated reports.

---

## Типовой сценарий использования

1. получить API key от ANU;
2. положить его в `.env` или в `QRNG_API_KEY`;
3. собрать проект;
4. запустить `org.ThreeDotsSierpinski.app.App`;
5. выбрать категорию визуализаций;
6. выбрать конкретный режим;
7. наблюдать, как random stream формирует структуру;
8. при необходимости переключить `QUANTUM / PSEUDO`;
9. нажать **Test RNG** для runtime sanity checks;
10. сохранить изображение кнопкой **Save PNG**;
11. нажать **Выйти**, чтобы вернуться к выбору режима.

---

## Краткое резюме

`rep-qrng-chaos-game` — учебно-практический Java-проект, где поток случайных чисел превращается в живые визуальные структуры: фракталы, Monte Carlo-оценки, 3D-рельефы, странные аттракторы, бифуркации, Lissajous-фигуры, перколяционные кластеры, stochastic processes и statistical sanity checks.

В текущем ISTZUSTAND проект организован как небольшой scientific visualization framework с пакетами `app`, `mode`, `rng`, `stats`, `math`, `model` и `config`, а визуализации сгруппированы по научным доменам через `VisualizationCategory`.
