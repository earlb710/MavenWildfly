package interfaces.comms.model;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Test class for EmailReaderStats.
 */
public class EmailReaderStatsTest {
    
    private EmailReaderStats stats;
    
    @Before
    public void setUp() {
        stats = new EmailReaderStats();
    }
    
    @Test
    public void testRecordSuccess() {
        stats.recordSuccess(10, 5000);
        
        assertEquals(10, stats.getTotalEmailsRead());
        assertEquals(5000, stats.getTotalSizeBytes());
        assertEquals(0, stats.getTotalErrors());
    }
    
    @Test
    public void testRecordMultipleSuccess() {
        stats.recordSuccess(10, 5000);
        stats.recordSuccess(5, 2000);
        stats.recordSuccess(15, 8000);
        
        assertEquals(30, stats.getTotalEmailsRead());
        assertEquals(15000, stats.getTotalSizeBytes());
        assertEquals(0, stats.getTotalErrors());
    }
    
    @Test
    public void testRecordError() {
        Map<String, Object> context = new HashMap<>();
        context.put("messageCount", 10);
        
        stats.recordError("getMailboxStats", "imap.example.com", "user@example.com", 
                "INBOX", "Connection timeout", "IOException: Connection timeout", context);
        
        assertEquals(0, stats.getTotalEmailsRead());
        assertEquals(0, stats.getTotalSizeBytes());
        assertEquals(1, stats.getTotalErrors());
        
        List<EmailReaderStats.ErrorInfo> errors = stats.getErrorHistory();
        assertEquals(1, errors.size());
        
        EmailReaderStats.ErrorInfo error = errors.get(0);
        assertEquals("getMailboxStats", error.getOperation());
        assertEquals("imap.example.com", error.getHost());
        assertEquals("user@example.com", error.getUsername());
        assertEquals("INBOX", error.getFolder());
        assertEquals("Connection timeout", error.getErrorMessage());
        assertEquals("IOException: Connection timeout", error.getErrorDetails());
    }
    
    @Test
    public void testMaxErrorHistory() {
        // Add 25 errors (more than MAX_ERROR_HISTORY = 20)
        for (int i = 0; i < 25; i++) {
            stats.recordError("getMailboxCount", "imap.example.com", "user" + i + "@example.com", 
                    "INBOX", "Error " + i, "Details " + i, null);
        }
        
        assertEquals(25, stats.getTotalErrors());
        
        List<EmailReaderStats.ErrorInfo> errors = stats.getErrorHistory();
        // Should only keep last 20 errors
        assertEquals(20, errors.size());
        
        // The first error in the list should be the most recent (user24)
        EmailReaderStats.ErrorInfo mostRecent = errors.get(0);
        assertEquals("user24@example.com", mostRecent.getUsername());
        
        // The last error in the list should be user5 (since we keep last 20 out of 25)
        EmailReaderStats.ErrorInfo oldest = errors.get(19);
        assertEquals("user5@example.com", oldest.getUsername());
    }
    
    @Test
    public void testToMap() {
        stats.recordSuccess(10, 5000);
        stats.recordError("getMailboxCount", "imap.example.com", "user@example.com", 
                "INBOX", "Error", "Details", null);
        
        Map<String, Object> map = stats.toMap();
        
        assertEquals(10L, map.get("totalEmailsRead"));
        assertEquals(5000L, map.get("totalSizeBytes"));
        assertEquals(1L, map.get("totalErrors"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> errors = (List<Map<String, Object>>) map.get("recentErrors");
        assertEquals(1, errors.size());
        assertEquals(1, map.get("recentErrorsCount"));
    }
    
    @Test
    public void testSuccessTracking() {
        Map<String, Object> context1 = new HashMap<>();
        context1.put("messageCount", 10);
        
        Map<String, Object> context2 = new HashMap<>();
        context2.put("messageCount", 5);
        
        // Record first success
        stats.recordSuccess("getMailboxStats", "imap.example.com", "user@example.com", 
                "INBOX", 10, 51200, 300, context1);
        
        // Record second success
        stats.recordSuccess("getMailboxCount", "imap.example.com", "user@example.com", 
                "SENT", 5, 25600, 150, context2);
        
        // Verify totals
        assertEquals(15, stats.getTotalEmailsRead());
        assertEquals(76800, stats.getTotalSizeBytes());
        
        // Verify success history
        List<EmailReaderStats.SuccessInfo> history = stats.getSuccessHistory();
        assertEquals(2, history.size());
        
        // Most recent first
        EmailReaderStats.SuccessInfo recent = history.get(0);
        assertEquals("getMailboxCount", recent.getOperation());
        assertEquals("imap.example.com", recent.getHost());
        assertEquals("user@example.com", recent.getUsername());
        assertEquals("SENT", recent.getFolder());
        assertEquals(5, recent.getEmailCount());
        assertEquals(25600, recent.getSizeBytes());
        assertEquals(150, recent.getProcessingTimeMs());
        assertNotNull(recent.getTimestamp());
        
        // Verify in map format
        Map<String, Object> map = stats.toMap();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> successes = (List<Map<String, Object>>) map.get("recentSuccesses");
        assertEquals(2, successes.size());
        assertEquals(2, map.get("recentSuccessesCount"));
        
        Map<String, Object> recentMap = successes.get(0);
        assertEquals("getMailboxCount", recentMap.get("operation"));
        assertEquals("SENT", recentMap.get("folder"));
        assertEquals(150L, recentMap.get("processingTimeMs"));
    }
    
    @Test
    public void testReset() {
        stats.recordSuccess(10, 5000);
        stats.recordSuccess("getMailboxStats", "imap.example.com", "user@example.com", 
                "INBOX", 5, 3000, 200, null);
        stats.recordError("getMailboxCount", "imap.example.com", "user@example.com", 
                "INBOX", "Error", "Details", null);
        
        assertEquals(15, stats.getTotalEmailsRead());
        assertEquals(1, stats.getTotalErrors());
        assertEquals(1, stats.getSuccessHistory().size());
        
        stats.reset();
        
        assertEquals(0, stats.getTotalEmailsRead());
        assertEquals(0, stats.getTotalSizeBytes());
        assertEquals(0, stats.getTotalErrors());
        assertEquals(0, stats.getErrorHistory().size());
        assertEquals(0, stats.getSuccessHistory().size());
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
        
        assertEquals(200, stats.getTotalEmailsRead());
        assertEquals(20000, stats.getTotalSizeBytes());
    }
}
