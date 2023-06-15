package org.ThreeDotsSierpinski;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class RandomNumberGeneratorTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void checkResultZero() {
        boolean result = RandomNumberGenerator.checkResult(0);
        assertTrue(result, "Expected true for result zero");
        assertEquals("", outContent.toString(), "Expected no print output for result zero");
    }

    @Test
    void checkResultNonZero() {
        boolean result = RandomNumberGenerator.checkResult(1);
        assertFalse(result, "Expected false for non-zero result");
    }

}
