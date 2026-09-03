package com.example.otel.agentdemos;

import com.example.otel.util.ConsoleFormatter;
import com.sun.net.httpserver.HttpServer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Demonstrates Automatic Distributed Tracing & W3C Context Propagation over HTTP.
 *
 * COMPARISON WITH ContextPropagationDemo:
 * -----------------------------------------------------------------------------------------
 * Manual SDK (ContextPropagationDemo)       | OTel Java Agent (AgentContextPropagationDemo)
 * -----------------------------------------------------------------------------------------
 * - Define custom TextMapSetter & Getter    | Handled automatically by the agent
 * - Manual propagator.inject(...)           | java.net.http.HttpClient automatically injects traceparent
 * - Manual propagator.extract(...)          | HTTP server layer automatically extracts traceparent
 * - Heavy boilerplate for every HTTP client | ZERO code changes! Just use standard HttpClient
 * -----------------------------------------------------------------------------------------
 */
public class AgentContextPropagationDemo {

    public void run() {
        ConsoleFormatter.printHeader("AGENT DEMO 3: Automatic HTTP Distributed Context Propagation");

        ConsoleFormatter.printExplanation(
                "In manual SDK code, distributed tracing requires defining TextMapSetter/TextMapGetter\n" +
                "  and explicitly calling inject() and extract() on HTTP header maps.\n" +
                "With the OpenTelemetry Java Agent, standard HTTP clients (java.net.http.HttpClient,\n" +
                "  Apache HttpClient, OkHttp) are automatically bytecode-instrumented to inject W3C traceparent\n" +
                "  headers, and HTTP servers automatically extract them!"
        );

        int port = 18088;
        HttpServer server = null;
        try {
            // Start a lightweight JDK HTTP Server
            server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
            server.createContext("/api/order", exchange -> {
                System.out.println("\n  [HTTP Server] Received incoming HTTP request at /api/order");
                System.out.println("  [HTTP Server] Inspecting incoming HTTP Headers:");

                // Look for W3C traceparent automatically injected by the OTel Agent on the client
                String traceparent = exchange.getRequestHeaders().getFirst("traceparent");
                if (traceparent != null) {
                    System.out.println("    >>> traceparent (Auto-Injected by OTel Agent!): " + traceparent);
                } else {
                    System.out.println("    (No traceparent header found - make sure JVM is started with -javaagent)");
                }

                String response = "{\"status\":\"CONFIRMED\",\"orderId\":\"ORD-AGENT-888\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            });
            server.start();

            // Execute HTTP Client call
            makeHttpClientCall(port);

        } catch (Exception e) {
            System.err.println("Error running HTTP demo: " + e.getMessage());
        } finally {
            if (server != null) {
                server.stop(0);
            }
        }
    }

    @WithSpan("agent.call-remote-order-service")
    public void makeHttpClientCall(int port) throws Exception {
        System.out.println("  [HTTP Client] Sending request using standard java.net.http.HttpClient...");
        Span clientSpan = Span.current();
        if (clientSpan.getSpanContext().isValid()) {
            System.out.printf("  [HTTP Client] Active TraceId: %s | SpanId: %s\n",
                    clientSpan.getSpanContext().getTraceId(),
                    clientSpan.getSpanContext().getSpanId());
        }

        // Standard Java 11+ HttpClient. Zero OTel SDK code!
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/order"))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("  [HTTP Client] Received Response: " + response.body());
    }
}
