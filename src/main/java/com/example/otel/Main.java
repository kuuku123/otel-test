package com.example.otel;

import com.example.otel.config.OpenTelemetryConfig;
import com.example.otel.demos.*;
import com.example.otel.util.ConsoleFormatter;

/**
 * Main entry point for the OpenTelemetry Java Study & Testing Project.
 *
 * Runs interactive/demonstrative scenarios covering all core aspects of OpenTelemetry SDK:
 * 1. Tracing Fundamentals (Root, Child, Attributes, Events, Exceptions)
 * 2. Metrics Fundamentals (Counters, UpDownCounters, Histograms, Gauges)
 * 3. Context Propagation & W3C TraceContext (Cross-service distributed tracing)
 * 4. Logs & Trace-Log Correlation
 * 5. Asynchronous & Multithreaded Tracing
 */
public class Main {

    public static void main(String[] args) {
        printBanner();

        // Initialize pure Java OpenTelemetry SDK configuration
        try (OpenTelemetryConfig otelConfig = new OpenTelemetryConfig()) {

            BasicTracingDemo tracingDemo = new BasicTracingDemo(otelConfig.getTracer("com.example.otel.traces"));
            MetricsDemo metricsDemo = new MetricsDemo(otelConfig);
            ContextPropagationDemo contextPropagationDemo = new ContextPropagationDemo(otelConfig.getOpenTelemetry());
            LogsCorrelationDemo logsDemo = new LogsCorrelationDemo(
                    otelConfig.getTracer("com.example.otel.logs"),
                    otelConfig.getLogger("com.example.otel.logs")
            );
            AsyncTracingDemo asyncTracingDemo = new AsyncTracingDemo(otelConfig.getTracer("com.example.otel.async"));

            if (args.length > 0) {
                switch (args[0].trim()) {
                    case "1" -> tracingDemo.run();
                    case "2" -> metricsDemo.run();
                    case "3" -> contextPropagationDemo.run();
                    case "4" -> logsDemo.run();
                    case "5" -> asyncTracingDemo.run();
                    default -> runAllDemos(tracingDemo, metricsDemo, contextPropagationDemo, logsDemo, asyncTracingDemo);
                }
            } else {
                runAllDemos(tracingDemo, metricsDemo, contextPropagationDemo, logsDemo, asyncTracingDemo);
            }

            ConsoleFormatter.printHeader("OPEN TELEMETRY STUDY EXECUTION COMPLETED");
            ConsoleFormatter.printExplanation("Explore the code in src/main/java/com/example/otel to study how each concept is implemented!");
        }
    }

    private static void runAllDemos(BasicTracingDemo tracingDemo,
                                   MetricsDemo metricsDemo,
                                   ContextPropagationDemo contextPropagationDemo,
                                   LogsCorrelationDemo logsDemo,
                                   AsyncTracingDemo asyncTracingDemo) {
        // Run all 5 comprehensive demos
        tracingDemo.run();
        pause(400);

        metricsDemo.run();
        pause(400);

        contextPropagationDemo.run();
        pause(400);

        logsDemo.run();
        pause(400);

        asyncTracingDemo.run();
    }

    private static void printBanner() {
        System.out.println(ConsoleFormatter.CYAN + ConsoleFormatter.BOLD);
        System.out.println("  ___                    _____     _                    _              ");
        System.out.println(" / _ \\ _ __   ___ _ __  |_   _|___| | ___ _ __ ___  ___| |_ _ __ _   _ ");
        System.out.println("| | | | '_ \\ / _ \\ '_ \\   | | / _ \\ |/ _ \\ '_ ` _ \\/ _ \\ __| '__| | | |");
        System.out.println("| |_| | |_) |  __/ | | |  | ||  __/ |  __/ | | | | |  __/ |_| |  | |_| |");
        System.out.println(" \\___/| .__/ \\___|_| |_|  |_| \\___|_|\\___|_| |_| |_|\\___|\\__|_|   \\__, |");
        System.out.println("      |_|                                                         |___/ ");
        System.out.println("                 Pure Java SDK Study & Testing Suite                    ");
        System.out.println(ConsoleFormatter.RESET);
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
