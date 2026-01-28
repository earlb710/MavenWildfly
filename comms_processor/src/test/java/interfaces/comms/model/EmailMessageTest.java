package interfaces.comms.model;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

/**
 * Test class for EmailMessage model.
 */
public class EmailMessageTest {
    
    private EmailMessage message;
    
    @Before
    public void setUp() {
        message = new EmailMessage();
    }
    
    @Test
    public void testDefaultConstructor() {
        EmailMessage msg = new EmailMessage();
        assertNotNull(msg);
    }
    
    @Test
    public void testFullConstructor() {
        List<String> recipients = Arrays.asList("user1@example.com", "user2@example.com");
        
        EmailMessage msg = new EmailMessage(
            1,
            "<abc123@example.com>",
            "Test Subject",
            "sender@example.com",
            recipients,
            "Mon Jan 01 10:30:00 UTC 2024",
            "Mon Jan 01 10:29:45 UTC 2024",
            5000,
            "This is the email body content"
        );
        
        assertEquals(Integer.valueOf(1), msg.getMessageNumber());
        assertEquals("<abc123@example.com>", msg.getMessageId());
        assertEquals("Test Subject", msg.getSubject());
        assertEquals("sender@example.com", msg.getFrom());
        assertEquals(recipients, msg.getTo());
        assertEquals("Mon Jan 01 10:30:00 UTC 2024", msg.getReceivedDate());
        assertEquals("Mon Jan 01 10:29:45 UTC 2024", msg.getSentDate());
        assertEquals(Integer.valueOf(5000), msg.getSize());
        assertEquals("This is the email body content", msg.getData());
    }
    
    @Test
    public void testSettersAndGetters() {
        message.setMessageNumber(42);
        assertEquals(Integer.valueOf(42), message.getMessageNumber());
        
        message.setMessageId("<test@example.com>");
        assertEquals("<test@example.com>", message.getMessageId());
        
        message.setSubject("Test Subject");
        assertEquals("Test Subject", message.getSubject());
        
        message.setFrom("sender@example.com");
        assertEquals("sender@example.com", message.getFrom());
        
        List<String> recipients = Arrays.asList("recipient@example.com");
        message.setTo(recipients);
        assertEquals(recipients, message.getTo());
        
        message.setReceivedDate("2024-01-01");
        assertEquals("2024-01-01", message.getReceivedDate());
        
        message.setSentDate("2024-01-01");
        assertEquals("2024-01-01", message.getSentDate());
        
        message.setSize(1000);
        assertEquals(Integer.valueOf(1000), message.getSize());
        
        message.setData("Email body");
        assertEquals("Email body", message.getData());
    }
    
    @Test
    public void testToString() {
        message.setMessageNumber(1);
        message.setMessageId("<test@example.com>");
        message.setSubject("Test");
        message.setData("Short data");
        
        String result = message.toString();
        assertNotNull(result);
        assertTrue(result.contains("messageNumber=1"));
        assertTrue(result.contains("messageId='<test@example.com>'"));
        assertTrue(result.contains("subject='Test'"));
    }
    
    @Test
    public void testToStringWithLongData() {
        message.setData("This is a very long email body content that should be truncated in the toString method");
        
        String result = message.toString();
        assertNotNull(result);
        assertTrue(result.contains("..."));
    }
}
