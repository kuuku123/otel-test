package com.example.otel.demos;

import com.example.otel.util.ConsoleFormatter;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

/**
 * Demonstrates OpenTelemetry Structured Logging and Trace-Log Correlation:
 *
 * 1. Emitting structured log records with Severity, Body, and Attributes
 * 2. Automatic Trace & Span ID correlation when a log is emitted inside an active Span Scope
 * 3. Comparing logs inside a span vs. logs outside a span (standalone logs)
 */
public class LogsCorrelationDemo {

    private final Tracer tracer;
    private final Logger logger;

    public LogsCorrelationDemo(Tracer tracer, Logger logger) {
        this.tracer = tracer;
        this.logger = logger;
    }

    public void run() {
        ConsoleFormatter.printHeader("DEMO 4: OpenTelemetry Logs & Trace-Log Correlation");

        ConsoleFormatter.printSubHeader("Scenario A: Emitting a Log OUTSIDE any active Span");
        ConsoleFormatter.printExplanation(
                "When emitted outside a span, the log record has no TraceId or SpanId associated."
        );

        logger.logRecordBuilder()
                .setSeverity(Severity.INFO)
                .setSeverityText("INFO")
                .setBody("Application startup complete. Ready to receive traffic.")
                .setAttribute(AttributeKey.stringKey("component"), "ApplicationLifecycle")
                .emit();

        ConsoleFormatter.printSubHeader("Scenario B: Emitting Logs INSIDE an active Span Scope");
        ConsoleFormatter.printExplanation(
                "Notice how log records emitted while a Span is active AUTOMATICALLY inherit\n" +
                "  the active Span's TraceId and SpanId. This enables 1-click navigation between logs & traces in APMs!"
        );

        Span authSpan = tracer.spanBuilder("authenticate-user")
                .startSpan();

        try (Scope scope = authSpan.makeCurrent()) {
            authSpan.setAttribute("user.id", "usr_998877");

            // Log 1: Step info inside span
            logger.logRecordBuilder()
                    .setSeverity(Severity.INFO)
                    .setSeverityText("INFO")
                    .setBody("Authenticating user with token")
                    .setAttribute(AttributeKey.stringKey("user.id"), "usr_998877")
                    .setAttribute(AttributeKey.stringKey("auth.provider"), "OAuth2")
                    .emit();

            // Log 2: Warning inside span
            logger.logRecordBuilder()
                    .setSeverity(Severity.WARN)
                    .setSeverityText("WARN")
                    .setBody("User token expires in less than 5 minutes")
                    .setAttribute(AttributeKey.longKey("token.remaining_seconds"), 280L)
                    .emit();

        } finally {
            authSpan.end();
        }
    }
}
