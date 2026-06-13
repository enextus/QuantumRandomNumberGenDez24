# rep-qrng-chaos-game

Java 25 / Swing-приложение для визуализации потоков случайных чисел. Проект начинался как **Chaos Game / Sierpinski Triangle** на квантовых случайных числах, а сейчас стал небольшой лабораторией генеративной случайности, фракталов, Monte Carlo-оценок, динамических систем и stochastic processes.

Проект использует два источника случайности:

- **QUANTUM** — истинные случайные числа из **ANU Quantum Random Numbers API**;
- **PSEUDO** — локальный fallback на `L128X256MixRandom`, если API недоступен, не задан ключ или исчерпан лимит.

Важно: приложение **не доказывает** «истинную случайность» генератора. Оно визуализирует поток чисел и даёт прикладные **statistical sanity checks** по тем значениям, которые реально были использованы в текущей сессии.

---

## Current status

Актуальное состояние после архитектурных cleanup-патчей:

| Метрика | Значение |
|---|---|
| Java | 25 |
| Build tool | Maven |
| Compiler setup | `maven-compiler-plugin` + `<release>25</release>` |
| Tests | запускать локально через `mvn test` |
| Last verified local result | `BUILD SUCCESS` |
| Last verified at | `2026-06-13T07:53:41+02:00` |
| JaCoCo | enabled |

README намеренно не фиксирует ожидаемое число тестов: оно часто меняется при добавлении режимов, smoke-тестов и integration-тестов. Source of truth для проверки — локальный Maven-прогон:

```bash
mvn test
```

### RNG budget policy

Heavy visual modes keep their rich animation speed in **PSEUDO** mode, but use a conservative per-frame budget in **QUANTUM** mode. This prevents Monte Carlo dashboards, density maps, random walks and other high-throughput modes from consuming ANU API quota too quickly.

The budget is intentionally simple: each heavy mode declares its normal local/PSEUDO batch size and a smaller QUANTUM batch size via `RngStepBudget`. `SierpinskiMode` still uses the global `dots.per.update` setting from `config.properties`.

---

## Visualization categories

### Chaos & fractals

- **Sierpinski Triangle** — классический Chaos Game.
- **Barnsley Fern** — IFS-фрактал папоротника Барнсли.
- **Fractal Flame** — нелинейный IFS / chaos-game flame renderer с QRNG-driven transform selection.
- **CGR Bit Stream** — Chaos Game Representation для битового потока.

### Monte Carlo

- **Monte Carlo π Dashboard** — оценка π через случайные точки в единичном квадрате.
- **Monte Carlo Mandelbrot Area** — оценка площади множества Мандельброта методом Monte Carlo.
- **Monte Carlo Mandelbrot 3D Area** — 3D-рельеф escape-time над комплексной плоскостью.
- **Buddhabrot** — histogram accumulation escaping Mandelbrot orbits: бело-сине-золотая density nebula.

### Physics / complex systems

- **Lorenz Attractor 3D** — 3D-аттрактор Лоренца с ансамблем траекторий и QRNG-jitter.
- **Aizawa / Rössler / Thomas / Dadras / Halvorsen Attractors** — дополнительные strange-attractor визуализации.
- **Lissajous family** — 3D, frequency, oscilloscope, spectral и quantum-vs-pseudo варианты фигур Лиссажу.
- **Bifurcation Diagram** — бифуркационная диаграмма логистического отображения.
- **Chirikov Standard Map** — фазовое пространство Hamiltonian chaos: KAM-islands и chaotic sea.
- **Percolation** — site percolation, top-connected clusters и spanning cluster.
- **Forest Fire** — клеточная модель роста леса, молний и распространения огня.
- **Abelian Sandpile** — self-organized criticality: случайные зёрна вызывают лавины toppling.

### Stochastic processes

