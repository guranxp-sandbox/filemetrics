package io.github.guranxpsandbox.filemetrics.internal;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeapMetricsCollectorTest {

    private final HeapMetricsCollector collector = new HeapMetricsCollector();

    @Test
    void shouldCollectHeapValuesWithExpectedKeys() {
        // when
        final Map<String, Object> values = collector.collect();

        // then
        assertEquals(3, values.size());
        assertTrue(values.containsKey("used_mb"));
        assertTrue(values.containsKey("committed_mb"));
        assertTrue(values.containsKey("max_mb"));
    }

    @Test
    void shouldRespectMemoryUsageInvariants() {
        // when
        final Map<String, Object> values = collector.collect();
        final long usedMb = (long) values.get("used_mb");
        final long committedMb = (long) values.get("committed_mb");
        final long maxMb = (long) values.get("max_mb");

        // then
        assertTrue(usedMb >= 0);
        assertTrue(committedMb >= usedMb);
        assertTrue(maxMb == -1 || maxMb >= committedMb);
    }

    @Test
    void shouldConvertBytesToMb() {
        assertEquals(1L, HeapMetricsCollector.toMb(1024L * 1024L));
        assertEquals(2L, HeapMetricsCollector.toMb(2L * 1024L * 1024L));
        assertEquals(0L, HeapMetricsCollector.toMb(0L));
    }

    @Test
    void shouldConvertUndefinedMaxToMinusOne() {
        assertEquals(-1L, HeapMetricsCollector.toMb(-1L));
    }
}
