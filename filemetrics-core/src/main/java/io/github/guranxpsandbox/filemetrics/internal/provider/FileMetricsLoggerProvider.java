package io.github.guranxpsandbox.filemetrics.internal.provider;

import io.github.guranxpsandbox.filemetrics.FileMetricsLogger;
import io.github.guranxpsandbox.filemetrics.MetricsLogger;

import java.io.File;

public final class FileMetricsLoggerProvider implements MetricsLoggerProvider {

    @Override
    public String implementationKey() {
        return "file";
    }

    @Override
    public MetricsLogger create(final String appName, final File logDir) {
        return new FileMetricsLogger(appName, logDir);
    }

    @Override
    public DaemonRequirements requirements() {
        return new DaemonRequirements(true, true);
    }
}
