# OpenTelemetry Java SDK Study & Testing Guide

A hands-on, pure Java example project designed to help you study, experiment with, and analyze the inner workings of **OpenTelemetry (OTel)** without any complex framework overhead or black-box agents.

---

## 🌟 What is OpenTelemetry?

OpenTelemetry (OTel) is a vendor-neutral, open-source observability framework providing APIs, SDKs, and tools to generate, collect, and export telemetry data (**Traces**, **Metrics**, and **Logs**).

### Core Architectural Principle: API vs. SDK Separation
* **`opentelemetry-api`**: Used in business/application code to create spans, record metrics, and log events. It provides interfaces and no-op defaults. It has zero heavy dependencies.
* **`opentelemetry-sdk`**: The implementation engine. Configures sampling, batching, threading, and exporters. Configured once at application startup.

```
┌─────────────────────────────────────────────────────────────┐
│                      Application Code                       │
│       (Calls OpenTelemetry API: Tracer, Meter, Logger)      │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                    OpenTelemetry SDK                        │
│ ┌───────────────────┐ ┌───────────────────┐ ┌─────────────┐ │
│ │  TracerProvider   │ │   MeterProvider   │ │LoggerProvider││
│ └─────────┬─────────┘ └─────────┬─────────┘ └──────┬──────┘ │
│           │                     │                  │        │
│    [SpanProcessors]      [MetricReaders]     [LogProcessors]│
│           │                     │                  │        │
│    [SpanExporters]      [MetricExporters]    [LogExporters] │
└───────────┼─────────────────────┼──────────────────┼────────┘
            ▼                     ▼                  ▼
     Pretty Console / OTLP / Collector / Jaeger / Prometheus
```

---

## 📂 Project Structure

```
opentelemetry-test/
├── pom.xml                                      # OpenTelemetry BOM & dependencies
├── docker-compose.yml                           # OTel Collector + Prometheus + Grafana stack
├── otel-collector-config.yaml                   # Collector pipeline configuration
├── jmx-scraper-config.yaml                      # OpenTelemetry JMX Scraper mapping rules
├── run-agent-demo.sh                            # Java Agent runner script
├── run-jmx-demo.sh                              # JMX MBean Server application runner
├── run-jmx-scraper.sh                           # OpenTelemetry JMX Scraper runner
├── README.md                                    # This study guide
└── src/
    ├── main/java/com/example/otel/
    │   ├── Main.java                            # Pure Java SDK runner
    │   ├── config/
    │   │   └── OpenTelemetryConfig.java         # Programmatic pure Java SDK setup
    │   ├── demos/                               # Pure Java OpenTelemetry SDK Demos
    │   │   ├── BasicTracingDemo.java            # Spans, parent-child, attributes, events, errors
    │   │   ├── MetricsDemo.java                 # Counters, UpDownCounters, Histograms, Gauges
    │   │   ├── ContextPropagationDemo.java      # W3C TraceContext & Baggage across boundaries
    │   │   ├── LogsCorrelationDemo.java         # LogRecords with Trace/Span ID correlation
    │   │   └── AsyncTracingDemo.java            # Multithreading & CompletableFuture context
    │   ├── agentdemos/                          # OpenTelemetry Java Agent Demos (-javaagent)
    │   │   ├── AgentMain.java                   # Java Agent runner
    │   │   ├── AgentTracingDemo.java            # @WithSpan & @SpanAttribute annotations
    │   │   ├── AgentAsyncDemo.java              # Auto context propagation across async threads
    │   │   ├── AgentContextPropagationDemo.java # Automatic HTTP client/server W3C injection
    │   │   └── AgentMetricsDemo.java            # Automatic JVM/System metrics & GlobalOpenTelemetry
    │   ├── jmxdemos/                            # JMX MBean & Remote Scraping Demos
    │   │   ├── JmxMain.java                     # JMX demo runner & live attribute monitor
    │   │   ├── AppMetricsMBean.java             # Standard MBean interface
    │   │   ├── AppMetrics.java                  # Standard MBean implementation
    │   │   ├── ConnectionPoolMXBean.java        # MXBean interface (Open MBean types)
    │   │   ├── ConnectionPool.java              # Connection pool MXBean implementation
    │   │   ├── JmxServerManager.java            # MBeanServer registration & JMX Remote RMI (port 9999)
    │   │   └── JmxTrafficSimulator.java         # Real-time background traffic generator
    │   └── util/
    │       ├── ConsoleFormatter.java            # Terminal styling & formatting utilities
    │       ├── PrettySpanExporter.java          # Custom exporter for clear span visualization
    │       ├── PrettyMetricExporter.java        # Custom exporter for metric batches
    │       └── PrettyLogRecordExporter.java     # Custom exporter for log correlation
    └── test/java/com/example/otel/
        ├── OpenTelemetryTest.java               # Unit tests for OpenTelemetry SDK
        └── JmxMBeanTest.java                    # Unit tests for JMX MBean / MBeanServer
```

