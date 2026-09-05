package io.github.guranxpsandbox.filemetrics.internal;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads the current heap memory usage via {@link java.lang.management}.
 */
public final class HeapMetricsCollector {

    private static final long BYTES_PER_MB = 1024L * 1024L;

    public Map<String, Object> collect() {
        final MemoryUsage usage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        final Map<String, Object> values = new LinkedHashMap<>();
        values.put("used_mb", toMb(usage.getUsed()));
        values.put("committed_mb", toMb(usage.getCommitted()));
        values.put("max_mb", toMb(usage.getMax()));
        return values;
    }

    static long toMb(final long bytes) {
        return bytes < 0 ? -1L : bytes / BYTES_PER_MB;
    }
}
