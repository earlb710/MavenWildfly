package interfaces.comms.model;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Test class for EmailSenderStats.
 */
public class EmailSenderStatsTest {
    
    private EmailSenderStats stats;
    
    @Before
    public void setUp() {
        stats = new EmailSenderStats();
    }
    
    @Test
    public void testRecordSuccess() {
        stats.recordSuccess(10, 5000);
        
        assertEquals(10, stats.getTotalEmailsSent());
        assertEquals(5000, stats.getTotalSizeBytes());
        assertEquals(0, stats.getTotalErrors());
    }
    
    @Test
    public void testRecordMultipleSuccess() {
        stats.recordSuccess(10, 5000);
        stats.recordSuccess(5, 2000);
        stats.recordSuccess(15, 8000);
        
        assertEquals(30, stats.getTotalEmailsSent());
        assertEquals(15000, stats.getTotalSizeBytes());
        assertEquals(0, stats.getTotalErrors());
    }
    
    @Test
    public void testRecordError() {
        Map<String, Object> context = new HashMap<>();
        context.put("fromAddress", "test@example.com");
        context.put("toAddress", "recipient@example.com");
        
        stats.recordError("sendEmail", "smtp.example.com", "user@example.com", 
                "Connection timeout", "IOException: Connection timeout", context);
        
        assertEquals(0, stats.getTotalEmailsSent());
        assertEquals(0, stats.getTotalSizeBytes());
        assertEquals(1, stats.getTotalErrors());
        
        List<EmailSenderStats.ErrorInfo> errors = stats.getErrorHistory();
        assertEquals(1, errors.size());
        
        EmailSenderStats.ErrorInfo error = errors.get(0);
        assertEquals("sendEmail", error.getOperation());
        assertEquals("smtp.example.com", error.getHost());
        assertEquals("user@example.com", error.getUsername());
        assertEquals("Connection timeout", error.getErrorMessage());
        assertEquals("IOException: Connection timeout", error.getErrorDetails());
    }
    
    @Test
    public void testMaxErrorHistory() {
        // Add 25 errors (more than MAX_ERROR_HISTORY = 20)
        for (int i = 0; i < 25; i++) {
            stats.recordError("sendEmail", "smtp.example.com", "user" + i + "@example.com", 
                    "Error " + i, "Details " + i, null);
        }
        
        assertEquals(25, stats.getTotalErrors());
        
        List<EmailSenderStats.ErrorInfo> errors = stats.getErrorHistory();
        // Should only keep last 20 errors
        assertEquals(20, errors.size());
        
        // The first error in the list should be the most recent (user24)
        EmailSenderStats.ErrorInfo mostRecent = errors.get(0);
        assertEquals("user24@example.com", mostRecent.getUsername());
        
        // The last error in the list should be user5 (since we keep last 20 out of 25)
        EmailSenderStats.ErrorInfo oldest = errors.get(19);
        assertEquals("user5@example.com", oldest.getUsername());
    }
    
    @Test
    public void testToMap() {
        stats.recordSuccess(10, 5000);
        stats.recordError("sendEmail", "smtp.example.com", "user@example.com", 
                "Error", "Details", null);
        
        Map<String, Object> map = stats.toMap();
        
        assertEquals(10L, map.get("totalEmailsSent"));
        assertEquals(5000L, map.get("totalSizeBytes"));
        assertEquals(1L, map.get("totalErrors"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> errors = (List<Map<String, Object>>) map.get("recentErrors");
        assertEquals(1, errors.size());
        assertEquals(1, map.get("recentErrorsCount"));
    }
    
    @Test
    public void testReset() {
        stats.recordSuccess(10, 5000);
        stats.recordError("sendEmail", "smtp.example.com", "user@example.com", 
                "Error", "Details", null);
        
        assertEquals(10, stats.getTotalEmailsSent());
        assertEquals(1, stats.getTotalErrors());
        
        stats.reset();
        
        assertEquals(0, stats.getTotalEmailsSent());
        assertEquals(0, stats.getTotalSizeBytes());
        assertEquals(0, stats.getTotalErrors());
        assertEquals(0, stats.getErrorHistory().size());
    }
    
    @Test
    public void testThreadSafety() throws InterruptedException {
        // Simple thread safety test
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                stats.recordSuccess(1, 100);
            }
        });
        
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                stats.recordSuccess(1, 100);
            }
        });
        
        t1.start();
        t2.start();
        
        t1.join();
        t2.join();
        
        assertEquals(200, stats.getTotalEmailsSent());
        assertEquals(20000, stats.getTotalSizeBytes());
    }
}
