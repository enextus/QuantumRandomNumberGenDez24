package org.ThreeDotsSierpinski;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DiceRollerTest {

    @BeforeEach
    public void setup() {
        QuantumRandomNumberGeneratorService qrngService = Mockito.mock(QuantumRandomNumberGeneratorService.class);

    }

    @Test
    public void testRollDice() {
        QuantumRandomNumberGeneratorService qrngService = new QuantumRandomNumberGeneratorService();
        DiceRoller diceRoller = new DiceRoller(qrngService);

        for (int i = 0; i < 1000; i++) {
            int roll = diceRoller.rollDice();
            assertTrue(roll >= Integer.MIN_VALUE && roll <= Integer.MAX_VALUE);
        }
    }

}
