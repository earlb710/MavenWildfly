package interfaces.comms.service;

import interfaces.comms.model.EmailMessage;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Test class demonstrating EmailProcessor interface usage.
 */
public class EmailProcessorTest {
    
    /**
     * Simple test implementation of EmailProcessor for testing purposes.
     */
    private static class TestEmailProcessor implements EmailProcessor {
        @Override
        public Map<String, Object> processEmail(EmailMessage emailMessage) throws Exception {
            Map<String, Object> result = new HashMap<>();
            
            if (emailMessage == null) {
                result.put("success", false);
                result.put("message", "Email message is null");
                return result;
            }
            
            // Simple processing: check if message has required fields
            if (emailMessage.getSubject() == null || emailMessage.getFrom() == null) {
                result.put("success", false);
                result.put("message", "Missing required fields");
                return result;
            }
            
            result.put("success", true);
            result.put("message", "Email processed successfully");
            result.put("processedSubject", emailMessage.getSubject());
            result.put("processedFrom", emailMessage.getFrom());
            
            return result;
        }
    }
    
    private EmailProcessor processor;
    
    @Before
    public void setUp() {
        processor = new TestEmailProcessor();
    }
    
    @Test
    public void testEmailProcessorImplementation() throws Exception {
        EmailMessage message = new EmailMessage(
            1,
            "<test@example.com>",
            "Test Subject",
            "sender@example.com",
            Arrays.asList("recipient@example.com"),
            "Mon Jan 01 10:30:00 UTC 2024",
            "Mon Jan 01 10:29:45 UTC 2024",
            1000,
            "Test email body"
        );
        
        Map<String, Object> result = processor.processEmail(message);
        
        assertNotNull(result);
        assertTrue((Boolean) result.get("success"));
        assertEquals("Email processed successfully", result.get("message"));
        assertEquals("Test Subject", result.get("processedSubject"));
        assertEquals("sender@example.com", result.get("processedFrom"));
    }
    
    @Test
    public void testEmailProcessorWithNullMessage() throws Exception {
        Map<String, Object> result = processor.processEmail(null);
        
        assertNotNull(result);
        assertFalse((Boolean) result.get("success"));
        assertEquals("Email message is null", result.get("message"));
    }
    
    @Test
    public void testEmailProcessorWithMissingFields() throws Exception {
        EmailMessage message = new EmailMessage();
        // Don't set subject and from - should fail validation
        
        Map<String, Object> result = processor.processEmail(message);
        
        assertNotNull(result);
        assertFalse((Boolean) result.get("success"));
        assertEquals("Missing required fields", result.get("message"));
    }
    
    @Test
    public void testEmailProcessorWithCompleteData() throws Exception {
        EmailMessage message = new EmailMessage();
        message.setMessageNumber(5);
        message.setMessageId("<msg5@example.com>");
        message.setSubject("Important Email");
        message.setFrom("boss@example.com");
        message.setTo(Arrays.asList("employee@example.com"));
        message.setReceivedDate("Mon Jan 28 10:00:00 UTC 2026");
        message.setSentDate("Mon Jan 28 09:59:00 UTC 2026");
        message.setSize(2500);
        message.setData("Please complete the report by end of day.");
        
        Map<String, Object> result = processor.processEmail(message);
        
        assertNotNull(result);
        assertTrue((Boolean) result.get("success"));
    }
}
