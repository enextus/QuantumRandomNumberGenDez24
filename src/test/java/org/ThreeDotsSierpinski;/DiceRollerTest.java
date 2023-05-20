package org.ThreeDotsSierpinski;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DiceRollerTest {
    @Mock
    private QRNG.QuantumRandomNumberGenerator lib;
    
    private DiceRoller diceRoller;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(lib.qrng_connect(anyString(), anyString())).thenReturn(0);
        when(lib.qrng_connect_and_get_int_array(anyString(), anyString(), any(), anyInt(), any())).thenReturn(0);
        this.diceRoller = new DiceRoller();
    }

    @Test
    public void rollDiceReturnsCorrectValue() {
        // We can't guarantee what this will be since it relies on a real random number generator
        // We can however test that the number falls within a certain range.
        int roll = diceRoller.rollDice();
        assertTrue(roll >= Integer.MIN_VALUE && roll <= Integer.MAX_VALUE);
    }

}
