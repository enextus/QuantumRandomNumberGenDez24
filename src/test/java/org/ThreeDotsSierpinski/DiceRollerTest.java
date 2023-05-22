package org.ThreeDotsSierpinski;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class DiceRollerTest {

    private static class TestRndNumProvider extends RndNumProvider {
        AtomicInteger connectCallCount = new AtomicInteger(0);

        public TestRndNumProvider(RndNumGeneratorService qrngService) {
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