- **Rule 30 Automaton** — elementary cellular automaton: чёрно-белый треугольник хаоса + raw RNG bit stream для сравнения.
- **Galton Board** — биномиальное распределение из бинарных случайных решений.
- **Random Walk Heatmap** — тепловая карта посещений случайного блуждания.
- **Voronoi Mosaic** — диаграмма Вороного с Lloyd relaxation.
- **DLA / Brownian Tree** — diffusion-limited aggregation.

---

## Что умеет приложение

- показывает стартовое окно выбора режима визуализации;
- поддерживает workflow loop: выбор режима → окно визуализации → **Выйти** → возврат к выбору режима;
- использует 3-колоночное меню выбора режимов;
- корректно работает с несколькими мониторами;
- рисует визуализации в реальном времени через Swing/AWT;
- получает числа из ANU QRNG API по HTTP с заголовком `x-api-key`;
- поддерживает локальный буфер случайных чисел и фоновую дозагрузку;
- не блокирует EDT при получении новых чисел;
- автоматически переключается в **PSEUDO** при недоступности API, отсутствии ключа или исчерпании лимита;
- пытается вернуться в **QUANTUM** после восстановления внешнего источника;
- поддерживает **Play / Stop** для паузы и продолжения анимации;
- поддерживает переключатель источника RNG: **QUANTUM / PSEUDO**;
- отображает уже использованные числа в правой части окна для светлых/AppleMac-режимов;
- числовая таблица адаптируется к высоте окна;
- запускает встроенные статистические тесты кнопкой **Test RNG**;
- показывает результаты тестов с уровнями качества `STRONG / MARGINAL / FAIL`;
- сохраняет текущую визуализацию в **PNG**;
- поддерживает mode-specific controls;
- поддерживает визуальный стиль **AppleMac** для Sierpinski UI;
- пишет application logs в `logs/app.log`;
- пишет TRUE RNG stream в `logs/rnds-true.log`.

---

## Visualization styles

### Classic

Базовый стиль приложения: светлые и тёмные режимы, цветные canvas-визуализации, dashboard-панели и mode-specific controls.

### AppleMac

Монохромный retro UI в духе classic Macintosh / System 6/7 для режима Sierpinski Triangle.

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

## Режимы визуализации

### Sierpinski Triangle

Классический Chaos Game.

Алгоритм:

1. берётся текущая точка;
2. очередное случайное число выбирает одну из трёх вершин треугольника;
3. новая точка ставится на середину отрезка между текущей точкой и выбранной вершиной;
4. процесс повторяется много раз.

В результате возникает треугольник Серпинского. В AppleMac-style режим получает отдельную plot-area, counter/status block и right sidebar с таблицей использованных чисел.

---

### Barnsley Fern

Папоротник Барнсли строится как **Iterated Function System**. Каждое случайное число выбирает одно из четырёх аффинных преобразований. Из последовательности таких решений постепенно появляется узнаваемая форма фрактального папоротника.

---

### CGR Bit Stream

Chaos Game Representation переводит битовый поток в 2D-карту. Пустоты, полосы и симметрии помогают визуально заметить скрытую структуру или перекосы в последовательности.

---

### Fractal Flame

Fractal Flame — нелинейный IFS / Chaos Game renderer. На каждом шаге очередное random number выбирает одно из нескольких нелинейных преобразований. Точки накапливаются как светящаяся density trail, и из случайного выбора transform постепенно появляется устойчивая flame-структура.

Особенности:

- QRNG/PSEUDO выбирает transform branch;
- presets: **Swirl**, **Sinusoidal**, **Bubble**;
- тёмный canvas и накопление полупрозрачных flame-pixels;
- хорошо показывает принцип `random choices → nonlinear transforms → visible structure`.

---

### Rule 30 Automaton

Elementary Cellular Automaton с правилом 30. Режим строит две синхронные чёрно-белые панели:

