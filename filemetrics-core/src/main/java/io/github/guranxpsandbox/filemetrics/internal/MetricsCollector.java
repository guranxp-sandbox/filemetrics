package io.github.guranxpsandbox.filemetrics.internal;

import java.util.Map;

/**
 * Produces one metric group for {@link MetricsCollectionDaemon} to log
 * on each collection tick.
 */
interface MetricsCollector {

    String type();

    Map<String, Object> collect();
}
