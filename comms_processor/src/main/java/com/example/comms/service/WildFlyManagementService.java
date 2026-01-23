package com.example.comms.service;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Service to interact with WildFly management API and retrieve information
 * about running services.
 */
@Singleton
@Startup
public class WildFlyManagementService {

    private static final Logger logger = Logger.getLogger(WildFlyManagementService.class.getName());
    
    private MBeanServer mbeanServer;

    @PostConstruct
    public void init() {
        mbeanServer = ManagementFactory.getPlatformMBeanServer();
        logger.info("WildFlyManagementService initialized");
    }

    /**
     * Retrieves a list of running services on the WildFly server.
     * This includes deployments, subsystems, and other managed resources.
     * 
     * @return List of service information maps
     */
    public List<Map<String, Object>> getRunningServices() {
        List<Map<String, Object>> services = new ArrayList<>();
        
        try {
            // Get all MBeans (these represent various services and components)
            Set<ObjectInstance> mBeans = mbeanServer.queryMBeans(null, null);
            
            // Filter and collect WildFly-specific services
            for (ObjectInstance mBean : mBeans) {
                ObjectName objectName = mBean.getObjectName();
                String domain = objectName.getDomain();
                
                // Focus on WildFly/JBoss specific services and deployments
                if (isRelevantService(domain)) {
                    Map<String, Object> serviceInfo = new HashMap<>();
                    serviceInfo.put("name", objectName.getKeyProperty("name"));
                    serviceInfo.put("type", objectName.getKeyProperty("type"));
                    serviceInfo.put("domain", domain);
                    serviceInfo.put("objectName", objectName.toString());
                    
                    // Try to get additional attributes if available
                    try {
                        String state = getServiceState(objectName);
                        if (state != null) {
                            serviceInfo.put("state", state);
                        }
                    } catch (Exception e) {
                        // Ignore if we can't get state - not all MBeans have this attribute
                    }
                    
                    services.add(serviceInfo);
                }
            }
            
            // Add runtime information
            addRuntimeInfo(services);
            
            logger.info("Retrieved " + services.size() + " services");
            
        } catch (Exception e) {
            logger.severe("Error retrieving services: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve services", e);
        }
        
        return services;
    }

    /**
     * Checks if a domain is relevant for our service listing.
     */
    private boolean isRelevantService(String domain) {
        return domain != null && (
            domain.startsWith("jboss") ||
            domain.startsWith("org.wildfly") ||
            domain.equals("java.lang") ||
            domain.equals("Catalina") ||
            domain.contains("deployment")
        );
    }

    /**
     * Attempts to get the state of a service from its MBean.
     */
    private String getServiceState(ObjectName objectName) {
        try {
            Object state = mbeanServer.getAttribute(objectName, "State");
            return state != null ? state.toString() : "UNKNOWN";
        } catch (Exception e) {
            // Not all MBeans have a State attribute
            try {
                Object status = mbeanServer.getAttribute(objectName, "status");
                return status != null ? status.toString() : null;
            } catch (Exception e2) {
                return null;
            }
        }
    }

    /**
     * Adds runtime information about the server itself.
     */
    private void addRuntimeInfo(List<Map<String, Object>> services) {
        // Add Java Runtime info
        Map<String, Object> javaRuntime = new HashMap<>();
        javaRuntime.put("name", "Java Runtime");
        javaRuntime.put("type", "Runtime");
        javaRuntime.put("domain", "system");
        javaRuntime.put("version", System.getProperty("java.version"));
        javaRuntime.put("vendor", System.getProperty("java.vendor"));
        javaRuntime.put("state", "RUNNING");
        services.add(javaRuntime);

        // Add WildFly Application Server info
        Map<String, Object> appServer = new HashMap<>();
        appServer.put("name", "WildFly Application Server");
        appServer.put("type", "ApplicationServer");
        appServer.put("domain", "system");
        appServer.put("state", "RUNNING");
        services.add(appServer);
    }

