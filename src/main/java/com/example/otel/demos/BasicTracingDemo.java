package com.example.otel.demos;

import com.example.otel.util.ConsoleFormatter;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

/**
 * Demonstrates the Core Concepts of Distributed Tracing in OpenTelemetry:
 *
 * 1. Root Spans vs. Child Spans (Parent-Child Hierarchy)
 * 2. Active Span Scope and ThreadLocal Context (`span.makeCurrent()`)
 * 3. Setting Span Attributes (Key-Value metadata)
 * 4. Recording Span Events (Point-in-time timestamped annotations)
 * 5. Error handling, Exception recording, and Span Status (OK vs ERROR)
 */
public class BasicTracingDemo {

    private final Tracer tracer;

    public BasicTracingDemo(Tracer tracer) {
        this.tracer = tracer;
    }

    public void run() {
        ConsoleFormatter.printHeader("DEMO 1: Tracing Fundamentals (Root, Child, Attributes, Events, Errors)");

        ConsoleFormatter.printSubHeader("Scenario A: Successful Multi-Step Checkout Flow");
        ConsoleFormatter.printExplanation(
                "Notice how child spans ('validate-inventory' & 'charge-payment') automatically inherit the SAME TraceId\n" +
                "  and their 'Parent Span ID' points to the 'checkout-order' Root Span ID."
        );
        processSuccessfulCheckout("ORDER-101", "CUST-999", 150.00);

        ConsoleFormatter.printSubHeader("Scenario B: Handling Failures and Exceptions in a Span");
        ConsoleFormatter.printExplanation(
                "Notice how when an exception occurs, we record the exception with span.recordException(e)\n" +
                "  and mark span.setStatus(StatusCode.ERROR). The span event list will capture exception details."
        );
        processFailedCheckout("ORDER-102", "CUST-888", 9999.00);
    }

    private void processSuccessfulCheckout(String orderId, String customerId, double amount) {
        // Step 1: Start the ROOT Span
        Span rootSpan = tracer.spanBuilder("checkout-order")
                .setSpanKind(SpanKind.SERVER)
                .startSpan();

        // Step 2: Make the span active in the current Context (Scope)
        // Using try-with-resources ensures Scope is closed when done
        try (Scope scope = rootSpan.makeCurrent()) {
            // Attach attributes to describe the business transaction
            rootSpan.setAttribute("order.id", orderId);
            rootSpan.setAttribute("customer.id", customerId);
            rootSpan.setAttribute("order.amount", amount);
            rootSpan.setAttribute("order.currency", "USD");

            // Event: Milestone during the span
            rootSpan.addEvent("CheckoutValidationStarted");

            // Step 3: Call a nested method that starts a CHILD Span
            validateInventory(orderId);

            // Step 4: Call another nested method (another CHILD Span)
            chargePayment(orderId, amount);

            rootSpan.addEvent("CheckoutCompleted", Attributes.of(AttributeKey.stringKey("outcome"), "SUCCESS"));
            rootSpan.setStatus(StatusCode.OK);

        } catch (Exception e) {
            rootSpan.setStatus(StatusCode.ERROR, e.getMessage());
            rootSpan.recordException(e);
        } finally {
            // Crucial: Always end spans in finally block to ensure end time and duration are recorded
            rootSpan.end();
        }
    }

    private void validateInventory(String orderId) {
        // Because rootSpan was made current in Scope, this spanBuilder automatically becomes a CHILD of rootSpan
        Span childSpan = tracer.spanBuilder("validate-inventory")
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();

        try (Scope scope = childSpan.makeCurrent()) {
            childSpan.setAttribute("inventory.warehouse", "US-East-1");
            childSpan.setAttribute("inventory.items_count", 3);

            // Simulate doing some work
            simulateDelay(120);

            childSpan.addEvent("InventoryReserved");
            childSpan.setStatus(StatusCode.OK);
        } finally {
            childSpan.end();
        }
    }

    private void chargePayment(String orderId, double amount) {
        Span childSpan = tracer.spanBuilder("charge-payment")
                .setSpanKind(SpanKind.CLIENT)
                .startSpan();

        try (Scope scope = childSpan.makeCurrent()) {
            childSpan.setAttribute("payment.gateway", "Stripe");
            childSpan.setAttribute("payment.amount", amount);

            // Simulate payment network call
            simulateDelay(200);

            childSpan.setStatus(StatusCode.OK);
        } finally {
            childSpan.end();
        }
    }

    private void processFailedCheckout(String orderId, String customerId, double amount) {
        Span rootSpan = tracer.spanBuilder("checkout-order-failed")
                .setSpanKind(SpanKind.SERVER)
                .startSpan();

        try (Scope scope = rootSpan.makeCurrent()) {
            rootSpan.setAttribute("order.id", orderId);
            rootSpan.setAttribute("customer.id", customerId);
            rootSpan.setAttribute("order.amount", amount);

            // Simulate calling a downstream dependency that throws an exception
            simulateFailingInventoryCall(orderId);

            rootSpan.setStatus(StatusCode.OK);
        } catch (Exception e) {
            // Record exception details (stack trace, message, type) on the span
            rootSpan.recordException(e, Attributes.of(AttributeKey.stringKey("exception.custom_hint"), "Check stock"));
            rootSpan.setStatus(StatusCode.ERROR, "Checkout failed due to: " + e.getMessage());
        } finally {
            rootSpan.end();
        }
    }

    private void simulateFailingInventoryCall(String orderId) {
        Span childSpan = tracer.spanBuilder("validate-inventory-fail")
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();

        try (Scope scope = childSpan.makeCurrent()) {
            childSpan.setAttribute("order.id", orderId);
            simulateDelay(50);

            throw new IllegalStateException("Out of stock: Item SKU-987 unavailable");
        } catch (Exception e) {
            childSpan.recordException(e);
            childSpan.setStatus(StatusCode.ERROR, e.getMessage());
            throw e; // rethrow to parent span
        } finally {
            childSpan.end();
        }
    }

    private void simulateDelay(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
