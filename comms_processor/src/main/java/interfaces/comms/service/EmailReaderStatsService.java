package interfaces.comms.service;

import interfaces.comms.model.EmailReaderStats;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Singleton service to manage email reader statistics.
 * Provides a centralized location for tracking email reading statistics.
 */
@Singleton
@Startup
public class EmailReaderStatsService {
    
    private static final Logger logger = Logger.getLogger(EmailReaderStatsService.class.getName());
    
    private EmailReaderStats stats;
    
    @PostConstruct
    public void init() {
        stats = new EmailReaderStats();
        logger.info("EmailReaderStatsService initialized");
    }
    
    /**
     * Gets the email reader stats instance.
     */
    public EmailReaderStats getStats() {
        return stats;
    }
    
    /**
     * Records successful email reading.
     * 
     * @param emailCount Number of emails read
     * @param sizeBytes Total size of emails read in bytes
     */
    public void recordSuccess(long emailCount, long sizeBytes) {
        stats.recordSuccess(emailCount, sizeBytes);
    }
    
    /**
     * Records an error in email reading.
     * 
     * @param operation The operation that failed
     * @param host The IMAP host
     * @param username The username
     * @param folder The folder being accessed
     * @param errorMessage The error message
     * @param errorDetails Additional error details
     * @param additionalContext Additional context information
     */
    public void recordError(String operation, String host, String username, String folder, String errorMessage, String errorDetails, Map<String, Object> additionalContext) {
        stats.recordError(operation, host, username, folder, errorMessage, errorDetails, additionalContext);
    }
    
    /**
     * Gets all statistics as a map.
     */
    public Map<String, Object> getStatsMap() {
        return stats.toMap();
    }
    
    /**
     * Resets all statistics.
     */
    public void reset() {
        stats.reset();
        logger.info("Email reader statistics reset");
    }
}
