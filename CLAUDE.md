# filemetrics

Open source Java library that writes JVM metrics and custom metrics to
file — no external dependencies in the core module.

## Purpose

The simplest possible way to log metrics from a Java app to file, without
requiring Prometheus, Grafana, or other infrastructure. One line of code:
`Metrics.start("app-name")`.

## Project decisions

```
Name:         filemetrics
GitHub:       https://github.com/guranxp-sandbox/filemetrics
Group id:     io.github.guranxpsandbox
Java minimum: 8 (bump to 21 in v2 once target apps upgrade)
License:      Apache 2.0
```

## API stability policy

Public classes and methods in `filemetrics-core` are stable from v1.0.
No breaking changes are introduced without a new major version.
Internal classes (package `*.internal`) are not considered public API.

## Module structure

```
filemetrics-core              → JVM and custom metrics to file, no external dependencies
filemetrics-prometheus        → Micrometer + Prometheus format, file and/or server
filemetrics-spring            → Spring Boot autoconfiguration
filemetrics-autoinstrument    → automatic instrumentation via reflection
```

## Java code standard (filemetrics-core)

1. **`final` everywhere** — every method parameter and local variable is
   declared `final`.
2. **No external dependencies** in `filemetrics-core` — only
   `java.lang.*`, `java.util.*`, `java.io.*`, `java.lang.management.*`,
   `java.time.*`. File I/O uses `java.io` (not `java.nio.file`).
3. **Threading** — daemon threads for file writing and cleanup.
   `close()` shuts down both.
4. **Error handling** — the host app is never affected by metrics
   problems. Warning to stderr, never an exception to the caller.
   Falls back to noop if the file can't be created.
5. **File format** — key-value, one line per metric group:
   ```
   2026-08-27T10:00:00Z app=order-service type=heap used_mb=312 committed_mb=400 max_mb=1024
   ```

## Testing

Unit tests are named `*Test.java` (run by Surefire, `mvn test`).
Integration tests are named `*IT.java` (run by Failsafe, only in the
`integration-tests` CI job, never re-running unit tests).

## Packages

```
io.github.guranxpsandbox.filemetrics   ← public API
io.github.guranxpsandbox.filemetrics.internal ← non-public API
```

## Current status

- `MetricsLogger` interface — done
- `NoOpMetricsLogger` — done (default)
- `InMemoryMetricsLogger` — done (inspectable, for tests)
- `FileMetricsLogger` — done (writes one line per metric group; no
  rotation/cleanup/permissions yet)
- `Metrics` (facade/entry point) — in progress: `start()`/`stop()`
  resolve a `MetricsLogger` via ServiceLoader (`metrics.implementation`)
  and run a daemon thread collecting heap and thread metrics every 60
  minutes. Metaspace/GC, opt-in metrics, config API, and cleanup are
  not started yet.

## Workflow

See WORKFLOW.md.
