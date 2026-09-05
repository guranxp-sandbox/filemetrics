# filemetrics

Open source Java-bibliotek som skriver JVM-metrics och custom metrics till fil — utan externa dependencies i core-modulen.

## Syfte

Enklaste möjliga sättet att logga metrics från en Java-app till fil, utan krav på Prometheus, Grafana eller annan infrastruktur. En rad kod: `Metrics.start("app-name")`.

## Projektbeslut

```
Namn:         filemetrics
GitHub:       https://github.com/guranxp-sandbox/filemetrics
Group id:     io.github.guranxpsandbox
Java minimum: 8 (bumpa till 21 i v2 när målapparna uppgraderas)
Licens:       Apache 2.0
```

## API-stabilitetspolicy

Publika klasser och metoder i `filemetrics-core` är stabila från v1.0.
Inga breaking changes introduceras utan en ny major-version.
Interna klasser (paket `*.internal`) räknas inte som publikt API.

## Modulstruktur

```
filemetrics-core              → JVM- och custom metrics till fil, inga externa dependencies
filemetrics-prometheus        → Micrometer + Prometheus-format, fil och/eller server
filemetrics-spring            → Spring Boot autoconfiguration
filemetrics-autoinstrument    → automatisk instrumentering via reflection
```

## Java-kodstandard (filemetrics-core)

1. **`final` everywhere** — varje metodparameter och lokal variabel deklareras `final`.
2. **Inga externa dependencies** i `filemetrics-core` — bara `java.lang.*`, `java.util.*`, `java.io.*`, `java.lang.management.*`.
3. **Trådning** — daemon-trådar för filskrivning och cleanup. `close()` stänger båda.
4. **Felhantering** — appen påverkas aldrig av metrics-problem. Varning i stderr, aldrig exception mot anroparen. Fallback till noop om fil inte kan skapas.
5. **Filformat** — key-value, en rad per metric-grupp:
   ```
   2026-08-27T10:00:00Z app=order-service type=heap used_mb=312 committed_mb=400 max_mb=1024
   ```

## Paket

```
io.github.guranxpsandbox.filemetrics   ← publikt API
io.github.guranxpsandbox.filemetrics.internal ← ej publikt API
```

## Nuläge

- `MetricsLogger` interface — klart
- `NoOpMetricsLogger` — klart (default)
- `InMemoryMetricsLogger` — ej påbörjad
- `FileMetricsLogger` — ej påbörjad
- `Metrics` (fasad/entry point) — ej påbörjad

## Workflow

Se WORKFLOW.md.