---

## 🚀 How to Run and Test

### 1. Run all scenarios (Console output):
```bash
mvn compile exec:java
```

### 2. Full Observability Stack (OTel Collector + Grafana + Prometheus + Tempo + Loki):
Start the entire local observability platform with Docker Compose:

```bash
# 1. Start OTel Collector, Grafana, Tempo, Prometheus, and Loki
docker compose up -d

# 2. Run the Java application sending data via OTLP (port 4318)
mvn compile exec:java -Dotel.exporter.otlp.endpoint=http://localhost:4318

# 3. Open Grafana in your browser (No login required)
# URL: http://localhost:3000
```

#### Where to Find Data in Grafana (`http://localhost:3000`):
* **Explore Traces (Tempo)**: Go to **Explore** -> Select datasource **Tempo** -> Search for service `opentelemetry-study-app` to view waterfall trace graphs.
* **Explore Metrics (Prometheus)**: Go to **Explore** -> Select datasource **Prometheus** -> Query metrics like `orders_processed_total` or `active_shopping_carts` or `checkout_duration_milliseconds_count`.
* **Explore Logs (Loki)**: Go to **Explore** -> Select datasource **Loki** -> Query `{service_name="opentelemetry-study-app"}` to inspect structured logs correlated to traces.

#### 4. Stop the Observability Stack:
```bash
docker compose down
```
Pass the scenario number as an argument:
```bash
mvn compile exec:java -Dexec.args="1"   # Scenario 1: Tracing Fundamentals
mvn compile exec:java -Dexec.args="2"   # Scenario 2: Metrics Instruments
mvn compile exec:java -Dexec.args="3"   # Scenario 3: W3C Context Propagation
mvn compile exec:java -Dexec.args="4"   # Scenario 4: Logs & Trace Correlation
mvn compile exec:java -Dexec.args="5"   # Scenario 5: Async & Multithreading
```

### 3. Run OpenTelemetry Java Agent Demo:
```bash
./run-agent-demo.sh
```

### 4. Run JMX MBeans Demo & OpenTelemetry Collector Scraping:
```bash
# Terminal 1: Launch Java application exposing MBeans on JMX port 9999
./run-jmx-demo.sh

# Terminal 2: Launch OpenTelemetry JMX Scraper (scrapes JMX :9999 -> sends OTLP to Collector :4317)
./run-jmx-scraper.sh

# Terminal 3 (or browser): Inspect metrics received by Collector & Prometheus
curl -s http://localhost:8889/metrics | grep jmx_
curl -s "http://localhost:9090/api/v1/query?query=jmx_orders_processed_total"
```

### 5. Run automated unit tests:
```bash
mvn test
```

---

## 🔍 Core Concepts Breakdown

### 1. Tracing Fundamentals (`BasicTracingDemo.java`)
* **Trace**: A directed acyclic graph (DAG) of Spans representing a complete end-to-end request.
* **Span**: A single unit of work (e.g., HTTP request, database query, cache lookup).
* **Parent-Child Relationship**: When `span.makeCurrent()` is active inside a `try (Scope scope = ...)`, any child span automatically inherits the same `TraceId` and sets its `ParentSpanId` to the active span.
* **Attributes**: Key-value metadata describing the span (e.g., `order.id = "ORDER-101"`).
* **Events**: Timestamped milestones within a span (e.g., `"InventoryReserved"`).
* **Exceptions**: Recorded via `span.recordException(e)` and `span.setStatus(StatusCode.ERROR)`.

