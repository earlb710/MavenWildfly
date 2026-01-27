package interfaces.comms.model;

import jakarta.mail.Store;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Model class to store IMAPS connection information and statistics.
 */
public class ImapConnectionInfo {
    private final String connectionKey;
    private final String host;
    private final String username;
    private final Store store;
    private Instant lastUsedTime;
    private final Instant createdTime;
    private final List<Instant> usageHistory;
    
    public ImapConnectionInfo(String host, String username, Store store) {
        this.host = host;
        this.username = username;
        this.store = store;
        this.connectionKey = generateKey(host, username);
        this.createdTime = Instant.now();
        this.lastUsedTime = Instant.now();
        this.usageHistory = new ArrayList<>();
        this.usageHistory.add(this.lastUsedTime);
    }
    
    public static String generateKey(String host, String username) {
        return host + ":" + username;
    }
    
    public String getConnectionKey() {
        return connectionKey;
    }
    
    public String getHost() {
        return host;
    }
    
    public String getUsername() {
        return username;
    }
    
    public Store getStore() {
        return store;
    }
    
    public Instant getLastUsedTime() {
        return lastUsedTime;
    }
    
    public Instant getCreatedTime() {
        return createdTime;
    }
    
    public List<Instant> getUsageHistory() {
        return new ArrayList<>(usageHistory);
    }
    
    public void updateLastUsed() {
        this.lastUsedTime = Instant.now();
        this.usageHistory.add(this.lastUsedTime);
    }
    
    public boolean isConnected() {
        try {
            return store != null && store.isConnected();
        } catch (Exception e) {
            return false;
        }
    }
    
    public void close() {
        if (store != null) {
            try {
                store.close();
            } catch (Exception e) {
                // Ignore close errors
            }
        }
    }
    
    public long getIdleTimeSeconds() {
        return Instant.now().getEpochSecond() - lastUsedTime.getEpochSecond();
    }
    
    public int getUsageCountLastDay() {
        Instant dayAgo = Instant.now().minusSeconds(86400);
        return (int) usageHistory.stream()
                .filter(time -> time.isAfter(dayAgo))
                .count();
    }
}
