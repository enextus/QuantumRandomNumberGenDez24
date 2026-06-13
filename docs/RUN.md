# Run guide

Практическая инструкция по запуску `rep-qrng-chaos-game` из IntelliJ IDEA, Maven и packaged JAR.

Проект рассчитан на **Java 25** и Maven. Даже без ANU API key приложение запускается и работает в локальном `PSEUDO` fallback-режиме на `L128X256MixRandom`.

---

## 1. Требования

| Компонент | Рекомендованное значение |
|---|---|
| JDK | Java 25, например BellSoft Liberica JDK 25 |
| Build tool | Maven 3.9.x |
| Main class | `org.ThreeDotsSierpinski.app.App` |
| GUI | Swing / AWT |

Проверка окружения:

```bash
java -version
mvn -version
```

---

## 2. Запуск из IntelliJ IDEA

1. Открыть корень проекта:

   ```text
   C:\Projects\rep-qrng-chaos-game
   ```

   Важно открывать именно папку, где лежит `pom.xml`.

2. Убедиться, что проект импортирован как Maven project.

   Если справа Maven panel показывает `unknown` или зависимости подсвечиваются красным:

   ```text
   Right click pom.xml → Add as Maven Project
   Maven panel → Reload All Maven Projects
   ```

3. Проверить JDK:

   ```text
   File → Project Structure → Project SDK → JDK 25
   Settings → Build Tools → Maven → Importing → JDK for importer → JDK 25
   ```

4. Создать Run Configuration:

   ```text
   Main class: org.ThreeDotsSierpinski.app.App
   Working directory: C:\Projects\rep-qrng-chaos-game
   ```

5. Запустить configuration.

Нормальный успешный runtime smoke-test выглядит так:

```text
Application started.
Selected mode: Sierpinski Triangle
GUI successfully launched.
Initial data loaded, starting animation.
Animation started: ...
Animation stopped.
RNProvider shutting down.
Visualization finished, returning to mode selection.
Process finished with exit code 0
```

---

## 3. Запуск через Maven

Из корня проекта:

```bash
cd /c/Projects/rep-qrng-chaos-game
mvn test
```

Сборка artifact:

```bash
mvn clean package
```

Если команда Maven запускается из другой папки, укажи `pom.xml` явно:

```bash
mvn -f /c/Projects/rep-qrng-chaos-game/pom.xml test
```

Типичная ошибка, если запускать Maven из `src/test/java/...`:

```text
there is no POM in this directory
```

Решение: перейти в корень проекта или использовать `-f path/to/pom.xml`.

---

## 4. Запуск packaged JAR

После:

```bash
mvn clean package
```

Maven Assembly Plugin создаёт fat JAR:

```text
target/rep-qrng-chaos-game-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Запуск из корня проекта:

```bash
java -jar target/rep-qrng-chaos-game-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Запуск из папки `target`:

```bash
cd target
java -jar rep-qrng-chaos-game-1.0-SNAPSHOT-jar-with-dependencies.jar
```

При запуске из `target` логи будут созданы относительно текущей working directory, например:

```text
C:\Projects\rep-qrng-chaos-game\target\logs\app.log
C:\Projects\rep-qrng-chaos-game\target\logs\rnds-true.log
```

При запуске из корня проекта логи обычно будут здесь:

```text
C:\Projects\rep-qrng-chaos-game\logs\app.log
C:\Projects\rep-qrng-chaos-game\logs\rnds-true.log
```

---

## 5. API key и fallback behavior

Проект ищет настройки в таком порядке:

1. переменные окружения;
2. `.env` в корне проекта;
3. `src/main/resources/config.properties`.

Правило преобразования имён:

```text
api.key → QRNG_API_KEY
api.url → QRNG_API_URL
panel.size.width → QRNG_PANEL_SIZE_WIDTH
```

Git Bash:

```bash
export QRNG_API_KEY=your_real_anu_api_key
mvn clean package
java -jar target/rep-qrng-chaos-game-1.0-SNAPSHOT-jar-with-dependencies.jar
```

PowerShell:

```powershell
$env:QRNG_API_KEY="your_real_anu_api_key"
mvn clean package
java -jar target/rep-qrng-chaos-game-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Если ключа нет, это нормальное поведение:

```text
API key is not configured. Falling back to pseudo-random mode.
Switched to PSEUDO mode. Reason: no API key
```

Если ANU API вернул лимит:

```text
HTTP error: 429 - {"message":"Limit Exceeded"}
Rate limit detected. Bypassing retries, activating fallback.
Rate limit active. Reconnect monitor disabled until manual retry.
```

Это тоже нормальное поведение. Приложение продолжает работать в `PSEUDO`, а reconnect monitor не крутится бессмысленно при rate limit.

---

## 6. Java 25 / FlatLaf native warning

На Java 25 при запуске может появиться warning:

```text
WARNING: A restricted method in java.lang.System has been called
WARNING: java.lang.System::load has been called by com.formdev.flatlaf.util.NativeLibrary
WARNING: Use --enable-native-access=ALL-UNNAMED to avoid a warning
```

Это предупреждение от Java 25 и FlatLaf native helper. Оно не является ошибкой проекта и не мешает запуску.

Если нужно убрать warning локально, можно запускать так:

```bash
java --enable-native-access=ALL-UNNAMED \
  -jar target/rep-qrng-chaos-game-1.0-SNAPSHOT-jar-with-dependencies.jar
```

---

## 7. Быстрый sanity checklist

Перед тем как считать сборку стабильной:

```bash
mvn test
mvn clean package
java -jar target/rep-qrng-chaos-game-1.0-SNAPSHOT-jar-with-dependencies.jar
```

В GUI проверить:

```text
1. Mode selector открывается.
2. Один лёгкий mode стартует и останавливается, например Sierpinski Triangle.
3. Один тяжёлый mode стартует и останавливается, например Buddhabrot или Abelian Sandpile.
4. Выйти из visualization → возврат к mode selector.
5. Закрыть selector → process exit code 0.
```

---

## 8. Useful targeted checks

```bash
mvn test -Dtest=RNProviderIntegrationTest
mvn test -Dtest=RngSamplerTest
mvn test -Dtest=VisualizationModesSmokeTest
mvn test -Dtest=VisualizationModeRegistryTest
```

Для документации тестов см. [`docs/TESTS.md`](TESTS.md).
