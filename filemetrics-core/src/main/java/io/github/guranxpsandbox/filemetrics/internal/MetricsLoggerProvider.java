package io.github.guranxpsandbox.filemetrics.internal;

import io.github.guranxpsandbox.filemetrics.MetricsLogger;

import java.io.File;

/**
 * SPI for plugging a {@link MetricsLogger} implementation into
 * {@code Metrics}. Providers are discovered via {@link java.util.ServiceLoader}
 * and selected by matching {@link #implementationKey()} against the
 * {@code metrics.implementation} system property.
 */
public interface MetricsLoggerProvider {

    String implementationKey();

    MetricsLogger create(String appName, File logDir);
}
