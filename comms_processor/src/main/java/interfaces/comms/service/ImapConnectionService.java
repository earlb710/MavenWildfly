package interfaces.comms.service;

import interfaces.comms.model.ImapConnectionInfo;
import jakarta.mail.Session;
import jakarta.mail.Store;
import javax.ejb.Stateless;
import javax.inject.Inject;
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
     * Uses TLS 1.2+ for secure connections and caches connections for reuse.
     * 
     * @param host The IMAPS server hostname or IP address
     * @param username The username for authentication
     * @param password The password for authentication
     * @return true if connection is successful, false otherwise
     */
    public boolean testConnection(String host, String username, String password) {
        try {
            // Validate input parameters
            if (host == null || host.trim().isEmpty()) {
                logger.warning("IMAPS connection failed: Host is required");
                return false;
            }
            
            if (username == null || username.trim().isEmpty()) {
                logger.warning("IMAPS connection failed: Username is required");
                return false;
            }
            
            if (password == null) {
                logger.warning("IMAPS connection failed: Password is required");
                return false;
            }
            
            // Configure IMAPS properties with latest encryption
            Properties props = getImapProperties(host);
            
            // Get or create cached connection
            logger.info("Attempting IMAPS connection to: " + host + " with user: " + username);
            ImapConnectionInfo connectionInfo = cacheService.getOrCreateConnection(host, username, password, props);
            
            if (connectionInfo.isConnected()) {
                logger.info("IMAPS connection successful to: " + host);
                return true;
            } else {
                logger.warning("IMAPS connection failed: Store not connected");
                return false;
            }
            
        } catch (Exception e) {
            logger.severe("IMAPS connection failed: " + e.getMessage());
            logger.fine("Exception details: " + e.getClass().getName());
            return false;
        }
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
