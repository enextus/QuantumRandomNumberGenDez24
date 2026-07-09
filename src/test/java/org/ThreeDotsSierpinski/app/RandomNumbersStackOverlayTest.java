package org.ThreeDotsSierpinski.app;

import org.ThreeDotsSierpinski.mode.VisualizationStyle;
import org.ThreeDotsSierpinski.rng.RNProvider;
import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RandomNumbersStackOverlayTest {

    private static final RNProvider.Sleeper NO_SLEEP = ignored -> {
    };

    @Test
    void drawReusesSnapshotWhileConsumedVersionIsUnchanged() {
        CountingProvider provider = new CountingProvider(List.of(1L, 12L, 123L, 1_234L, 12_345L));

        drawOverlay(provider);
        drawOverlay(provider);

        assertEquals(1, provider.snapshotRequests.get());
    }

    @Test
    void drawRefreshesSnapshotWhenConsumedVersionChanges() {
        CountingProvider provider = new CountingProvider(List.of(1L, 12L, 123L, 1_234L, 12_345L));

        drawOverlay(provider);
        provider.setNumbers(List.of(2L, 23L, 234L, 2_345L, 23_456L));
        drawOverlay(provider);

        assertEquals(2, provider.snapshotRequests.get());
    }

    private static void drawOverlay(CountingProvider provider) {
        BufferedImage image = new BufferedImage(640, 640, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            RandomNumbersStackOverlay.draw(
                    graphics,
                    image.getWidth(),
                    image.getHeight(),
                    false,
                    VisualizationStyle.DEFAULT,
                    provider
            );
        } finally {
            graphics.dispose();
        }
    }

    private static final class CountingProvider extends RNProvider {
        private final AtomicInteger snapshotRequests = new AtomicInteger();
        private long consumedVersion = 1L;
        private List<Long> numbers;

        private CountingProvider(List<Long> numbers) {
            super(testSettings(), false, NO_SLEEP, false);
            this.numbers = List.copyOf(numbers);
        }

        @Override
        public long getConsumedVersion() {
            return consumedVersion;
        }

        @Override
        public List<Long> getLastConsumedNumbers(int limit) {
            snapshotRequests.incrementAndGet();
            return numbers.stream()
                    .skip(Math.max(0, numbers.size() - limit))
                    .toList();
        }

        private void setNumbers(List<Long> newNumbers) {
            this.numbers = List.copyOf(newNumbers);
            consumedVersion++;
        }

        private static RNProvider.ProviderSettings testSettings() {
            return new RNProvider.ProviderSettings(
                    "http://localhost:1",
                    "test-api-key",
                    "uint16",
                    5,
                    2,
                    100,
                    100,
                    100,
                    3,
                    1,
                    1L,
                    1L
            );
        }
    }
}
