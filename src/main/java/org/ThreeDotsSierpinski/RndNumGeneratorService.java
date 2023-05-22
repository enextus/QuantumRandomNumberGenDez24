package org.ThreeDotsSierpinski;

import com.sun.jna.ptr.IntByReference;

public class RndNumGeneratorService {
    private RndNumGenerator.iQuantumRandomNumberGenerator qrng;

    public RndNumGeneratorService() {
        this.qrng = qrng;
    }

    public boolean connect(String username, String password) {
        try {
            if (RndNumGenerator.checkResult(qrng.qrng_connect(username, password))) {
                return true;
            }
        } catch (Exception e) {
            System.out.println("Failed to connect to RndNumGenerator: " + e.getMessage());
        }
        return false;
    }

    public void disconnect() {
        try {
            qrng.qrng_disconnect();
        } catch (Exception e) {
            System.out.println("Failed to disconnect from RndNumGenerator: " + e.getMessage());
        }
    }

    public int[] getAndPrintIntegerArray() {
        int[] intArray = new int[RndNumGenerator.INT_AMOUNT];
        IntByReference actualIntsReceived = new IntByReference();

        try {
            int getArrayResult = qrng.qrng_get_int_array(intArray, intArray.length, actualIntsReceived);

            if (getArrayResult != 0) {
                System.out.println(RndNumGenerator.FAILED_TO_GET_INTEGER_ARRAY);
            } else {
                System.out.println(RndNumGenerator.RECEIVED + actualIntsReceived.getValue() + RndNumGenerator.INTEGERS_FROM_THE_QRNG);
                for (int i = 0; i < actualIntsReceived.getValue(); i++) {
                    System.out.println(intArray[i]);
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to get integer array from RndNumGenerator: " + e.getMessage());
        }

        return intArray;
    }

}
