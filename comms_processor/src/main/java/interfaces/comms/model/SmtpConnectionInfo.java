package interfaces.comms.model;

import jakarta.mail.Transport;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Model class to store SMTP connection information and statistics.
 * Thread-safe for concurrent access.
 */
public class SmtpConnectionInfo {
    private static final long SECONDS_PER_DAY = 86400;
    private static final int MAX_USAGE_HISTORY_SIZE = 1000;
    
    private final String connectionKey;
    private final String host;
    private final String username;
    // Note: Password stored for automatic reconnection. In production, consider using
    // secure storage mechanisms like char arrays or encryption to protect credentials in memory.
    private final String password; // Stored for automatic reconnection
    private final int port;
    private final Transport transport;
    private volatile Instant lastUsedTime;
    private final Instant createdTime;
    private final List<Instant> usageHistory;
    private final AtomicInteger emailsSentSinceConnect;
    private final Object lock = new Object();
    
    public SmtpConnectionInfo(String host, String username, String password, int port, Transport transport) {
        this.host = host;
        this.username = username;
        this.password = password;
        this.port = port;
        this.transport = transport;
        this.connectionKey = generateKey(host, username);
        this.createdTime = Instant.now();
        this.lastUsedTime = Instant.now();
        this.usageHistory = new ArrayList<>();
        this.usageHistory.add(this.lastUsedTime);
        this.emailsSentSinceConnect = new AtomicInteger(0);
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
    
    public String getPassword() {
        return password;
    }
    
    public int getPort() {
        return port;
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
        synchronized (lock) {
            return new ArrayList<>(usageHistory);
        }
    }
    
    public void updateLastUsed() {
        Instant now = Instant.now();
        synchronized (lock) {
            this.lastUsedTime = now;
            this.usageHistory.add(now);
            
            // Trim history if it grows too large
            if (usageHistory.size() > MAX_USAGE_HISTORY_SIZE) {
                // Keep only recent entries (last day + buffer)
                Instant cutoff = now.minusSeconds(SECONDS_PER_DAY + 3600);
                List<Instant> recentHistory = usageHistory.stream()
                        .filter(time -> time.isAfter(cutoff))
                        .collect(Collectors.toList());
                usageHistory.clear();
                usageHistory.addAll(recentHistory);
            }
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
        synchronized (lock) {
            return (int) usageHistory.stream()
                    .filter(time -> time.isAfter(dayAgo))
                    .count();
        }
    }
    
    /**
     * Gets the number of emails sent since the last connection.
     */
    public int getEmailsSentSinceConnect() {
        return emailsSentSinceConnect.get();
    }
    
    /**
     * Increments the count of emails sent since connection.
     * Thread-safe using AtomicInteger.
     */
    public void incrementEmailsSent() {
        emailsSentSinceConnect.incrementAndGet();
    }
    
    /**
     * Resets the emails sent counter (typically after reconnection).
     */
    public void resetEmailsSent() {
        emailsSentSinceConnect.set(0);
    }
}
