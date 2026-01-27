package interfaces.comms.service;

import interfaces.comms.model.SmtpConnectionInfo;
import jakarta.mail.Session;
import jakarta.mail.Transport;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages a cache of SMTP connections with automatic cleanup.
 * Maintains up to a configurable number of connections (default 50).
 * Automatically closes idle connections after 5 minutes of inactivity.
 */
@Singleton
@Startup
public class SmtpConnectionCacheService {

    private static final Logger logger = Logger.getLogger(SmtpConnectionCacheService.class.getName());
    
    private static final int DEFAULT_MAX_CONNECTIONS = 50;
    private static final int DEFAULT_MAX_POOL_SIZE = 100;
    private static final long IDLE_TIMEOUT_SECONDS = 300; // 5 minutes
    
    private final Map<String, SmtpConnectionInfo> connectionCache = new ConcurrentHashMap<>();
    private int maxConnections = DEFAULT_MAX_CONNECTIONS;
    private int maxPoolSize = DEFAULT_MAX_POOL_SIZE;
    
    @PostConstruct
    public void init() {
        // Read system properties
        maxConnections = getSystemPropertyInt("email-sender.maxConnections", DEFAULT_MAX_CONNECTIONS);
        maxPoolSize = getSystemPropertyInt("email-sender.maxPoolSize", DEFAULT_MAX_POOL_SIZE);
        
        logger.info("SmtpConnectionCacheService initialized with maxConnections: " + maxConnections + 
                    ", maxPoolSize: " + maxPoolSize);
    }
    
    /**
     * Reads an integer system property with a default value.
     */
    private int getSystemPropertyInt(String propertyName, int defaultValue) {
        try {
            String value = System.getProperty(propertyName);
            if (value != null && !value.trim().isEmpty()) {
                return Integer.parseInt(value.trim());
            }
        } catch (NumberFormatException e) {
            logger.warning("Invalid value for system property " + propertyName + ", using default: " + defaultValue);
        }
        return defaultValue;
    }
    
    @PreDestroy
    public void cleanup() {
        logger.info("Closing all cached SMTP connections");
        connectionCache.values().forEach(SmtpConnectionInfo::close);
        connectionCache.clear();
    }
    
    /**
     * Gets or creates a cached SMTP connection.
     * 
     * @param host SMTP server host
     * @param username Username for authentication
     * @param password Password for authentication
     * @param props Connection properties
     * @return SmtpConnectionInfo object containing the connection
     * @throws Exception if connection fails
     */
    public SmtpConnectionInfo getOrCreateConnection(String host, String username, String password, Properties props) throws Exception {
        String key = SmtpConnectionInfo.generateKey(host, username);
        
        // Try to get and update existing connection atomically
        SmtpConnectionInfo existingInfo = connectionCache.computeIfPresent(key, (k, info) -> {
            if (info.isConnected()) {
                info.updateLastUsed();
                return info;
            } else {
                // Connection is stale, will be replaced
                info.close();
                return null; // Remove from cache
            }
        });
        
        if (existingInfo != null) {
            logger.fine("Reusing cached SMTP connection for: " + key);
            return existingInfo;
        }
        
        // Check if we need to evict connections
        if (connectionCache.size() >= maxConnections) {
            evictOldestConnection();
        }
        
        // Create new connection
        Session session = Session.getInstance(props, null);
        Transport transport = session.getTransport("smtps");
        transport.connect(host, username, password);
        
        SmtpConnectionInfo newInfo = new SmtpConnectionInfo(host, username, transport);
        connectionCache.put(key, newInfo);
        
        logger.info("Created new cached SMTP connection for: " + key + " (total: " + connectionCache.size() + ")");
        return newInfo;
    }
    
    /**
     * Gets an existing cached SMTP connection without creating a new one.
     * 
     * @param host SMTP server host
     * @param username Username for authentication
     * @return SmtpConnectionInfo object containing the connection, or null if not found
     */
    public SmtpConnectionInfo getExistingConnection(String host, String username) {
        String key = SmtpConnectionInfo.generateKey(host, username);
        
        // Try to get and update existing connection atomically
        SmtpConnectionInfo existingInfo = connectionCache.computeIfPresent(key, (k, info) -> {
            if (info.isConnected()) {
                info.updateLastUsed();
                return info;
            } else {
                // Connection is stale, remove it
                info.close();
                return null;
            }
        });
        
        if (existingInfo != null) {
            logger.fine("Found cached SMTP connection for: " + key);
        } else {
            logger.fine("No cached SMTP connection found for: " + key);
        }
        
        return existingInfo;
    }
    
