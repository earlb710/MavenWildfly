package interfaces.comms.service;

import interfaces.comms.model.ImapConnectionInfo;
import jakarta.mail.Session;
import jakarta.mail.Store;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Service to test IMAPS server connections with TLS encryption.
 * Provides functionality to verify IMAPS server connectivity using host, username, and password.
 * Uses connection caching for improved performance.
 */
@Stateless
public class ImapConnectionService {

    private static final Logger logger = Logger.getLogger(ImapConnectionService.class.getName());
    
    private static final String IMAPS_PROTOCOL = "imaps";
    private static final int DEFAULT_TIMEOUT = 10000; // 10 seconds
    
    @Inject
    private ImapConnectionCacheService cacheService;

    /**
     * Tests connection to an IMAPS server using the provided credentials.
     * Uses TLS 1.2+ for secure connections. Does NOT cache the connection.
     * 
     * @param host The IMAPS server hostname or IP address
     * @param username The username for authentication
     * @param password The password for authentication
     * @return Map containing success status and connection time in milliseconds
     */
    public Map<String, Object> testConnection(String host, String username, String password) {
        Map<String, Object> result = new HashMap<>();
        Store store = null;
        long startTime = System.currentTimeMillis();
        
        try {
            // Validate input parameters
            if (host == null || host.trim().isEmpty()) {
                logger.warning("IMAPS connection failed: Host is required");
                result.put("success", false);
                result.put("error", "Host is required");
                return result;
            }
            
            if (username == null || username.trim().isEmpty()) {
                logger.warning("IMAPS connection failed: Username is required");
                result.put("success", false);
                result.put("error", "Username is required");
                return result;
            }
            
            if (password == null) {
                logger.warning("IMAPS connection failed: Password is required");
                result.put("success", false);
                result.put("error", "Password is required");
                return result;
            }
            
            // Configure IMAPS properties with latest encryption
            Properties props = getImapProperties(host);
            
            // Create new connection (no caching for test)
            logger.info("Attempting IMAPS connection to: " + host + " with user: " + username);
            Session session = Session.getInstance(props, null);
            store = session.getStore(IMAPS_PROTOCOL);
            store.connect(host, username, password);
            
            long connectionTime = System.currentTimeMillis() - startTime;
            
            if (store.isConnected()) {
                logger.info("IMAPS connection successful to: " + host + " in " + connectionTime + "ms");
                result.put("success", true);
                result.put("connectionTimeMs", connectionTime);
            } else {
                logger.warning("IMAPS connection failed: Store not connected");
                result.put("success", false);
                result.put("error", "Store not connected");
                result.put("connectionTimeMs", connectionTime);
            }
            
        } catch (Exception e) {
            long connectionTime = System.currentTimeMillis() - startTime;
            logger.severe("IMAPS connection failed: " + e.getMessage());
            logger.fine("Exception details: " + e.getClass().getName());
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("connectionTimeMs", connectionTime);
        } finally {
            // Always close the test connection
            if (store != null) {
                try {
                    store.close();
                    logger.fine("Test IMAPS connection closed");
                } catch (Exception e) {
                    logger.warning("Error closing test IMAPS connection: " + e.getMessage());
                }
            }
        }
        
        return result;
    }
    
    /**
     * Opens a cached IMAPS connection.
     * 
     * @param host The IMAPS server hostname or IP address
     * @param username The username for authentication
     * @param password The password for authentication
     * @return Map containing success status and connection info
     */
    public Map<String, Object> openConnection(String host, String username, String password) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            logger.info("Attempting to open IMAPS connection - host: " + host + ", username: " + username);
            
            // Validate input parameters
            Map<String, Object> validationResult = validateCredentials(host, username, password);
            if (validationResult != null) {
                return validationResult;
            }
            
            // Configure IMAPS properties
            Properties props = getImapProperties(host);
            
            // Get or create cached connection
            ImapConnectionInfo connectionInfo = cacheService.getOrCreateConnection(host, username, password, props);
            
