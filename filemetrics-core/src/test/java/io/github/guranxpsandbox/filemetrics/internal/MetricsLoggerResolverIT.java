package io.github.guranxpsandbox.filemetrics.internal;

import io.github.guranxpsandbox.filemetrics.FileMetricsLogger;
import io.github.guranxpsandbox.filemetrics.InMemoryMetricsLogger;
import io.github.guranxpsandbox.filemetrics.MetricsLogger;
import io.github.guranxpsandbox.filemetrics.NoOpMetricsLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricsLoggerResolverIT {

    private static final String PROPERTY = "metrics.implementation";

    @AfterEach
    void clearProperty() {
        System.clearProperty(PROPERTY);
    }

    @Test
    void shouldResolveNoOpLoggerWhenPropertyNotSet(@TempDir final File logDir) {
        // given
        System.clearProperty(PROPERTY);

        // when
        final MetricsLogger logger =
                MetricsLoggerResolver.resolve("order-service", logDir);

        // then
        assertTrue(logger instanceof NoOpMetricsLogger);
    }

    @Test
    void shouldResolveNoOpLoggerWhenPropertyIsNoop(@TempDir final File logDir) {
        // given
        System.setProperty(PROPERTY, "noop");

        // when
        final MetricsLogger logger =
                MetricsLoggerResolver.resolve("order-service", logDir);

        // then
        assertTrue(logger instanceof NoOpMetricsLogger);
    }

    @Test
    void shouldResolveInMemoryLoggerWhenPropertyIsInmemory(@TempDir final File logDir) {
        // given
        System.setProperty(PROPERTY, "inmemory");

        // when
        final MetricsLogger logger =
                MetricsLoggerResolver.resolve("order-service", logDir);

        // then
        assertTrue(logger instanceof InMemoryMetricsLogger);
    }

    @Test
    void shouldResolveFileLoggerWhenPropertyIsFile(@TempDir final File logDir) {
        // given
        System.setProperty(PROPERTY, "file");

        // when
        final MetricsLogger logger =
                MetricsLoggerResolver.resolve("order-service", logDir);

        // then
        assertTrue(logger instanceof FileMetricsLogger);
    }

    @Test
    void shouldResolveCaseInsensitively(@TempDir final File logDir) {
        // given
        System.setProperty(PROPERTY, "INMEMORY");

        // when
        final MetricsLogger logger =
                MetricsLoggerResolver.resolve("order-service", logDir);

        // then
        assertTrue(logger instanceof InMemoryMetricsLogger);
    }

    @Test
    void shouldFallBackToNoOpWhenPropertyIsUnknown(@TempDir final File logDir) {
        // given
        System.setProperty(PROPERTY, "does-not-exist");

        // when
        final MetricsLogger logger =
                MetricsLoggerResolver.resolve("order-service", logDir);

        // then
        assertTrue(logger instanceof NoOpMetricsLogger);
    }
}
