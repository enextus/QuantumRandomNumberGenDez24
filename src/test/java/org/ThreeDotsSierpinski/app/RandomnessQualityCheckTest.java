package org.ThreeDotsSierpinski.app;

import org.ThreeDotsSierpinski.stats.TestResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Randomness quality check")
class RandomnessQualityCheckTest {

    @Test
    @DisplayName("runs statistical tests in SwingWorker background thread and returns a snapshot")
    void runsStatisticalTestsOffEdtAndReturnsSnapshot() throws Exception {
        var originalNumbers = new ArrayList<>(LongStream.range(0, 20).boxed().toList());
        var callbackLatch = new CountDownLatch(1);
        var runnerWasOnEdt = new AtomicBoolean(true);
        var callbackWasOnEdt = new AtomicBoolean(false);
        var callbackResult = new AtomicReference<RandomnessQualityCheck.Result>();
        var callbackFailure = new AtomicReference<Throwable>();

        var worker = new RandomnessQualityCheck.Worker(
                originalNumbers,
                0.05,
                (numbers, alpha) -> {
                    runnerWasOnEdt.set(SwingUtilities.isEventDispatchThread());
                    assertEquals(20, numbers.size());
                    assertEquals(0.05, alpha);
                    return List.of(new TestResult("dummy", true, "ok"));
                },
                new RandomnessQualityCheck.Callback() {
                    @Override
                    public void onSuccess(RandomnessQualityCheck.Result result) {
                        callbackWasOnEdt.set(SwingUtilities.isEventDispatchThread());
                        callbackResult.set(result);
                        callbackLatch.countDown();
                    }

                    @Override
                    public void onFailure(Throwable error) {
                        callbackFailure.set(error);
                        callbackLatch.countDown();
                    }
                }
        );

        originalNumbers.clear();
        worker.execute();

        assertTrue(callbackLatch.await(5, TimeUnit.SECONDS));
        assertNull(callbackFailure.get());
        assertNotNull(callbackResult.get());
        assertFalse(runnerWasOnEdt.get());
        assertTrue(callbackWasOnEdt.get());
        assertEquals(20, callbackResult.get().sampleSize());
        assertEquals(1, callbackResult.get().results().size());
        assertEquals(1, callbackResult.get().passedCount());
    }
}
