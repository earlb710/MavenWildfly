package interfaces.comms.service;

import interfaces.comms.model.EmailSenderStats;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

/**
 * Test class to verify that SMTP errors are properly tracked in statistics.
 * This tests the fix for the issue where validation errors (like "Connection not open")
 * were not being recorded in the stats.
 */
public class SmtpErrorTrackingTest {
    
    private EmailSenderStatsService statsService;
    private EmailSenderStats stats;
    
    @Before
    public void setUp() {
        statsService = new EmailSenderStatsService();
        // Manually initialize the service (normally done by @PostConstruct)
        statsService.init();
        // Access the underlying stats object through the service
        stats = statsService.getStats();
    }
    
    @Test
    public void testConnectionNotOpenErrorIsTracked() {
        // Simulate recording a "Connection not open" error
        statsService.recordError(
            "sendTextMessage",
            "smtp.gmail.com",
            "user@gmail.com",
            "Connection not open",
            "Connection not open. Use /api/smtp/open to establish a connection first",
            null
        );
        
        // Verify the error was recorded
        assertEquals(1, stats.getTotalErrors());
        
        List<EmailSenderStats.ErrorInfo> errors = stats.getErrorHistory();
        assertEquals(1, errors.size());
        
        EmailSenderStats.ErrorInfo error = errors.get(0);
        assertEquals("sendTextMessage", error.getOperation());
        assertEquals("smtp.gmail.com", error.getHost());
        assertEquals("user@gmail.com", error.getUsername());
        assertEquals("Connection not open", error.getErrorMessage());
    }
    
    @Test
    public void testValidationErrorsAreTracked() {
        // Test missing smtpHost error
        statsService.recordError(
            "sendTextMessage",
            null,
            null,
            "Missing parameter",
            "smtpHost is required",
            null
        );
        
        assertEquals(1, stats.getTotalErrors());
        
        // Test missing toAddress error
        statsService.recordError(
            "sendTextMessage",
            "smtp.gmail.com",
            "user@gmail.com",
            "Missing parameter",
            "toAddress is required",
            null
        );
        
        assertEquals(2, stats.getTotalErrors());
        
        List<EmailSenderStats.ErrorInfo> errors = stats.getErrorHistory();
        assertEquals(2, errors.size());
    }
    
    @Test
    public void testMultipleErrorsAreTracked() {
        // Simulate multiple errors that would happen in real scenarios
        
        // Connection not open error
        statsService.recordError(
            "sendTextMessage",
            "smtp.gmail.com",
            "user@gmail.com",
            "Connection not open",
            "Connection not open. Use /api/smtp/open to establish a connection first",
            null
        );
        
        // Batch size validation error
        statsService.recordError(
            "sendEmails",
            "smtp.gmail.com",
            "user@gmail.com",
            "Invalid batch size",
            "Batch size (0) is below minimum (1)",
            null
        );
        
        // Password not available error
        statsService.recordError(
            "sendEmails",
            "smtp.gmail.com",
            "user@gmail.com",
            "Password not available",
            "Password not available for automatic reconnection. Connection may have expired.",
            null
        );
        
        // Verify all errors were tracked
        assertEquals(3, stats.getTotalErrors());
        
        List<EmailSenderStats.ErrorInfo> errors = stats.getErrorHistory();
        assertEquals(3, errors.size());
        
        // Verify the errors are in reverse chronological order (most recent first)
        assertEquals("Password not available", errors.get(0).getErrorMessage());
        assertEquals("Invalid batch size", errors.get(1).getErrorMessage());
        assertEquals("Connection not open", errors.get(2).getErrorMessage());
    }
    
    @Test
    public void testStatsMapIncludesErrors() {
        // Record an error
        statsService.recordError(
            "sendTextMessage",
            "smtp.gmail.com",
            "user@gmail.com",
            "Connection not open",
            "Connection not open. Use /api/smtp/open to establish a connection first",
            null
        );
        
        // Get the stats as a map (as returned by the REST API)
        Map<String, Object> statsMap = statsService.getStatsMap();
        
        // Verify error count is included
        assertEquals(1L, statsMap.get("totalErrors"));
        
        // Verify recent errors are included
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> recentErrors = (List<Map<String, Object>>) statsMap.get("recentErrors");
        assertNotNull(recentErrors);
        assertEquals(1, recentErrors.size());
        
        Map<String, Object> errorMap = recentErrors.get(0);
        assertEquals("sendTextMessage", errorMap.get("operation"));
        assertEquals("Connection not open", errorMap.get("errorMessage"));
    }
    
    @Test
    public void testNoErrorsInitially() {
        // Verify that a new stats service starts with no errors
        assertEquals(0, stats.getTotalErrors());
        assertEquals(0, stats.getErrorHistory().size());
    }
}
