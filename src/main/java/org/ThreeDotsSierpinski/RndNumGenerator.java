package org.ThreeDotsSierpinski;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;

import java.io.IOException;
import java.io.InputStream;

import java.util.Properties;

public class RndNumGenerator {
    protected static final int INT_AMOUNT = 10000;
    protected static final String CONFIG_FILE_PATH = "config.properties";
    protected static final String CONNECTION_FAILED = "Connection failed!";
    protected static final String DISCONNECTED_FROM_THE_SERVICE = "Disconnected from the service.";
    protected static final String SORRY_UNABLE_TO_FIND = "Sorry, unable to find ";
    protected static final String USERNAME = "username";
    protected static final String PASSWORD = "password";
    protected static final String EMPTYSTRING = "";
    protected static final String FAILED_TO_GET_INTEGER_ARRAY = "Failed to get integer array!";
    protected static final String RECEIVED = "Received ";
    protected static final String INTEGERS_FROM_THE_QRNG = " integers from the RndNumGenerator: ";
    protected static final String LIB_QRNG_DLL_NAME = "libQRNG.dll";
    protected static final String LIB_LIB_QRNG_DLL_PATH = "lib/" + LIB_QRNG_DLL_NAME;

    public interface iQuantumRandomNumberGenerator extends Library {
        iQuantumRandomNumberGenerator INSTANCE = Native.load(LIB_LIB_QRNG_DLL_PATH, iQuantumRandomNumberGenerator.class);

        int qrng_connect(String username, String password);

        void qrng_disconnect();

        int qrng_get_int_array(int[] int_array, int int_array_size, IntByReference actual_ints_rcvd);

        int qrng_connect_and_get_int_array(String username, String password, int[] int_array, int int_array_size, IntByReference actual_ints_rcvd);
    }

    public static void main(String[] args) {

        iQuantumRandomNumberGenerator lib = iQuantumRandomNumberGenerator.INSTANCE;

        Properties prop = new Properties();

        String username = EMPTYSTRING, password = EMPTYSTRING;

        try (InputStream input = RndNumGenerator.class.getClassLoader().getResourceAsStream(CONFIG_FILE_PATH)) {

            if (input == null) {
                System.out.println(SORRY_UNABLE_TO_FIND + CONFIG_FILE_PATH);
                System.exit(-1);
            }

            prop.load(input);
            username = prop.getProperty(USERNAME);
            password = prop.getProperty(PASSWORD);

        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(-1);
        }

        if (checkResult(lib.qrng_connect(username, password))) {
            getIntegerArray(lib);
            lib.qrng_disconnect();
            System.out.println("\n" + DISCONNECTED_FROM_THE_SERVICE);
        }

    }

    static void getIntegerArray(iQuantumRandomNumberGenerator lib) {
        // Create an array to hold the integers returned by the RndNumGenerator
        int[] intArray = new int[INT_AMOUNT];  // Change the size of this array based on your needs

        // Create an IntByReference instance to hold the actual number of integers received
        IntByReference actualIntsReceived = new IntByReference();

        // Call the qrng_get_int_array method
        int getArrayResult = lib.qrng_get_int_array(intArray, intArray.length, actualIntsReceived);

        if (getArrayResult != 0) {
            // Failed to get integer array, handle this case
            System.out.println(FAILED_TO_GET_INTEGER_ARRAY);
        } else {
            // Successfully got the integer array, print it
            System.out.println(RECEIVED + actualIntsReceived.getValue() + INTEGERS_FROM_THE_QRNG);
            for (int i = 0; i < actualIntsReceived.getValue(); i++) {
                System.out.println(intArray[i]);
            }
        }

    }

    static boolean checkResult(int result) {
        if (result != 0) {
            System.out.println(RndNumGenerator.CONNECTION_FAILED);
            return false;
        }

        return true;
    }

}