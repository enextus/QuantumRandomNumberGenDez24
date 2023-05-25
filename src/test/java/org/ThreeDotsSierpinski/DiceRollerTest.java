package org.ThreeDotsSierpinski;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class DiceRollerTest {

    private static class TestRandomNumberProvider extends RandomNumberProvider {
        AtomicInteger connectCallCount = new AtomicInteger(0);

        public TestRandomNumberProvider(RandomNumberService qrngService) {
            super(qrngService);
        }

        @Override
        void getNextValue(List<Integer> values) {
            super.getNextValue(values);
            connectCallCount.incrementAndGet();
        }

        public int getConnectCallCount() {
            return connectCallCount.get();
        }
    }

}
