package com.example.otel.jmxdemos;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation of ConnectionPoolMXBean.
 */
public class ConnectionPool implements ConnectionPoolMXBean {

    private final int maxPoolSize;
    private final AtomicInteger activeConnections = new AtomicInteger(3);
    private final AtomicLong totalAcquired = new AtomicLong(0);

    public ConnectionPool(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    @Override
    public int getActiveConnections() {
        return activeConnections.get();
    }

    @Override
    public int getIdleConnections() {
        return Math.max(0, maxPoolSize - activeConnections.get());
    }

    @Override
    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    @Override
    public long getTotalAcquiredCount() {
        return totalAcquired.get();
    }

    @Override
    public boolean acquireConnection() {
        while (true) {
            int current = activeConnections.get();
            if (current >= maxPoolSize) {
                return false;
            }
            if (activeConnections.compareAndSet(current, current + 1)) {
                totalAcquired.incrementAndGet();
                return true;
            }
        }
    }

    @Override
    public void releaseConnection() {
        while (true) {
            int current = activeConnections.get();
            if (current <= 0) {
                return;
            }
            if (activeConnections.compareAndSet(current, current - 1)) {
                return;
            }
        }
    }

    public void setActiveConnections(int count) {
        int bounded = Math.max(0, Math.min(maxPoolSize, count));
        activeConnections.set(bounded);
    }
}
