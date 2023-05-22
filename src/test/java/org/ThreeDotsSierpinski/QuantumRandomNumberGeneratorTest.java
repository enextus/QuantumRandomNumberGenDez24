package org.ThreeDotsSierpinski;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.sun.jna.ptr.IntByReference;

import static org.mockito.Mockito.when;

public class QuantumRandomNumberGeneratorTest {

    private QuantumRandomNumberGenerator.iQuantumRandomNumberGenerator qrngMock;
    private QuantumRandomNumberGenerator quantumRandomNumberGenerator;

    @BeforeEach
    void setUp() {
        qrngMock = Mockito.mock(QuantumRandomNumberGenerator.iQuantumRandomNumberGenerator.class);
        quantumRandomNumberGenerator = new QuantumRandomNumberGenerator();
    }

    @Test
    void getIntegerArray_success() {
        int[] intArray = new int[QuantumRandomNumberGenerator.INT_AMOUNT];
        IntByReference actualIntsReceived = new IntByReference();

        when(qrngMock.qrng_get_int_array(intArray, intArray.length, actualIntsReceived)).thenReturn(0);

        quantumRandomNumberGenerator.getIntegerArray(qrngMock);
        // assertion based on expected behavior
    }

    @Test
    void getAndPrintIntegerArray_failure() {
        int[] intArray = new int[QuantumRandomNumberGenerator.INT_AMOUNT];
        IntByReference actualIntsReceived = new IntByReference();

        when(qrngMock.qrng_get_int_array(intArray, intArray.length, actualIntsReceived)).thenReturn(-1);

        quantumRandomNumberGenerator.getIntegerArray(qrngMock);
        // assertion based on expected behavior
    }

    // similarly, other methods can be tested

}
