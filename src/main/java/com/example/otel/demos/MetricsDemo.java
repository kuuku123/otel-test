package com.example.otel.demos;

import com.example.otel.config.OpenTelemetryConfig;
import com.example.otel.util.ConsoleFormatter;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.Meter;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Demonstrates OpenTelemetry Metric Instruments:
 *
 * 1. Synchronous Instruments:
 *    - Counter (monotonically increasing): `orders.processed.total`
 *    - UpDownCounter (increments/decrements): `active.shopping_carts`
 *    - Histogram (distribution / latency buckets): `checkout.latency`
 *
 * 2. Asynchronous (Observable) Instruments:
 *    - Observable Gauge (callback-based observation): `system.memory.used_mb`
 *
 * 3. Dimensional Attributes (Labels):
 *    - Slicing metrics by `status`, `payment_method`, `tier`, etc.
 */
public class MetricsDemo {

    private final Meter meter;
    private final OpenTelemetryConfig config;

    public MetricsDemo(OpenTelemetryConfig config) {
        this.config = config;
        this.meter = config.getMeter("com.example.otel.metrics");
    }

    public void run() {
        ConsoleFormatter.printHeader("DEMO 2: Metrics Fundamentals (Counters, UpDownCounters, Histograms, Gauges)");

        ConsoleFormatter.printSubHeader("1. Counter: Tracking Total Completed Orders");
        ConsoleFormatter.printExplanation(
                "A Counter is monotonically increasing. Notice how recordings with different attributes\n" +
                "  create distinct metric time series (e.g. tier=VIP vs tier=Standard)."
        );

        LongCounter orderCounter = meter.counterBuilder("orders.processed.total")
                .setDescription("Total number of processed orders")
                .setUnit("orders")
                .build();

        // Record metrics with dimensional attributes
        orderCounter.add(1, Attributes.of(AttributeKey.stringKey("tier"), "VIP", AttributeKey.stringKey("status"), "success"));
        orderCounter.add(1, Attributes.of(AttributeKey.stringKey("tier"), "VIP", AttributeKey.stringKey("status"), "success"));
        orderCounter.add(1, Attributes.of(AttributeKey.stringKey("tier"), "Standard", AttributeKey.stringKey("status"), "success"));
        orderCounter.add(1, Attributes.of(AttributeKey.stringKey("tier"), "Standard", AttributeKey.stringKey("status"), "failed"));

        ConsoleFormatter.printSubHeader("2. UpDownCounter: Tracking Active In-Flight Connections / Carts");
        ConsoleFormatter.printExplanation(
                "An UpDownCounter can both increase (+1) and decrease (-1) as state changes over time."
        );

        LongUpDownCounter activeCartsCounter = meter.upDownCounterBuilder("active.shopping_carts")
                .setDescription("Number of currently active shopping carts")
                .setUnit("carts")
                .build();

        // 3 users start carts
        activeCartsCounter.add(1, Attributes.of(AttributeKey.stringKey("region"), "US"));
        activeCartsCounter.add(1, Attributes.of(AttributeKey.stringKey("region"), "US"));
        activeCartsCounter.add(1, Attributes.of(AttributeKey.stringKey("region"), "EU"));
        // 1 US user completes checkout (cart leaves)
        activeCartsCounter.add(-1, Attributes.of(AttributeKey.stringKey("region"), "US"));

        ConsoleFormatter.printSubHeader("3. Histogram: Tracking Latency & Request Distributions");
        ConsoleFormatter.printExplanation(
                "A Histogram measures distribution of values across bucket boundaries.\n" +
                "  Notice how count, sum, min, max, and bucket counts are automatically aggregated."
        );

        DoubleHistogram checkoutDuration = meter.histogramBuilder("checkout.duration")
                .setDescription("Duration of checkout processing in milliseconds")
                .setUnit("ms")
                .build();

        // Record a variety of execution latencies
        checkoutDuration.record(45.5, Attributes.of(AttributeKey.stringKey("payment_method"), "CreditCard"));
        checkoutDuration.record(120.0, Attributes.of(AttributeKey.stringKey("payment_method"), "CreditCard"));
        checkoutDuration.record(85.3, Attributes.of(AttributeKey.stringKey("payment_method"), "PayPal"));
        checkoutDuration.record(350.2, Attributes.of(AttributeKey.stringKey("payment_method"), "Crypto"));
        checkoutDuration.record(25.0, Attributes.of(AttributeKey.stringKey("payment_method"), "CreditCard"));

        ConsoleFormatter.printSubHeader("4. Asynchronous Observable Gauges: System & JVM Monitoring");
        ConsoleFormatter.printExplanation(
                "Observable Gauges use callbacks to sample current values on-demand only when metrics are scraped.\n" +
                "  Unlike counters/histograms which are pushed actively, gauges are polled asynchronously by the SDK."
        );

        // 4.1 Observable Long Gauge: Simulated and Real JVM Memory
        AtomicLong simulatedMemoryUsage = new AtomicLong(512); // in MB
        meter.gaugeBuilder("system.memory.used_mb")
                .setDescription("Simulated memory usage across pools")
                .setUnit("MiB")
                .ofLongs()
                .buildWithCallback(measurement -> {
                    measurement.record(simulatedMemoryUsage.get(), Attributes.of(AttributeKey.stringKey("pool"), "heap"));
                    measurement.record(128, Attributes.of(AttributeKey.stringKey("pool"), "non-heap"));
                });

        // 4.2 Observable Long Gauge: Active JVM Threads
        meter.gaugeBuilder("jvm.threads.active")
                .setDescription("Current number of live JVM threads")
                .setUnit("threads")
                .ofLongs()
                .buildWithCallback(measurement -> {
                    int threadCount = java.lang.management.ManagementFactory.getThreadMXBean().getThreadCount();
                    measurement.record(threadCount, Attributes.of(AttributeKey.stringKey("daemon"), "all"));
                });

        // 4.3 Observable Double Gauge: CPU Utilization (0.0 to 1.0)
        java.util.concurrent.atomic.AtomicReference<Double> cpuLoad = new java.util.concurrent.atomic.AtomicReference<>(0.45);
        meter.gaugeBuilder("system.cpu.utilization")
                .setDescription("Current system CPU utilization ratio (0.0 to 1.0)")
                .setUnit("1")
                .buildWithCallback(measurement -> {
                    measurement.record(cpuLoad.get(), Attributes.of(AttributeKey.stringKey("core"), "all"));
                });

        // Simulate state changes
        simulatedMemoryUsage.set(768);
        cpuLoad.set(0.68);

        // Force flush so we can see the batch printed out immediately in our PrettyMetricExporter
        ConsoleFormatter.printExplanation("Flushing metric reader to display exported metrics snapshot...");
        config.flushMetrics();
    }

