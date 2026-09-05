package io.github.guranxpsandbox.filemetrics.internal;

import java.util.List;
import java.util.Map;

/**
 * Produces one or more metric groups of the same {@link #type()} for
 * {@link MetricsCollectionDaemon} to log on each collection tick (e.g.
 * one group per garbage collector bean for GC metrics).
 */
interface MetricsCollector {

    String type();

    List<Map<String, Object>> collect();
}
