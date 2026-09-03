package com.example.otel.agentdemos;

import com.example.otel.util.ConsoleFormatter;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;

/**
 * Demonstrates Metrics Capabilities with the OpenTelemetry Java Agent.
 *
 * COMPARISON WITH MetricsDemo:
 * -----------------------------------------------------------------------------------------
 * Manual SDK (MetricsDemo)                  | OTel Java Agent (AgentMetricsDemo)
 * -----------------------------------------------------------------------------------------
 * - Manually build JVM memory/thread gauges | Agent automatically collects all JVM & System metrics
 * - Manually instrument HTTP client latency | Agent automatically records http.client.duration
 * - Manual meter initialization & wiring   | Agent exposes GlobalOpenTelemetry.get().getMeter()
 * - Heavy manual maintenance               | Infrastructure & runtime metrics are 100% automated
 * -----------------------------------------------------------------------------------------
 */
public class AgentMetricsDemo {

    public void run() {
        ConsoleFormatter.printHeader("AGENT DEMO 4: Automated & Custom Metrics with OTel Java Agent");

        ConsoleFormatter.printSubHeader("1. Automatic Out-of-the-Box Metrics Collected by Agent");
        ConsoleFormatter.printExplanation(
                "When running with -javaagent:opentelemetry-javaagent.jar, the following are\n" +
                "  collected and exported automatically WITHOUT A SINGLE LINE OF CODE:\n" +
                "  • JVM Memory:  jvm.memory.used, jvm.memory.committed, jvm.memory.limit (heap & non-heap)\n" +
                "  • Garbage Col: jvm.gc.duration (pause times per generation)\n" +
                "  • Threading:   jvm.thread.count (daemon, live, peak)\n" +
                "  • System/CPU:  system.cpu.utilization, process.cpu.utilization\n" +
                "  • HTTP/Netty:  http.client.request.duration, http.server.request.duration"
        );

        ConsoleFormatter.printSubHeader("2. Application Business Metrics via GlobalOpenTelemetry");
        ConsoleFormatter.printExplanation(
                "When you DO want custom business metrics (e.g. total revenue, items purchased),\n" +
                "  the agent initializes GlobalOpenTelemetry automatically.\n" +
                "  You can simply obtain a Meter from GlobalOpenTelemetry without configuring SDK exporters:"
        );

        Meter meter = GlobalOpenTelemetry.getMeter("com.example.otel.agent.business");
        LongCounter orderCounter = meter.counterBuilder("agent.orders.count")
                .setDescription("Custom business counter with Agent")
                .setUnit("orders")
                .build();

        DoubleHistogram orderAmountHistogram = meter.histogramBuilder("agent.orders.amount")
                .setDescription("Distribution of order amounts")
                .setUnit("USD")
                .build();

        // Simulate recording custom metrics
        for (int i = 1; i <= 3; i++) {
            double amount = 45.0 * i;
            System.out.printf("  [Business Metric] Recording order #%d ($%.2f)\n", i, amount);
            orderCounter.add(1, Attributes.of(AttributeKey.stringKey("tier"), "Premium"));
            orderAmountHistogram.record(amount, Attributes.of(AttributeKey.stringKey("tier"), "Premium"));
            processBusinessMethod("ORD-METRIC-" + i);
        }

        ConsoleFormatter.printSubHeader("3. Metrics Generated from @WithSpan Methods");
        ConsoleFormatter.printExplanation(
                "In Prometheus and OpenTelemetry Collector (via the 'spanmetrics' connector),\n" +
                "  every @WithSpan method automatically yields:\n" +
                "  - calls_total (Count of method invocations by status)\n" +
                "  - duration_milliseconds (Latency histogram per method)\n" +
                "This means you get full RED metrics (Rate, Errors, Duration) simply by adding @WithSpan!"
        );
    }

    @WithSpan("agent.monitored-operation")
    public void processBusinessMethod(@SpanAttribute("order.id") String orderId) {
        try {
            Thread.sleep(20);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
