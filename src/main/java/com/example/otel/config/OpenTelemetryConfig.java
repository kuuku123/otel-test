package com.example.otel.config;

import com.example.otel.util.ConsoleFormatter;
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
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.SdkLoggerProviderBuilder;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure Java OpenTelemetry SDK configuration.
 *
 * Supports both:
 * 1. Console / Stdout Pretty-Printing (for local study & debugging)
 * 2. OTLP Exporter (sending Traces, Metrics, and Logs to an OpenTelemetry Collector)
 */
public class OpenTelemetryConfig implements AutoCloseable {

    private final OpenTelemetrySdk openTelemetrySdk;
    private final List<PeriodicMetricReader> metricReaders = new ArrayList<>();
    private final String otlpEndpoint;

    public OpenTelemetryConfig() {
        this(resolveOtlpEndpoint());
    }

    public OpenTelemetryConfig(String otlpEndpoint) {
        this.otlpEndpoint = otlpEndpoint;

        // Step 1: Define Resource (Service metadata)
        Resource serviceResource = Resource.getDefault().merge(
                Resource.create(
                        Attributes.of(
                                AttributeKey.stringKey("service.name"), "opentelemetry-study-app",
                                AttributeKey.stringKey("service.version"), "1.0.0",
                                AttributeKey.stringKey("deployment.environment"), "development"
                        )
                )
        );

        // Step 2: Build TracerProvider
        SdkTracerProviderBuilder tracerProviderBuilder = SdkTracerProvider.builder()
                .setResource(serviceResource)
                .addSpanProcessor(SimpleSpanProcessor.create(new PrettySpanExporter()));

        // Step 3: Build MeterProvider
        SdkMeterProviderBuilder meterProviderBuilder = SdkMeterProvider.builder()
                .setResource(serviceResource);

        PeriodicMetricReader consoleMetricReader = PeriodicMetricReader.builder(new PrettyMetricExporter())
                .setInterval(Duration.ofMillis(500))
                .build();
        meterProviderBuilder.registerMetricReader(consoleMetricReader);
        metricReaders.add(consoleMetricReader);

        // Step 4: Build LoggerProvider
        SdkLoggerProviderBuilder loggerProviderBuilder = SdkLoggerProvider.builder()
                .setResource(serviceResource)
                .addLogRecordProcessor(SimpleLogRecordProcessor.create(new PrettyLogRecordExporter()));

        // Step 5: Configure OTLP Exporter if endpoint is provided
        if (otlpEndpoint != null && !otlpEndpoint.isBlank()) {
            ConsoleFormatter.printInfo("OTLP Collector Enabled", otlpEndpoint);
            configureOtlpExporters(otlpEndpoint, tracerProviderBuilder, meterProviderBuilder, loggerProviderBuilder);
        }

        SdkTracerProvider tracerProvider = tracerProviderBuilder.build();
        SdkMeterProvider meterProvider = meterProviderBuilder.build();
        SdkLoggerProvider loggerProvider = loggerProviderBuilder.build();

        // Step 6: Configure Context Propagators (W3C standard)
        ContextPropagators contextPropagators = ContextPropagators.create(
                TextMapPropagator.composite(
                        W3CTraceContextPropagator.getInstance(),
                        W3CBaggagePropagator.getInstance()
                )
        );

        // Step 7: Build OpenTelemetrySdk
        this.openTelemetrySdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setMeterProvider(meterProvider)
                .setLoggerProvider(loggerProvider)
                .setPropagators(contextPropagators)
                .build();
    }

    private void configureOtlpExporters(String endpoint,
                                       SdkTracerProviderBuilder tracerBuilder,
                                       SdkMeterProviderBuilder meterBuilder,
                                       SdkLoggerProviderBuilder loggerBuilder) {
        String base = endpoint.replaceAll("/+$", "");

        // 1. OTLP HTTP Span Exporter
        OtlpHttpSpanExporter spanExporter = OtlpHttpSpanExporter.builder()
                .setEndpoint(base.endsWith("/v1/traces") ? base : base + "/v1/traces")
                .setTimeout(Duration.ofSeconds(3))
                .build();
        tracerBuilder.addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build());

        // 2. OTLP HTTP Metric Exporter
        OtlpHttpMetricExporter metricExporter = OtlpHttpMetricExporter.builder()
                .setEndpoint(base.endsWith("/v1/metrics") ? base : base + "/v1/metrics")
                .setTimeout(Duration.ofSeconds(3))
                .build();
        PeriodicMetricReader otlpMetricReader = PeriodicMetricReader.builder(metricExporter)
                .setInterval(Duration.ofSeconds(1))
                .build();
        meterBuilder.registerMetricReader(otlpMetricReader);
        metricReaders.add(otlpMetricReader);

        // 3. OTLP HTTP LogRecord Exporter
        OtlpHttpLogRecordExporter logExporter = OtlpHttpLogRecordExporter.builder()
                .setEndpoint(base.endsWith("/v1/logs") ? base : base + "/v1/logs")
                .setTimeout(Duration.ofSeconds(3))
                .build();
        loggerBuilder.addLogRecordProcessor(BatchLogRecordProcessor.builder(logExporter).build());
    }

    private static String resolveOtlpEndpoint() {
        String env = System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT");
        if (env != null && !env.isBlank()) {
            return env;
        }
        return System.getProperty("otel.exporter.otlp.endpoint");
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

    public void flushMetrics() {
        for (PeriodicMetricReader reader : metricReaders) {
            reader.forceFlush().join(5, java.util.concurrent.TimeUnit.SECONDS);
        }
    }

    public String getOtlpEndpoint() {
        return otlpEndpoint;
    }

    @Override
    public void close() {
        openTelemetrySdk.close();
    }
}
