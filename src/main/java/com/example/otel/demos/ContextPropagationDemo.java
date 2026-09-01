package com.example.otel.demos;

import com.example.otel.util.ConsoleFormatter;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;

import java.util.HashMap;
import java.util.Map;

/**
 * Demonstrates W3C Context Propagation across Distributed Boundaries (HTTP/gRPC/Messaging):
 *
 * 1. Injecting Active Span Context & Baggage into HTTP Headers (`TextMapSetter`)
 * 2. Inspecting the W3C `traceparent` and `baggage` standard header formats
 * 3. Extracting Context on the receiving server (`TextMapGetter`)
 * 4. Continuing the distributed trace seamlessly on the downstream service
 */
public class ContextPropagationDemo {

    private final OpenTelemetry openTelemetry;
    private final Tracer tracer;

    // Setter for injecting context into an HTTP Request Header Map
    private static final TextMapSetter<Map<String, String>> HTTP_SETTER =
            (carrier, key, value) -> {
                if (carrier != null) {
                    carrier.put(key, value);
                }
            };

    // Getter for extracting context from an HTTP Request Header Map
    private static final TextMapGetter<Map<String, String>> HTTP_GETTER =
            new TextMapGetter<>() {
                @Override
                public Iterable<String> keys(Map<String, String> carrier) {
                    return carrier.keySet();
                }

                @Override
                public String get(Map<String, String> carrier, String key) {
                    return carrier == null ? null : carrier.get(key);
                }
            };

    public ContextPropagationDemo(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
        this.tracer = openTelemetry.getTracer("com.example.otel.propagation");
    }

    public void run() {
        ConsoleFormatter.printHeader("DEMO 3: Context Propagation & W3C TraceContext Injection/Extraction");

        ConsoleFormatter.printExplanation(
                "In a microservices architecture, Service A calls Service B over HTTP.\n" +
                "  To maintain the same trace across services, OpenTelemetry serializes context into W3C standard headers."
        );

        // Step 1: Simulate Service A (Frontend / API Gateway)
        Map<String, String> outgoingHttpHeaders = new HashMap<>();

        ConsoleFormatter.printSubHeader("Step 1: Service A (Client) creates Span & Baggage, then INJECTS into HTTP Headers");

        Span clientSpan = tracer.spanBuilder("http-client-call: GET /orders/42")
                .setSpanKind(SpanKind.CLIENT)
                .startSpan();

        try (Scope scope = clientSpan.makeCurrent()) {
            // Attach Baggage (Baggage propagates across all downstream services, unlike span attributes which are local)
            Baggage baggage = Baggage.builder()
                    .put("user.tier", "Enterprise")
                    .put("tenant.id", "acme-corp-123")
                    .build();

            // Store baggage in the current context
            Context contextWithBaggage = Context.current().with(baggage);

            // Inject the context (Trace ID, Span ID, Sampling Flag, and Baggage) into HTTP Headers
            openTelemetry.getPropagators().getTextMapPropagator().inject(
                    contextWithBaggage,
                    outgoingHttpHeaders,
                    HTTP_SETTER
            );

            ConsoleFormatter.printInfo("Client Trace ID", clientSpan.getSpanContext().getTraceId());
            ConsoleFormatter.printInfo("Client Span ID", clientSpan.getSpanContext().getSpanId());

            System.out.println("\n" + ConsoleFormatter.YELLOW + "Generated HTTP Request Headers (W3C standard):" + ConsoleFormatter.RESET);
            outgoingHttpHeaders.forEach((k, v) ->
                    System.out.printf("  %s%s%s: %s%s%s%n", ConsoleFormatter.BOLD, k, ConsoleFormatter.RESET, ConsoleFormatter.CYAN, v, ConsoleFormatter.RESET)
            );

            ConsoleFormatter.printExplanation(
                    "Note the 'traceparent' format: 00-{32-hex-trace-id}-{16-hex-span-id}-{flags}\n" +
                    "  '00' is version, '01' flag means sampled."
            );

        } finally {
            clientSpan.end();
        }

        // Step 2: Simulate Service B (Downstream Backend Service receiving the HTTP request)
        ConsoleFormatter.printSubHeader("Step 2: Service B (Server) receives HTTP Headers & EXTRACTS Context");

        simulateServiceBReceivingRequest(outgoingHttpHeaders);
    }

    private void simulateServiceBReceivingRequest(Map<String, String> incomingHttpHeaders) {
        // Extract parent context from incoming HTTP headers
        Context extractedContext = openTelemetry.getPropagators().getTextMapPropagator().extract(
                Context.current(),
                incomingHttpHeaders,
                HTTP_GETTER
        );

        // Read baggage that traveled from Service A
        Baggage receivedBaggage = Baggage.fromContext(extractedContext);
        String tenantId = receivedBaggage.getEntryValue("tenant.id");
        String userTier = receivedBaggage.getEntryValue("user.tier");

        ConsoleFormatter.printInfo("Extracted Baggage 'tenant.id'", tenantId);
        ConsoleFormatter.printInfo("Extracted Baggage 'user.tier'", userTier);

        // Start server span using the EXTRACTED context as its parent
        Span serverSpan = tracer.spanBuilder("http-server-handle: GET /orders/42")
                .setParent(extractedContext)
                .setSpanKind(SpanKind.SERVER)
                .startSpan();

        try (Scope scope = serverSpan.makeCurrent()) {
            serverSpan.setAttribute("tenant.id", tenantId != null ? tenantId : "unknown");
            serverSpan.setAttribute("http.status_code", 200);

            ConsoleFormatter.printInfo("Server Span Trace ID", serverSpan.getSpanContext().getTraceId());
            ConsoleFormatter.printInfo("Server Span ID", serverSpan.getSpanContext().getSpanId());

            ConsoleFormatter.printSuccess("Distributed Trace unbroken! Notice Trace ID matched between Client and Server.");
        } finally {
            serverSpan.end();
        }
    }
}
