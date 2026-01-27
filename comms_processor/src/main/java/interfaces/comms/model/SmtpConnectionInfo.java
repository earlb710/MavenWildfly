package interfaces.comms.model;

import jakarta.mail.Transport;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Model class to store SMTP connection information and statistics.
 */
public class SmtpConnectionInfo {
    private static final long SECONDS_PER_DAY = 86400;
    private static final int MAX_USAGE_HISTORY_SIZE = 1000;
    
    private final String connectionKey;
    private final String host;
    private final String username;
    private final Transport transport;
    private Instant lastUsedTime;
    private final Instant createdTime;
    private final List<Instant> usageHistory;
    
    public SmtpConnectionInfo(String host, String username, Transport transport) {
        this.host = host;
        this.username = username;
        this.transport = transport;
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
    
    public Transport getTransport() {
        return transport;
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
        
        // Trim history if it grows too large
        if (usageHistory.size() > MAX_USAGE_HISTORY_SIZE) {
            // Keep only recent entries (last day + buffer)
            Instant cutoff = Instant.now().minusSeconds(SECONDS_PER_DAY + 3600);
            List<Instant> recentHistory = new ArrayList<>();
            for (Instant time : usageHistory) {
                if (time.isAfter(cutoff)) {
                    recentHistory.add(time);
                }
            }
            usageHistory.clear();
            usageHistory.addAll(recentHistory);
        }
    }
    
    public boolean isConnected() {
        try {
            return transport != null && transport.isConnected();
        } catch (Exception e) {
            return false;
        }
    }
    
    public void close() {
        if (transport != null) {
            try {
                transport.close();
            } catch (Exception e) {
                // Ignore close errors
            }
        }
    }
    
    public long getIdleTimeSeconds() {
        return Instant.now().getEpochSecond() - lastUsedTime.getEpochSecond();
    }
    
    public int getUsageCountLastDay() {
        Instant dayAgo = Instant.now().minusSeconds(SECONDS_PER_DAY);
        return (int) usageHistory.stream()
                .filter(time -> time.isAfter(dayAgo))
                .count();
    }
}
