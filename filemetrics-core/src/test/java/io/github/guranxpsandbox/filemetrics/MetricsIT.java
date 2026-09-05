package io.github.guranxpsandbox.filemetrics;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricsIT {

    @AfterEach
    void stopAndClearProperty() {
        Metrics.stop();
        System.clearProperty("metrics.implementation");
    }

    @Test
    void shouldNotThrowWhenStartingAndStopping() {
        assertDoesNotThrow(() -> {
            Metrics.start("order-service");
            Metrics.stop();
        });
    }

    @Test
    void shouldNotThrowWhenStoppingWithoutStarting() {
        assertDoesNotThrow(Metrics::stop);
    }

    @Test
    void shouldNotThrowWhenStartingWithInMemoryImplementation() {
        // given
        System.setProperty("metrics.implementation", "inmemory");

        // when / then
        assertDoesNotThrow(() -> Metrics.start("order-service"));
    }

    @Test
    void shouldStartCollectionDaemonAsDaemonThreadWhenStarting() {
        // when
        Metrics.start("order-service");

        // then
        assertNotNull(Metrics.collectionDaemon);
        assertTrue(Metrics.collectionDaemon.isAlive());
        assertTrue(Metrics.collectionDaemon.isDaemon());
    }

    @Test
    void shouldStopCollectionDaemonWhenStopping() throws InterruptedException {
        // given
        Metrics.start("order-service");
        final Thread daemon = Metrics.collectionDaemon;

        // when
        Metrics.stop();
        daemon.join(2_000L);

        // then
        assertFalse(daemon.isAlive());
    }
}
