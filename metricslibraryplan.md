# filemetrics — Project Plan

## Bakgrund

Vi undersöker varför minnesanvändningen är hög (~90%) på en Linux-maskin som kör
ett antal Java- och C++-applikationer. För att förstå problemet behöver vi metrics
per Java-app. Målet är ett återanvändbart open source-bibliotek på GitHub.

---

## Projektbeslut

```
Namn:         filemetrics
GitHub:       github.com/guranxp-sandbox/filemetrics
Group id:     io.github.guranxp-sandbox
Java minimum: 8 (bumpa till 21 i v2 när målapparna uppgraderas)
Licens:       Apache 2.0
```

---

## API-stabilitetspolicy

Publika klasser och metoder i `filemetrics-core` är stabila från v1.0.
Inga breaking changes introduceras utan en ny major-version.
Interna klasser (paket `*.internal`) räknas inte som publikt API.

Konsekvens för användare: uppgradering från v1.x till v1.y kräver
aldrig kodändringar — bara ett uppdaterat versionsnummer i pom.xml.
Uppgradering till v2 (Java 21) är frivillig och kräver enbart
versionsbump — API:t är detsamma om det inte explicit annonseras som brutet.

---

## Modulstruktur

```
filemetrics-core              → JVM- och custom metrics till fil, inga externa dependencies
filemetrics-prometheus        → Micrometer + Prometheus-format, fil och/eller server
filemetrics-spring            → Spring Boot autoconfiguration
filemetrics-autoinstrument    → automatisk instrumentering via reflection/aspekter
```

### Beroenden mellan moduler

```
filemetrics-core          ← bas, inga externa dependencies
filemetrics-prometheus    → drar in filemetrics-core + micrometer-core + micrometer-registry-prometheus
filemetrics-spring        → drar in filemetrics-core + spring-boot-actuator
filemetrics-autoinstrument → drar in filemetrics-core + micrometer-core
```

---

## filemetrics-core

### Syfte
Samla JVM-metrics och custom metrics och skriva till fil. Inga externa dependencies — bara java.lang.management.

### Default metrics (alltid på)
```
Heap        → used, committed, max
Metaspace   → used, committed
Threads     → live, peak, deadlocked
GC          → count, time per collector
```

### Opt-in metrics
```
Direct memory   → used, count
Code cache      → used
Klassladdning   → loaded, unloaded
CPU             → process load, system load
```

### API

```java
// Minimal — en rad
Metrics.start("order-service");

// Med konfiguration
Metrics.builder()
    .appName("order-service")
    .logDir("/var/log/metrics")   // default: ./metrics
    .interval(60, TimeUnit.MINUTES) // default: 60 min
    .keepDays(7)                   // default: 7 dagar
    .withDirectMemory()            // opt-in
    .withClassLoading()            // opt-in
    .withCpu()                     // opt-in
    .start();

// Stoppa — symmetri med start
Metrics.stop();
```

### Implementationer

Styrs via system property `metrics.implementation`:

```
file      → FileMetricsLogger, skriver till fil
inmemory  → InMemoryMetricsLogger, håller i minne (för tester)
noop      → NoOpMetricsLogger, gör ingenting (DEFAULT)
```

```bash
# Aktivera filloggning
-Dmetrics.implementation=file

# Default — gör ingenting
java -jar app.jar
```

### ServiceLoader

Implementationen väljs via ServiceLoader:

```
src/main/resources/META-INF/services/io.github.guranxp-sandbox.filemetrics.MetricsLogger
→ innehåller alla tre implementationer
```

### Filformat

Key-value, en rad per metric-grupp:

```
2026-08-27T10:00:00Z app=order-service type=heap used_mb=312 committed_mb=400 max_mb=1024
2026-08-27T10:00:00Z app=order-service type=metaspace used_mb=128 committed_mb=132
2026-08-27T10:00:00Z app=order-service type=threads live=94 peak=120 deadlocked=0
2026-08-27T10:00:00Z app=order-service type=gc name="G1 Young" count=42 time_ms=1823
2026-08-27T10:00:00Z app=order-service type=gc name="G1 Old" count=3 time_ms=612
```

Opt-in:
```
2026-08-27T10:00:00Z app=order-service type=direct used_mb=45 count=1200
2026-08-27T10:00:00Z app=order-service type=classloading loaded=8432 unloaded=12
2026-08-27T10:00:00Z app=order-service type=cpu process_load=0.45 system_load=0.67
```

### Filhantering

```
Ny fil per dag:   order-service-2026-08-27.log
Rotation:         daglig, automatisk
Cleanup:          filer äldre än keepDays raderas automatiskt
Rättigheter:      sätts automatiskt vid skapande (600)
Fel:              varning i stderr, fallback till noop, appen påverkas aldrig
```

### Konfiguration via properties

