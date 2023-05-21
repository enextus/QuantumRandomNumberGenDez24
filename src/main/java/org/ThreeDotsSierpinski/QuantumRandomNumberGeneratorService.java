package org.ThreeDotsSierpinski;

import com.sun.jna.ptr.IntByReference;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.ThreeDotsSierpinski.QRNG.checkResult;

public class QuantumRandomNumberGeneratorService {

    public List<Integer> getIntegers() {
        List<Integer> values = new ArrayList<>();
        connect(values);
        return values;
    }

    private void connect(List<Integer> values) {
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

            int getArrayResult = lib.qrng_connect_and_get_int_array(username, password, intArray, intArray.length, actualIntsReceived);

            if (getArrayResult != 0) {
                System.out.println(QRNG.FAILED_TO_GET_INTEGER_ARRAY);
            } else {
                for (int i = 0; i < actualIntsReceived.getValue(); i++) {
                    values.add(intArray[i]);
                }
            }

            lib.qrng_disconnect();
        }
    }
}
