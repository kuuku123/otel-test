package com.example.otel.config;

import com.example.otel.util.PrettyLogRecordExporter;
import com.example.otel.util.PrettyMetricExporter;
import com.example.otel.util.PrettySpanExporter;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;

import java.time.Duration;

/**
 * Pure Java OpenTelemetry SDK configuration.
 *
 * This class configures the 3 core pillars (Traces, Metrics, Logs) programmatically:
 * 1. Resource (Service Identity & Metadata)
 * 2. TracerProvider (for Spans & Tracing)
 * 3. MeterProvider (for Counters, Gauges, Histograms)
 * 4. LoggerProvider (for LogRecords and Context Correlation)
 * 5. ContextPropagators (W3C standard TraceContext & Baggage)
 */
public class OpenTelemetryConfig implements AutoCloseable {

    private final OpenTelemetrySdk openTelemetrySdk;
    private final SdkTracerProvider tracerProvider;
    private final SdkMeterProvider meterProvider;
    private final SdkLoggerProvider loggerProvider;
    private final PeriodicMetricReader metricReader;

    public OpenTelemetryConfig() {
        // Step 1: Define the Resource (Identifies the service emitting telemetry)
        Resource serviceResource = Resource.getDefault().merge(
                Resource.create(
                        Attributes.of(
                                AttributeKey.stringKey("service.name"), "opentelemetry-study-app",
                                AttributeKey.stringKey("service.version"), "1.0.0",
                                AttributeKey.stringKey("deployment.environment"), "development"
                        )
                )
        );

        // Step 2: Configure TracerProvider with a SpanProcessor and Exporter
        // SimpleSpanProcessor exports spans synchronously on span.end() (ideal for testing & debugging)
        this.tracerProvider = SdkTracerProvider.builder()
                .setResource(serviceResource)
                .addSpanProcessor(SimpleSpanProcessor.create(new PrettySpanExporter()))
                .build();

        // Step 3: Configure MeterProvider with PeriodicMetricReader and MetricExporter
        this.metricReader = PeriodicMetricReader.builder(new PrettyMetricExporter())
                .setInterval(Duration.ofMillis(500)) // Periodic flush interval
                .build();

        this.meterProvider = SdkMeterProvider.builder()
                .setResource(serviceResource)
                .registerMetricReader(metricReader)
                .build();

        // Step 4: Configure LoggerProvider with LogRecordProcessor and Exporter
        this.loggerProvider = SdkLoggerProvider.builder()
                .setResource(serviceResource)
                .addLogRecordProcessor(SimpleLogRecordProcessor.create(new PrettyLogRecordExporter()))
                .build();

        // Step 5: Configure Context Propagators (W3C TraceContext + Baggage)
        ContextPropagators contextPropagators = ContextPropagators.create(
                TextMapPropagator.composite(
                        W3CTraceContextPropagator.getInstance(),
                        W3CBaggagePropagator.getInstance()
                )
        );

        // Step 6: Assemble the complete OpenTelemetrySdk instance
        this.openTelemetrySdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setMeterProvider(meterProvider)
                .setLoggerProvider(loggerProvider)
                .setPropagators(contextPropagators)
                .build();
    }

    public OpenTelemetry getOpenTelemetry() {
        return openTelemetrySdk;
    }

    public Tracer getTracer(String instrumentationScopeName) {
        return openTelemetrySdk.getTracer(instrumentationScopeName);
    }

    public Meter getMeter(String instrumentationScopeName) {
        return openTelemetrySdk.getMeter(instrumentationScopeName);
    }

    public Logger getLogger(String instrumentationScopeName) {
        return openTelemetrySdk.getLogsBridge().get(instrumentationScopeName);
    }

    /**
     * Manually triggers a metric collection flush so metrics are printed immediately.
     */
    public void flushMetrics() {
        metricReader.forceFlush().join(5, java.util.concurrent.TimeUnit.SECONDS);
    }

    @Override
    public void close() {
        // Clean shutdown flushes all remaining spans, metrics, and logs
        openTelemetrySdk.close();
    }
}
