package org.ThreeDotsSierpinski;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;

import java.io.IOException;
import java.io.InputStream;

import java.util.*;

public class RandomNumberGenerator {
    protected static final int INT_AMOUNT = 10000;
    protected static final String CONFIG_FILE_PATH = "config.properties";
    protected static final String CONNECTION_FAILED = "Connection failed!";
    protected static final String SORRY_UNABLE_TO_FIND = "Sorry, unable to find ";
    protected static final String USERNAME = "username";
    protected static final String PASSWORD = "password";
    protected static final String EMPTYSTRING = "";
    protected static final String FAILED_TO_GET_INTEGER_ARRAY = "Failed to get integer array!";
    protected static final String LIB_QRNG_DLL_NAME = "libQRNG.dll";
    protected static final String LIB_LIB_QRNG_DLL_PATH = "lib/" + LIB_QRNG_DLL_NAME;

    public interface iQuantumRandomNumberGenerator extends Library {
        iQuantumRandomNumberGenerator INSTANCE = Native.load(LIB_LIB_QRNG_DLL_PATH, iQuantumRandomNumberGenerator.class);

        int qrng_connect(String username, String password);

        void qrng_disconnect();

        // int qrng_get_int_array(int[] int_array, int int_array_size, IntByReference actual_ints_rcvd);

        int qrng_connect_and_get_int_array(String username, String password, int[] int_array, int int_array_size, IntByReference actual_ints_rcvd);
    }

    static boolean checkResult(int result) {
        if (result != 0) {
            System.out.println(RandomNumberGenerator.CONNECTION_FAILED);
            return false;
        }

        return true;
    }

}
