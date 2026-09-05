package io.github.guranxpsandbox.filemetrics.internal.provider;

import io.github.guranxpsandbox.filemetrics.InMemoryMetricsLogger;
import io.github.guranxpsandbox.filemetrics.MetricsLogger;

import java.io.File;

public final class InMemoryMetricsLoggerProvider implements MetricsLoggerProvider {

    @Override
    public String implementationKey() {
        return "inmemory";
    }

    @Override
    public MetricsLogger create(final String appName, final File logDir) {
        return new InMemoryMetricsLogger();
    }

    @Override
    public DaemonRequirements requirements() {
        return new DaemonRequirements(true, false);
    }
}
