package org.ThreeDotsSierpinski;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class DotMoverTest {

    private DotMover dotMover;
    private DiceRoller diceRoller;

    @BeforeEach
    void setUp() {
        diceRoller = Mockito.mock(DiceRoller.class);
        dotMover = new DotMover(diceRoller);
    }

    @Test
    void initialDotCounter() {
        assertEquals(0, dotMover.getDotCounter());
    }

    @Test
    void moveDot_lesserThanMinIntegerOverThree() {
        when(diceRoller.rollDice()).thenReturn(Integer.MIN_VALUE / 3 - 1);
        dotMover.moveDot();
        assertEquals(1, dotMover.getDotCounter());
    }

    @Test
    void moveDot_lessThanOrEqualToMaxIntegerOverThree() {
        when(diceRoller.rollDice()).thenReturn(Integer.MAX_VALUE / 3);
        dotMover.moveDot();
        assertEquals(1, dotMover.getDotCounter());
    }

    @Test
    void moveDot_greaterThanMaxIntegerOverThree() {
        when(diceRoller.rollDice()).thenReturn(Integer.MAX_VALUE / 3 + 1);
        dotMover.moveDot();
        assertEquals(1, dotMover.getDotCounter());
    }
}
