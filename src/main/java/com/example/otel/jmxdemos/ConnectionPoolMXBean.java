package com.example.otel.jmxdemos;

/**
 * MXBean interface for a Database Connection Pool.
 *
 * MXBeans (unlike standard MBeans) convert data types to Open MBean types
 * automatically, making them widely accessible across different JMX tooling.
 */
public interface ConnectionPoolMXBean {

    /**
     * Number of database connections currently checked out / in active use.
     */
    int getActiveConnections();

    /**
     * Number of database connections currently sitting idle in the pool.
     */
    int getIdleConnections();

    /**
     * Total maximum capacity configured for the pool.
     */
    int getMaxPoolSize();

    /**
     * Cumulative count of connections acquired since startup.
     */
    long getTotalAcquiredCount();

    /**
     * JMX Operation: Acquire a connection simulation.
     */
    boolean acquireConnection();

    /**
     * JMX Operation: Release a connection simulation.
     */
    void releaseConnection();
}
