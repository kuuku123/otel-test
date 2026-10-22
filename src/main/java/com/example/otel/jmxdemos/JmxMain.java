package com.example.otel.jmxdemos;

import com.example.otel.util.ConsoleFormatter;

import java.util.concurrent.CountDownLatch;

/**
 * Main entry point for the JMX & MBean Study Suite.
 *
 * Demonstrates:
 * 1. Defining Standard MBeans (AppMetricsMBean) and MXBeans (ConnectionPoolMXBean)
 * 2. Registering MBeans into the Java Platform MBeanServer
 * 3. Exposing MBeans remotely over RMI (port 9999) for OpenTelemetry Scraping
 * 4. Continuous background traffic simulation with live attribute updates
 */
public class JmxMain {

    public static void main(String[] args) {
        printBanner();

        AppMetrics appMetrics = new AppMetrics();
        ConnectionPool connectionPool = new ConnectionPool(20);

        try (JmxServerManager jmxManager = new JmxServerManager();
             JmxTrafficSimulator simulator = new JmxTrafficSimulator(appMetrics, connectionPool)) {

            // 1. Register MBeans with Platform MBeanServer
            ConsoleFormatter.printHeader("STEP 1: Registering MBeans in Platform MBeanServer");
            jmxManager.registerMBeans(appMetrics, connectionPool);

            for (var name : jmxManager.getRegisteredNames()) {
                ConsoleFormatter.printSuccess("Registered MBean: " + name);
            }

            // 2. Enable JMX Remote Connector (RMI)
            ConsoleFormatter.printHeader("STEP 2: Enabling JMX Remote Connectivity");
            int jmxPort = JmxServerManager.DEFAULT_JMX_PORT;
            jmxManager.startJmxRemoteServer(jmxPort);

            String serviceUrl = "service:jmx:rmi:///jndi/rmi://localhost:" + jmxPort + "/jmxrmi";
            ConsoleFormatter.printInfo("JMX Service URL", serviceUrl);
            ConsoleFormatter.printInfo("JMX RMI Port", jmxPort);
            ConsoleFormatter.printInfo("Target Systems", "jvm, com.example.otel:*");
            ConsoleFormatter.printExplanation(
                    "OpenTelemetry Collector (or opentelemetry-jmx-scraper) can now attach to\n" +
                    "  " + serviceUrl + "\n" +
                    "  and collect all MBean attributes into OpenTelemetry metric instruments!"
            );

            // 3. Start Background Traffic Simulation
            ConsoleFormatter.printHeader("STEP 3: Starting Real-Time Workload Simulator");
            simulator.start();
            ConsoleFormatter.printSuccess("Traffic simulator active: generating orders & connection pool activity.");

            // 4. Live Display Loop
            ConsoleFormatter.printHeader("STEP 4: Live MBean Attribute Monitor (Press Ctrl+C to Stop)");
            ConsoleFormatter.printExplanation("Notice the values updating dynamically below. The OTel JMX Scraper scrapes these live values.");

            int durationSeconds = 0; // 0 = infinite until Ctrl+C
            if (args.length > 0) {
                try {
                    durationSeconds = Integer.parseInt(args[0].trim());
                } catch (NumberFormatException ignored) {}
            }

            runLiveTicker(appMetrics, connectionPool, durationSeconds);

        } catch (Exception e) {
            System.err.println(ConsoleFormatter.RED + "Error running JMX Demo: " + e.getMessage() + ConsoleFormatter.RESET);
            e.printStackTrace();
        }
    }

    private static void runLiveTicker(AppMetrics appMetrics, ConnectionPool pool, int maxSeconds) {
        long startTime = System.currentTimeMillis();

        CountDownLatch shutdownLatch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n" + ConsoleFormatter.YELLOW + "Shutting down JMX Demo gracefully..." + ConsoleFormatter.RESET);
            shutdownLatch.countDown();
        }));

        while (shutdownLatch.getCount() > 0) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            long elapsedSec = (System.currentTimeMillis() - startTime) / 1000;

            System.out.printf(
                    "%s[%02ds elapsed]%s Orders: %s%d%s | Latency: %s%.1f ms%s | Active Conns: %s%d%s | Queue: %s%d%s | DB Pool: %s%d/%d%s | Status: %s%s%s%n",
                    ConsoleFormatter.GRAY, elapsedSec, ConsoleFormatter.RESET,
                    ConsoleFormatter.CYAN, appMetrics.getProcessedOrdersCount(), ConsoleFormatter.RESET,
                    ConsoleFormatter.YELLOW, appMetrics.getAverageResponseTimeMs(), ConsoleFormatter.RESET,
                    ConsoleFormatter.PURPLE, appMetrics.getActiveConnections(), ConsoleFormatter.RESET,
                    ConsoleFormatter.BLUE, appMetrics.getPendingQueueDepth(), ConsoleFormatter.RESET,
                    ConsoleFormatter.GREEN, pool.getActiveConnections(), pool.getMaxPoolSize(), ConsoleFormatter.RESET,
                    ConsoleFormatter.BOLD, appMetrics.getSystemHealthStatus(), ConsoleFormatter.RESET
            );

            if (maxSeconds > 0 && elapsedSec >= maxSeconds) {
                System.out.println(ConsoleFormatter.GREEN + "\nSpecified runtime of " + maxSeconds + "s completed." + ConsoleFormatter.RESET);
                break;
            }
        }
    }

    private static void printBanner() {
        System.out.println(ConsoleFormatter.GREEN + ConsoleFormatter.BOLD);
        System.out.println("  ___  __  ____  __   ___                  _ _             ");
        System.out.println(" |_  |/  \\/ ___|/ /  / _ \\ _ __   ___ _ __| | | ___   __ _ ");
        System.out.println("   | | | | \\___ \\/ /  | | | | '_ \\ / _ \\ '_ \\ | |/ _ \\ / _` |");
        System.out.println("/\\_| | |_| |___) / /  | |_| | |_) |  __/ | | | | | (_) | (_| |");
        System.out.println("\\____/\\__/|____/_/    \\___/| .__/ \\___|_| |_|_|_|\\___/ \\__, |");
        System.out.println("                           |_|                         |___/ ");
        System.out.println("         JMX MBean & OpenTelemetry Collector Scraping Suite  ");
        System.out.println(ConsoleFormatter.RESET);
    }
}
