package interfaces.comms.service;

import interfaces.comms.model.ImapConnectionInfo;
import jakarta.mail.Session;
import jakarta.mail.Store;

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
 * Manages a cache of IMAPS connections with automatic cleanup.
 * Maintains up to a configurable number of connections (default 50).
 * Automatically closes idle connections after 5 minutes of inactivity.
 */
@Singleton
@Startup
public class ImapConnectionCacheService {

    private static final Logger logger = Logger.getLogger(ImapConnectionCacheService.class.getName());
    
    private static final int DEFAULT_MAX_CONNECTIONS = 50;
    private static final long IDLE_TIMEOUT_SECONDS = 300; // 5 minutes
    
    private final Map<String, ImapConnectionInfo> connectionCache = new ConcurrentHashMap<>();
    private int maxConnections = DEFAULT_MAX_CONNECTIONS;
    
    @PostConstruct
    public void init() {
        logger.info("ImapConnectionCacheService initialized with max connections: " + maxConnections);
    }
    
    @PreDestroy
    public void cleanup() {
        logger.info("Closing all cached IMAPS connections");
        connectionCache.values().forEach(ImapConnectionInfo::close);
        connectionCache.clear();
    }
    
    /**
     * Gets or creates a cached IMAPS connection.
     * 
     * @param host IMAPS server host
     * @param username Username for authentication
     * @param password Password for authentication
     * @param props Connection properties
     * @return ImapConnectionInfo object containing the connection
     * @throws Exception if connection fails
     */
    public ImapConnectionInfo getOrCreateConnection(String host, String username, String password, Properties props) throws Exception {
        String key = ImapConnectionInfo.generateKey(host, username);
        
        // Try to get and update existing connection atomically
        ImapConnectionInfo existingInfo = connectionCache.computeIfPresent(key, (k, info) -> {
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
            logger.fine("Reusing cached connection for: " + key);
            return existingInfo;
        }
        
        // Check if we need to evict connections
        if (connectionCache.size() >= maxConnections) {
            evictOldestConnection();
        }
        
        // Create new connection
        Session session = Session.getInstance(props, null);
        Store store = session.getStore("imaps");
        store.connect(host, username, password);
        
        ImapConnectionInfo newInfo = new ImapConnectionInfo(host, username, store);
        connectionCache.put(key, newInfo);
        
        logger.info("Created new cached connection for: " + key + " (total: " + connectionCache.size() + ")");
        return newInfo;
    }
    
    /**
     * Gets all active connection information.
     * 
     * @return List of all cached connections
     */
    public List<Map<String, Object>> getAllConnections() {
        List<Map<String, Object>> connections = new ArrayList<>();
        
        for (ImapConnectionInfo info : connectionCache.values()) {
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
                .filter(ImapConnectionInfo::isConnected)
                .count());
        
        return stats;
    }
    
    /**
     * Scheduled task to clean up idle connections.
     * Runs every minute to check for connections idle for more than 5 minutes.
     */
    @Schedule(hour = "*", minute = "*", second = "0", persistent = false)
    public void cleanupIdleConnections() {
        int removedCount = 0;
        
        // Use iterator for safe removal during iteration
        for (String key : new ArrayList<>(connectionCache.keySet())) {
            connectionCache.computeIfPresent(key, (k, info) -> {
                long idleTime = info.getIdleTimeSeconds();
                if (idleTime > IDLE_TIMEOUT_SECONDS) {
                    logger.info("Closing idle connection: " + k + " (idle for " + idleTime + " seconds)");
                    info.close();
                    return null; // Remove from cache
                }
                return info; // Keep in cache
            });
            
            // Check if removed
            if (!connectionCache.containsKey(key)) {
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            logger.info("Cleaned up " + removedCount + " idle connections");
        }
    }
    
    /**
     * Evicts the oldest connection when cache is full.
     */
    private void evictOldestConnection() {
        ImapConnectionInfo oldest = connectionCache.values().stream()
                .min(Comparator.comparing(ImapConnectionInfo::getLastUsedTime))
                .orElse(null);
        
        if (oldest != null) {
            String key = oldest.getConnectionKey();
            logger.info("Evicting oldest connection: " + key);
            oldest.close();
            connectionCache.remove(key);
        }
    }
    
    /**
     * Sets the maximum number of connections in the cache.
     * 
     * @param maxConnections Maximum number of connections (default 50)
     */
    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
        logger.info("Max connections set to: " + maxConnections);
    }
    
    /**
     * Gets the maximum number of connections.
     * 
     * @return Maximum number of connections
     */
    public int getMaxConnections() {
        return maxConnections;
    }
}