```java
Span rootSpan = tracer.spanBuilder("checkout-order").startSpan();
try (Scope scope = rootSpan.makeCurrent()) {
    rootSpan.setAttribute("order.id", "ORDER-101");
    
    // Child span inherits parent context automatically
    Span childSpan = tracer.spanBuilder("validate-inventory").startSpan();
    try (Scope childScope = childSpan.makeCurrent()) {
        // work
    } finally {
        childSpan.end();
    }
} finally {
    rootSpan.end(); // Always end in finally!
}
```

---

### 2. Metrics Instruments (`MetricsDemo.java`)
OpenTelemetry defines specific instrument types for different telemetry needs:

| Instrument | Behavior | Example Use Case |
|---|---|---|
| **`LongCounter`** | Monotonically increasing only (+N) | Total orders processed, total HTTP requests |
| **`LongUpDownCounter`** | Can increase (+N) and decrease (-N) | Active shopping carts, active DB connections |
| **`DoubleHistogram`** | Aggregates into statistical distribution buckets | Request latency (ms), payload sizes (bytes) |
| **`ObservableGauge`** | Callback sampled asynchronously upon collection | JVM heap memory usage, CPU percentage |

```java
// Synchronous Counter
LongCounter orderCounter = meter.counterBuilder("orders.total").build();
orderCounter.add(1, Attributes.of(AttributeKey.stringKey("status"), "success"));

// Histogram (Latency / Duration)
DoubleHistogram latency = meter.histogramBuilder("http.duration").setUnit("ms").build();
latency.record(45.5, Attributes.of(AttributeKey.stringKey("route"), "/api/checkout"));
```

---

### 3. Context Propagation & W3C TraceContext (`ContextPropagationDemo.java`)
When a request hops across networks or threads, context must be serialized into wire protocols:

* **W3C `traceparent` Header Format**:
  ```
  00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01
  ┬─ ──────────────────────────────┬─ ────────────────┬─ ─┬
  │                                │                  │   └─ Trace Flags (01 = sampled)
  │                                │                  └─ Span ID (16 hex chars / 8 bytes)
  │                                └─ Trace ID (32 hex chars / 16 bytes)
  └─ Version (00)
  ```
* **Baggage**: Key-value pairs that propagate across all downstream microservices (unlike Span Attributes which stay local to the single span).

```java
// Injecting context into HTTP headers on Client side
openTelemetry.getPropagators().getTextMapPropagator()
    .inject(Context.current(), httpHeaders, (carrier, key, val) -> carrier.put(key, val));

// Extracting context on Server side
Context extracted = openTelemetry.getPropagators().getTextMapPropagator()
    .extract(Context.current(), httpHeaders, getter);
Span serverSpan = tracer.spanBuilder("handle-request").setParent(extracted).startSpan();
```

---

### 4. Logs & Trace Correlation (`LogsCorrelationDemo.java`)
OpenTelemetry correlates structured logs with traces:
* If a log is emitted within an active `Span`'s `Scope`, OpenTelemetry automatically injects the active `TraceId` and `SpanId` into the log record.
* This allows APM dashboards (Datadog, Grafana Tempo/Loki, Dynatrace, New Relic) to link every log line directly to its exact trace span.

---

### 5. Asynchronous & Multithreaded Tracing (`AsyncTracingDemo.java`)
* Java's OpenTelemetry default context relies on `ThreadLocal`.
* When passing tasks to thread pools (`ExecutorService`, `CompletableFuture`), the context must be explicitly wrapped via `Context.current().wrap(runnable)` so child spans retain parentage.

```java
Context currentContext = Context.current();
CompletableFuture.runAsync(
    currentContext.wrap(() -> {
        // Child span inside worker thread sees parentSpan!
        Span asyncSpan = tracer.spanBuilder("async-worker").startSpan();
        try (Scope scope = asyncSpan.makeCurrent()) {
            // task logic
        } finally {
            asyncSpan.end();
        }
    }),
    threadPool
);
```

---

### 6. Automated Unit Testing with `InMemorySpanExporter` (`OpenTelemetryTest.java`)
You can test telemetry behavior deterministically in unit tests without setting up external collectors:

```java
InMemorySpanExporter spanExporter = InMemorySpanExporter.create();
SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
    .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
    .build();

// Verify captured spans
List<SpanData> spans = spanExporter.getFinishedSpanItems();
assertThat(spans).hasSize(1);
assertThat(spans.get(0).getName()).isEqualTo("checkout-order");
```

