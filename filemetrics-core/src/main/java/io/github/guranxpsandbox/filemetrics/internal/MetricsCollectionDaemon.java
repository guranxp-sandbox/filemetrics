package io.github.guranxpsandbox.filemetrics.internal;

import io.github.guranxpsandbox.filemetrics.MetricsLogger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Daemon thread that logs each default metric group on a fixed
 * interval until {@link #shutdown()} is called.
 */
public final class MetricsCollectionDaemon extends Thread {

    private final MetricsLogger logger;
    private final List<MetricsCollector> collectors;
    private final long intervalMillis;
    private volatile boolean running = true;

    public MetricsCollectionDaemon(final MetricsLogger logger, final long intervalMillis) {
        super("filemetrics-collector");
        this.logger = logger;
        this.collectors = Arrays.asList(
                new HeapMetricsCollector(),
                new ThreadMetricsCollector(),
                new MetaspaceMetricsCollector(),
                new GcMetricsCollector());
        this.intervalMillis = intervalMillis;
        setDaemon(true);
    }

    @Override
    public void run() {
        while (running) {
            for (final MetricsCollector collector : collectors) {
                for (final Map<String, Object> values : collector.collect()) {
                    logger.log(collector.type(), values);
                }
            }
            sleepQuietly();
        }
    }

    public void shutdown() {
        running = false;
        interrupt();
    }

    private void sleepQuietly() {
        try {
            Thread.sleep(intervalMillis);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
