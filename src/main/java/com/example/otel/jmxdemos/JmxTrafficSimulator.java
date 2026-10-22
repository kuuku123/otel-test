package com.example.otel.jmxdemos;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Background traffic generator that simulates realistic application workload.
 * Dynamically updates MBean values so that JMX metric scrapers observe live changing metrics.
 */
public class JmxTrafficSimulator implements AutoCloseable {

    private final AppMetrics appMetrics;
    private final ConnectionPool connectionPool;
    private final ScheduledExecutorService scheduler;
    private final Random random = new Random();
    private volatile boolean running = false;

    public JmxTrafficSimulator(AppMetrics appMetrics, ConnectionPool connectionPool) {
        this.appMetrics = appMetrics;
        this.connectionPool = connectionPool;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "jmx-traffic-simulator");
            t.setDaemon(true);
            return t;
        });
    }

    public synchronized void start() {
        if (running) return;
        running = true;

        scheduler.scheduleAtFixedRate(this::tick, 500, 1000, TimeUnit.MILLISECONDS);
    }

    private void tick() {
        try {
            // 1. Simulate batch of 1 to 5 orders
            int batchSize = 1 + random.nextInt(5);
            for (int i = 0; i < batchSize; i++) {
                // Simulate order latency (40ms - 220ms, occasional spike)
                double latency = 40.0 + random.nextDouble() * 180.0;
                if (random.nextDouble() < 0.05) {
                    latency += 300.0; // occasional latency spike
                }
                appMetrics.recordOrder(latency);
            }

            // 2. Fluctuate active client connections (between 4 and 25)
            int activeConns = 4 + random.nextInt(22);
            appMetrics.setActiveConnections(activeConns);

            // 3. Fluctuate queue depth (between 0 and 35)
            int queueDepth = random.nextInt(36);
            appMetrics.setQueueDepth(queueDepth);

            // 4. Simulate database connection pool usage
            int poolActive = 2 + random.nextInt(connectionPool.getMaxPoolSize() - 3);
            connectionPool.setActiveConnections(poolActive);
            connectionPool.acquireConnection();
            connectionPool.releaseConnection();

        } catch (Exception ignored) {
        }
    }

    @Override
    public synchronized void close() {
        running = false;
        scheduler.shutdownNow();
    }
}
