package org.ThreeDotsSierpinski;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class DiceRollerTest {

    private DiceRoller diceRoller;
    private QuantumRandomNumberGeneratorService qrngService;

    @BeforeEach
    public void setup() {
        qrngService = Mockito.mock(QuantumRandomNumberGeneratorService.class);
        diceRoller = new DiceRoller(qrngService);
    }

    @Test
    public void testRollDice() {
        List<Integer> testValues = Arrays.asList(1, 2, 3, 4, 5, 6);
        when(qrngService.getIntegers()).thenReturn(testValues);

        for (int expected : testValues) {
            assertEquals(expected, diceRoller.rollDice());
        }
    }
}
