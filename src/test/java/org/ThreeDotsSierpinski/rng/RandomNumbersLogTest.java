package org.ThreeDotsSierpinski.rng;

import org.ThreeDotsSierpinski.app.*;
import org.ThreeDotsSierpinski.config.*;
import org.ThreeDotsSierpinski.math.*;
import org.ThreeDotsSierpinski.mode.*;
import org.ThreeDotsSierpinski.model.*;
import org.ThreeDotsSierpinski.rng.*;
import org.ThreeDotsSierpinski.stats.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RandomNumbersLog")
@Tag("fast")
class RandomNumbersLogTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Writes TRUE random numbers as comma-separated batches")
    void writesTrueNumbersAsCommaSeparatedBatches() throws IOException {
        Path trueLog = tempDir.resolve("rnds-true.log");
        Path pseudoLog = tempDir.resolve("rnds-pseudo.log");

        try (RandomNumbersLog log = new RandomNumbersLog(trueLog, pseudoLog, false)) {
            log.writeTrueNumber(10);
            log.writeTrueNumber(20);
            log.writeTrueNumber(30);
            log.finishBatch();

            log.writeTrueNumber(40);
            log.writeTrueNumber(50);
        }

        String content = Files.readString(trueLog);

        assertEquals("10,20,30" + System.lineSeparator() + System.lineSeparator()
                + "40,50" + System.lineSeparator() + System.lineSeparator(), content);
    }

    @Test
    @DisplayName("Does not create PSEUDO log when pseudo logging is disabled")
    void doesNotCreatePseudoLogWhenDisabled() {
        Path trueLog = tempDir.resolve("rnds-true.log");
        Path pseudoLog = tempDir.resolve("rnds-pseudo.log");

        try (RandomNumbersLog log = new RandomNumbersLog(trueLog, pseudoLog, false)) {
            log.writePseudoNumber(123);
            log.writePseudoNumber(456);
            log.finishBatch();
        }

        assertFalse(Files.exists(pseudoLog));
    }

    @Test
    @DisplayName("Can optionally write PSEUDO random numbers to a separate file")
    void canOptionallyWritePseudoNumbersSeparately() throws IOException {
        Path trueLog = tempDir.resolve("rnds-true.log");
        Path pseudoLog = tempDir.resolve("rnds-pseudo.log");

        try (RandomNumbersLog log = new RandomNumbersLog(trueLog, pseudoLog, true)) {
            log.writeTrueNumber(1);
            log.writeTrueNumber(2);
            log.writePseudoNumber(100);
            log.writePseudoNumber(200);
        }

        assertEquals("1,2" + System.lineSeparator() + System.lineSeparator(), Files.readString(trueLog));
        assertEquals("100,200" + System.lineSeparator() + System.lineSeparator(), Files.readString(pseudoLog));
    }
}