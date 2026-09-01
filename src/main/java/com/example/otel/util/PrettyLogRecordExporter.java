package com.example.otel.util;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;

import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * A custom OpenTelemetry LogRecordExporter that pretty-prints structured log records
 * and displays how OpenTelemetry correlates logs with Traces and Spans.
 */
public class PrettyLogRecordExporter implements LogRecordExporter {

    @Override
    public CompletableResultCode export(Collection<LogRecordData> logs) {
        for (LogRecordData log : logs) {
            String traceId = log.getSpanContext().getTraceId();
            String spanId = log.getSpanContext().getSpanId();
            boolean hasTrace = TraceId.isValid(traceId);
            boolean hasSpan = SpanId.isValid(spanId);

            String severityColor = switch (log.getSeverity()) {
                case ERROR, FATAL, FATAL2, FATAL3, FATAL4, ERROR2, ERROR3, ERROR4 -> ConsoleFormatter.RED;
                case WARN, WARN2, WARN3, WARN4 -> ConsoleFormatter.YELLOW;
                case INFO, INFO2, INFO3, INFO4 -> ConsoleFormatter.GREEN;
                default -> ConsoleFormatter.GRAY;
            };

            System.out.println(ConsoleFormatter.PURPLE + "┌────────────────── [ EXPORTED LOG RECORD ] ──────────────────" + ConsoleFormatter.RESET);
            System.out.printf("│ %-16s : %s%s%s%n", "Severity", severityColor, log.getSeverity() + " (" + log.getSeverityText() + ")", ConsoleFormatter.RESET);
            System.out.printf("│ %-16s : %s%s%s%n", "Body Message", ConsoleFormatter.BOLD, log.getBody().asString(), ConsoleFormatter.RESET);
            System.out.printf("│ %-16s : %s%s%s%n", "Trace ID",
                    hasTrace ? ConsoleFormatter.PURPLE + traceId : ConsoleFormatter.GRAY + "(No Active Trace)",
                    ConsoleFormatter.RESET, "");
            System.out.printf("│ %-16s : %s%s%s%n", "Span ID",
                    hasSpan ? ConsoleFormatter.BLUE + spanId : ConsoleFormatter.GRAY + "(No Active Span)",
                    ConsoleFormatter.RESET, "");
            System.out.printf("│ %-16s : %s%n", "Timestamp",
                    Instant.ofEpochMilli(TimeUnit.NANOSECONDS.toMillis(log.getTimestampEpochNanos())));

            Attributes attributes = log.getAttributes();
            if (!attributes.isEmpty()) {
                System.out.println("│ " + ConsoleFormatter.YELLOW + "Log Attributes:" + ConsoleFormatter.RESET);
                attributes.forEach((key, value) ->
                        System.out.printf("│   • %-22s = %s%n", key.getKey(), value)
                );
            }
            System.out.println(ConsoleFormatter.PURPLE + "└─────────────────────────────────────────────────────────────" + ConsoleFormatter.RESET);
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
