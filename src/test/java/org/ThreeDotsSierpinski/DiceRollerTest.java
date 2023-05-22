package org.ThreeDotsSierpinski;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class DiceRollerTest {

    private static class TestDiceRoller extends DiceRoller {
        AtomicInteger connectCallCount = new AtomicInteger(0);

        public TestDiceRoller(QuantumRandomNumberGeneratorService qrngService) {
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
