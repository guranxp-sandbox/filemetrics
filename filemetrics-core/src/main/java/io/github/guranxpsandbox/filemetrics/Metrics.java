package io.github.guranxpsandbox.filemetrics;

import io.github.guranxpsandbox.filemetrics.internal.MetricsCollectionDaemon;
import io.github.guranxpsandbox.filemetrics.internal.MetricsLoggerResolver;
import io.github.guranxpsandbox.filemetrics.internal.MetricsOptions;

import java.io.File;
import java.time.Duration;

/**
 * Entry point for filemetrics. {@code Metrics.start("app-name")} resolves
 * and activates a {@link MetricsLogger} implementation based on the
 * {@code metrics.implementation} system property, then starts a daemon
 * thread collecting heap metrics on a fixed interval.
 * {@code Metrics.stop()} shuts the thread down and releases the logger.
 * Safe to call repeatedly; never throws to the caller.
 * Use {@link #builder()} to configure the log directory, interval, or
 * opt-in metrics.
 */
public final class Metrics {

    private static final File DEFAULT_LOG_DIR = new File("./metrics");
    private static final Duration DEFAULT_INTERVAL = Duration.ofMinutes(60L);

    static volatile MetricsLogger activeLogger = new NoOpMetricsLogger();
    static volatile MetricsCollectionDaemon collectionDaemon;

    private Metrics() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static void start(final String appName) {
        builder().appName(appName).start();
    }

    public static synchronized void stop() {
        if (collectionDaemon != null) {
            collectionDaemon.shutdown();
            collectionDaemon = null;
        }
        activeLogger.close();
        activeLogger = new NoOpMetricsLogger();
    }

    private static synchronized void apply(final String appName, final File logDir,
            final Duration interval, final MetricsOptions options) {
        activeLogger = MetricsLoggerResolver.resolve(appName, logDir);
        collectionDaemon = new MetricsCollectionDaemon(activeLogger, interval.toMillis(), options);
        collectionDaemon.start();
    }

    /**
     * Configures and starts filemetrics. Obtain via {@link Metrics#builder()}.
     */
    public static final class Builder {

        private String appName;
        private File logDir = DEFAULT_LOG_DIR;
        private Duration interval = DEFAULT_INTERVAL;
        private boolean directMemory;
        private boolean classLoading;
        private boolean cpu;
        private boolean codeCache;

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
            final MetricsOptions options =
                    new MetricsOptions(directMemory, classLoading, cpu, codeCache);
            apply(appName, logDir, interval, options);
        }
    }
}