- слева — детерминированная CA-эволюция, где первая строка задаётся текущим RNG-потоком;
- справа — прямой raw RNG bit stream с той же геометрией строк;
- редкие мутации ячеек могут включаться/выключаться и тоже берутся из текущего RNG-потока;
- кнопка **Random rule** позволяет выбрать другое elementary CA rule из текущего QRNG/PSEUDO stream, а **Reset Rule 30** возвращает классический Rule 30.

Смысл режима — показать различие между **детерминированным хаосом**, который рождается из простого правила, и непосредственной тканью битов из TRUE/PSEUDO RNG.

---

### Monte Carlo π Dashboard

Случайные точки бросаются в квадрат `[0,1] × [0,1]`. Доля точек внутри четверти окружности даёт оценку π:

```text
π ≈ 4 × inside / total
```

Dashboard показывает sample space, convergence chart, absolute error и метрики `TOTAL SAMPLES`, `POINTS INSIDE`, `POINTS OUTSIDE`, `ESTIMATE OF π`, `ABSOLUTE ERROR`, `RELATIVE ERROR`.

---

### Monte Carlo Mandelbrot Area

Случайные точки выбираются в комплексной плоскости. Для каждой точки выполняется bounded/escaped test для множества Мандельброта. Доля bounded-точек даёт оценку площади множества в выбранном окне.

Особенности:

- окно комплексной плоскости примерно `Re [-2.0, 1.0]`, `Im [-1.5, 1.5]`;
- настраиваемое число `max iterations`;
- отображение bounded/inside и escaped-точек;
- dashboard с area estimate и error vs reference.

---

### Monte Carlo Mandelbrot 3D Area

3D-вариант Mandelbrot Monte Carlo: escape-time превращается в высоту над комплексной плоскостью. Вращающееся облако показывает relief множества, ridge-зоны и fast escape области.

---

### Buddhabrot

Buddhabrot строится не по bounded/inside-точкам, а по траекториям **escaping orbits**. Случайная точка `c` в комплексной плоскости запускает орбиту `zₙ₊₁ = zₙ² + c`; если орбита убегает, её путь накапливается в histogram-buffer. Из такой плотности постепенно проявляется туманная "призрачная" фигура.

Особенности:

- Monte Carlo sampling точек `c` в окне Мандельброта;
- аналитический skip главной кардиоиды и period-2 bulb для ускорения;
- log-scaled density rendering;
- палитра **white → blue → gold** на чёрном фоне;
- встроенный help overlay через кнопку **(?)** рядом с заголовком;
- diagnostic strip: `x-balance`, `y-balance`, `quadrant balance`, `serial correlation`, `accepted ratio`, `symmetry score`;
- mode-specific controls: **Reset** и выбор числа итераций.

---

### Lorenz Attractor 3D

Ансамбль близких траекторий интегрируется методом RK4 в системе Лоренца. QRNG/PSEUDO-числа дают микроджиттер параметров `σ / ρ / β`, поэтому режим показывает чувствительную зависимость от начальных условий и параметров.

Dashboard показывает:

- trace points;
- particles;
- spread;
- current parameters;
- jitter;
- trail length.

---

### Bifurcation Diagram

Бифуркационная диаграмма логистического отображения:

```text
xₙ₊₁ = r · xₙ · (1 - xₙ)
```

Параметр `r` идёт по горизонтали. После warmup-итераций рисуются устойчивые значения `x`. Режим показывает переход:

```text
стабильная точка → 2-cycle → 4-cycle → каскад бифуркаций → хаос → окна порядка
```

---

### Chirikov Standard Map

Chirikov Standard Map показывает Hamiltonian chaos на торе фазового пространства. QRNG/PSEUDO задаёт начальные точки `(x, p)`, а затем они детерминированно развиваются по standard map. При малых `K` видны invariant curves и острова порядка, при больших `K` появляется chaotic sea.

Особенности:

- фазовое пространство: `x` по горизонтали, `p` по вертикали;
- параметр **K** переключается из mode-specific controls;
- QRNG отвечает за sampling начальных условий;
- режим хорошо демонстрирует сосуществование order и chaos;
- нижний diagnostic strip показывает `x/p seed balance`, `quadrant balance`, `corrXP`, `64×64 cell occupancy`, coverage и normalized entropy для оценки качества initial phase sampling.

