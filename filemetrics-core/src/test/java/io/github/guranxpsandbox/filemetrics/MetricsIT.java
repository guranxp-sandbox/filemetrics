package io.github.guranxpsandbox.filemetrics;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.time.Duration;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricsIT {

    private static final long POLL_TIMEOUT_MILLIS = 2_000L;

    @AfterEach
    void stopAndClearProperty() {
        Metrics.stop();
        System.clearProperty("metrics.implementation");
    }

    @Test
    void shouldNotThrowWhenStartingAndStopping() {
        assertDoesNotThrow(() -> {
            Metrics.start("order-service");
            Metrics.stop();
        });
    }

    @Test
    void shouldNotThrowWhenStoppingWithoutStarting() {
        assertDoesNotThrow(Metrics::stop);
    }

    @Test
    void shouldNotThrowWhenStartingWithInMemoryImplementation() {
        // given
        System.setProperty("metrics.implementation", "inmemory");

        // when / then
        assertDoesNotThrow(() -> Metrics.start("order-service"));
    }

    @Test
    void shouldStartCollectionDaemonAsDaemonThreadWhenStarting() {
        // when
        Metrics.start("order-service");

        // then
        assertNotNull(Metrics.collectionDaemon);
        assertTrue(Metrics.collectionDaemon.isAlive());
        assertTrue(Metrics.collectionDaemon.isDaemon());
    }

    @Test
    void shouldStopCollectionDaemonWhenStopping() throws InterruptedException {
        // given
        Metrics.start("order-service");
        final Thread daemon = Metrics.collectionDaemon;

        // when
        Metrics.stop();
        daemon.join(POLL_TIMEOUT_MILLIS);

        // then
        assertFalse(daemon.isAlive());
    }

    @Test
    void shouldThrowWhenBuilderStartedWithoutAppName() {
        assertThrows(IllegalStateException.class, () -> Metrics.builder().start());
    }

    @Test
    void shouldUseCustomIntervalFromBuilder() throws InterruptedException {
        // given
        System.setProperty("metrics.implementation", "inmemory");

        // when
        Metrics.builder()
                .appName("order-service")
                .interval(Duration.ofMillis(20L))
                .start();

        // then
        waitUntil(() -> entryCount() >= 2);
    }

    @Test
    void shouldUseCustomLogDirFromBuilder(@TempDir final File logDir) throws InterruptedException {
        // given
        System.setProperty("metrics.implementation", "file");
        final File expectedFile = new File(logDir, "order-service-" + LocalDate.now() + ".log");

        // when
        Metrics.builder()
                .appName("order-service")
                .logDir(logDir.getAbsolutePath())
                .interval(Duration.ofMillis(20L))
                .start();

        // then
        waitUntil(expectedFile::exists);
    }

    private static long entryCount() {
        return ((InMemoryMetricsLogger) Metrics.activeLogger).entries().size();
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