    /**
     * Gets all active connection information.
     * 
     * @return List of all cached connections
     */
    public List<Map<String, Object>> getAllConnections() {
        List<Map<String, Object>> connections = new ArrayList<>();
        
        for (SmtpConnectionInfo info : connectionCache.values()) {
            Map<String, Object> connData = new HashMap<>();
            connData.put("host", info.getHost());
            connData.put("username", info.getUsername());
            connData.put("connected", info.isConnected());
            connData.put("createdTime", info.getCreatedTime().toString());
            connData.put("lastUsedTime", info.getLastUsedTime().toString());
            connData.put("idleTimeSeconds", info.getIdleTimeSeconds());
            connData.put("usageCountLastDay", info.getUsageCountLastDay());
            
            connections.add(connData);
        }
        
        return connections;
    }
    
    /**
     * Gets cache statistics.
     * 
     * @return Map containing cache statistics
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalConnections", connectionCache.size());
        stats.put("maxConnections", maxConnections);
        stats.put("activeConnections", connectionCache.values().stream()
                .filter(SmtpConnectionInfo::isConnected)
                .count());
        
        return stats;
    }
    
    /**
     * Scheduled task to clean up idle connections.
     * Runs every minute to check for connections idle for more than 5 minutes.
     */
    @Schedule(hour = "*", minute = "*", second = "0", persistent = false)
    public void cleanupIdleConnections() {
        java.util.concurrent.atomic.AtomicInteger removedCount = new java.util.concurrent.atomic.AtomicInteger(0);
        
        // Use iterator for safe removal during iteration
        for (String key : new ArrayList<>(connectionCache.keySet())) {
            connectionCache.computeIfPresent(key, (k, info) -> {
                long idleTime = info.getIdleTimeSeconds();
                if (idleTime > IDLE_TIMEOUT_SECONDS) {
                    logger.info("Closing idle SMTP connection: " + k + " (idle for " + idleTime + " seconds)");
                    info.close();
                    removedCount.incrementAndGet();
                    return null; // Remove from cache
                }
                return info; // Keep in cache
            });
        }
        
        int removed = removedCount.get();
        if (removed > 0) {
            logger.info("Cleaned up " + removed + " idle SMTP connections");
        }
    }
    
    /**
     * Evicts the oldest connection when cache is full.
     */
    private void evictOldestConnection() {
        SmtpConnectionInfo oldest = connectionCache.values().stream()
                .min(Comparator.comparing(SmtpConnectionInfo::getLastUsedTime))
                .orElse(null);
        
        if (oldest != null) {
            String key = oldest.getConnectionKey();
            logger.info("Evicting oldest SMTP connection: " + key);
            oldest.close();
            connectionCache.remove(key);
        }
    }
    
    /**
     * Closes a specific cached connection.
     * 
     * @param host SMTP server host
     * @param username Username
     * @return true if connection was found and closed, false otherwise
     */
    public boolean closeConnection(String host, String username) {
        String key = SmtpConnectionInfo.generateKey(host, username);
        
        SmtpConnectionInfo info = connectionCache.remove(key);
        if (info != null) {
            info.close();
            logger.info("Manually closed SMTP connection: " + key);
            return true;
        }
        
        logger.fine("SMTP connection not found for closing: " + key);
        return false;
    }
    
    /**
     * Sets the maximum number of connections in the cache.
     * 
     * @param maxConnections Maximum number of connections (default 50)
     */
    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
        logger.info("Max SMTP connections set to: " + maxConnections);
    }
    
    /**
     * Gets the maximum number of connections.
     * 
     * @return Maximum number of connections
     */
    public int getMaxConnections() {
        return maxConnections;
    }
    
    /**
     * Reconnects an existing SMTP connection.
     * Closes the existing connection and creates a new one.
     * 
     * @param host SMTP server host
     * @param username Username for authentication
     * @param password Password for authentication
     * @param props Connection properties
     * @return SmtpConnectionInfo object containing the new connection
     * @throws Exception if reconnection fails
     */
    public SmtpConnectionInfo reconnect(String host, String username, String password, Properties props) throws Exception {
        String key = SmtpConnectionInfo.generateKey(host, username);
        
        // Close existing connection
        SmtpConnectionInfo oldInfo = connectionCache.remove(key);
        if (oldInfo != null) {
            oldInfo.close();
            logger.info("Closed existing SMTP connection for reconnection: " + key);
        }
        
        // Create new connection
        Session session = Session.getInstance(props, null);
        Transport transport = session.getTransport("smtps");
        transport.connect(host, username, password);
        
        SmtpConnectionInfo newInfo = new SmtpConnectionInfo(host, username, transport);
        connectionCache.put(key, newInfo);
        
        logger.info("Reconnected SMTP connection: " + key + " (total: " + connectionCache.size() + ")");
        return newInfo;
    }
}
