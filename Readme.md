# Quantum Sierpinski Triangle

## Проект: Визуализация фрактала Серпинского на квантовых случайных числах

### Обзор

Java Swing-приложение, которое строит фрактал Серпинского методом Chaos Game,
используя квантовые случайные числа от ANU QRNG API (Australian National University).
Квантовый генератор основан на вакуумных флуктуациях — это настоящий физический шум,
а не псевдослучайный алгоритм.

Если фрактал строится корректно — это визуальное подтверждение качества квантового
генератора. Для формальной проверки встроен тест Колмогорова-Смирнова.

### Быстрый старт

```bash
# 1. Клонировать репозиторий
git clone <url>
cd QuantumRandomNumberGenDez25

# 2. Настроить API-ключ (получить на https://quantumnumbers.anu.edu.au)
cp .env.example .env
# Вписать ключ в .env: QRNG_API_KEY=ваш_ключ

# 3. Собрать и запустить
mvn clean package
java -jar target/QuantumRandomNumberGeneratorSerpinski-1.0-SNAPSHOT-jar-with-dependencies.jar
```

### Требования

- Java 25+ (LTS)
- Maven 3.8+
- API-ключ ANU QRNG (бесплатный, Trial: 100 запросов/месяц)

### Настройка API-ключа

Ключ загружается по цепочке приоритетов (от высшего к низшему):

1. Переменная окружения `QRNG_API_KEY`
2. Файл `.env` в корне проекта
3. `config.properties` (содержит только placeholder)

Файл `.env` добавлен в `.gitignore` и не попадает в git.

---

## Архитектура

### GUI (Swing)

- **App.java** — точка входа. Создаёт главное окно, панель статуса, кнопки Play/Stop и «Проверить качество». Связывает компоненты и запускает фоновую загрузку данных.
- **DotController.java** — панель рисования (JPanel). Управляет таймером анимации, рендерингом точек на offscreen-буфере и отображением стека случайных чисел. Делегирует вычисление позиций в `SierpinskiAlgorithm`.
- **Dot.java** — immutable record, представляющий точку на плоскости с защитным копированием.

### Алгоритм

- **SierpinskiAlgorithm.java** — чистая математика Chaos Game без зависимостей от Swing. Принимает размеры области и диапазон случайных чисел, вычисляет новую позицию точки (середина отрезка к случайной вершине треугольника).

### Данные (API + обработка)

- **RNProvider.java** — загрузка квантовых случайных чисел из ANU API. Неблокирующий `getNextRandomNumber()` (безопасен для EDT), exponential backoff при ошибках (1с → 2с → 4с → 8с → 16с), фоновая предзагрузка при снижении буфера.
- **RandomNumberProcessor.java** — маппинг чисел из диапазона API (uint8/uint16/hex16) в произвольный целевой диапазон с равномерным floor-распределением.
- **Config.java** — загрузка конфигурации с приоритетами: переменная окружения → `.env` файл → `config.properties`. Преобразует ключи из `dot.notation` в `QRNG_UPPER_SNAKE_CASE`.

### Observer

- **RNLoadListener.java** — интерфейс для уведомлений о состоянии загрузки данных.
- **RNLoadListenerImpl.java** — реализация: обновляет статус-бар и отображает сырые данные API в отдельном окне под главным.

### Тестирование качества

- **RandomnessTest.java** — интерфейс для тестов случайности.
- **KolmogorovSmirnovTest.java** — тест Колмогорова-Смирнова для проверки равномерности распределения квантовых чисел.

### Инфраструктура

- **LoggerConfig.java** — настройка `java.util.logging` с файловым и консольным выводом, автосоздание директории `logs/`, graceful fallback при ошибках.

### Тесты (9 файлов)

- `ConfigTest` — приоритеты загрузки, `toEnvVarName()`, обработка отсутствующих ключей
- `DotTest` — иммутабельность record, защитное копирование
- `SierpinskiAlgorithmTest` — Chaos Game: вершины, границы, сходимость, пустой центр, детерминизм
- `RandomNumberProcessorTest` — hex-парсинг, floor-маппинг, равномерность на границах
- `KolmogorovSmirnovTestUnitTest` — конструкторы, валидация, статистические свойства
- `NISTRandomnessTest`, `NISTRandomnessTestUnitTest` — NIST-тесты частот и серий
- `StatisticalRandomnessTest` — хи-квадрат, автокорреляция, монотонность, покрытие
- `LoggerConfigTest` — инициализация логгера, fallback

---

## Project: Sierpinski fractal visualization using quantum random numbers

### Overview

A Java Swing application that builds a Sierpinski fractal using the Chaos Game method,
powered by quantum random numbers from the ANU QRNG API (Australian National University).
The quantum generator is based on vacuum fluctuations — true physical noise,
not a pseudorandom algorithm.

A correctly formed fractal serves as visual confirmation of the quantum generator's quality.
A built-in Kolmogorov-Smirnov test provides formal statistical verification.

### Quick start

```bash
# 1. Clone the repository
git clone <url>
cd QuantumRandomNumberGenDez25

# 2. Configure API key (get one at https://quantumnumbers.anu.edu.au)
cp .env.example .env
# Edit .env: QRNG_API_KEY=your_key

# 3. Build and run
mvn clean package
java -jar target/QuantumRandomNumberGeneratorSerpinski-1.0-SNAPSHOT-jar-with-dependencies.jar
```

### Requirements

- Java 25+ (LTS)
- Maven 3.8+
- ANU QRNG API key (free, Trial: 100 requests/month)

### API key configuration

The key is loaded using a priority chain (highest to lowest):

1. Environment variable `QRNG_API_KEY`
2. `.env` file in the project root
3. `config.properties` (contains a placeholder only)

The `.env` file is listed in `.gitignore` and is never committed.