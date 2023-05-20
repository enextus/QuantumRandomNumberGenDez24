package org.ThreeDotsSerpinski;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;

public class DiceRollerTest {
    private DiceRoller diceRoller;

    @BeforeEach
    public void setUp() {
        this.diceRoller = new DiceRoller();
    }

    @RepeatedTest(1000)
    public void testRollDice() {
        int result = diceRoller.rollDice();
        assertTrue(result >= 1 && result <= 6, "The dice roll should be between 1 and 6");
    }
}
