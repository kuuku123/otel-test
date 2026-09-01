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

        ConsoleFormatter.printSubHeader("4. Asynchronous Observable Gauge: Memory Usage");
        ConsoleFormatter.printExplanation(
                "An Observable Gauge uses a callback to sample the current value only when metrics are collected."
        );

        AtomicLong simulatedMemoryUsage = new AtomicLong(512); // in MB
        meter.gaugeBuilder("system.memory.used_mb")
                .setDescription("Current simulated JVM memory usage")
                .setUnit("MiB")
                .ofLongs()
                .buildWithCallback(observableLongMeasurement -> {
                    observableLongMeasurement.record(simulatedMemoryUsage.get(), Attributes.of(AttributeKey.stringKey("pool"), "heap"));
                });

        // Simulate state change
        simulatedMemoryUsage.set(768);

        // Force flush so we can see the batch printed out immediately in our PrettyMetricExporter
        ConsoleFormatter.printExplanation("Flushing metric reader to display exported metrics snapshot...");
        config.flushMetrics();
    }
}
