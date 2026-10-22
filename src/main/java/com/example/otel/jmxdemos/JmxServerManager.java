package com.example.otel.jmxdemos;

import com.example.otel.util.ConsoleFormatter;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages MBeanServer registration and JMX Remote Connector (RMI).
 *
 * Exposes MBeans locally and remotely so that external agents (such as the
 * OpenTelemetry JMX Scraper or JMX metrics gatherer) can connect and scrape metrics.
 */
public class JmxServerManager implements AutoCloseable {

    public static final int DEFAULT_JMX_PORT = 9999;
    public static final String APP_METRICS_OBJECT_NAME = "com.example.otel:type=AppMetrics,name=OrderService";
    public static final String CONNECTION_POOL_OBJECT_NAME = "com.example.otel:type=ConnectionPool,name=DatabasePool";

    private final MBeanServer mBeanServer;
    private final List<ObjectName> registeredNames = new ArrayList<>();
    private JMXConnectorServer connectorServer;
    private int jmxPort;
    private String serviceUrl;

    public JmxServerManager() {
        this.mBeanServer = ManagementFactory.getPlatformMBeanServer();
    }

    /**
     * Registers application MBeans with the Platform MBeanServer.
     */
    public void registerMBeans(AppMetrics appMetrics, ConnectionPool connectionPool) throws Exception {
        ObjectName appMetricsName = new ObjectName(APP_METRICS_OBJECT_NAME);
        if (mBeanServer.isRegistered(appMetricsName)) {
            mBeanServer.unregisterMBean(appMetricsName);
        }
        mBeanServer.registerMBean(appMetrics, appMetricsName);
        registeredNames.add(appMetricsName);

        ObjectName poolName = new ObjectName(CONNECTION_POOL_OBJECT_NAME);
        if (mBeanServer.isRegistered(poolName)) {
            mBeanServer.unregisterMBean(poolName);
        }
        mBeanServer.registerMBean(connectionPool, poolName);
        registeredNames.add(poolName);
    }

    /**
     * Starts the JMX Remote Connector on the specified port if not already started via JVM arguments.
     * Service URL: service:jmx:rmi:///jndi/rmi://localhost:{port}/jmxrmi
     */
    public synchronized void startJmxRemoteServer(int port) {
        this.jmxPort = port;
        this.serviceUrl = "service:jmx:rmi:///jndi/rmi://localhost:" + port + "/jmxrmi";

        // Check if remote JMX was already activated via JVM command-line flags
        if (System.getProperty("com.sun.management.jmxremote.port") != null ||
            "true".equalsIgnoreCase(System.getProperty("com.sun.management.jmxremote"))) {
            ConsoleFormatter.printSuccess("JMX Remote Connector active via JVM arguments on port " + port);
            return;
        }

        try {
            // Check if RMI registry already exists on port
            try {
                LocateRegistry.createRegistry(port);
            } catch (Exception e) {
                // If already created (e.g. by JVM -Dcom.sun.management.jmxremote), locate it
                LocateRegistry.getRegistry(port);
            }

            JMXServiceURL url = new JMXServiceURL(serviceUrl);
            connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(url, null, mBeanServer);
            connectorServer.start();
            ConsoleFormatter.printSuccess("Programmatic JMX Remote Connector started on port " + port);
        } catch (IOException e) {
            // If JVM flags (-Dcom.sun.management.jmxremote.port=9999) already bound the port, that's fine
            ConsoleFormatter.printExplanation(
                    "JMX remote port " + port + " is active (via JVM flags or running connector): " + e.getMessage()
            );
        }
    }

    public MBeanServer getMBeanServer() {
        return mBeanServer;
    }

    public List<ObjectName> getRegisteredNames() {
        return registeredNames;
    }

    public String getServiceUrl() {
        return serviceUrl;
    }

    public int getJmxPort() {
        return jmxPort;
    }

    @Override
    public void close() {
        if (connectorServer != null) {
            try {
                connectorServer.stop();
            } catch (IOException ignored) {}
        }
        for (ObjectName name : registeredNames) {
            try {
                if (mBeanServer.isRegistered(name)) {
                    mBeanServer.unregisterMBean(name);
                }
            } catch (Exception ignored) {}
        }
    }
}
