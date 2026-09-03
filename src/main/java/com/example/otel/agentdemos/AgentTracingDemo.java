package com.example.otel.agentdemos;

import com.example.otel.util.ConsoleFormatter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;

/**
 * Demonstrates Annotation-Driven Distributed Tracing using the OpenTelemetry Java Agent.
 *
 * COMPARISON WITH BasicTracingDemo:
 * -----------------------------------------------------------------------------------------
 * Manual SDK (BasicTracingDemo)             | OTel Java Agent (AgentTracingDemo)
 * -----------------------------------------------------------------------------------------
 * - tracer.spanBuilder("...").startSpan()   | @WithSpan("...")
 * - try (Scope s = span.makeCurrent())      | Handled automatically by the agent
 * - span.setAttribute("key", val)           | @SpanAttribute("key") on method parameters
 * - span.recordException(e)                 | Any thrown exception is automatically recorded
 * - span.setStatus(StatusCode.ERROR)        | Span status automatically set to ERROR on exception
 * - finally { span.end(); }                 | Span ends automatically when method returns
 * -----------------------------------------------------------------------------------------
 */
public class AgentTracingDemo {

    public void run() {
        ConsoleFormatter.printHeader("AGENT DEMO 1: Annotation-Driven Tracing (@WithSpan & @SpanAttribute)");

        ConsoleFormatter.printSubHeader("Scenario A: Successful Order Checkout Flow");
        ConsoleFormatter.printExplanation(
                "Every method annotated with @WithSpan is automatically wrapped in a Span.\n" +
                "Notice that there are ZERO calls to Tracer, SpanBuilder, or Scope in this code!"
        );
        processSuccessfulOrder("ORDER-AGENT-101", "CUST-999", 249.99);

        ConsoleFormatter.printSubHeader("Scenario B: Exception Handling & Automatic Error Recording");
        ConsoleFormatter.printExplanation(
                "When processFailedOrder throws an exception, the Java Agent automatically:\n" +
                "  1. Catches the exception\n" +
                "  2. Records the exception event with full stack trace onto the Span\n" +
                "  3. Sets the Span status to ERROR\n" +
                "  4. Closes the Span cleanly before re-throwing"
        );
        try {
            processFailedOrder("ORDER-AGENT-102", "CUST-888", 9999.00);
        } catch (Exception e) {
            System.out.println("  [Caller] Caught expected exception: " + e.getMessage());
        }
    }

    /**
     * Root span created purely via @WithSpan annotation.
     * Parameters annotated with @SpanAttribute are automatically added as span attributes.
     */
    @WithSpan(value = "agent.checkout-order", kind = SpanKind.SERVER)
    public void processSuccessfulOrder(@SpanAttribute("order.id") String orderId,
                                       @SpanAttribute("customer.id") String customerId,
                                       @SpanAttribute("order.amount") double amount) {

        System.out.printf("  [Business] Processing order %s for customer %s ($%.2f)\n", orderId, customerId, amount);
        printCurrentTraceAndSpan("checkout-order (Root Span)");

        // Calling nested methods creates nested Child Spans automatically
        validateInventory(orderId);
        chargePayment(orderId, amount);
    }

    @WithSpan(value = "agent.validate-inventory", kind = SpanKind.INTERNAL)
    public void validateInventory(@SpanAttribute("order.id") String orderId) {
        System.out.printf("  [Business] Validating warehouse stock for %s...\n", orderId);
        printCurrentTraceAndSpan("validate-inventory (Child Span)");
        simulateWork(30);
    }

    @WithSpan(value = "agent.charge-payment", kind = SpanKind.INTERNAL)
    public void chargePayment(@SpanAttribute("order.id") String orderId,
                              @SpanAttribute("order.amount") double amount) {
        System.out.printf("  [Business] Charging $%.2f to customer payment gateway...\n", amount);
        printCurrentTraceAndSpan("charge-payment (Child Span)");
        simulateWork(40);
    }

    @WithSpan(value = "agent.failed-order", kind = SpanKind.SERVER)
    public void processFailedOrder(@SpanAttribute("order.id") String orderId,
                                   @SpanAttribute("customer.id") String customerId,
                                   @SpanAttribute("order.amount") double amount) {
        System.out.printf("  [Business] Processing invalid order %s ($%.2f)...\n", orderId, amount);
        printCurrentTraceAndSpan("failed-order (Root Span)");
        simulateWork(25);
        throw new IllegalStateException("Payment declined: Insufficient funds or invalid card for order " + orderId);
    }

    private void printCurrentTraceAndSpan(String stepName) {
        Span currentSpan = Span.current();
        if (currentSpan.getSpanContext().isValid()) {
            System.out.printf("    -> [%s] TraceId: %s | SpanId: %s\n",
                    stepName,
                    currentSpan.getSpanContext().getTraceId(),
                    currentSpan.getSpanContext().getSpanId()
            );
        } else {
            System.out.printf("    -> [%s] (Running without Java Agent: No active span context)\n", stepName);
        }
    }

    private void simulateWork(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
