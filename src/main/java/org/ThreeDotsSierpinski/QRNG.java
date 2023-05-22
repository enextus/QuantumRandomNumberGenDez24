package org.ThreeDotsSierpinski;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class QRNG {
    protected static final int INT_AMOUNT = 1000;
    protected static final String CONFIG_FILE_PATH = "config.properties";
    protected static final String CONNECTION_FAILED = "Connection failed!";
    protected static final String OPERATION_SUCCESSFUL = "Operation successful.";
    protected static final String DISCONNECTED_FROM_THE_SERVICE = "Disconnected from the service.";
    protected static final String SORRY_UNABLE_TO_FIND = "Sorry, unable to find ";
    protected static final String USERNAME = "username";
    protected static final String PASSWORD = "password";
    protected static final String EMPTYSTRING = "";
    protected static final String FAILED_TO_GET_INTEGER_ARRAY = "Failed to get integer array!";
    protected static final String RECEIVED = "Received ";
    protected static final String INTEGERS_FROM_THE_QRNG = " integers from the QRNG:\n";
    protected static final String LIB_QRNG_DLL_NAME = "libQRNG.dll";
    protected static final String LIB_LIB_QRNG_DLL_PATH = "lib/" + LIB_QRNG_DLL_NAME;

    public interface QuantumRandomNumberGenerator extends Library {
        QuantumRandomNumberGenerator INSTANCE = Native.load(LIB_LIB_QRNG_DLL_PATH, QuantumRandomNumberGenerator.class);

        int qrng_connect(String username, String password);

        void qrng_disconnect();

        int qrng_get_int_array(int[] int_array, int int_array_size, IntByReference actual_ints_rcvd);

        int qrng_connect_SSL(String username, String password);

        int qrng_get_byte_array(byte[] byte_array, int byte_array_size, IntByReference actual_bytes_rcvd);

        int qrng_get_double_array(double[] double_array, int double_array_size, IntByReference actual_doubles_rcvd);

        int qrng_generate_password(String tobeused_password_chars, String generated_password, int password_length);

        int qrng_connect_and_get_byte_array(String username, String password, byte[] byte_array, int byte_array_size, IntByReference actual_bytes_rcvd);

        int qrng_connect_and_get_double_array(String username, String password, double[] double_array, int double_array_size, IntByReference actual_doubles_rcvd);

        int qrng_connect_and_get_int_array(String username, String password, int[] int_array, int int_array_size, IntByReference actual_ints_rcvd);

    }

    public static void main(String[] args) {

        QuantumRandomNumberGenerator lib = QuantumRandomNumberGenerator.INSTANCE;

        Properties prop = new Properties();
        String username = EMPTYSTRING, password = EMPTYSTRING;

        try (InputStream input = QRNG.class.getClassLoader().getResourceAsStream(CONFIG_FILE_PATH)) {

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
            // get some QRNG-integers
            getAndPrintIntegerArray(lib);

            // disconnect
            lib.qrng_disconnect();
            System.out.println("\n" + DISCONNECTED_FROM_THE_SERVICE);
        }
    }

    static void getAndPrintIntegerArray(QuantumRandomNumberGenerator lib) {

        // Create an array to hold the integers returned by the QRNG
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
            System.out.println(QRNG.CONNECTION_FAILED);
            return false;
        }

        System.out.println("111" + OPERATION_SUCCESSFUL);
        return true;
    }

}
