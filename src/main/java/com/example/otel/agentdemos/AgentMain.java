package com.example.otel.agentdemos;

import com.example.otel.util.ConsoleFormatter;

/**
 * Main Entry Point for OpenTelemetry Java Agent Demos.
 *
 * This suite mirrors the demos in com.example.otel.demos, but demonstrates how
 * the OpenTelemetry Java Agent (-javaagent:opentelemetry-javaagent.jar) completely
 * eliminates manual boilerplate:
 *
 * 1. AgentTracingDemo: @WithSpan & @SpanAttribute vs manual Tracer/SpanBuilder/Scope
 * 2. AgentAsyncDemo: Automatic ThreadPool / CompletableFuture context propagation
 * 3. AgentContextPropagationDemo: Automatic HTTP client & server W3C TraceContext headers
 * 4. AgentMetricsDemo: Automatic JVM/GC/Thread/CPU metrics + RED metrics from @WithSpan
 */
public class AgentMain {

    public static void main(String[] args) {
        printBanner();

        AgentTracingDemo tracingDemo = new AgentTracingDemo();
        AgentAsyncDemo asyncDemo = new AgentAsyncDemo();
        AgentContextPropagationDemo httpDemo = new AgentContextPropagationDemo();
        AgentMetricsDemo metricsDemo = new AgentMetricsDemo();

        if (args.length > 0) {
            switch (args[0].trim().toLowerCase()) {
                case "1" -> tracingDemo.run();
                case "2" -> asyncDemo.run();
                case "3" -> httpDemo.run();
                case "4" -> metricsDemo.run();
                default -> runAllDemos(tracingDemo, asyncDemo, httpDemo, metricsDemo);
            }
        } else {
            runAllDemos(tracingDemo, asyncDemo, httpDemo, metricsDemo);
        }

        ConsoleFormatter.printHeader("OPENTELEMETRY JAVA AGENT SUITE COMPLETED");
        ConsoleFormatter.printExplanation(
                "Compare the code in 'src/main/java/com/example/otel/agentdemos/' with 'com/example/otel/demos/'!\n" +
                "Notice how the Java Agent achieved the exact same observability with ZERO manual SDK plumbing."
        );
    }

    private static void runAllDemos(AgentTracingDemo tracingDemo,
                                   AgentAsyncDemo asyncDemo,
                                   AgentContextPropagationDemo httpDemo,
                                   AgentMetricsDemo metricsDemo) {
        tracingDemo.run();
        pause(400);

        asyncDemo.run();
        pause(400);

        httpDemo.run();
        pause(400);

        metricsDemo.run();
    }

    private static void printBanner() {
        System.out.println(ConsoleFormatter.PURPLE + ConsoleFormatter.BOLD);
        System.out.println("  ___  _____     _         _                     _     ____                      ");
        System.out.println(" / _ \\|_   _|___| |       | | __ ___   ____ _   / \\   / ___| ___ _ __  _   _ ___ ");
        System.out.println("| | | | | | / _ \\ | _____ | |/ _` \\ \\ / / _` | / _ \\ | |  _ / _ \\ '_ \\| | | / __|");
        System.out.println("| |_| | | ||  __/ ||_____|| | (_| |\\ V / (_| |/ ___ \\| |_| |  __/ | | | |_| \\__ \\");
        System.out.println(" \\___/  |_| \\___|_|       |_|\\__,_| \\_/ \\__,_/_/   \\_\\\\____|\\___|_| |_|\\__,_|___/");
        System.out.println("                                                                                 ");
        System.out.println("               Zero-Code & Annotation-Driven Study Suite                         ");
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