            if (connectionInfo.isConnected()) {
                logger.info("IMAPS connection opened successfully - host: " + host + ", username: " + username);
                result.put("success", true);
                result.put("host", host);
                result.put("username", username);
                result.put("cached", true);
            } else {
                logger.severe("IMAPS connection failed: Connection not established - host: " + host + ", username: " + username);
                result.put("success", false);
                result.put("error", "Connection not established");
            }
            
        } catch (Exception e) {
            logger.severe("Failed to open cached IMAPS connection - host: " + host + ", username: " + username + ", error: " + e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Closes a cached IMAPS connection.
     * 
     * @param host The IMAPS server hostname or IP address
     * @param username The username
     * @return Map containing success status
     */
    public Map<String, Object> closeConnection(String host, String username) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            logger.info("Attempting to close IMAPS connection - host: " + host + ", username: " + username);
            boolean closed = cacheService.closeConnection(host, username);
            result.put("success", closed);
            if (!closed) {
                logger.warning("IMAPS connection not found in cache - host: " + host + ", username: " + username);
                result.put("message", "Connection not found in cache");
            } else {
                logger.info("IMAPS connection closed successfully - host: " + host + ", username: " + username);
            }
        } catch (Exception e) {
            logger.severe("Failed to close IMAPS connection - host: " + host + ", username: " + username + ", error: " + e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Gets the count of emails in a folder for an existing cached connection.
     * Connection must already be open/cached, otherwise returns an error.
     * 
     * @param mailboxHost The mailbox host
     * @param mailboxUser The mailbox username
     * @param mailboxFolder The folder name (default "INBOX")
     * @return Map containing success status and message count
     */
    public Map<String, Object> getMailboxCount(String mailboxHost, String mailboxUser, String mailboxFolder) {
        Map<String, Object> result = new HashMap<>();
        jakarta.mail.Folder folder = null;
        
        try {
            logger.info("Attempting to get mailbox count - host: " + mailboxHost + ", user: " + mailboxUser + ", folder: " + (mailboxFolder != null ? mailboxFolder : "INBOX"));
            
            // Validate and get connection
            Map<String, Object> validationResult = validateAndGetConnection(mailboxHost, mailboxUser, mailboxFolder);
            if (validationResult.containsKey("error")) {
                return validationResult;
            }
            
            ImapConnectionInfo connectionInfo = (ImapConnectionInfo) validationResult.get("connectionInfo");
            mailboxFolder = (String) validationResult.get("mailboxFolder");
            
            // Get the folder and count messages
            folder = connectionInfo.getStore().getFolder(mailboxFolder);
            folder.open(jakarta.mail.Folder.READ_ONLY);
            
            int messageCount = folder.getMessageCount();
            
            logger.info("Mailbox count retrieved successfully - host: " + mailboxHost + ", user: " + mailboxUser + ", folder: " + mailboxFolder + ", count: " + messageCount);
            
            result.put("success", true);
            result.put("mailboxHost", mailboxHost);
            result.put("mailboxUser", mailboxUser);
            result.put("mailboxFolder", mailboxFolder);
            result.put("messageCount", messageCount);
            
        } catch (Exception e) {
            logger.severe("Failed to get mailbox count - host: " + mailboxHost + ", user: " + mailboxUser + ", folder: " + mailboxFolder + ", error: " + e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
        } finally {
            // Always close the folder
            if (folder != null) {
                try {
                    folder.close(false);
                } catch (Exception e) {
                    logger.warning("Error closing folder: " + e.getMessage());
                }
            }
        }
        
        return result;
    }
    
    /**
     * Gets detailed statistics for emails in a folder for an existing cached connection.
     * Connection must already be open/cached, otherwise returns an error.
     * 
     * @param mailboxHost The mailbox host
     * @param mailboxUser The mailbox username
     * @param mailboxFolder The folder name (default "INBOX")
     * @return Map containing success status and detailed statistics
     */
    public Map<String, Object> getMailboxStats(String mailboxHost, String mailboxUser, String mailboxFolder) {
        Map<String, Object> result = new HashMap<>();
        jakarta.mail.Folder folder = null;
        
        try {
            logger.info("Attempting to get mailbox stats - host: " + mailboxHost + ", user: " + mailboxUser + ", folder: " + (mailboxFolder != null ? mailboxFolder : "INBOX"));
            
            // Validate and get connection
            Map<String, Object> validationResult = validateAndGetConnection(mailboxHost, mailboxUser, mailboxFolder);
            if (validationResult.containsKey("error")) {
                return validationResult;
            }
            
            ImapConnectionInfo connectionInfo = (ImapConnectionInfo) validationResult.get("connectionInfo");
            mailboxFolder = (String) validationResult.get("mailboxFolder");
            
            // Get the folder and open it
            folder = connectionInfo.getStore().getFolder(mailboxFolder);
            folder.open(jakarta.mail.Folder.READ_ONLY);
            
            int messageCount = folder.getMessageCount();
            logger.info("Processing " + messageCount + " messages for statistics - folder: " + mailboxFolder);
            
            // Initialize statistics
            long totalSize = 0;
            Integer biggestSize = null;
            Integer smallestSize = null;
            java.util.Date oldestDate = null;
            java.util.Date newestDate = null;
            
            if (messageCount > 0) {
                // Process messages in batches to avoid memory issues
                int batchSize = 100;
                for (int start = 1; start <= messageCount; start += batchSize) {
                    int end = Math.min(start + batchSize - 1, messageCount);
                    jakarta.mail.Message[] messages = folder.getMessages(start, end);
                    
                    // Fetch message metadata in batch for better performance
                    jakarta.mail.FetchProfile fetchProfile = new jakarta.mail.FetchProfile();
                    fetchProfile.add(jakarta.mail.FetchProfile.Item.SIZE);
                    fetchProfile.add(jakarta.mail.FetchProfile.Item.ENVELOPE);
                    folder.fetch(messages, fetchProfile);
                    
                    logger.fine("Processing batch " + start + "-" + end + " of " + messageCount);
                    
                    for (jakarta.mail.Message message : messages) {
                        // Get message size
                        int size = message.getSize();
                        if (size > 0) {
                            totalSize += size;
                            if (biggestSize == null || size > biggestSize) {
                                biggestSize = size;
                            }
                            if (smallestSize == null || size < smallestSize) {
                                smallestSize = size;
                            }
                        }
                        
                        // Get message date
                        java.util.Date receivedDate = message.getReceivedDate();
                        if (receivedDate != null) {
                            if (oldestDate == null || receivedDate.before(oldestDate)) {
                                oldestDate = receivedDate;
                            }
                            if (newestDate == null || receivedDate.after(newestDate)) {
                                newestDate = receivedDate;
                            }
                        }
                    }
                }
            }
            
            logger.info("Mailbox stats retrieved successfully - host: " + mailboxHost + ", user: " + mailboxUser + ", folder: " + mailboxFolder + 
                       ", messageCount: " + messageCount + ", totalSize: " + totalSize + " bytes");
            
            result.put("success", true);
            result.put("mailboxHost", mailboxHost);
            result.put("mailboxUser", mailboxUser);
            result.put("mailboxFolder", mailboxFolder);
            result.put("messageCount", messageCount);
            result.put("totalSize", totalSize);
            result.put("biggestEmailSize", biggestSize);
            result.put("smallestEmailSize", smallestSize);
            result.put("oldestDate", oldestDate != null ? oldestDate.toString() : null);
            result.put("newestDate", newestDate != null ? newestDate.toString() : null);
            
        } catch (Exception e) {
            logger.severe("Failed to get mailbox stats - host: " + mailboxHost + ", user: " + mailboxUser + ", folder: " + mailboxFolder + ", error: " + e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
        } finally {
            // Always close the folder
            if (folder != null) {
                try {
                    folder.close(false);
                } catch (Exception e) {
                    logger.warning("Error closing folder: " + e.getMessage());
                }
            }
        }
        
        return result;
    }
    
    /**
     * Validates mailbox parameters and retrieves cached connection.
     * Returns either an error map or a map with connectionInfo and mailboxFolder.
     * 
     * @param mailboxHost The mailbox host
     * @param mailboxUser The mailbox username
     * @param mailboxFolder The folder name (will default to INBOX if empty)
     * @return Map containing either error or connectionInfo and mailboxFolder
     */
    private Map<String, Object> validateAndGetConnection(String mailboxHost, String mailboxUser, String mailboxFolder) {
        Map<String, Object> result = new HashMap<>();
        
        // Validate required fields
        if (mailboxHost == null || mailboxHost.trim().isEmpty()) {
            result.put("success", false);
            result.put("error", "mailboxHost is required");
            return result;
        }
        
        if (mailboxUser == null || mailboxUser.trim().isEmpty()) {
            result.put("success", false);
            result.put("error", "mailboxUser is required");
            return result;
        }
        
        // Default to INBOX if not specified
        if (mailboxFolder == null || mailboxFolder.trim().isEmpty()) {
            mailboxFolder = "INBOX";
        }
        
        // Get existing cached connection (do NOT create new one)
        ImapConnectionInfo connectionInfo = cacheService.getExistingConnection(mailboxHost, mailboxUser);
        
        if (connectionInfo == null || !connectionInfo.isConnected()) {
            result.put("success", false);
            result.put("error", "Connection not open. Use /api/imap/open to establish a connection first");
            return result;
        }
        
        result.put("connectionInfo", connectionInfo);
        result.put("mailboxFolder", mailboxFolder);
        return result;
    }
    
    /**
     * Validates credentials for IMAPS connection.
     * 
     * @param host The host
     * @param username The username
     * @param password The password
     * @return Map with error if validation fails, null if valid
     */
    private Map<String, Object> validateCredentials(String host, String username, String password) {
        Map<String, Object> result = new HashMap<>();
        
        if (host == null || host.trim().isEmpty()) {
            result.put("success", false);
            result.put("error", "Host is required");
            return result;
        }
        
        if (username == null || username.trim().isEmpty()) {
            result.put("success", false);
            result.put("error", "Username is required");
            return result;
        }
        
        if (password == null) {
            result.put("success", false);
            result.put("error", "Password is required");
            return result;
        }
        
        return null; // Valid
    }
    
    /**
     * Creates IMAPS connection properties with TLS encryption settings.
     * 
     * @param host The IMAPS server hostname
     * @return Properties configured for IMAPS connection
     */
    private Properties getImapProperties(String host) {
        Properties props = new Properties();
        
        // Enable IMAPS protocol
        props.setProperty("mail.store.protocol", IMAPS_PROTOCOL);
        
        // Configure connection settings
        props.setProperty("mail.imaps.host", host);
        props.setProperty("mail.imaps.port", "993"); // Standard IMAPS port
        props.setProperty("mail.imaps.connectiontimeout", String.valueOf(DEFAULT_TIMEOUT));
        props.setProperty("mail.imaps.timeout", String.valueOf(DEFAULT_TIMEOUT));
        
        // Enable TLS/SSL with latest encryption
        props.setProperty("mail.imaps.ssl.enable", "true");
        props.setProperty("mail.imaps.ssl.protocols", "TLSv1.2 TLSv1.3"); // Use TLS 1.2 and 1.3
        props.setProperty("mail.imaps.ssl.checkserveridentity", "true");
        // Note: In production, configure proper certificate trust store instead of trusting all
        // For testing purposes only - this accepts all certificates
        props.setProperty("mail.imaps.ssl.trust", "*");
        
        return props;
    }
}
