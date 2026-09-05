# filemetrics

Open source Java library that writes JVM metrics and custom metrics to
file — no external dependencies in the core module.

The simplest possible way to log metrics from a Java app to file,
without requiring Prometheus, Grafana, or other infrastructure. One
line of code: `Metrics.start("app-name")`.

## Status

This project is early-stage and not yet published to Maven Central.

- `filemetrics-core` — done: `MetricsLogger` interface,
  `NoOpMetricsLogger` (default), `InMemoryMetricsLogger` (for tests),
  `FileMetricsLogger`, and the `Metrics` facade (lifecycle,
  configuration, default + opt-in metric collection, log file
  cleanup, restricted file permissions).
- `filemetrics-prometheus`, `filemetrics-spring`,
  `filemetrics-autoinstrument` — not started.

## Usage

```java
// Minimal — one line, writes to file every 60 minutes by default
Metrics.start("order-service");

// With configuration
Metrics.builder()
    .appName("order-service")
    .logDir("/var/log/metrics")          // default: ./metrics
    .interval(Duration.ofMinutes(15))    // default: 60 min
    .keepDays(14)                        // default: 7
    .withDirectMemory()                  // opt-in
    .withClassLoading()                  // opt-in
    .withCpu()                           // opt-in
    .withCodeCache()                     // opt-in
    .start();

// Stop — symmetric with start
Metrics.stop();
```

Select the logger implementation via a system property (defaults to
`noop`, so nothing happens unless you opt in):

```bash
-Dmetrics.implementation=file
```

Default metrics collected: heap, threads, metaspace, GC. Opt-in:
direct memory, class loading, CPU, code cache.

### Custom metrics

Log your own metric groups through the same file and lifecycle
`Metrics.start()` set up:

```java
Metrics.log("cache", Map.of("hits", 42, "misses", 3));
```

A no-op before `start()` is called. Writes immediately, with no
internal buffering — for anything logged often (e.g. once per
request), aggregate a count/total/max yourself and call `log()`
periodically, rather than once per event. See
[ARCHITECTURE.md](ARCHITECTURE.md) for details.

## Modules

```
filemetrics-core              → JVM and custom metrics to file, no
                                 external dependencies
filemetrics-prometheus        → Micrometer + Prometheus format, file
                                 and/or server
filemetrics-spring            → Spring Boot autoconfiguration
filemetrics-autoinstrument    → automatic instrumentation via
                                 reflection
```

`filemetrics-core` has no external dependencies. The other modules
build on top of it and pull in their own dependencies (Micrometer,
Spring Boot, etc.) as needed.

## Requirements

- Java 8+ (bumping to 21 in v2 once target apps upgrade)

## License

Apache 2.0

## Contributing

See [WORKFLOW.md](WORKFLOW.md) for the development workflow,
[CLAUDE.md](CLAUDE.md) for project conventions, and
[ARCHITECTURE.md](ARCHITECTURE.md) for how metrics collection,
storage, and threading fit together.
