package io.github.guranxpsandbox.filemetrics;

import io.github.guranxpsandbox.filemetrics.internal.MetricsLoggerResolver;

import java.io.File;

/**
 * Entry point for filemetrics. {@code Metrics.start("app-name")} resolves
 * and activates a {@link MetricsLogger} implementation based on the
 * {@code metrics.implementation} system property; {@code Metrics.stop()}
 * releases it. Safe to call repeatedly; never throws to the caller.
 */
public final class Metrics {

    private static final File DEFAULT_LOG_DIR = new File("./metrics");

    private static volatile MetricsLogger activeLogger = new NoOpMetricsLogger();

    private Metrics() {
    }

    public static synchronized void start(final String appName) {
        activeLogger = MetricsLoggerResolver.resolve(appName, DEFAULT_LOG_DIR);
    }

    public static synchronized void stop() {
        activeLogger.close();
        activeLogger = new NoOpMetricsLogger();
    }
}
