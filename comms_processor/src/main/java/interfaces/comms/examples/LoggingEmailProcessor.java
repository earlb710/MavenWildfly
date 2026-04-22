package interfaces.comms.examples;

import interfaces.comms.model.EmailMessage;
import interfaces.comms.service.EmailProcessor;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Example EmailProcessor implementation for demonstration and testing.
 * 
 * This simple processor logs email details and returns success.
 * In a real implementation, you would add your business logic here
 * (e.g., save to database, apply rules, trigger workflows, etc.).
 */
public class LoggingEmailProcessor implements EmailProcessor {
    
    private static final Logger logger = Logger.getLogger(LoggingEmailProcessor.class.getName());
    
    @Override
    public Map<String, Object> processEmail(EmailMessage emailMessage) throws Exception {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Validate email
            if (emailMessage == null) {
                result.put("success", false);
                result.put("message", "Email message is null");
                return result;
            }
            
            // Log email details
            logger.info("Processing email: " + 
                       "Subject='" + emailMessage.getSubject() + "', " +
                       "From='" + emailMessage.getFrom() + "', " +
                       "MessageId='" + emailMessage.getMessageId() + "', " +
                       "Size=" + emailMessage.getSize() + " bytes");
            
            // Simulate some processing (in real scenario, add your business logic here)
            // Examples:
            // - Save to database
            // - Extract and store attachments
            // - Apply business rules
            // - Trigger workflows
            // - Send notifications
            // - Forward to another system
            
            // Return success
            result.put("success", true);
            result.put("message", "Email processed successfully");
            result.put("subject", emailMessage.getSubject());
            result.put("size", emailMessage.getSize());
            result.put("processedAt", System.currentTimeMillis());
            
        } catch (Exception e) {
            logger.severe("Error processing email: " + e.getMessage());
            result.put("success", false);
            result.put("message", "Error: " + e.getMessage());
            throw e;
        }
        
        return result;
    }
}
