package interfaces.comms.model;

import java.time.Instant;
import java.util.ArrayList;
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
 * - Last 20 successful operations with timing information
 */
public class EmailSenderStats {
    private static final int MAX_ERROR_HISTORY = 20;
    private static final int MAX_SUCCESS_HISTORY = 20;
    
    private final AtomicLong totalEmailsSent = new AtomicLong(0);
    private final AtomicLong totalSizeBytes = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final LinkedList<ErrorInfo> errorHistory = new LinkedList<>();
    private final Object errorLock = new Object();
    private final LinkedList<SuccessInfo> successHistory = new LinkedList<>();
    private final Object successLock = new Object();
    
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
     * Inner class to store success information.
     */
    public static class SuccessInfo {
        private final Instant timestamp;
        private final String operation;
        private final String host;
        private final String username;
        private final long emailCount;
        private final long sizeBytes;
        private final long processingTimeMs;
        private final Map<String, Object> additionalContext;
        
        public SuccessInfo(String operation, String host, String username, long emailCount, long sizeBytes, long processingTimeMs, Map<String, Object> additionalContext) {
            this.timestamp = Instant.now();
            this.operation = operation;
            this.host = host;
            this.username = username;
            this.emailCount = emailCount;
            this.sizeBytes = sizeBytes;
            this.processingTimeMs = processingTimeMs;
            this.additionalContext = additionalContext != null ? new HashMap<>(additionalContext) : new HashMap<>();
        }
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("timestamp", timestamp.toString());
            map.put("operation", operation);
            map.put("host", host);
            map.put("username", username);
            map.put("emailCount", emailCount);
            map.put("sizeBytes", sizeBytes);
            map.put("processingTimeMs", processingTimeMs);
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
        
        public long getEmailCount() {
            return emailCount;
        }
        
        public long getSizeBytes() {
            return sizeBytes;
        }
        
        public long getProcessingTimeMs() {
            return processingTimeMs;
        }
        
        public Map<String, Object> getAdditionalContext() {
            return new HashMap<>(additionalContext);
        }
    }
    
    /**
     * Records a successful email send (backward compatible).
     * 
     * @param emailCount Number of emails sent
     * @param sizeBytes Total size of emails sent in bytes
     */
    public void recordSuccess(long emailCount, long sizeBytes) {
        totalEmailsSent.addAndGet(emailCount);
        totalSizeBytes.addAndGet(sizeBytes);
    }
    
    /**
     * Records a successful email send with timing and context information.
     * 
     * @param operation The operation name (e.g., "sendEmail", "sendTextMessage")
     * @param host The SMTP host
     * @param username The username
     * @param emailCount Number of emails sent
     * @param sizeBytes Total size of emails sent in bytes
     * @param processingTimeMs Processing time in milliseconds
     * @param additionalContext Additional context information
     */
    public void recordSuccess(String operation, String host, String username, long emailCount, long sizeBytes, long processingTimeMs, Map<String, Object> additionalContext) {
        totalEmailsSent.addAndGet(emailCount);
        totalSizeBytes.addAndGet(sizeBytes);
        
        SuccessInfo successInfo = new SuccessInfo(operation, host, username, emailCount, sizeBytes, processingTimeMs, additionalContext);
        
        synchronized (successLock) {
            successHistory.addFirst(successInfo);
            // Keep only last 20 successes
            while (successHistory.size() > MAX_SUCCESS_HISTORY) {
                successHistory.removeLast();
            }
        }
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
     * Gets the success history (last 20 successes).
     */
    public List<SuccessInfo> getSuccessHistory() {
        synchronized (successLock) {
            return new ArrayList<>(successHistory);
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
        
        List<Map<String, Object>> successes = new ArrayList<>();
        synchronized (successLock) {
            for (SuccessInfo success : successHistory) {
                successes.add(success.toMap());
            }
        }
        map.put("recentSuccesses", successes);
        map.put("recentSuccessesCount", successes.size());
        
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
        synchronized (successLock) {
            successHistory.clear();
        }
    }
}
