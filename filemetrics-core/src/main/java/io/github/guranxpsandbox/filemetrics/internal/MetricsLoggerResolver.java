package io.github.guranxpsandbox.filemetrics.internal;

import io.github.guranxpsandbox.filemetrics.MetricsLogger;
import io.github.guranxpsandbox.filemetrics.NoOpMetricsLogger;

import java.io.File;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * Resolves the active {@link MetricsLogger} from the
 * {@code metrics.implementation} system property, matching it against
 * {@link MetricsLoggerProvider} implementations discovered via
 * {@link ServiceLoader}. Never throws — falls back to
 * {@link NoOpMetricsLogger} whenever resolution fails, so the host app
 * is never affected by misconfiguration.
 */
public final class MetricsLoggerResolver {

    private static final String PROPERTY = "metrics.implementation";
    private static final String DEFAULT_KEY = "noop";

    private MetricsLoggerResolver() {
    }

    public static MetricsLogger resolve(final String appName, final File logDir) {
        return resolve(appName, logDir, System.getProperty(PROPERTY, DEFAULT_KEY));
    }

    static MetricsLogger resolve(final String appName, final File logDir,
            final String implementationKey) {
        final String key = implementationKey.trim();
        if (DEFAULT_KEY.equalsIgnoreCase(key)) {
            return new NoOpMetricsLogger();
        }
        try {
            for (final MetricsLoggerProvider provider
                    : ServiceLoader.load(MetricsLoggerProvider.class)) {
                if (provider.implementationKey().equalsIgnoreCase(key)) {
                    return provider.create(appName, logDir);
                }
            }
        } catch (final RuntimeException | ServiceConfigurationError e) {
            System.err.println("[filemetrics] failed to resolve metrics "
                    + "implementation '" + key + "': " + e.getMessage());
            return new NoOpMetricsLogger();
        }
        System.err.println("[filemetrics] unknown metrics.implementation '"
                + key + "', falling back to noop");
        return new NoOpMetricsLogger();
    }
}
