package io.github.guranxpsandbox.filemetrics.internal;

import io.github.guranxpsandbox.filemetrics.MetricsLogger;
import io.github.guranxpsandbox.filemetrics.NoOpMetricsLogger;

import java.io.File;

public final class NoOpMetricsLoggerProvider implements MetricsLoggerProvider {

    @Override
    public String implementationKey() {
        return "noop";
    }

    @Override
    public MetricsLogger create(final String appName, final File logDir) {
        return new NoOpMetricsLogger();
    }
}
