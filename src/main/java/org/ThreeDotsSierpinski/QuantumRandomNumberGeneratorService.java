package org.ThreeDotsSierpinski;

import com.sun.jna.ptr.IntByReference;

public class QuantumRandomNumberGeneratorService {
    private QuantumRandomNumberGenerator.iQuantumRandomNumberGenerator qrng;

    public QuantumRandomNumberGeneratorService() {
        this.qrng = qrng;
    }

    public boolean connect(String username, String password) {
        try {
            if (QuantumRandomNumberGenerator.checkResult(qrng.qrng_connect(username, password))) {
                return true;
            }
        } catch (Exception e) {
            System.out.println("Failed to connect to QuantumRandomNumberGenerator: " + e.getMessage());
        }
        return false;
    }

    public void disconnect() {
        try {
            qrng.qrng_disconnect();
        } catch (Exception e) {
            System.out.println("Failed to disconnect from QuantumRandomNumberGenerator: " + e.getMessage());
        }
    }

    public int[] getAndPrintIntegerArray() {
        int[] intArray = new int[QuantumRandomNumberGenerator.INT_AMOUNT];
        IntByReference actualIntsReceived = new IntByReference();

        try {
            int getArrayResult = qrng.qrng_get_int_array(intArray, intArray.length, actualIntsReceived);

            if (getArrayResult != 0) {
                System.out.println(QuantumRandomNumberGenerator.FAILED_TO_GET_INTEGER_ARRAY);
            } else {
                System.out.println(QuantumRandomNumberGenerator.RECEIVED + actualIntsReceived.getValue() + QuantumRandomNumberGenerator.INTEGERS_FROM_THE_QRNG);
                for (int i = 0; i < actualIntsReceived.getValue(); i++) {
                    System.out.println(intArray[i]);
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to get integer array from QuantumRandomNumberGenerator: " + e.getMessage());
        }

        return intArray;
    }

}
