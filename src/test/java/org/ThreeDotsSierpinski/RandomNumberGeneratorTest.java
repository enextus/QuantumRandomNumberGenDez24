package org.ThreeDotsSierpinski;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class RandomNumberGeneratorTest {

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
    void testRandomNumberGeneration() {
        // Пример проверки
        int randomNumber = (int) (Math.random() * 100);
        assertTrue(randomNumber >= 0 && randomNumber < 100, "Число должно быть в пределах [0, 100)");
    }
}
