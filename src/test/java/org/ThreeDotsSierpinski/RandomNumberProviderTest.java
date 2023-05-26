package org.ThreeDotsSierpinski;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class RandomNumberProviderTest {

    private RandomNumberProvider randomNumberProvider;
    private RandomNumberService randomNumberServiceMock;

    @BeforeEach
    public void setup() {
        // Here you would setup your mocks or stubs if necessary
        // randomNumberServiceMock = ... ;
        // For simplicity, we will just create a new instance of RandomNumberProvider
        randomNumberProvider = new RandomNumberProvider(randomNumberServiceMock);
    }

    @Test
    public void testGetIntegerList() {
        List<Integer> integerList = randomNumberProvider.getIntegerList();

        // Assuming that the external system will return non-empty list
        // Here we are just checking if list is not empty
        assertFalse(integerList.isEmpty());
    }



    // Other tests can be added here
}
