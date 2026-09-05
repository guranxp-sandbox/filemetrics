# Architecture

Conceptual overview of `filemetrics-core`: how metrics are collected,
stored, and cleaned up, and how the threading model works. Not a
line-by-line code walkthrough — see the source and its Javadoc for
that. Update this file when the architecture itself changes, not on
every small addition.

## Big picture

```
Metrics.start("app")            (or Metrics.builder()...start())
        │
        ▼
MetricsLoggerResolver.resolve() ──▶ picks a MetricsLogger via
        │                           ServiceLoader (see below)
        ▼
   MetricsLogger                 (NoOp / InMemory / File)
        ▲                ▲
        │                │
MetricsCollectionDaemon   CleanupDaemon
(logs metric groups        (deletes old log files
 on a fixed interval)        on the same interval)
```

`Metrics` is a thin facade: it resolves a logger, starts two daemon
threads, and remembers both so `Metrics.stop()` (or the JVM shutdown
hook) can shut them down again.

## Storage: the `MetricsLogger` abstraction

`MetricsLogger` (public API) is the single contract every storage
backend implements:

```java
void log(String type, Map<String, Object> values);
void close();
```

Three implementations ship in `filemetrics-core`:

- `NoOpMetricsLogger` — discards everything. The default, so the
  library does nothing until explicitly configured.
- `InMemoryMetricsLogger` — keeps every logged group in memory,
  inspectable via `entries()`. Used by the project's own integration
  tests; a consuming app could use it the same way in its own tests.
- `FileMetricsLogger` — writes to a daily file (see "File storage
  format" below).

### How the active implementation is selected

`Metrics` never hardcodes which `MetricsLogger` to use. Instead:

1. `MetricsLoggerResolver.resolve(appName, logDir)` reads the
   `metrics.implementation` system property (default `noop`).
2. If it's `noop`, done — no lookup needed.
3. Otherwise, it iterates `MetricsLoggerProvider` implementations
   discovered via `ServiceLoader` (registered in
   `META-INF/services/...internal.MetricsLoggerProvider`), and picks
   the one whose `implementationKey()` matches (`file`, `inmemory`).
4. That provider's `create(appName, logDir)` builds the real logger.
5. Any failure at any step (unknown key, `ServiceConfigurationError`,
   etc.) falls back to `NoOpMetricsLogger` — never throws.

**Why the indirection instead of `ServiceLoader.load(MetricsLogger
.class)` directly?** `ServiceLoader` requires a public no-arg
constructor to instantiate a provider. `FileMetricsLogger` needs
`appName` and `logDir`, which aren't known until `Metrics.start()` is
called — so `ServiceLoader` instead discovers tiny `MetricsLoggerProvider`
factories (each *does* have a no-arg constructor) that build the real
logger with the right arguments on demand. This also means a future
module (e.g. `filemetrics-prometheus`) can add its own logger without
`filemetrics-core` ever depending on it — it just ships its own
provider + `META-INF/services` entry.

## Collecting a metric: built-in vs. custom

### Built-in (JVM) metrics

Every built-in metric is one small class implementing the internal
`MetricsCollector` interface:

```java
String type();                        // e.g. "heap", "gc"
List<Map<String, Object>> collect();  // one map per metric group
```

`collect()` returns a *list* rather than a single map because most
metrics produce exactly one group per tick (heap, threads, ...), but
GC produces one group per garbage collector bean — the list
accommodates both without a separate abstraction.

`MetricsCollectionDaemon` owns the full list of active collectors —
the four defaults (`HeapMetricsCollector`, `ThreadMetricsCollector`,
`MetaspaceMetricsCollector`, `GcMetricsCollector`) plus whichever
opt-in ones (`DirectMemoryMetricsCollector`, `ClassLoadingMetricsCollector`,
`CpuMetricsCollector`, `CodeCacheMetricsCollector`) `MetricsOptions`
says to include, based on the `withX()` flags on `Metrics.builder()`.
On every tick it calls `collector.collect()` for each and logs every
returned group under that collector's `type()`.

To add a new built-in metric: implement `MetricsCollector`, then add
it to `MetricsCollectionDaemon`'s collector list (unconditionally for
a default metric, or behind a new `MetricsOptions` flag for an
opt-in one).

