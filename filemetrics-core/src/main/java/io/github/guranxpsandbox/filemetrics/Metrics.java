package io.github.guranxpsandbox.filemetrics;

import io.github.guranxpsandbox.filemetrics.internal.BuilderProperties;
import io.github.guranxpsandbox.filemetrics.internal.CleanupDaemon;
import io.github.guranxpsandbox.filemetrics.internal.MetricsCollectionDaemon;
import io.github.guranxpsandbox.filemetrics.internal.MetricsLoggerResolver;
import io.github.guranxpsandbox.filemetrics.internal.MetricsOptions;

import java.io.File;
import java.time.Duration;
import java.util.Map;

/**
 * Entry point for filemetrics. {@code Metrics.start("app-name")} resolves
 * and activates a {@link MetricsLogger} implementation based on the
 * {@code metrics.implementation} system property, then starts a daemon
 * thread collecting heap metrics and a daemon thread cleaning up old log
 * files, both on a fixed interval. {@code Metrics.stop()} shuts both
 * threads down — waiting (bounded) for each to actually terminate
 * before returning, so no write is left in flight — and releases the
 * logger. Safe to call repeatedly; never throws to the caller. Use
 * {@link #builder()} to configure the log directory, interval,
 * retention, or opt-in metrics. A JVM shutdown hook calls
 * {@link #stop()} automatically, so an app that never calls it
 * explicitly still shuts down cleanly.
 */
public final class Metrics {

    private static final long SHUTDOWN_JOIN_TIMEOUT_MILLIS = 5_000L;

    static volatile MetricsLogger activeLogger = new NoOpMetricsLogger();
    static volatile MetricsCollectionDaemon collectionDaemon;
    static volatile CleanupDaemon cleanupDaemon;

    static final Thread shutdownHook = new Thread(Metrics::stop, "filemetrics-shutdown-hook");

    static {
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    private Metrics() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static void start(final String appName) {
        builder().appName(appName).start();
    }

    /**
     * Logs a custom metric group through whichever {@link MetricsLogger}
     * {@link #start} (or {@link Builder#start()}) activated. A no-op
     * before {@code start()} is called, since the active logger defaults
     * to {@link NoOpMetricsLogger}.
     */
    public static void log(final String type, final Map<String, Object> values) {
        activeLogger.log(type, values);
    }

    public static synchronized void stop() {
        if (collectionDaemon != null) {
            collectionDaemon.shutdown();
            joinQuietly(collectionDaemon);
            collectionDaemon = null;
        }
        if (cleanupDaemon != null) {
            cleanupDaemon.shutdown();
            joinQuietly(cleanupDaemon);
            cleanupDaemon = null;
        }
        activeLogger.close();
        activeLogger = new NoOpMetricsLogger();
    }

    private static void joinQuietly(final Thread thread) {
        try {
            thread.join(SHUTDOWN_JOIN_TIMEOUT_MILLIS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static synchronized void apply(final String appName, final File logDir,
            final Duration interval, final int keepDays, final MetricsOptions options) {
        activeLogger = MetricsLoggerResolver.resolve(appName, logDir);
        collectionDaemon = new MetricsCollectionDaemon(activeLogger, interval.toMillis(), options);
        collectionDaemon.start();
        cleanupDaemon = new CleanupDaemon(logDir, appName, keepDays, interval.toMillis());
        cleanupDaemon.start();
    }

    /**
     * Configures and starts filemetrics. Obtain via {@link Metrics#builder()}.
     */
    public static final class Builder {

        private String appName;
        private File logDir;
        private Duration interval;
        private Integer keepDays;
        private Boolean directMemory;
        private Boolean classLoading;
        private Boolean cpu;
        private Boolean codeCache;

        private Builder() {
        }

        public Builder appName(final String appName) {
            this.appName = appName;
            return this;
        }

        public Builder logDir(final String logDir) {
            this.logDir = new File(logDir);
            return this;
        }

        public Builder interval(final Duration interval) {
            this.interval = interval;
            return this;
        }

        public Builder keepDays(final int keepDays) {
            this.keepDays = keepDays;
            return this;
        }

        public Builder withDirectMemory() {
            this.directMemory = true;
            return this;
        }

        public Builder withClassLoading() {
            this.classLoading = true;
            return this;
        }

        public Builder withCpu() {
            this.cpu = true;
            return this;
        }

        public Builder withCodeCache() {
            this.codeCache = true;
            return this;
        }

        public void start() {
            if (appName == null) {
                throw new IllegalStateException("appName must be set before calling start()");
            }
            final MetricsOptions options = new MetricsOptions(
                    BuilderProperties.flag(directMemory, "metrics.opt.direct"),
                    BuilderProperties.flag(classLoading, "metrics.opt.classloading"),
                    BuilderProperties.flag(cpu, "metrics.opt.cpu"),
                    BuilderProperties.flag(codeCache, "metrics.opt.codecache"));
            apply(appName, BuilderProperties.logDir(logDir), BuilderProperties.interval(interval),
                    BuilderProperties.keepDays(keepDays), options);
        }
    }
}
