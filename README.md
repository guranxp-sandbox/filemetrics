# filemetrics

Open source Java library that writes JVM metrics and custom metrics to
file — no external dependencies in the core module.

The simplest possible way to log metrics from a Java app to file,
without requiring Prometheus, Grafana, or other infrastructure. Goal:
one line of code, `Metrics.start("app-name")`.

## Status

This project is early-stage and not yet published to Maven Central.

- `MetricsLogger` interface — done
- `NoOpMetricsLogger` — done (default)
- `InMemoryMetricsLogger` — not started
- `FileMetricsLogger` — not started
- `Metrics` (facade/entry point) — not started

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

See [WORKFLOW.md](WORKFLOW.md) for the development workflow and
[CLAUDE.md](CLAUDE.md) for project conventions.