---

### Percolation

Site percolation на квадратной решётке. Каждая клетка получает случайное решение: открыта или закрыта. После каждого шага пересчитывается top-connected cluster. Если он достигает нижней границы, возникает **spanning cluster**.

Особенности:

- стартовая вероятность открытия клетки около `p≈0.59`;
- cyan показывает top-connected cluster;
- gold показывает spanning state;
- mode-specific controls позволяют менять `p` и сбрасывать решётку.

---

### Forest Fire

Клеточная модель forest-fire dynamics. Деревья растут случайно, молнии поджигают клетки, огонь распространяется по соседям, а затем лес снова восстанавливается. Режим показывает self-organized patterns и динамику распространения.

---

### Abelian Sandpile

Abelian Sandpile / Bak–Tang–Wiesenfeld model показывает self-organized criticality. QRNG/PSEUDO выбирает клетки, куда падают новые зёрна. Когда высота клетки достигает 4, она topples и отдаёт зёрна соседям, что может запустить avalanche cascade.

Особенности:

- lattice heights `0..3` окрашены разными цветами;
- текущая avalanche подсвечивается;
- metrics: grains, last/max avalanche, total topples, consumed random numbers;
- controls позволяют менять batch size зёрен за animation step.

---

### Galton Board

Доска Гальтона показывает, как из множества бинарных случайных решений возникает биномиальное распределение.

```text
bit 0 → left
bit 1 → right
```

Особенности:

- анимированные падающие шарики;
- цвет шарика зависит от path bias;
- нижняя гистограмма показывает реальные bucket counts;
- ожидаемая биномиальная форма накладывается как reference.

---

### Random Walk Heatmap

Несколько walkers стартуют из центра. Каждое случайное число выбирает одно из 8 направлений движения. Чем чаще пиксель посещается, тем ярче и теплее становится его цвет.

---

### Voronoi Mosaic

Случайные точки становятся центрами ячеек диаграммы Вороного. Lloyd relaxation сдвигает точки к центроидам ячеек и делает мозаику более равномерной.

Особенности:

- цветные многоугольные ячейки;
- затемнённые границы между ячейками;
- белые метки центров;
- переключатель **Метки ВКЛ./ВЫКЛ.**.

---

### DLA / Brownian Tree

Diffusion-Limited Aggregation. Частицы случайно блуждают по полю и прилипают к уже существующему кластеру. Получается структура, похожая на кораллы, молнии, кристаллы или рост колоний.

---

## Технологический стек

- **Java 25**
- **Swing / AWT**
- **Maven**
- **Maven Compiler Plugin** с `release 25`
- **Jackson Databind** для JSON
- **FlatLaf** для Swing UI
- **JUnit 5** для тестов
- **JaCoCo** для coverage report
- встроенный `com.sun.net.httpserver.HttpServer` для integration-тестов `RNProvider`
- **ANU Quantum Random Numbers API** как внешний источник данных

Smoke-тесты используют lightweight test doubles. Mockito-зависимости удалены из `pom.xml`.

---

## Как это работает

### 1. Источник случайных чисел

`RNProvider` управляет состоянием источника случайных чисел, локальной очередью, history/ring-buffer, fallback `QUANTUM → PSEUDO` и reconnect lifecycle. HTTP-запросы к ANU API, проверка статуса ответа и JSON parsing вынесены в `QuantumNumbersApiClient`.

По умолчанию проект запрашивает:

- тип данных: `uint16`;
- длину массива: `1024`;
- минимальный локальный буфер: `100`;
- максимум запросов за сессию: `100`.

При временных ошибках используется retry с exponential backoff. Если загрузка не удалась, API key не задан или лимит запросов исчерпан, `RNProvider` переключается в `PSEUDO` и начинает выдавать числа из локального генератора `L128X256MixRandom`.

