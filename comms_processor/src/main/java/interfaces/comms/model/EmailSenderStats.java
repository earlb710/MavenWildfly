package interfaces.comms.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Statistics tracker for email sending operations.
 * Thread-safe implementation that tracks:
 * - Total number of emails sent
 * - Total size of emails sent (in bytes)
 * - Number of errors
 * - Last 20 error requests with full error information
 */
public class EmailSenderStats {
    private static final int MAX_ERROR_HISTORY = 20;
    
    private final AtomicLong totalEmailsSent = new AtomicLong(0);
    private final AtomicLong totalSizeBytes = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final LinkedList<ErrorInfo> errorHistory = new LinkedList<>();
    private final Object errorLock = new Object();
    
    /**
     * Inner class to store error information.
     */
    public static class ErrorInfo {
        private final Instant timestamp;
        private final String operation;
        private final String host;
        private final String username;
        private final String errorMessage;
        private final String errorDetails;
        private final Map<String, Object> additionalContext;
        
        public ErrorInfo(String operation, String host, String username, String errorMessage, String errorDetails, Map<String, Object> additionalContext) {
            this.timestamp = Instant.now();
            this.operation = operation;
            this.host = host;
            this.username = username;
            this.errorMessage = errorMessage;
            this.errorDetails = errorDetails;
            this.additionalContext = additionalContext != null ? new HashMap<>(additionalContext) : new HashMap<>();
        }
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("timestamp", timestamp.toString());
            map.put("operation", operation);
            map.put("host", host);
            map.put("username", username);
            map.put("errorMessage", errorMessage);
            map.put("errorDetails", errorDetails);
            if (!additionalContext.isEmpty()) {
                map.put("context", additionalContext);
            }
            return map;
        }
        
        public Instant getTimestamp() {
            return timestamp;
        }
        
        public String getOperation() {
            return operation;
        }
        
        public String getHost() {
            return host;
        }
        
        public String getUsername() {
            return username;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public String getErrorDetails() {
            return errorDetails;
        }
        
        public Map<String, Object> getAdditionalContext() {
            return new HashMap<>(additionalContext);
        }
    }
    
    /**
     * Records a successful email send.
     * 
     * @param emailCount Number of emails sent
     * @param sizeBytes Total size of emails sent in bytes
     */
    public void recordSuccess(long emailCount, long sizeBytes) {
        totalEmailsSent.addAndGet(emailCount);
        totalSizeBytes.addAndGet(sizeBytes);
    }
    
    /**
     * Records an error in email sending.
     * 
     * @param operation The operation that failed (e.g., "sendEmail", "sendTextMessage")
     * @param host The SMTP host
     * @param username The username
     * @param errorMessage The error message
     * @param errorDetails Additional error details
     * @param additionalContext Additional context information
     */
    public void recordError(String operation, String host, String username, String errorMessage, String errorDetails, Map<String, Object> additionalContext) {
        totalErrors.incrementAndGet();
        
        ErrorInfo errorInfo = new ErrorInfo(operation, host, username, errorMessage, errorDetails, additionalContext);
        
        synchronized (errorLock) {
            errorHistory.addFirst(errorInfo);
            // Keep only last 20 errors
            while (errorHistory.size() > MAX_ERROR_HISTORY) {
                errorHistory.removeLast();
            }
        }
    }
    
    /**
     * Gets the total number of emails sent.
     */
    public long getTotalEmailsSent() {
        return totalEmailsSent.get();
    }
    
    /**
     * Gets the total size of emails sent in bytes.
     */
    public long getTotalSizeBytes() {
        return totalSizeBytes.get();
    }
    
    /**
     * Gets the total number of errors.
     */
    public long getTotalErrors() {
        return totalErrors.get();
    }
    
    /**
     * Gets the error history (last 20 errors).
     */
    public List<ErrorInfo> getErrorHistory() {
        synchronized (errorLock) {
            return new ArrayList<>(errorHistory);
        }
    }
    
    /**
     * Gets all statistics as a map.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("totalEmailsSent", totalEmailsSent.get());
        map.put("totalSizeBytes", totalSizeBytes.get());
        map.put("totalErrors", totalErrors.get());
        
        List<Map<String, Object>> errors = new ArrayList<>();
        synchronized (errorLock) {
            for (ErrorInfo error : errorHistory) {
                errors.add(error.toMap());
            }
        }
        map.put("recentErrors", errors);
        map.put("recentErrorsCount", errors.size());
        
        return map;
    }
    
    /**
     * Resets all statistics.
     */
    public void reset() {
        totalEmailsSent.set(0);
        totalSizeBytes.set(0);
        totalErrors.set(0);
        synchronized (errorLock) {
            errorHistory.clear();
        }
    }
}
