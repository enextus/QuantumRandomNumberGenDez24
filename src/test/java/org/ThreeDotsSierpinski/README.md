# Тесты для Quantum Sierpinski Triangle

## Обзор

Создан полноценный набор JUnit 5 тестов для проекта. Старые тесты были удалены, так как имели серьёзные проблемы:

### Проблемы старых тестов:
| Файл | Проблема |
|------|----------|
| `KolmogorovSmirnovTest.java` | Не JUnit тест (main класс), конфликт имён с main классом |
| `ChiSquareTest.java` | Использует `java.util.Random` вместо `RNProvider` |
| `RunsTest.java` | Делит на 255 (uint8) вместо 65535 (uint16) |
| `KolmogorovTest.java` | Не JUnit тест (main класс) |
| `RandomnessTester.java` | GUI приложение, не тест |
| `NISTRandomnessTest.java` | Утилитный класс без тестов |
| `RandomNumberGeneratorTest.java` | Тестирует `Math.random()` вместо `RNProvider` |

## Новые тесты

### 1. ConfigTest.java
Тестирует класс `Config`:
- Загрузка строковых, числовых параметров
- API конфигурация (url, key, timeouts)
- Panel параметры
- Обработка ошибок для несуществующих ключей

### 2. RandomNumberProcessorTest.java
Тестирует класс `RandomNumberProcessor`:
- Преобразование HEX в числа
- Генерация чисел в диапазоне
- Граничные значения (0, 65535)
- Работа с uint8/uint16
- Равномерность распределения

### 3. KolmogorovSmirnovTestUnitTest.java
Тестирует класс `KolmogorovSmirnovTest`:
- Конструкторы (default, параметризованный)
- Валидация входных данных (null, пустой список, некорректный alpha)
- Статистические тесты (равномерное, смещённое распределение)
- Интеграция с интерфейсом `RandomnessTest`

### 4. StatisticalRandomnessTest.java
Комплексные статистические тесты:
- **Частотный тест** - равномерность битов и чисел
- **Тест на серии (Runs)** - отсутствие паттернов
- **Хи-квадрат тест** - равномерность по интервалам
- **Автокорреляция** - независимость последовательных чисел
- **Монотонность** - отсутствие длинных возрастающих/убывающих серий
- **Покрытие диапазона** - использование всего диапазона значений

### 5. DotTest.java
Тестирует record `Dot`:
- Создание с координатами
- Равенство и hashCode
- Граничные координаты

### 6. SierpinskiAlgorithmTest.java
Тестирует алгоритм Chaos Game:
- Перемещение к вершинам A, B, C
- Точки остаются внутри треугольника
- Сходимость к фрактальной структуре
- Равномерность выбора вершин
- Центральная пустая область
- Воспроизводимость

### 7. NISTRandomnessTestUnitTest.java
Тестирует утилитный класс `NISTRandomnessTest`:
- frequencyTest() - частотный тест
- runsTest() - тест на серии
- Обработка некорректных входных данных
- Преобразование чисел в биты

## Установка

Скопируйте все файлы в:
```
src/test/java/org/ThreeDotsSierpinski/
```

## Запуск тестов

```bash
# Все тесты
mvn test

# Конкретный тест
mvn test -Dtest=ConfigTest

# С подробным выводом
mvn test -Dtest=StatisticalRandomnessTest -X
```

## Зависимости (уже в pom.xml)

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.3</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <version>5.12.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <version>3.27.2</version>
    <scope>test</scope>
</dependency>
```

## Структура тестов

```
src/test/java/org/ThreeDotsSierpinski/
├── ConfigTest.java                    # Unit тесты Config
├── RandomNumberProcessorTest.java     # Unit тесты RandomNumberProcessor
├── KolmogorovSmirnovTestUnitTest.java # Unit тесты KolmogorovSmirnovTest
├── StatisticalRandomnessTest.java     # Статистические тесты
├── DotTest.java                       # Unit тесты Dot
├── SierpinskiAlgorithmTest.java       # Тесты алгоритма
└── NISTRandomnessTestUnitTest.java    # Unit тесты NISTRandomnessTest
```

## Покрытие

| Класс | Покрытие |
|-------|----------|
| Config | ✅ Полное |
| RandomNumberProcessor | ✅ Полное |
| KolmogorovSmirnovTest | ✅ Полное |
| Dot | ✅ Полное |
| NISTRandomnessTest | ✅ Полное |
| Алгоритм Серпинского | ✅ Полное |
| RNProvider | ⚠️ Требует интеграционных тестов с API |
| DotController | ⚠️ GUI компоненты сложно тестировать |

## Примечания

1. **Тесты не требуют реального API** - используют `SecureRandom` для генерации тестовых данных
2. **Избежан конфликт имён** - `KolmogorovSmirnovTestUnitTest` вместо `KolmogorovSmirnovTest`
3. **Диапазон uint16** - все тесты используют правильный диапазон 0-65535
4. **Статистические тесты** - используют стандартные методы проверки случайности (Chi-Square, Runs, K-S)