После нескольких pseudo-batch-циклов провайдер пытается снова обратиться к ANU API. Если загрузка успешна, приложение возвращается в `QUANTUM`. Background loading/reconnect защищены lifecycle-флагом `shutdownRequested`, чтобы после shutdown не продолжались callbacks, queue updates или запись в log.

---

### 2. Логгирование случайных чисел

Проект ведёт отдельное логгирование случайных чисел:

```text
logs/rnds-true.log
logs/rnds-pseudo.log
```

Текущая рабочая политика: TRUE random numbers пишутся в `logs/rnds-true.log`. PSEUDO logging инфраструктурно поддержан отдельно, но pseudo-числа не должны смешиваться с true-логом.

Каждый новый lifecycle `RNProvider` / запуск режима отделяется пустой строкой.

---

### 3. Абстракция режимов визуализации

Все режимы реализуют интерфейс `VisualizationMode`.

Режим отвечает за:

- уникальный id;
- имя;
- описание;
- иконку;
- инициализацию canvas;
- один шаг анимации;
- количество нарисованных точек;
- количество использованных случайных чисел;
- dark/light background flag;
- point counter placement;
- mode-specific controls;
- redraw текущего состояния без потребления новых чисел;
- optional reserved drawing areas.

Реестр доступных режимов находится в:

```java
VisualizationModes.all()
```

---

### 4. Отрисовка

`DotController` работает как Swing-панель orchestration-level, которая:

- хранит `offscreenImage`;
- обновляет изображение через `javax.swing.Timer`;
- делегирует шаг визуализации выбранному `VisualizationMode`;
- выполняет работу с canvas на EDT;
- показывает счётчик точек;
- показывает текущий режим RNG;
- отображает стек использованных чисел через `RandomNumbersStackOverlay`;
- поддерживает `refreshVisualization()`;
- делегирует AppleMac frame/layout/styling в `AppleMacChrome`;
- поддерживает AppleMac plot/sidebar separation;
- поддерживает reserved UI areas;
- останавливает timers через `shutdown()`.

---

### 5. Workflow приложения