    /**
     * Retrieves a list of all available datasources configured in WildFly.
     * This includes datasources registered in the JBoss/WildFly subsystem.
     * 
     * @return List of datasource information maps
     */
    public List<Map<String, Object>> getDatasources() {
        List<Map<String, Object>> datasources = new ArrayList<>();
        
        try {
            // Query for JBoss datasource MBeans
            ObjectName datasourceQuery = new ObjectName("jboss.as:subsystem=datasources,data-source=*");
            Set<ObjectInstance> datasourceMBeans = mbeanServer.queryMBeans(datasourceQuery, null);
            
            for (ObjectInstance mBean : datasourceMBeans) {
                ObjectName objectName = mBean.getObjectName();
                Map<String, Object> datasourceInfo = new HashMap<>();
                
                // Get datasource name from the ObjectName
                String dsName = objectName.getKeyProperty("data-source");
                if (dsName != null) {
                    datasourceInfo.put("name", dsName);
                    datasourceInfo.put("objectName", objectName.toString());
                    
                    // Try to get additional datasource attributes
                    try {
                        Object jndiName = mbeanServer.getAttribute(objectName, "jndi-name");
                        if (jndiName != null) {
                            datasourceInfo.put("jndiName", jndiName.toString());
                        }
                    } catch (Exception e) {
                        // Attribute might not be available
                    }
                    
                    try {
                        Object enabled = mbeanServer.getAttribute(objectName, "enabled");
                        if (enabled != null) {
                            datasourceInfo.put("enabled", enabled);
                        }
                    } catch (Exception e) {
                        // Attribute might not be available
                    }
                    
                    try {
                        Object driverName = mbeanServer.getAttribute(objectName, "driver-name");
                        if (driverName != null) {
                            datasourceInfo.put("driverName", driverName.toString());
                        }
                    } catch (Exception e) {
                        // Attribute might not be available
                    }
                    
                    try {
                        Object connectionUrl = mbeanServer.getAttribute(objectName, "connection-url");
                        if (connectionUrl != null) {
                            datasourceInfo.put("connectionUrl", connectionUrl.toString());
                        }
                    } catch (Exception e) {
                        // Attribute might not be available
                    }
                    
                    datasources.add(datasourceInfo);
                }
            }
            
            // Also check for XA datasources
            ObjectName xaDatasourceQuery = new ObjectName("jboss.as:subsystem=datasources,xa-data-source=*");
            Set<ObjectInstance> xaDatasourceMBeans = mbeanServer.queryMBeans(xaDatasourceQuery, null);
            
            for (ObjectInstance mBean : xaDatasourceMBeans) {
                ObjectName objectName = mBean.getObjectName();
                Map<String, Object> datasourceInfo = new HashMap<>();
                
                String dsName = objectName.getKeyProperty("xa-data-source");
                if (dsName != null) {
                    datasourceInfo.put("name", dsName);
                    datasourceInfo.put("type", "XA");
                    datasourceInfo.put("objectName", objectName.toString());
                    
                    try {
                        Object jndiName = mbeanServer.getAttribute(objectName, "jndi-name");
                        if (jndiName != null) {
                            datasourceInfo.put("jndiName", jndiName.toString());
                        }
                    } catch (Exception e) {
                        // Attribute might not be available
                    }
                    
                    try {
                        Object enabled = mbeanServer.getAttribute(objectName, "enabled");
                        if (enabled != null) {
                            datasourceInfo.put("enabled", enabled);
                        }
                    } catch (Exception e) {
                        // Attribute might not be available
                    }
                    
                    datasources.add(datasourceInfo);
                }
            }
            
            logger.info("Retrieved " + datasources.size() + " datasources");
            
        } catch (Exception e) {
            logger.severe("Error retrieving datasources: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve datasources", e);
        }
        
        return datasources;
    }
}
