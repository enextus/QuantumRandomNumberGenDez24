package org.ThreeDotsSerpinski;

import com.sun.jna.ptr.IntByReference;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

class DiceRoller {
    private final List<Integer> values;
    private int currentIndex;

    public DiceRoller() {
        values = new ArrayList<>();
        QRNG.QuantumRandomNumberGenerator lib = QRNG.QuantumRandomNumberGenerator.INSTANCE;

        Properties prop = new Properties();
        String username = QRNG.EMPTYSTRING, password = QRNG.EMPTYSTRING;

        try (InputStream input = QRNG.class.getClassLoader().getResourceAsStream(QRNG.CONFIG_FILE_PATH)) {

            if (input == null) {
                System.out.println(QRNG.SORRY_UNABLE_TO_FIND + QRNG.CONFIG_FILE_PATH);
                System.exit(-1);
            }

            prop.load(input);
            username = prop.getProperty(QRNG.USERNAME);
            password = prop.getProperty(QRNG.PASSWORD);

        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(-1);
        }

        if (checkResult(lib.qrng_connect(username, password))) {
            int[] intArray = new int[QRNG.INT_AMOUNT];
            IntByReference actualIntsReceived = new IntByReference();

            int getArrayResult = lib.qrng_get_int_array(intArray, intArray.length, actualIntsReceived);

            if (getArrayResult != 0) {
                System.out.println(QRNG.FAILED_TO_GET_INTEGER_ARRAY);
            } else {
                for (int i = 0; i < actualIntsReceived.getValue(); i++) {
                    values.add(intArray[i]);
                }
            }

            lib.qrng_disconnect();
            System.out.println(QRNG.DISCONNECTED_FROM_THE_SERVICE);
        }

        currentIndex = 0;
    }

    public int rollDice() {
        if (currentIndex >= values.size()) {
            currentIndex = 0;
        }

        int value = values.get(currentIndex);
        currentIndex++;

        return value;
    }

    private boolean checkResult(int result) {
        if (result != 0) {
            System.out.println(QRNG.CONNECTION_FAILED);
            return false;
        }

        System.out.println(QRNG.OPERATION_SUCCESSFUL);
        return true;
    }

}