```text
App.main()
    ↓
ModeSelectionDialog
    ↓
Visualization JFrame + DotController
    ↓
Выйти / закрытие окна
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
- style selector;
- запуском статистических тестов;
- сохранением PNG;
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
│   ├── DotController
│   ├── AppleMacChrome
│   └── RandomNumbersStackOverlay
├── mode
│   ├── VisualizationMode
│   ├── VisualizationModes
│   ├── VisualizationCategory
│   ├── VisualizationStyle
│   ├── chaos
│   │   ├── SierpinskiMode
│   │   ├── BarnsleyFernMode
│   │   ├── FractalFlameMode
│   │   └── ChaosGameRepresentationMode
│   ├── montecarlo
│   │   ├── MonteCarloPiMode
│   │   ├── MonteCarloMandelbrotAreaMode
│   │   ├── MonteCarloMandelbrot3DAreaMode
│   │   └── BuddhabrotMode
│   ├── physics
│   │   ├── LorenzAttractor3DMode
│   │   ├── AizawaAttractorMode
│   │   ├── RosslerAttractorMode
│   │   ├── ThomasAttractorMode
│   │   ├── DadrasAttractorMode
│   │   ├── HalvorsenAttractorMode
│   │   ├── AbstractStrangeAttractorMode
│   │   ├── Lissajous3DMode
│   │   ├── LissajousFrequencyMode
│   │   ├── LissajousOscilloscopeMode
│   │   ├── LissajousQuantumVsPseudoMode
│   │   ├── LissajousSpectralAnalyzerMode
│   │   ├── AbstractLissajousMode
│   │   ├── ChirikovStandardMapMode
│   │   ├── BifurcationDiagramMode
│   │   ├── PercolationMode
│   │   ├── ForestFireMode
│   │   └── AbelianSandpileMode
│   └── stochastic
│       ├── GaltonBoardMode
│       ├── RandomWalkHeatmapMode
│       ├── VoronoiMode
│       ├── Rule30AutomatonMode
│       ├── SpectralPlotMode
│       └── DLAMode
├── rng
│   ├── RNProvider
│   ├── QuantumNumbersApiClient
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
- **`ModeSelectionDialog`** — 3-колоночное окно выбора режима.
- **`DotController`** — центральная Swing-панель визуализации и orchestration для animation/render lifecycle.
- **`AppleMacChrome`** — AppleMac frame/layout/component styling.
- **`RandomNumbersStackOverlay`** — rendering таблицы использованных случайных чисел с ограниченным snapshot хвоста history.
- **`VisualizationMode`** — общий контракт режима визуализации.
- **`VisualizationModes`** — registry доступных concrete modes.
- **`VisualizationStyle`** — стиль визуализации, включая `APPLE_MAC`.
- **`RNProvider`** — state machine, queue/history, fallback и reconnect lifecycle для `QUANTUM → PSEUDO → QUANTUM`.
- **`QuantumNumbersApiClient`** — HTTP-клиент ANU API и JSON parsing.
- **`RandomNumbersLog`** — файловое логгирование потоков случайных чисел.
- **`RandomnessTestSuite`** — runtime sanity checks.
- **`Config`** — загрузка настроек из environment, `.env`, `config.properties`.
- **`LoggerConfig`** — файловое и консольное логирование.

---

## Runtime-тесты случайности

`RandomnessTestSuite` запускает 4 теста:

- **`KolmogorovSmirnovTest`** — отклонение эмпирического распределения от равномерного;
- **`FrequencyBitTest`** — баланс битов `0/1`;
- **`ChiSquareUniformityTest`** — χ²-проверка равномерности по корзинам;
- **`RunsBitTest`** — тест серий по битовой последовательности.

Кнопка **Test RNG** показывает диалог с уровнями качества:

- `STRONG`;
- `MARGINAL`;
- `FAIL`.

---

## Требования

- **JDK 25**
- **Maven 3.8+**
- API key для **ANU Quantum Random Numbers API** для режима `QUANTUM`
- доступ в интернет для живого источника квантовых чисел

В `pom.xml`:

```xml
<maven.compiler.release>25</maven.compiler.release>
<maven-compiler-plugin.version>3.15.0</maven-compiler-plugin.version>
```

Компиляция закреплена через явный `maven-compiler-plugin` и `<release>${maven.compiler.release}</release>`, а не через пару `source/target`.

Даже без API key приложение стартует в `PSEUDO`.

---

## Настройка API-ключа

Проект ищет настройки в таком порядке:

1. переменные окружения;
2. файл `.env` в корне проекта;
3. `src/main/resources/config.properties`.

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

Основные значения из `config.properties`:

| Параметр | Значение по умолчанию | Назначение |
|---|---:|---|
| `api.url` | `https://api.quantumnumbers.anu.edu.au` | Базовый URL API |
| `api.data.type` | `uint16` | Тип случайных данных |
| `api.array.length` | `1024` | Число элементов в одном запросе |
| `api.block.size` | `2` | Размер логического блока |
| `api.max.requests` | `100` | Максимум API-запросов за сессию |
| `api.connect.timeout` | `10000` | Таймаут соединения, мс |
| `api.read.timeout` | `15000` | Таймаут чтения, мс |
| `random.queue.min.size` | `100` | Порог дозагрузки буфера |
| `random.min.value` | `0` | Нижняя граница диапазона |
| `random.max.value` | `65535` | Верхняя граница диапазона |
| `panel.size.width` | `600` | Базовая ширина области рисования |
| `panel.size.height` | `600` | Базовая высота области рисования |
| `dot.size` | `2` | Размер точки |
| `timer.delay` | `150` | Интервал таймера, мс |
| `dots.per.update` | `5` | Число новых точек за тик Sierpinski |
| `window.scale.width` | `1.5` | Масштаб окна по ширине |
| `window.scale.height` | `1.1` | Масштаб окна по высоте |
| `log.file.name` | `logs/app.log` | Application log |
| `log.level` | `INFO` | Уровень логирования |