```
metrics.implementation=file|inmemory|noop
metrics.log.dir=./metrics
metrics.interval=60
metrics.keep.days=7
metrics.opt.direct=false
metrics.opt.classloading=false
metrics.opt.cpu=false
metrics.opt.codecache=false
```

### Trådning

```
En daemon-tråd för filskrivning
En daemon-tråd för cleanup
Metrics.stop() stänger båda
```

### Säkerhet

```
Filrättigheter sätts automatiskt (600)
Appen startar alltid oavsett metrics-fel
Varning i stderr vid problem
```

### Testning

```
InMemoryMetricsLogger → unit/integrationstester, inspekterbar
NoOpMetricsLogger     → tester som inte bryr sig om metrics
Metrics.stop()        → städa upp trådar i teardown
```

---

## filemetrics-prometheus

### Syfte
Prometheus-format via Micrometer. Pluggar in i Micrometer som ett eget MeterRegistry.

### Lägen

```
Fil-läge    → Prometheus-format till fil, default
Server-läge → HTTP endpoint /metrics, kräver konfiguration
Båda        → fil + server simultaneously
```

### Fil-läge
Fungerar direkt utan konfiguration — default om port inte är satt.

### Server-läge
Startar bara om explicit konfigurerat:

```
metrics.prometheus.port=9090
metrics.prometheus.allowed.ips=192.168.1.100
```

Om inte konfigurerat → ingen server, appen påverkas inte.

### Konfiguration

```
metrics.prometheus.mode=file|server|both
metrics.prometheus.port=9090
metrics.prometheus.allowed.ips=127.0.0.1
metrics.prometheus.file.enabled=true
metrics.prometheus.file.dir=/var/log/metrics
```

### Webbserver utan extra dependency

```java
// com.sun.net.httpserver — inbyggt i JDK
HttpServer server = HttpServer.create(new InetSocketAddress(9090), 0);
```

### Filformat

Prometheus-format med timestamp per metric:

```
jvm_memory_used_bytes{area="heap"} 327155712 1724580000000
jvm_threads_live_threads 94 1724580000000
```

---

## filemetrics-spring

### Syfte
Zero-config integration med Spring Boot via autoconfiguration.

### Autoconfiguration

```
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
→ io.github.guranxp-sandbox.filemetrics.spring.MetricsAutoConfiguration
```

### Webbserver-detektering

```
Webbserver finns    → Actuator + befintlig server (Tomcat/Undertow/Jetty)
Ingen webbserver    → com.sun.net.httpserver
Ingen server alls   → fil-läge
```

Användaren kan även explicit välja:
```
metrics.server.type=actuator|lightweight
```

### Lägen

```
metrics.mode=file|server|both
```

### Konfiguration via application.properties

```
metrics.enabled=true
metrics.app.name=order-service
metrics.mode=file
metrics.file.dir=/var/log/metrics
metrics.file.keep-days=7
management.endpoints.web.exposure.include=prometheus
```

### Spring-profiler

```
# application-local.properties
metrics.enabled=true
metrics.implementation=file

# application-prod.properties
metrics.enabled=false
```

---

## filemetrics-autoinstrument

### Syfte
Automatisk instrumentering av kända bibliotek via reflection och Micrometer.

### Aktivering

```java
Metrics.start("order-service").autoInstrument();
```

### Vad som instrumenteras automatiskt

```
HikariCP        → connection pool metrics
ExecutorService → trådpool metrics
RestTemplate    → utgående HTTP metrics
WebClient       → utgående HTTP metrics
OkHttpClient    → utgående HTTP metrics
JDBC            → databasanrop
```

### Hur

Via reflection — kollar om klassen finns i classpath:

```java
if (isPresent("com.zaxxer.hikari.HikariDataSource")) {
    // instrumentera HikariCP automatiskt
}
```

### Manuell instrumentering av trådpooler

```java
ExecutorService pool = Executors.newFixedThreadPool(10);
ExecutorServiceMetrics.monitor(registry, pool, "zeromq-pool");
```

---

## Generella principer

```
Appen påverkas aldrig av metrics-problem
Varning i stderr vid fel, aldrig exception mot appen
Default är noop — måste explicit aktiveras
start() / stop() symmetri
Minimal minnesåtgång — core har inga externa dependencies
Fallback till noop om fil inte kan skapas
```

---

## Nästa steg

1. Sätt upp GitHub repo (guranxp-sandbox/filemetrics) med Maven multi-module struktur
2. Börja med filemetrics-core
3. Implementera FileMetricsLogger
4. Implementera InMemoryMetricsLogger och NoOpMetricsLogger
5. Lägg till tester
6. Bygg filemetrics-prometheus
7. Bygg filemetrics-spring
8. Bygg filemetrics-autoinstrument
9. Dokumentation och README
