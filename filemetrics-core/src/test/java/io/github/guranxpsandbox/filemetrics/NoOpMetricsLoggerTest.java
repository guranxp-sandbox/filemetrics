package io.github.guranxpsandbox.filemetrics;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NoOpMetricsLoggerTest {

    private final MetricsLogger logger = new NoOpMetricsLogger();

    @Test
    void logWithValuesDoesNotThrow() {
        Map<String, Object> values = new HashMap<>();
        values.put("used_mb", 312);
        values.put("committed_mb", 400);

        assertDoesNotThrow(() -> logger.log("heap", values));
    }

    @Test
    void logWithEmptyMapDoesNotThrow() {
        assertDoesNotThrow(() -> logger.log("heap", Collections.emptyMap()));
    }

    @Test
    void logWithNullValueInMapDoesNotThrow() {
        Map<String, Object> values = new HashMap<>();
        values.put("name", null);

        assertDoesNotThrow(() -> logger.log("gc", values));
    }

    @Test
    void closeDoesNotThrow() {
        assertDoesNotThrow(logger::close);
    }

    @Test
    void closeCanBeCalledMultipleTimesWithoutSideEffects() {
        logger.close();
        assertDoesNotThrow(logger::close);
    }
}
