package com.example.otel;

import com.example.otel.jmxdemos.AppMetrics;
import com.example.otel.jmxdemos.ConnectionPool;
import com.example.otel.jmxdemos.JmxServerManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests verifying JMX MBean and MXBean behavior and MBeanServer registration.
 */
class JmxMBeanTest {

    @Test
    @DisplayName("Verify AppMetricsMBean attributes and operations via MBeanServer")
    void testAppMetricsViaMBeanServer() throws Exception {
        AppMetrics metrics = new AppMetrics();
        ConnectionPool pool = new ConnectionPool(10);

        try (JmxServerManager manager = new JmxServerManager()) {
            manager.registerMBeans(metrics, pool);
            MBeanServer mbs = manager.getMBeanServer();

            ObjectName appName = new ObjectName(JmxServerManager.APP_METRICS_OBJECT_NAME);
            assertThat(mbs.isRegistered(appName)).isTrue();

            // Initial state
            assertThat(mbs.getAttribute(appName, "ProcessedOrdersCount")).isEqualTo(0L);
            assertThat(mbs.getAttribute(appName, "SystemHealthStatus")).isEqualTo("HEALTHY");

            // Process orders
            metrics.recordOrder(120.0);
            metrics.recordOrder(80.0);
            assertThat(mbs.getAttribute(appName, "ProcessedOrdersCount")).isEqualTo(2L);
            assertThat(mbs.getAttribute(appName, "AverageResponseTimeMs")).isEqualTo(100.0);

            // Invoke JMX operation
            mbs.invoke(appName, "processOrder",
                    new Object[]{"ORDER-999", 50.0, 100.0},
                    new String[]{"java.lang.String", "double", "double"});
            assertThat(mbs.getAttribute(appName, "ProcessedOrdersCount")).isEqualTo(3L);

            // Writable attribute
            mbs.setAttribute(appName, new javax.management.Attribute("MaintenanceMode", true));
            assertThat(mbs.getAttribute(appName, "MaintenanceMode")).isEqualTo(true);
            assertThat(mbs.getAttribute(appName, "SystemHealthStatus")).isEqualTo("MAINTENANCE");

            // Reset
            mbs.invoke(appName, "resetStatistics", new Object[]{}, new String[]{});
            assertThat(mbs.getAttribute(appName, "ProcessedOrdersCount")).isEqualTo(0L);
        }
    }

    @Test
    @DisplayName("Verify ConnectionPoolMXBean attributes and operations via MBeanServer")
    void testConnectionPoolViaMBeanServer() throws Exception {
        AppMetrics metrics = new AppMetrics();
        ConnectionPool pool = new ConnectionPool(15);

        try (JmxServerManager manager = new JmxServerManager()) {
            manager.registerMBeans(metrics, pool);
            MBeanServer mbs = manager.getMBeanServer();

            ObjectName poolName = new ObjectName(JmxServerManager.CONNECTION_POOL_OBJECT_NAME);
            assertThat(mbs.isRegistered(poolName)).isTrue();

            assertThat(mbs.getAttribute(poolName, "MaxPoolSize")).isEqualTo(15);
            assertThat(mbs.getAttribute(poolName, "ActiveConnections")).isEqualTo(3);
            assertThat(mbs.getAttribute(poolName, "IdleConnections")).isEqualTo(12);

            // Acquire connection operation
            Boolean acquired = (Boolean) mbs.invoke(poolName, "acquireConnection", new Object[]{}, new String[]{});
            assertThat(acquired).isTrue();
            assertThat(mbs.getAttribute(poolName, "ActiveConnections")).isEqualTo(4);
            assertThat(mbs.getAttribute(poolName, "TotalAcquiredCount")).isEqualTo(1L);

            // Release connection operation
            mbs.invoke(poolName, "releaseConnection", new Object[]{}, new String[]{});
            assertThat(mbs.getAttribute(poolName, "ActiveConnections")).isEqualTo(3);
        }
    }
}
