package io.github.guranxpsandbox.filemetrics.internal;

import io.github.guranxpsandbox.filemetrics.MetricsLogger;

/**
 * Daemon thread that logs heap metrics on a fixed interval until
 * {@link #shutdown()} is called.
 */
public final class MetricsCollectionDaemon extends Thread {

    private final MetricsLogger logger;
    private final HeapMetricsCollector heapMetricsCollector;
    private final long intervalMillis;
    private volatile boolean running = true;

    public MetricsCollectionDaemon(final MetricsLogger logger, final long intervalMillis) {
        super("filemetrics-collector");
        this.logger = logger;
        this.heapMetricsCollector = new HeapMetricsCollector();
        this.intervalMillis = intervalMillis;
        setDaemon(true);
    }

    @Override
    public void run() {
        while (running) {
            logger.log("heap", heapMetricsCollector.collect());
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
