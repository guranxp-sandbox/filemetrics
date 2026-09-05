package io.github.guranxpsandbox.filemetrics.internal;

import io.github.guranxpsandbox.filemetrics.FileMetricsLogger;
import io.github.guranxpsandbox.filemetrics.InMemoryMetricsLogger;
import io.github.guranxpsandbox.filemetrics.NoOpMetricsLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricsLoggerResolverIT {

    @Test
    void shouldResolveNoOpLoggerNeedingNoDaemonsWhenKeyIsNoop(@TempDir final File logDir) {
        // when
        final ResolvedLogger resolved =
                MetricsLoggerResolver.resolve("order-service", logDir, "noop");

        // then
        assertInstanceOf(NoOpMetricsLogger.class, resolved.logger());
        assertFalse(resolved.requirements().collection());
        assertFalse(resolved.requirements().cleanup());
    }

    @Test
    void shouldResolveInMemoryLoggerNeedingOnlyCollectionWhenKeyIsInmemory(
            @TempDir final File logDir) {
        // when
        final ResolvedLogger resolved =
                MetricsLoggerResolver.resolve("order-service", logDir, "inmemory");

        // then
        assertInstanceOf(InMemoryMetricsLogger.class, resolved.logger());
        assertTrue(resolved.requirements().collection());
        assertFalse(resolved.requirements().cleanup());
    }

    @Test
    void shouldResolveFileLoggerNeedingBothDaemonsWhenKeyIsFile(@TempDir final File logDir) {
        // when
        final ResolvedLogger resolved =
                MetricsLoggerResolver.resolve("order-service", logDir, "file");

        // then
        assertInstanceOf(FileMetricsLogger.class, resolved.logger());
        assertTrue(resolved.requirements().collection());
        assertTrue(resolved.requirements().cleanup());
    }

    @Test
    void shouldResolveCaseInsensitively(@TempDir final File logDir) {
        // when
        final ResolvedLogger resolved =
                MetricsLoggerResolver.resolve("order-service", logDir, "INMEMORY");

        // then
        assertInstanceOf(InMemoryMetricsLogger.class, resolved.logger());
    }

    @Test
    void shouldFallBackToNoOpWhenKeyIsUnknown(@TempDir final File logDir) {
        // when
        final ResolvedLogger resolved =
                MetricsLoggerResolver.resolve("order-service", logDir, "does-not-exist");

        // then
        assertInstanceOf(NoOpMetricsLogger.class, resolved.logger());
        assertFalse(resolved.requirements().collection());
        assertFalse(resolved.requirements().cleanup());
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
            final ResolvedLogger resolved = MetricsLoggerResolver.resolve("order-service", logDir);

            // then
            assertInstanceOf(InMemoryMetricsLogger.class, resolved.logger());
        }
    }
}
