package com.example.otel;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests showing how to test OpenTelemetry instrumentation in Java using
 * InMemorySpanExporter and InMemoryMetricReader from `opentelemetry-sdk-testing`.
 */
class OpenTelemetryTest {

    private InMemorySpanExporter spanExporter;
    private InMemoryMetricReader metricReader;
    private SdkTracerProvider tracerProvider;
    private SdkMeterProvider meterProvider;
    private OpenTelemetrySdk openTelemetry;

    @BeforeEach
    void setUp() {
        spanExporter = InMemorySpanExporter.create();
        tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                .build();

        metricReader = InMemoryMetricReader.create();
        meterProvider = SdkMeterProvider.builder()
                .registerMetricReader(metricReader)
                .build();

        openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setMeterProvider(meterProvider)
                .build();
    }

    @AfterEach
    void tearDown() {
        openTelemetry.close();
    }

    @Test
    @DisplayName("Should capture Parent-Child Spans with correct hierarchy and attributes")
    void testSpanHierarchyAndAttributes() {
        Tracer tracer = openTelemetry.getTracer("test-tracer");

        Span parentSpan = tracer.spanBuilder("parent-operation").startSpan();
        try (Scope parentScope = parentSpan.makeCurrent()) {
            parentSpan.setAttribute("app.user_id", "user_123");

            Span childSpan = tracer.spanBuilder("child-operation").startSpan();
            try (Scope childScope = childSpan.makeCurrent()) {
                childSpan.setAttribute("db.table", "users");
                childSpan.addEvent("query_executed");
                childSpan.setStatus(StatusCode.OK);
            } finally {
                childSpan.end();
            }

            parentSpan.setStatus(StatusCode.OK);
        } finally {
            parentSpan.end();
        }

        List<SpanData> finishedSpans = spanExporter.getFinishedSpanItems();
        assertThat(finishedSpans).hasSize(2);

        SpanData finishedChild = finishedSpans.get(0);
        SpanData finishedParent = finishedSpans.get(1);

        assertThat(finishedChild.getName()).isEqualTo("child-operation");
        assertThat(finishedParent.getName()).isEqualTo("parent-operation");

        // Verify shared Trace ID
        assertThat(finishedChild.getTraceId()).isEqualTo(finishedParent.getTraceId());

        // Verify parent-child relationship
        assertThat(finishedChild.getParentSpanId()).isEqualTo(finishedParent.getSpanId());

        // Verify attributes
        assertThat(finishedChild.getAttributes().get(AttributeKey.stringKey("db.table"))).isEqualTo("users");
        assertThat(finishedParent.getAttributes().get(AttributeKey.stringKey("app.user_id"))).isEqualTo("user_123");
    }

    @Test
    @DisplayName("Should accurately record and aggregate Counter metric values")
    void testCounterMetrics() {
        Meter meter = openTelemetry.getMeter("test-meter");
        LongCounter counter = meter.counterBuilder("processed_items").build();

        counter.add(5, Attributes.of(AttributeKey.stringKey("type"), "A"));
        counter.add(3, Attributes.of(AttributeKey.stringKey("type"), "A"));
        counter.add(2, Attributes.of(AttributeKey.stringKey("type"), "B"));

        Collection<MetricData> metrics = metricReader.collectAllMetrics();
        assertThat(metrics).isNotEmpty();

        MetricData metricData = metrics.stream()
                .filter(m -> m.getName().equals("processed_items"))
                .findFirst()
                .orElseThrow();

        List<LongPointData> points = List.copyOf(metricData.getLongSumData().getPoints());
        assertThat(points).hasSize(2);

        long sumTypeA = points.stream()
                .filter(p -> "A".equals(p.getAttributes().get(AttributeKey.stringKey("type"))))
                .mapToLong(LongPointData::getValue)
                .sum();

        long sumTypeB = points.stream()
                .filter(p -> "B".equals(p.getAttributes().get(AttributeKey.stringKey("type"))))
                .mapToLong(LongPointData::getValue)
                .sum();

        assertThat(sumTypeA).isEqualTo(8);
        assertThat(sumTypeB).isEqualTo(2);
    }

    @Test
    @DisplayName("Should inject and extract W3C TraceContext across carriers")
    void testTraceContextPropagation() {
        Tracer tracer = openTelemetry.getTracer("propagation-tracer");
        W3CTraceContextPropagator propagator = W3CTraceContextPropagator.getInstance();

        Span span = tracer.spanBuilder("http-request").startSpan();
        Map<String, String> headers = new HashMap<>();

        try (Scope scope = span.makeCurrent()) {
            propagator.inject(Context.current(), headers, (carrier, key, val) -> carrier.put(key, val));
        } finally {
            span.end();
        }

        assertThat(headers).containsKey("traceparent");
        String traceparent = headers.get("traceparent");
        assertThat(traceparent).contains(span.getSpanContext().getTraceId());
        assertThat(traceparent).contains(span.getSpanContext().getSpanId());

        // Extract context on the receiving end
        Context extracted = propagator.extract(Context.current(), headers, new io.opentelemetry.context.propagation.TextMapGetter<Map<String, String>>() {
            @Override
            public Iterable<String> keys(Map<String, String> carrier) {
                return carrier.keySet();
            }

            @Override
            public String get(Map<String, String> carrier, String key) {
                return carrier.get(key);
            }
        });

        Span extractedSpan = Span.fromContext(extracted);
        assertThat(extractedSpan.getSpanContext().getTraceId()).isEqualTo(span.getSpanContext().getTraceId());
        assertThat(extractedSpan.getSpanContext().getSpanId()).isEqualTo(span.getSpanContext().getSpanId());
    }
}