---

## ZIP-контекст для ChatGPT

Для быстрой передачи актуального состояния проекта в ChatGPT используется один Git Bash-скрипт:

```text
scripts/make-chatgpt-context-zip.sh
```

Он создаёт архив только с полезным проектным контекстом:

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

Скрипт использует красивую progress-анимацию и fallback-упаковку без лишнего `META-INF` manifest noise.

---

## Тесты

Проект содержит unit-, component- и integration-тесты на **JUnit 5**. Точное количество тестов не фиксируется в README; актуальный результат показывает Maven при запуске `mvn test`.

Последний подтверждённый локальный прогон:

```text
BUILD SUCCESS
Failures: 0
Errors: 0
Skipped: 0
Finished at: 2026-06-13T07:53:41+02:00
```

### Что покрыто

- конфигурация и преобразование ключей;
- immutable `Dot` record;
- математика Sierpinski Chaos Game;
- обработка чисел и HEX;
- K-S, Frequency, Chi-Square и Runs tests;
- `RandomnessTestSuite`;
- `TestResult` и quality-семантика;
- `LoggerConfig`;
- `RNProviderIntegrationTest` с локальным mock HTTP-сервером;
- registry режимов визуализации;
- smoke-тесты режимов визуализации;
- dedicated tests для Lorenz и Mandelbrot modes.

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
```

---

## Ограничения и важные замечания

- для реального квантового режима нужен рабочий **ANU API key**;
- работа в `QUANTUM` зависит от сети и доступности внешнего API;
- лимиты ANU по запросам и битам влияют на длительность непрерывной сессии;
- fallback-режим сохраняет непрерывность приложения, но в этот момент используются псевдослучайные числа;
- статистические тесты встроены для практической оценки конкретной выборки, а не как строгая криптографическая сертификация;
- визуальные режимы показывают разные свойства случайности: фрактальность, cellular automata, raw bit streams, блуждание, агрегацию, связность, распределение, spatial partitioning, chaos, bifurcation и Monte Carlo convergence;
- в репозитории не стоит хранить `target/`, `logs/`, временные build artifacts и generated reports.

---

## Типовой сценарий использования

1. получить API key от ANU;
2. положить его в `.env` или в `QRNG_API_KEY`;
3. собрать проект;
4. запустить `org.ThreeDotsSierpinski.app.App`;
5. выбрать режим визуализации;
6. наблюдать, как random stream формирует структуру;
7. при необходимости переключить `QUANTUM / PSEUDO`;
8. при необходимости выбрать style, например `AppleMac` для Sierpinski;
9. нажать **Test RNG** для runtime sanity checks;
10. сохранить изображение кнопкой **Save PNG**;
11. нажать **Выйти**, чтобы вернуться к выбору режима.

---

## Краткое резюме

`rep-qrng-chaos-game` — учебно-практический Java-проект, где поток случайных чисел превращается в живые визуальные структуры: фракталы, Monte Carlo-оценки, Buddhabrot density maps, Fractal Flames, Hamiltonian chaos maps, sandpile criticality, 3D-рельефы, аттракторы, бифуркации, перколяционные кластеры, stochastic processes и statistical sanity checks.

После последних cleanup-патчей проект организован как небольшой scientific visualization framework с пакетами `app`, `mode`, `rng`, `stats`, `math`, `model` и `config`, а визуализации сгруппированы по научным доменам: `chaos`, `montecarlo`, `physics`, `stochastic`. Registry режимов вынесен в `VisualizationModes`, AppleMac/random-stack rendering вынесены из `DotController`, а ANU API HTTP/JSON слой вынесен из `RNProvider` в `QuantumNumbersApiClient`.
