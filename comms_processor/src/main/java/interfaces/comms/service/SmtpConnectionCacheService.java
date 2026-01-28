package interfaces.comms.service;

import interfaces.comms.model.SmtpConnectionInfo;
import jakarta.mail.Session;
import jakarta.mail.Transport;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages a cache of SMTP connections with automatic cleanup and a ready pool for connection recycling.
 * Maintains up to a configurable number of connections (default 50).
 * Automatically closes idle connections after 5 minutes of inactivity.
 * When connections are closed, they're moved to a ready pool for potential reuse.
 */
@Singleton
@Startup
public class SmtpConnectionCacheService {

    private static final Logger logger = Logger.getLogger(SmtpConnectionCacheService.class.getName());
    
    private static final int DEFAULT_MAX_CONNECTIONS = 50;
    private static final int DEFAULT_MAX_POOL_SIZE = 100;
    private static final long IDLE_TIMEOUT_SECONDS = 300; // 5 minutes
    private static final long READY_POOL_TIMEOUT_SECONDS = 300; // 5 minutes
    
    private final Map<String, SmtpConnectionInfo> connectionCache = new ConcurrentHashMap<>();
    private final Map<String, ReadyConnection> readyPool = new ConcurrentHashMap<>();
    private int maxConnections = DEFAULT_MAX_CONNECTIONS;
    private int maxPoolSize = DEFAULT_MAX_POOL_SIZE;
    
    /**
     * Inner class to store connection credentials and metadata for the ready pool.
     */
    private static class ReadyConnection {
        final String host;
        final String username;
        final String password;
        final Properties props;
        final Instant addedTime;
        
        ReadyConnection(String host, String username, String password, Properties props) {
            this.host = host;
            this.username = username;
            this.password = password;
            this.props = props;
            this.addedTime = Instant.now();
        }
        
        long getAgeSeconds() {
            return Instant.now().getEpochSecond() - addedTime.getEpochSecond();
        }
    }
    
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
        readyPool.clear();
        logger.info("Cleared ready pool");
    }
    
    /**
     * Gets or creates a cached SMTP connection.
     * First checks the ready pool for existing connection credentials.
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
        
        // Extract port from properties with error handling
        int port;
        try {
            port = Integer.parseInt(props.getProperty("mail.smtps.port", "465"));
        } catch (NumberFormatException e) {
            logger.warning("Invalid port number in properties, using default 465");
            port = 465;
        }
        
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
        
        // Check ready pool first before creating new connection
        ReadyConnection readyConn = readyPool.remove(key);
        if (readyConn != null) {
            logger.info("Found connection in ready pool, reusing credentials for: " + key);
            // Use credentials from ready pool
            password = readyConn.password;
            props = readyConn.props;
        }
        
        // Check if we need to enforce max pool size
        int totalSize = connectionCache.size() + readyPool.size();
        if (totalSize >= maxPoolSize) {
            // Close oldest connection in ready pool if available
            evictOldestFromReadyPool();
        }
        
        // Check if we need to evict from active connections
        if (connectionCache.size() >= maxConnections) {
            evictOldestConnection();
        }
        
        // Create new connection
        Session session = Session.getInstance(props, null);
        Transport transport = session.getTransport("smtps");
        transport.connect(host, username, password);
        
        SmtpConnectionInfo newInfo = new SmtpConnectionInfo(host, username, password, port, transport);
        connectionCache.put(key, newInfo);
        
        logger.info("Created new cached SMTP connection for: " + key + " (active: " + connectionCache.size() + ", ready: " + readyPool.size() + ")");
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
        stats.put("readyPoolSize", readyPool.size());
        stats.put("maxPoolSize", maxPoolSize);
        stats.put("totalPooled", connectionCache.size() + readyPool.size());
        
        return stats;
    }
    
    /**
     * Scheduled task to clean up idle connections.
     * Runs every minute to check for connections idle for more than 5 minutes.
     */
    @Schedule(hour = "*", minute = "*", second = "0", persistent = false)
    public void cleanupIdleConnections() {
        java.util.concurrent.atomic.AtomicInteger removedCount = new java.util.concurrent.atomic.AtomicInteger(0);
        
        // Clean up active connections
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
        
        // Clean up ready pool
        cleanupReadyPool();
    }
    
    /**
     * Cleans up connections in ready pool that are older than 5 minutes.
     */
    private void cleanupReadyPool() {
        int removedCount = 0;
        
        for (String key : new ArrayList<>(readyPool.keySet())) {
            ReadyConnection conn = readyPool.get(key);
            if (conn != null && conn.getAgeSeconds() > READY_POOL_TIMEOUT_SECONDS) {
                readyPool.remove(key);
                removedCount++;
                logger.info("Removed connection from SMTP ready pool: " + key + " (age: " + conn.getAgeSeconds() + " seconds)");
            }
        }
        
        if (removedCount > 0) {
            logger.info("Cleaned up " + removedCount + " connections from SMTP ready pool");
        }
    }
    
    /**
     * Evicts the oldest connection from ready pool when pool is full.
     */
    private void evictOldestFromReadyPool() {
        if (readyPool.isEmpty()) {
            return;
        }
        
        String oldestKey = null;
        ReadyConnection oldest = null;
        
        for (Map.Entry<String, ReadyConnection> entry : readyPool.entrySet()) {
            if (oldest == null || entry.getValue().addedTime.isBefore(oldest.addedTime)) {
                oldestKey = entry.getKey();
                oldest = entry.getValue();
            }
        }
        
        if (oldestKey != null) {
            readyPool.remove(oldestKey);
            logger.info("Evicted oldest connection from SMTP ready pool: " + oldestKey);
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
     * Closes a specific cached connection and moves it to the ready pool.
     * 
     * @param host SMTP server host
     * @param username Username
     * @param password Password (needed for ready pool)
     * @param props Connection properties (needed for ready pool)
     * @return true if connection was found and closed, false otherwise
     */
    public boolean closeConnection(String host, String username, String password, Properties props) {
        String key = SmtpConnectionInfo.generateKey(host, username);
        
        SmtpConnectionInfo info = connectionCache.remove(key);
        if (info != null) {
            info.close();
            
            // Move to ready pool for potential reuse
            ReadyConnection readyConn = new ReadyConnection(host, username, password, props);
            readyPool.put(key, readyConn);
            
            logger.info("Closed SMTP connection and moved to ready pool: " + key + 
                       " (active: " + connectionCache.size() + ", ready: " + readyPool.size() + ")");
            return true;
        }
        
        logger.fine("SMTP connection not found for closing: " + key);
        return false;
    }
    
    /**
     * Closes a specific cached connection (legacy method without password/props).
     * Connection is closed but NOT moved to ready pool.
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
            logger.info("Manually closed SMTP connection (not pooled): " + key);
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
        
        // Extract port from properties with error handling
        int port;
        try {
            port = Integer.parseInt(props.getProperty("mail.smtps.port", "465"));
        } catch (NumberFormatException e) {
            logger.warning("Invalid port number in properties, using default 465");
            port = 465;
        }
        
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
        
        SmtpConnectionInfo newInfo = new SmtpConnectionInfo(host, username, password, port, transport);
        connectionCache.put(key, newInfo);
        
        logger.info("Reconnected SMTP connection: " + key + " (total: " + connectionCache.size() + ")");
        return newInfo;
    }
}
