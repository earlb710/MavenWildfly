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
                result.put("success", true);
                result.put("host", host);
                result.put("username", username);
                result.put("cached", true);
            } else {
                result.put("success", false);
                result.put("error", "Connection not established");
            }
            
        } catch (Exception e) {
            logger.severe("Failed to open cached connection: " + e.getMessage());
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
            boolean closed = cacheService.closeConnection(host, username);
            result.put("success", closed);
            if (!closed) {
                result.put("message", "Connection not found in cache");
            }
        } catch (Exception e) {
            logger.severe("Failed to close connection: " + e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Gets the count of emails in a folder for an existing cached connection.
     * Connection must already be open/cached, otherwise returns an error.
     * 
     * @param mailboxIdentifier The mailbox identifier in format "username@host"
     * @param folder The folder name (default "INBOX")
     * @return Map containing success status and message count
     */
    public Map<String, Object> getMailboxCount(String mailboxIdentifier, String folder) {
        Map<String, Object> result = new HashMap<>();
        jakarta.mail.Folder mailFolder = null;
        
        try {
            // Validate mailbox identifier
            if (mailboxIdentifier == null || mailboxIdentifier.trim().isEmpty()) {
                result.put("success", false);
                result.put("error", "Mailbox identifier (username@host) is required");
                return result;
            }
            
            // Parse username@host format
            String[] parts = mailboxIdentifier.split("@", 2);
            if (parts.length != 2) {
                result.put("success", false);
                result.put("error", "Invalid mailbox identifier format. Expected: username@host");
                return result;
            }
            
            String username = parts[0];
            String host = parts[1];
            
            if (username.isEmpty() || host.isEmpty()) {
                result.put("success", false);
                result.put("error", "Invalid mailbox identifier. Username and host cannot be empty");
                return result;
            }
            
            // Default to INBOX if not specified
            if (folder == null || folder.trim().isEmpty()) {
                folder = "INBOX";
            }
            
            // Get existing cached connection (do NOT create new one)
            ImapConnectionInfo connectionInfo = cacheService.getExistingConnection(host, username);
            
            if (connectionInfo == null) {
                result.put("success", false);
                result.put("error", "Connection not open. Use /api/imap/open to establish a connection first");
                return result;
            }
            
            if (!connectionInfo.isConnected()) {
                result.put("success", false);
                result.put("error", "Connection is not active. Use /api/imap/open to establish a connection");
                return result;
            }
            
            // Get the folder and count messages
            mailFolder = connectionInfo.getStore().getFolder(folder);
            mailFolder.open(jakarta.mail.Folder.READ_ONLY);
            
            int messageCount = mailFolder.getMessageCount();
            
            result.put("success", true);
            result.put("folder", folder);
            result.put("messageCount", messageCount);
            result.put("mailboxIdentifier", mailboxIdentifier);
            
        } catch (Exception e) {
            logger.severe("Failed to get mailbox count: " + e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
        } finally {
            // Always close the folder
            if (mailFolder != null) {
                try {
                    mailFolder.close(false);
                } catch (Exception e) {
                    logger.warning("Error closing folder: " + e.getMessage());
                }
            }
        }
        
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
