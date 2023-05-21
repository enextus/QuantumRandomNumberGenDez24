package org.ThreeDotsSierpinski;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.sun.jna.ptr.IntByReference;

import static org.mockito.Mockito.when;

public class QRNGTest {

    private QRNG.QuantumRandomNumberGenerator qrngMock;
    private QRNG qrng;

    @BeforeEach
    void setUp() {
        qrngMock = Mockito.mock(QRNG.QuantumRandomNumberGenerator.class);
        qrng = new QRNG();
    }

    @Test
    void getAndPrintIntegerArray_success() {
        int[] intArray = new int[QRNG.INT_AMOUNT];
        IntByReference actualIntsReceived = new IntByReference();

        when(qrngMock.qrng_get_int_array(intArray, intArray.length, actualIntsReceived)).thenReturn(0);

        qrng.getAndPrintIntegerArray(qrngMock);
        // assertion based on expected behavior
    }

    @Test
    void getAndPrintIntegerArray_failure() {
        int[] intArray = new int[QRNG.INT_AMOUNT];
        IntByReference actualIntsReceived = new IntByReference();

        when(qrngMock.qrng_get_int_array(intArray, intArray.length, actualIntsReceived)).thenReturn(-1);

        qrng.getAndPrintIntegerArray(qrngMock);
        // assertion based on expected behavior
    }

    // similarly, other methods can be tested

}