### Custom metrics — current limitation

`MetricsLogger` is public API, so nothing stops an application from
writing its own metric groups: construct a logger directly and call
`log()` on it —

```java
MetricsLogger logger =
    new FileMetricsLogger("order-service", new File("./metrics"));
logger.log("cache", Map.of("hits", 42, "misses", 3));
```

This works today, but it's disconnected from whatever `Metrics.start()`
set up — it's a second, independent logger instance with its own
lifecycle (nothing closes it, no `keepDays` cleanup runs against it
unless you also drive that yourself). There is currently no
`Metrics.log(type, values)` that reuses the facade's own active
logger and daemons. That's a known gap, not a design decision — worth
closing in a future change.

## File storage format

`FileMetricsLogger` writes one line per metric group to
`<logDir>/<appName>-<yyyy-MM-dd>.log`:

```
2026-08-27T10:00:00Z app=order-service type=heap used_mb=312 committed_mb=400 max_mb=1024
```

- Timestamp: `Instant.now()` truncated to seconds (`MetricLineFormatter`).
- The date in the filename *is* the rotation mechanism — a new day
  means a new file, with no separate rotation logic needed.
- Every `log()` call: ensures `logDir` exists, creates the file if
  missing, restricts it to owner read/write (`FilePermissions`,
  approximating POSIX 600 via `java.io.File` — not an exact guarantee
  on every platform), formats the line, and appends it.
- Any I/O failure is caught, warned to stderr, and swallowed — the
  host application is never affected by a metrics-write failure.

Old files are deleted by `LogFileCleaner`, driven by `CleanupDaemon`:
it lists `logDir`, parses each `<appName>-<date>.log` name back into
a date, and deletes any file older than `keepDays`. Files that don't
match that exact naming pattern (wrong app name, unexpected format)
are left alone.

## Threading model

Both daemons share one base class, `IntervalDaemon`:

```
while (running) {
    tick();          // subclass-specific work
    sleep(interval);
}
```

`shutdown()` sets `running = false` *and* interrupts the thread, so a
daemon sleeping through a long interval still stops promptly rather
than waiting out the full sleep. Both daemons are marked as JVM
daemon threads, so they never keep the JVM alive on their own.

- `MetricsCollectionDaemon.tick()` — loops over the active collector
  list and logs every group each one returns.
- `CleanupDaemon.tick()` — runs `LogFileCleaner.clean(...)` once.

Both currently run on the *same* interval (`Metrics.builder().interval(...)`)
— there's no separate cleanup frequency, since none is documented and
reusing one interval keeps the configuration surface smaller.

`Metrics.stop()` shuts both daemons down and closes the active
logger. A JVM shutdown hook (registered once, in a `static`
initializer — see `Metrics.shutdownHook`) calls `stop()` automatically,
so an application that never calls it explicitly still shuts down
cleanly. The hook is registered once and never removed; `stop()` is
idempotent, so the hook firing after an already-explicit `stop()` is
harmless.

## Default behavior

Until `Metrics.start()`/`Metrics.builder()...start()` is called,
nothing happens — no threads, no files. Once started, with no further
configuration:

| Setting                  | Default                             |
|--------------------------|--------------------------------------|
| `metrics.implementation` | `noop` (discards until set to `file`/`inmemory`) |
| log directory            | `./metrics`                         |
| collection interval      | 60 minutes                          |
| retention (`keepDays`)   | 7 days                              |
| opt-in metrics           | all off (direct memory, classloading, CPU, code cache) |

So even calling `Metrics.start("app")` with `metrics.implementation`
unset collects nothing observable — the daemons run, but log through
a `NoOpMetricsLogger`. This is deliberate: the library never writes
anything to disk unless a host app explicitly opts in via that system
property.
