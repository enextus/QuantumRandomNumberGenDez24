package org.ThreeDotsSierpinski.quality;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards against accidental copies of production classes in src/test/java.
 *
 * Maven puts target/test-classes before target/classes on the test classpath.
 * Therefore, if src/test/java contains a class with the same fully qualified
 * name as a production class, tests may execute the test copy instead of the
 * real production implementation.
 */
final class NoProductionShadowClassesTest {

    @Test
    void testSourcesMustNotShadowProductionSources() throws IOException {
        Path projectRoot = Path.of("").toAbsolutePath().normalize();
        Path mainSources = projectRoot.resolve("src/main/java");
        Path testSources = projectRoot.resolve("src/test/java");

        if (!Files.isDirectory(mainSources) || !Files.isDirectory(testSources)) {
            return;
        }

        List<String> shadowedSources;

        try (Stream<Path> paths = Files.walk(mainSources)) {
            shadowedSources = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .map(mainSources::relativize)
                    .filter(relativePath -> Files.exists(testSources.resolve(relativePath)))
                    .map(Path::toString)
                    .sorted()
                    .toList();
        }

        assertTrue(
                shadowedSources.isEmpty(),
                () -> "src/test/java must not contain copies of production classes from src/main/java. "
                        + "These files shadow real production classes on the Maven test classpath:%n"
                        .formatted()
                        + String.join(System.lineSeparator(), shadowedSources)
        );
    }
}
