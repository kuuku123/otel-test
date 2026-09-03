package com.example.otel.agentdemos;

import com.example.otel.util.ConsoleFormatter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Demonstrates Asynchronous Multithreaded Tracing using OpenTelemetry Java Agent.
 *
 * COMPARISON WITH AsyncTracingDemo:
 * -----------------------------------------------------------------------------------------
 * Manual SDK (AsyncTracingDemo)             | OTel Java Agent (AgentAsyncDemo)
 * -----------------------------------------------------------------------------------------
 * - Context currentContext = Context.current();
 * - threadPool.submit(currentContext.wrap(...)); | Plain threadPool.submit(...) or CompletableFuture!
 * - Manual propagation across threads       | Agent automatically intercepts Executor & CompletableFuture
 * -----------------------------------------------------------------------------------------
 */
public class AgentAsyncDemo {

    public void run() {
        ConsoleFormatter.printHeader("AGENT DEMO 2: Automatic Async & Multithreaded Context Propagation");

        ConsoleFormatter.printExplanation(
                "In manual SDK code, ThreadLocal trace context is lost when crossing thread boundaries,\n" +
                "  requiring manual wrapping via Context.current().wrap(Runnable).\n" +
                "With the OpenTelemetry Java Agent, ExecutorService and CompletableFuture are bytecode-instrumented!\n" +
                "The agent automatically passes TraceContext across worker threads without any wrapper code."
        );

        ExecutorService executor = Executors.newFixedThreadPool(3);
        try {
            orchestrateAsyncBatch("BATCH-AGENT-99", executor);
        } finally {
            executor.shutdown();
        }
    }

    @WithSpan(value = "agent.async-orchestrator", kind = SpanKind.SERVER)
    public void orchestrateAsyncBatch(@SpanAttribute("batch.id") String batchId, ExecutorService executor) {
        System.out.printf("  [Orchestrator] Starting parallel fan-out for %s on thread %s\n",
                batchId, Thread.currentThread().getName());
        printSpan("orchestrator-root");

        // Notice: ZERO manual Context.wrap() calls! Pure standard CompletableFuture
        CompletableFuture<Void> fraudTask = CompletableFuture.runAsync(
                () -> checkFraud(batchId),
                executor
        );

        CompletableFuture<Void> inventoryTask = CompletableFuture.runAsync(
                () -> allocateInventory(batchId),
                executor
        );

        CompletableFuture<Void> notificationTask = CompletableFuture.runAsync(
                () -> sendPushNotification(batchId),
                executor
        );

        CompletableFuture.allOf(fraudTask, inventoryTask, notificationTask).join();
        System.out.println("  [Orchestrator] All parallel async branches completed successfully.");
    }

    @WithSpan(value = "agent.async-fraud-check", kind = SpanKind.INTERNAL)
    public void checkFraud(@SpanAttribute("batch.id") String batchId) {
        System.out.printf("    -> [Async Worker 1] Running Fraud Check on thread: %s\n", Thread.currentThread().getName());
        printSpan("async-fraud-check");
        simulateWork(40);
    }

    @WithSpan(value = "agent.async-inventory", kind = SpanKind.INTERNAL)
    public void allocateInventory(@SpanAttribute("batch.id") String batchId) {
        System.out.printf("    -> [Async Worker 2] Allocating Inventory on thread: %s\n", Thread.currentThread().getName());
        printSpan("async-inventory");
        simulateWork(60);
    }

    @WithSpan(value = "agent.async-notification", kind = SpanKind.INTERNAL)
    public void sendPushNotification(@SpanAttribute("batch.id") String batchId) {
        System.out.printf("    -> [Async Worker 3] Sending Notification on thread: %s\n", Thread.currentThread().getName());
        printSpan("async-notification");
        simulateWork(30);
    }

    private void printSpan(String stepName) {
        Span span = Span.current();
        if (span.getSpanContext().isValid()) {
            System.out.printf("       [Trace] TraceId: %s | SpanId: %s\n",
                    span.getSpanContext().getTraceId(),
                    span.getSpanContext().getSpanId()
            );
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
