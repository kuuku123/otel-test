package com.example.otel.jmxdemos;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * Implementation of AppMetricsMBean.
 * Provides thread-safe recording of application metrics exposed over JMX.
 */
public class AppMetrics implements AppMetricsMBean {

    private final AtomicLong processedOrders = new AtomicLong(0);
    private final AtomicInteger activeConnections = new AtomicInteger(5);
    private final AtomicInteger queueDepth = new AtomicInteger(0);
    private final DoubleAdder totalLatency = new DoubleAdder();
    private final AtomicLong latencyCount = new AtomicLong(0);
    private final AtomicBoolean maintenanceMode = new AtomicBoolean(false);

    @Override
    public long getProcessedOrdersCount() {
        return processedOrders.get();
    }

    @Override
    public int getActiveConnections() {
        return activeConnections.get();
    }

    @Override
    public int getPendingQueueDepth() {
        return queueDepth.get();
    }

    @Override
    public double getAverageResponseTimeMs() {
        long count = latencyCount.get();
        if (count == 0) {
            return 0.0;
        }
        return Math.round((totalLatency.sum() / count) * 100.0) / 100.0;
    }

    @Override
    public String getSystemHealthStatus() {
        if (maintenanceMode.get()) {
            return "MAINTENANCE";
        }
        int queue = queueDepth.get();
        double latency = getAverageResponseTimeMs();
        if (queue > 80 || latency > 500) {
            return "CRITICAL";
        } else if (queue > 30 || latency > 200) {
            return "DEGRADED";
        }
        return "HEALTHY";
    }

    @Override
    public boolean isMaintenanceMode() {
        return maintenanceMode.get();
    }

    @Override
    public void setMaintenanceMode(boolean maintenanceMode) {
        this.maintenanceMode.set(maintenanceMode);
    }

    @Override
    public void processOrder(String orderId, double amount, double latencyMs) {
        processedOrders.incrementAndGet();
        totalLatency.add(latencyMs);
        latencyCount.incrementAndGet();
    }

    @Override
    public void resetStatistics() {
        processedOrders.set(0);
        latencyCount.set(0);
        totalLatency.reset();
    }

    @Override
    public String healthCheck() {
        return String.format("System Status: %s | Processed Orders: %d | Active Connections: %d | Queue Depth: %d | Avg Latency: %.2f ms",
                getSystemHealthStatus(),
                getProcessedOrdersCount(),
                getActiveConnections(),
                getPendingQueueDepth(),
                getAverageResponseTimeMs());
    }

    // Helper methods for simulated activity
    public void recordOrder(double latencyMs) {
        processedOrders.incrementAndGet();
        totalLatency.add(latencyMs);
        latencyCount.incrementAndGet();
    }

    public void setActiveConnections(int count) {
        activeConnections.set(Math.max(0, count));
    }

    public void setQueueDepth(int depth) {
        queueDepth.set(Math.max(0, depth));
    }
}
