package org.ThreeDotsSierpinski;

import com.sun.jna.ptr.IntByReference;

import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;

import org.jetbrains.annotations.NotNull;

import static org.ThreeDotsSierpinski.RndNumGenerator.checkResult;

class RndNumProvider {
    private final List<Integer> values;
    private int currentIndex;
    private final ExecutorService executorService;
    private Future<List<Integer>> futureValues;

    public RndNumProvider(RndNumGeneratorService qrngService) {
        values = getIntegers();
        executorService = Executors.newSingleThreadExecutor();
    }

    void connect(List<Integer> values) {
        RndNumGenerator.iQuantumRandomNumberGenerator lib = RndNumGenerator.iQuantumRandomNumberGenerator.INSTANCE;

        Properties prop = new Properties();
        String username = RndNumGenerator.EMPTYSTRING, password = RndNumGenerator.EMPTYSTRING;

        try (InputStream input = RndNumGenerator.class.getClassLoader().getResourceAsStream(RndNumGenerator.CONFIG_FILE_PATH)) {
            if (input == null) {
                System.out.println(RndNumGenerator.SORRY_UNABLE_TO_FIND + RndNumGenerator.CONFIG_FILE_PATH);
                System.exit(-1);
            }

            prop.load(input);
            username = prop.getProperty(RndNumGenerator.USERNAME);
            password = prop.getProperty(RndNumGenerator.PASSWORD);

        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(-1);
        }

        if (checkResult(lib.qrng_connect(username, password))) {

            int[] intArray = new int[RndNumGenerator.INT_AMOUNT];
            IntByReference actualIntsReceived = new IntByReference();

            int getArrayResult = lib.qrng_connect_and_get_int_array(username, password, intArray, intArray.length, actualIntsReceived);

            if (getArrayResult != 0) {
                System.out.println(RndNumGenerator.FAILED_TO_GET_INTEGER_ARRAY);
            } else {
                for (int i = 0; i < actualIntsReceived.getValue(); i++) {
                    values.add(intArray[i]);
                }
            }

            lib.qrng_disconnect();
        }
    }

    @NotNull
    List<Integer> getIntegers() {
        List<Integer> values = new ArrayList<>();
        connect(values);
        currentIndex = 0;
        return values;
    }

    public int rollDice() {
        if (currentIndex >= values.size()) {
            if (futureValues != null && futureValues.isDone()) {
                try {
                    values.clear();
                    values.addAll(futureValues.get());
                    currentIndex = 0;
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }

        if (currentIndex > values.size() * 0.8 && (futureValues == null || futureValues.isDone())) // when we have used 80% of the values,
            futureValues = executorService.submit(this::getIntegers); // start preparing new values

        int value = values.get(currentIndex);
        currentIndex++;

        return value;
    }

}
