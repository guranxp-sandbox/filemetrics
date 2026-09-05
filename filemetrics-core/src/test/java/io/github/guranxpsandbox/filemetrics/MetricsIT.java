package io.github.guranxpsandbox.filemetrics;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void shouldStartCleanupDaemonAsDaemonThreadWhenStarting() {
        // when
        Metrics.start("order-service");

        // then
        assertNotNull(Metrics.cleanupDaemon);
        assertTrue(Metrics.cleanupDaemon.isAlive());
        assertTrue(Metrics.cleanupDaemon.isDaemon());
    }

    @Test
    void shouldStopCleanupDaemonWhenStopping() throws InterruptedException {
        // given
        Metrics.start("order-service");
        final Thread daemon = Metrics.cleanupDaemon;

        // when
        Metrics.stop();
        daemon.join(POLL_TIMEOUT_MILLIS);

        // then
        assertFalse(daemon.isAlive());
    }

    @Test
    void shouldUseCustomKeepDaysFromBuilder(@TempDir final File logDir) throws IOException,
            InterruptedException {
        // given
        System.setProperty("metrics.implementation", "file");
        final File oldFile =
                new File(logDir, "order-service-" + LocalDate.now().minusDays(10) + ".log");
        assertTrue(oldFile.createNewFile());

        // when
        Metrics.builder()
                .appName("order-service")
                .logDir(logDir.getAbsolutePath())
                .interval(Duration.ofMillis(20L))
                .keepDays(7)
                .start();

        // then
        waitUntil(() -> !oldFile.exists());
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

    @Test
    void shouldNotCollectOptInMetricsByDefault() throws InterruptedException {
        // given
        System.setProperty("metrics.implementation", "inmemory");

        // when
        Metrics.builder()
                .appName("order-service")
                .interval(Duration.ofMillis(20L))
                .start();

        // then
        waitUntil(() -> entryCount() >= 2);
        assertEquals(0, entriesOfType("direct"));
        assertEquals(0, entriesOfType("classloading"));
        assertEquals(0, entriesOfType("cpu"));
        assertEquals(0, entriesOfType("codecache"));
    }

    @Test
    void shouldCollectOptInMetricsWhenEnabledOnBuilder() throws InterruptedException {
        // given
        System.setProperty("metrics.implementation", "inmemory");

        // when
        Metrics.builder()
                .appName("order-service")
                .interval(Duration.ofMillis(20L))
                .withDirectMemory()
                .withClassLoading()
                .withCpu()
                .withCodeCache()
                .start();

        // then
        waitUntil(() -> entriesOfType("codecache") >= 2);
        assertTrue(entriesOfType("direct") >= 2);
        assertTrue(entriesOfType("classloading") >= 2);
        assertTrue(entriesOfType("cpu") >= 2);
        assertTrue(entriesOfType("codecache") >= 2);
    }

    private static long entryCount() {
        return ((InMemoryMetricsLogger) Metrics.activeLogger).entries().size();
    }

    private static long entriesOfType(final String type) {
        return ((InMemoryMetricsLogger) Metrics.activeLogger).entries().stream()
                .filter(entry -> type.equals(entry.type()))
                .count();
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
