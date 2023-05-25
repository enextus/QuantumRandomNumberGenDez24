package org.ThreeDotsSierpinski;

import com.sun.jna.ptr.IntByReference;

public class RandomNumberService {
    private RandomNumberGenerator.iQuantumRandomNumberGenerator qrng;

    public RandomNumberService() {
        this.qrng = qrng;
    }

    public boolean connect(String username, String password) {
        try {
            if (RandomNumberGenerator.checkResult(qrng.qrng_connect(username, password))) {
                return true;
            }
        } catch (Exception e) {
            System.out.println("Failed to getNextValue to RandomNumberGenerator: " + e.getMessage());
        }
        return false;
    }

    public void disconnect() {
        try {
            qrng.qrng_disconnect();
        } catch (Exception e) {
            System.out.println("Failed to disconnect from RandomNumberGenerator: " + e.getMessage());
        }
    }

    public int[] getAndPrintIntegerArray() {
        int[] intArray = new int[RandomNumberGenerator.INT_AMOUNT];
        IntByReference actualIntsReceived = new IntByReference();

        try {
            int getArrayResult = qrng.qrng_get_int_array(intArray, intArray.length, actualIntsReceived);

            if (getArrayResult != 0) {
                System.out.println(RandomNumberGenerator.FAILED_TO_GET_INTEGER_ARRAY);
            } else {
                System.out.println(RandomNumberGenerator.RECEIVED + actualIntsReceived.getValue() + RandomNumberGenerator.INTEGERS_FROM_THE_QRNG);
                for (int i = 0; i < actualIntsReceived.getValue(); i++) {
                    System.out.println(intArray[i]);
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to get integer array from RandomNumberGenerator: " + e.getMessage());
        }

        return intArray;
    }

}
