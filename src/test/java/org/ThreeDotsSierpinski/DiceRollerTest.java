package org.ThreeDotsSierpinski;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class DiceRollerTest {

    private static class TestRndNumberProvider extends RndNumberProvider {
        AtomicInteger connectCallCount = new AtomicInteger(0);

        public TestRndNumberProvider(RndNumberGeneratorService qrngService) {
            super(qrngService);
        }

        @Override
        void connect(List<Integer> values) {
            super.connect(values);
            connectCallCount.incrementAndGet();
        }

        public int getConnectCallCount() {
            return connectCallCount.get();
        }
    }

}
