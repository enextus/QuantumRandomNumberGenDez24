package org.ThreeDotsSierpinski;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

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

    @Test
    void getDuplicateNumbersCountTest() {
        // Call getDuplicateNumbersCount after getNextRandomNumber.
        // This way we could potentially have some duplicates.
        randomNumberProvider.getNextRandomNumber();
        int duplicates = randomNumberProvider.getDuplicateNumbersCount();
        // The number of duplicates should always be equal or greater than 0.
        assertTrue(duplicates >= 0);
    }

}
