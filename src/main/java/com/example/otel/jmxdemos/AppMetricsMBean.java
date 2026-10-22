package com.example.otel.jmxdemos;

/**
 * Standard MBean interface exposing application runtime and business metrics via JMX.
 *
 * OpenTelemetry JMX Scraper and other JMX clients can query these attributes
 * and map them to OpenTelemetry Counter, UpDownCounter, and Gauge metrics.
 */
public interface AppMetricsMBean {

    /**
     * Monotonically increasing counter of total orders processed.
     * Maps to an OpenTelemetry Counter instrument.
     */
    long getProcessedOrdersCount();

    /**
     * Gauge / UpDownCounter tracking current active connections.
     * Can increase and decrease dynamically.
     */
    int getActiveConnections();

    /**
     * Gauge tracking current pending work queue depth.
     */
    int getPendingQueueDepth();

    /**
     * Gauge tracking moving average response latency in milliseconds.
     */
    double getAverageResponseTimeMs();

    /**
     * String attribute reporting the overall health status ("HEALTHY", "DEGRADED", "CRITICAL").
     */
    String getSystemHealthStatus();

    /**
     * Writable attribute: Maintenance mode flag.
     */
    boolean isMaintenanceMode();

    /**
     * Set maintenance mode on or off.
     */
    void setMaintenanceMode(boolean maintenanceMode);

    /**
     * JMX Operation: Process an order with given details and latency.
     */
    void processOrder(String orderId, double amount, double latencyMs);

    /**
     * JMX Operation: Reset internal statistical counters.
     */
    void resetStatistics();

    /**
     * JMX Operation: Run a health check and return a descriptive status string.
     */
    String healthCheck();
}
