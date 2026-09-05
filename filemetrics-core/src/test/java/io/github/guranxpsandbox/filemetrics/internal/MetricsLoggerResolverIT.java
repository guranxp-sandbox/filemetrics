package io.github.guranxpsandbox.filemetrics.internal;

import io.github.guranxpsandbox.filemetrics.FileMetricsLogger;
import io.github.guranxpsandbox.filemetrics.InMemoryMetricsLogger;
import io.github.guranxpsandbox.filemetrics.MetricsLogger;
import io.github.guranxpsandbox.filemetrics.NoOpMetricsLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class MetricsLoggerResolverIT {

    @Test
    void shouldResolveNoOpLoggerWhenKeyIsNoop(@TempDir final File logDir) {
        // when
        final MetricsLogger logger =
                MetricsLoggerResolver.resolve("order-service", logDir, "noop");

        // then
        assertInstanceOf(NoOpMetricsLogger.class, logger);
    }

    @Test
    void shouldResolveInMemoryLoggerWhenKeyIsInmemory(@TempDir final File logDir) {
        // when
        final MetricsLogger logger =
                MetricsLoggerResolver.resolve("order-service", logDir, "inmemory");

        // then
        assertInstanceOf(InMemoryMetricsLogger.class, logger);
    }

    @Test
    void shouldResolveFileLoggerWhenKeyIsFile(@TempDir final File logDir) {
        // when
        final MetricsLogger logger =
                MetricsLoggerResolver.resolve("order-service", logDir, "file");

        // then
        assertInstanceOf(FileMetricsLogger.class, logger);
    }

    @Test
    void shouldResolveCaseInsensitively(@TempDir final File logDir) {
        // when
        final MetricsLogger logger =
                MetricsLoggerResolver.resolve("order-service", logDir, "INMEMORY");

        // then
        assertInstanceOf(InMemoryMetricsLogger.class, logger);
    }

    @Test
    void shouldFallBackToNoOpWhenKeyIsUnknown(@TempDir final File logDir) {
        // when
        final MetricsLogger logger =
                MetricsLoggerResolver.resolve("order-service", logDir, "does-not-exist");

        // then
        assertInstanceOf(NoOpMetricsLogger.class, logger);
    }

    /**
     * The only test in this class touching the real system property —
     * everything else exercises the pure, key-parameterized overload above.
     */
    @Nested
    class SystemPropertyPlumbing {

        private static final String PROPERTY = "metrics.implementation";

        @AfterEach
        void clearProperty() {
            System.clearProperty(PROPERTY);
        }

        @Test
        void shouldReadImplementationKeyFromSystemProperty(@TempDir final File logDir) {
            // given
            System.setProperty(PROPERTY, "inmemory");

            // when
            final MetricsLogger logger = MetricsLoggerResolver.resolve("order-service", logDir);

            // then
            assertInstanceOf(InMemoryMetricsLogger.class, logger);
        }
    }
}
