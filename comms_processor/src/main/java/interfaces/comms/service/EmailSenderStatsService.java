package interfaces.comms.service;

import interfaces.comms.model.EmailSenderStats;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Singleton service to manage email sender statistics.
 * Provides a centralized location for tracking email sending statistics.
 */
@Singleton
@Startup
public class EmailSenderStatsService {
    
    private static final Logger logger = Logger.getLogger(EmailSenderStatsService.class.getName());
    
    private EmailSenderStats stats;
    
    @PostConstruct
    public void init() {
        stats = new EmailSenderStats();
        logger.info("EmailSenderStatsService initialized");
    }
    
    /**
     * Gets the email sender stats instance.
     */
    public EmailSenderStats getStats() {
        return stats;
    }
    
    /**
     * Records a successful email send.
     * 
     * @param emailCount Number of emails sent
     * @param sizeBytes Total size of emails sent in bytes
     */
    public void recordSuccess(long emailCount, long sizeBytes) {
        stats.recordSuccess(emailCount, sizeBytes);
    }
    
    /**
     * Records an error in email sending.
     * 
     * @param operation The operation that failed
     * @param host The SMTP host
     * @param username The username
     * @param errorMessage The error message
     * @param errorDetails Additional error details
     * @param additionalContext Additional context information
     */
    public void recordError(String operation, String host, String username, String errorMessage, String errorDetails, Map<String, Object> additionalContext) {
        stats.recordError(operation, host, username, errorMessage, errorDetails, additionalContext);
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
        logger.info("Email sender statistics reset");
    }
}
