package io.github.guranxpsandbox.filemetrics.internal;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreadMetricsCollectorTest {

    private final ThreadMetricsCollector collector = new ThreadMetricsCollector();

    @Test
    void shouldReportThreadsAsType() {
        assertEquals("threads", collector.type());
    }

    @Test
    void shouldCollectThreadValuesWithExpectedKeys() {
        // when
        final Map<String, Object> values = collector.collect();

        // then
        assertEquals(3, values.size());
        assertTrue(values.containsKey("live"));
        assertTrue(values.containsKey("peak"));
        assertTrue(values.containsKey("deadlocked"));
    }

    @Test
    void shouldRespectThreadCountInvariants() {
        // when
        final Map<String, Object> values = collector.collect();
        final int live = (int) values.get("live");
        final int peak = (int) values.get("peak");
        final int deadlocked = (int) values.get("deadlocked");

        // then
        assertTrue(live >= 1);
        assertTrue(peak >= live);
        assertTrue(deadlocked >= 0);
    }

    @Test
    void shouldReportZeroDeadlockedThreadsInHealthyState() {
        // when
        final Map<String, Object> values = collector.collect();

        // then
        assertEquals(0, values.get("deadlocked"));
    }
}
