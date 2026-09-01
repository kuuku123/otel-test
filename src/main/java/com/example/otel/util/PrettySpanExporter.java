package com.example.otel.util;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * A custom OpenTelemetry SpanExporter that pretty-prints spans to stdout
 * in an easy-to-read format for learning and analyzing trace data.
 */
public class PrettySpanExporter implements SpanExporter {

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        for (SpanData span : spans) {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(span.getEndEpochNanos() - span.getStartEpochNanos());
            boolean hasParent = SpanId.isValid(span.getParentSpanId());
            boolean isError = span.getStatus().getStatusCode() == StatusCode.ERROR;

            System.out.println(ConsoleFormatter.CYAN + "┌────────────────── [ EXPORTED SPAN: " + ConsoleFormatter.BOLD + span.getName() + ConsoleFormatter.CYAN + " ] ──────────────────" + ConsoleFormatter.RESET);
            System.out.printf("│ %-16s : %s%s%s%n", "Trace ID", ConsoleFormatter.PURPLE, span.getTraceId(), ConsoleFormatter.RESET);
            System.out.printf("│ %-16s : %s%s%s%n", "Span ID", ConsoleFormatter.BLUE, span.getSpanId(), ConsoleFormatter.RESET);
            System.out.printf("│ %-16s : %s%n", "Parent Span ID", hasParent ? span.getParentSpanId() : "(Root Span - No Parent)");
            System.out.printf("│ %-16s : %s%n", "Span Kind", span.getKind());
            System.out.printf("│ %-16s : %s%s%s (Duration: %d ms)%n", "Status",
                    isError ? ConsoleFormatter.RED : ConsoleFormatter.GREEN,
                    span.getStatus().getStatusCode(),
                    ConsoleFormatter.RESET,
                    durationMs);
            System.out.printf("│ %-16s : %s%n", "Start Time", Instant.ofEpochMilli(TimeUnit.NANOSECONDS.toMillis(span.getStartEpochNanos())));

            // Attributes
            Attributes attributes = span.getAttributes();
            if (!attributes.isEmpty()) {
                System.out.println("│ " + ConsoleFormatter.YELLOW + "Attributes:" + ConsoleFormatter.RESET);
                attributes.forEach((key, value) ->
                    System.out.printf("│   • %-22s = %s%n", key.getKey(), value)
                );
            }

            // Events (Logs/Annotations attached to Span)
            if (!span.getEvents().isEmpty()) {
                System.out.println("│ " + ConsoleFormatter.YELLOW + "Span Events (Milestones/Exceptions):" + ConsoleFormatter.RESET);
                for (EventData event : span.getEvents()) {
                    long eventOffsetMs = TimeUnit.NANOSECONDS.toMillis(event.getEpochNanos() - span.getStartEpochNanos());
                    System.out.printf("│   • [+%dms] %s%s%s%n", eventOffsetMs, ConsoleFormatter.BOLD, event.getName(), ConsoleFormatter.RESET);
                    if (!event.getAttributes().isEmpty()) {
                        event.getAttributes().forEach((k, v) ->
                            System.out.printf("│       - %s: %s%n", k.getKey(), v)
                        );
                    }
                }
            }

            System.out.println(ConsoleFormatter.CYAN + "└─────────────────────────────────────────────────────────────────" + ConsoleFormatter.RESET);
        }
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        return CompletableResultCode.ofSuccess();
    }
}