    /**
     * Continuously generates fluctuating live traffic (orders, shopping carts, latencies, gauges)
     * so you can watch live dynamic time-series charts and trends in Grafana.
     */
    public void simulateContinuousTraffic(int cycles, long intervalMillis) {
        ConsoleFormatter.printHeader("LIVE TIME-SERIES TRAFFIC SIMULATOR (Generating dynamic data)");
        ConsoleFormatter.printExplanation(
                "Simulating live traffic across " + cycles + " intervals (" + intervalMillis + "ms delay).\n" +
                "Open Grafana (http://localhost:3000/explore) and set Auto-Refresh to '5s' to watch trends live!"
        );

        LongCounter orderCounter = meter.counterBuilder("orders.processed.total")
                .setDescription("Total number of processed orders")
                .setUnit("orders")
                .build();

        LongUpDownCounter activeCarts = meter.upDownCounterBuilder("active.shopping_carts")
                .setDescription("Number of currently active shopping carts")
                .setUnit("carts")
                .build();

        DoubleHistogram checkoutDuration = meter.histogramBuilder("checkout.duration")
                .setDescription("Duration of checkout processing in milliseconds")
                .setUnit("ms")
                .build();

        java.util.Random random = new java.util.Random();
        String[] paymentMethods = {"CreditCard", "PayPal", "ApplePay", "Crypto"};
        String[] tiers = {"VIP", "Standard", "Guest"};

        for (int i = 1; i <= cycles; i++) {
            // 1. Simulate burst of orders with random tiers & outcomes
            int batchSize = 1 + random.nextInt(5);
            for (int j = 0; j < batchSize; j++) {
                String tier = tiers[random.nextInt(tiers.length)];
                String status = random.nextDouble() > 0.1 ? "success" : "failed";
                orderCounter.add(1, Attributes.of(AttributeKey.stringKey("tier"), tier, AttributeKey.stringKey("status"), status));

                // Latency follows realistic distribution (50ms base + spike)
                double latency = 30.0 + random.nextGaussian() * 20.0 + (random.nextDouble() > 0.85 ? 200.0 : 0.0);
                latency = Math.max(10.0, latency);
                String method = paymentMethods[random.nextInt(paymentMethods.length)];
                checkoutDuration.record(latency, Attributes.of(AttributeKey.stringKey("payment_method"), method));
            }

            // 2. Fluctuating active carts (+/-)
            int cartDelta = random.nextInt(5) - 2; // -2 to +2
            activeCarts.add(cartDelta, Attributes.of(AttributeKey.stringKey("region"), random.nextBoolean() ? "US" : "EU"));

            System.out.printf("\r[Interval %d/%d] Emitted %d orders (Total so far: %d events)...", i, cycles, batchSize, i * batchSize);

            try {
                Thread.sleep(intervalMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println("\n");
        config.flushMetrics();
        ConsoleFormatter.printInfo("Traffic Simulation Completed", "Check Grafana time-series graphs now!");
    }
}
