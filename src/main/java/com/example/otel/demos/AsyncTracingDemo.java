package com.example.otel.demos;

import com.example.otel.util.ConsoleFormatter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Demonstrates Asynchronous & Multithreaded Tracing in OpenTelemetry:
 *
 * 1. Why standard Java ThreadLocal context is lost when jumping threads
 * 2. How to use `Context.current().wrap(Runnable/Callable)` or `Context.current().makeCurrent()`
 * 3. Fan-out parallel tasks (Async CompletableFutures) with unbroken parent-child traces
 */
public class AsyncTracingDemo {

    private final Tracer tracer;

    public AsyncTracingDemo(Tracer tracer) {
        this.tracer = tracer;
    }

    public void run() {
        ConsoleFormatter.printHeader("DEMO 5: Asynchronous & Multithreaded Tracing");

        ConsoleFormatter.printExplanation(
                "OpenTelemetry uses ThreadLocal context by default. When dispatching tasks to worker\n" +
                "  threads or thread pools (CompletableFuture/Executors), context must be explicitly wrapped\n" +
                "  so child spans know who their parent is across thread boundaries."
        );

        ExecutorService threadPool = Executors.newFixedThreadPool(3);

        try {
            runParallelFanOut(threadPool);
        } finally {
            threadPool.shutdown();
        }
    }

    private void runParallelFanOut(ExecutorService threadPool) {
        Span parentSpan = tracer.spanBuilder("order-fulfillment-orchestrator")
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();

        try (Scope scope = parentSpan.makeCurrent()) {
            parentSpan.setAttribute("order.batch_id", "BATCH-7788");

            // Capture the current context so worker threads can inherit it
            Context currentContext = Context.current();

            ConsoleFormatter.printSubHeader("Launching 3 Parallel Async Tasks via CompletableFuture & ThreadPool");

            // Task 1: Check fraud (Async wrapped runnable)
            CompletableFuture<Void> fraudFuture = CompletableFuture.runAsync(
                    currentContext.wrap(() -> executeAsyncTask("fraud-detection-service", 80)),
                    threadPool
            );

            // Task 2: Reserve Warehouse Item (Async wrapped runnable)
            CompletableFuture<Void> warehouseFuture = CompletableFuture.runAsync(
                    currentContext.wrap(() -> executeAsyncTask("reserve-warehouse-stock", 140)),
                    threadPool
            );

            // Task 3: Send Confirmation Email (Async wrapped runnable)
            CompletableFuture<Void> emailFuture = CompletableFuture.runAsync(
                    currentContext.wrap(() -> executeAsyncTask("prepare-email-receipt", 60)),
                    threadPool
            );

            // Wait for all parallel tasks to finish
            CompletableFuture.allOf(fraudFuture, warehouseFuture, emailFuture).join();

            ConsoleFormatter.printSuccess("All parallel asynchronous tasks completed!");
            parentSpan.setStatus(StatusCode.OK);

        } finally {
            parentSpan.end();
        }
    }

    private void executeAsyncTask(String taskName, long delayMillis) {
        // Because Context was passed via context.wrap(), this span automatically sees the parent span!
        Span asyncSpan = tracer.spanBuilder(taskName)
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();

        try (Scope scope = asyncSpan.makeCurrent()) {
            asyncSpan.setAttribute("thread.name", Thread.currentThread().getName());
            simulateDelay(delayMillis);
            asyncSpan.setStatus(StatusCode.OK);
        } finally {
            asyncSpan.end();
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
