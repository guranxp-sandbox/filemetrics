package io.github.guranxpsandbox.filemetrics.internal;

import io.github.guranxpsandbox.filemetrics.InMemoryMetricsLogger;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricsCollectionDaemonIT {

    private static final long SHORT_INTERVAL_MILLIS = 20L;
    private static final long POLL_TIMEOUT_MILLIS = 2_000L;

    @Test
    void shouldCollectHeapMetricsPeriodically() throws InterruptedException {
        // given
        final InMemoryMetricsLogger logger = new InMemoryMetricsLogger();
        final MetricsCollectionDaemon daemon =
                new MetricsCollectionDaemon(logger, SHORT_INTERVAL_MILLIS);

        // when
        daemon.start();
        try {
            waitUntil(() -> countOfType(logger, "heap") >= 2);
        } finally {
            daemon.shutdown();
        }

        // then
        final List<InMemoryMetricsLogger.LoggedMetric> entries = logger.entries();
        assertTrue(countOfType(logger, "heap") >= 2);
        assertEquals("heap", entries.get(0).type());
        assertTrue(entries.get(0).values().containsKey("used_mb"));
    }

    @Test
    void shouldCollectThreadMetricsPeriodically() throws InterruptedException {
        // given
        final InMemoryMetricsLogger logger = new InMemoryMetricsLogger();
        final MetricsCollectionDaemon daemon =
                new MetricsCollectionDaemon(logger, SHORT_INTERVAL_MILLIS);

        // when
        daemon.start();
        try {
            waitUntil(() -> countOfType(logger, "threads") >= 2);
        } finally {
            daemon.shutdown();
        }

        // then
        final List<InMemoryMetricsLogger.LoggedMetric> threadEntries = logger.entries().stream()
                .filter(entry -> "threads".equals(entry.type()))
                .collect(Collectors.toList());
        assertTrue(threadEntries.size() >= 2);
        assertTrue(threadEntries.get(0).values().containsKey("live"));
    }

    private static long countOfType(final InMemoryMetricsLogger logger, final String type) {
        return logger.entries().stream().filter(entry -> type.equals(entry.type())).count();
    }

    @Test
    void shouldRunAsDaemonThread() {
        // given
        final InMemoryMetricsLogger logger = new InMemoryMetricsLogger();
        final MetricsCollectionDaemon daemon =
                new MetricsCollectionDaemon(logger, SHORT_INTERVAL_MILLIS);

        // then
        assertTrue(daemon.isDaemon());
    }

    @Test
    void shouldStopCollectingAfterShutdown() throws InterruptedException {
        // given
        final InMemoryMetricsLogger logger = new InMemoryMetricsLogger();
        final MetricsCollectionDaemon daemon =
                new MetricsCollectionDaemon(logger, SHORT_INTERVAL_MILLIS);
        daemon.start();
        waitUntil(() -> !logger.entries().isEmpty());

        // when
        daemon.shutdown();
        daemon.join(POLL_TIMEOUT_MILLIS);
        final int countAtShutdown = logger.entries().size();
        Thread.sleep(SHORT_INTERVAL_MILLIS * 10);

        // then
        assertFalse(daemon.isAlive());
        assertEquals(countAtShutdown, logger.entries().size());
    }

    @Test
    void shouldTerminatePromptlyEvenWhileSleeping() throws InterruptedException {
        // given
        final InMemoryMetricsLogger logger = new InMemoryMetricsLogger();
        final long longInterval = 5_000L;
        final MetricsCollectionDaemon daemon =
                new MetricsCollectionDaemon(logger, longInterval);
        daemon.start();
        waitUntil(() -> !logger.entries().isEmpty());

        // when
        final long before = System.currentTimeMillis();
        daemon.shutdown();
        daemon.join(POLL_TIMEOUT_MILLIS);
        final long elapsed = System.currentTimeMillis() - before;

        // then
        assertFalse(daemon.isAlive());
        assertTrue(elapsed < longInterval);
    }

    private static void waitUntil(final Condition condition) throws InterruptedException {
        final long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MILLIS;
        while (!condition.isMet()) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("condition not met within timeout");
            }
            Thread.sleep(5L);
        }
    }

    private interface Condition {
        boolean isMet();
    }
}
