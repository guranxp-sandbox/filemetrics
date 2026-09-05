package io.github.guranxpsandbox.filemetrics;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

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
}