---

### 7. JMX MBeans & OpenTelemetry Collector Scraping (`jmxdemos/`)

Java Management Extensions (JMX) is Java's built-in standard for application management and monitoring:
* **Standard MBean (`AppMetricsMBean.java`)**: Exposes strongly typed getters/setters and operations (`processOrder`, `resetStatistics`).
* **MXBean (`ConnectionPoolMXBean.java`)**: An Open MBean that maps custom data types into universal Open Type equivalents without requiring clients to have application domain classes.
* **Platform MBeanServer (`ManagementFactory.getPlatformMBeanServer()`)**: The central registry where MBeans are registered under unique `ObjectName` identifiers (`com.example.otel:type=AppMetrics,name=OrderService`).
* **JMX Remote RMI Connector**: Enables remote clients (like the OpenTelemetry JMX Scraper) to connect via `service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi`.

#### How OpenTelemetry Scrapes JMX MBeans:
```
┌───────────────────────────┐         ┌───────────────────────────────┐
│     Java Application      │         │   OpenTelemetry JMX Scraper   │
│  ┌─────────────────────┐  │         │  (opentelemetry-jmx-scraper)  │
│  │ Platform MBeanServer│  │  JMX    │                               │
│  │  • AppMetrics       │◄─┼─────────┼─► Reads MBeans via RMI        │
│  │  • ConnectionPool   │  │  (:9999)│   Maps via scraper YAML config│
│  └─────────────────────┘  │         │   Generates OTel Metric Types │
└───────────────────────────┘         └──────────────┬────────────────┘
                                                     │ OTLP gRPC (:4317)
                                                     ▼
┌───────────────────────────┐         ┌───────────────────────────────┐
│     Grafana Dashboards    │         │    OpenTelemetry Collector    │
│  • jmx_orders_processed   │◄────────┼── Exposes Prometheus (:8889)  │
│  • jmx_connections_active │ (pull)  │   Forward to Prometheus (:9090│
└───────────────────────────┘         └───────────────────────────────┘
```

#### Declarative JMX Mapping Configuration (`jmx-scraper-config.yaml`):
```yaml
rules:
  - bean: "com.example.otel:type=AppMetrics,name=*"
    mapping:
      ProcessedOrdersCount:
        metric: jmx.orders.processed.total
        desc: "Total number of completed orders exposed via MBean"
        type: counter
        unit: "1"
      ActiveConnections:
        metric: jmx.connections.active
        desc: "Active client connections exposed via MBean"
        type: updowncounter
        unit: "1"
      PendingQueueDepth:
        metric: jmx.queue.depth
        desc: "Pending work queue depth exposed via MBean"
        type: gauge
        unit: "1"
      AverageResponseTimeMs:
        metric: jmx.orders.latency
        desc: "Moving average response latency in milliseconds"
        type: gauge
        unit: "ms"
```

#### Metrics Available in Prometheus & Grafana:
* `jmx_orders_processed_total`: Cumulative counter of processed orders
* `jmx_connections_active`: Live in-flight client connections (gauge)
* `jmx_queue_depth_ratio`: Current backlog work queue depth (gauge)
* `jmx_orders_latency_milliseconds`: Running average latency (gauge)
* `jmx_db_pool_active`: Active database connections in use (gauge)
* `jmx_db_pool_acquired_total`: Cumulative database connections acquired (counter)

---

## 📚 Summary of Key OpenTelemetry Terms

| Concept | Description |
|---|---|
| **TracerProvider / MeterProvider / LoggerProvider** | Factory singletons for creating `Tracer`, `Meter`, and `Logger` instances. |
| **SpanProcessor** | Intercepts span lifecycle (`onStart`, `onEnd`). Examples: `SimpleSpanProcessor` (sync), `BatchSpanProcessor` (async). |
| **Exporter** | Sends telemetry data to destinations (Console, OTLP gRPC/HTTP, Jaeger, Prometheus). |
| **Resource** | Immutable metadata describing the entity emitting telemetry (e.g. `service.name`, `service.version`, `environment`). |
| **Context** | An immutable container for passing in-process telemetry state across execution units. |
| **Scope** | A closeable handle controlling the lifetime of active context in the current thread (`try-with-resources`). |
