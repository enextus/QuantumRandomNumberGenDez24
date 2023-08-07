package org.ThreeDotsSierpinski;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RandomNumberProviderTest {

    private RandomNumberProvider randomNumberProvider;

    @BeforeEach
    void setUp() {
        randomNumberProvider = new RandomNumberProvider();
    }

    @Test
    void getNextRandomNumberTest() {
        int randomNumber = randomNumberProvider.getNextRandomNumber();
        // As this is a random number, there's not a lot we can assert about it.
        // Let's just make sure it's a valid integer.
        assertTrue(randomNumber >= Integer.MIN_VALUE && randomNumber <= Integer.MAX_VALUE);
    }

}
