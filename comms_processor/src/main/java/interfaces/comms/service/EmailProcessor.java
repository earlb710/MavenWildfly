package interfaces.comms.service;

import interfaces.comms.model.EmailMessage;
import java.util.Map;

/**
 * Interface for processing email messages.
 * 
 * This interface defines the contract for email processing implementations.
 * Implementations can perform various operations on emails such as:
 * - Parsing and extracting information
 * - Storing emails in a database
 * - Applying business rules
 * - Forwarding or transforming emails
 * - Triggering workflows based on email content
 * 
 * The EmailMessage parameter contains all email metadata (messageNumber, messageId, 
 * subject, from, to, receivedDate, sentDate, size) plus the email content/body 
 * in the data field.
 */
public interface EmailProcessor {
    
    /**
     * Processes an email message.
     * 
     * This method should be implemented to perform the desired processing
     * logic on the provided email message.
     * 
     * @param emailMessage The email message to process, containing metadata and content
     * @return A map containing the processing result with at least:
     *         - "success" (Boolean): true if processing succeeded, false otherwise
     *         - "message" (String): A human-readable message describing the result
     *         - Additional fields may be added based on implementation needs
     * @throws Exception if an error occurs during processing
     */
    Map<String, Object> processEmail(EmailMessage emailMessage) throws Exception;
}
