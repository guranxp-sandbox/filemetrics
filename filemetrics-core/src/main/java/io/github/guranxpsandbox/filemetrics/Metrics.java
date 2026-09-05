package io.github.guranxpsandbox.filemetrics;

import io.github.guranxpsandbox.filemetrics.internal.MetricsCollectionDaemon;
import io.github.guranxpsandbox.filemetrics.internal.MetricsLoggerResolver;

import java.io.File;

/**
 * Entry point for filemetrics. {@code Metrics.start("app-name")} resolves
 * and activates a {@link MetricsLogger} implementation based on the
 * {@code metrics.implementation} system property, then starts a daemon
 * thread collecting heap metrics on a fixed interval.
 * {@code Metrics.stop()} shuts the thread down and releases the logger.
 * Safe to call repeatedly; never throws to the caller.
 */
public final class Metrics {

    private static final File DEFAULT_LOG_DIR = new File("./metrics");
    private static final long DEFAULT_INTERVAL_MILLIS = 60L * 60L * 1000L;

    private static volatile MetricsLogger activeLogger = new NoOpMetricsLogger();
    static volatile MetricsCollectionDaemon collectionDaemon;

    private Metrics() {
    }

    public static synchronized void start(final String appName) {
        activeLogger = MetricsLoggerResolver.resolve(appName, DEFAULT_LOG_DIR);
        collectionDaemon = new MetricsCollectionDaemon(activeLogger, DEFAULT_INTERVAL_MILLIS);
        collectionDaemon.start();
    }

    public static synchronized void stop() {
        if (collectionDaemon != null) {
            collectionDaemon.shutdown();
            collectionDaemon = null;
        }
        activeLogger.close();
        activeLogger = new NoOpMetricsLogger();
    }
}
