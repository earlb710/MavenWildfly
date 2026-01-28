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
 * Statistics tracker for email reading operations.
 * Thread-safe implementation that tracks:
 * - Total number of emails read
 * - Total size of emails read (in bytes)
 * - Number of errors
 * - Last 20 error requests with full error information
 */
public class EmailReaderStats {
    private static final int MAX_ERROR_HISTORY = 20;
    
    private final AtomicLong totalEmailsRead = new AtomicLong(0);
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
        private final String folder;
        private final String errorMessage;
        private final String errorDetails;
        private final Map<String, Object> additionalContext;
        
        public ErrorInfo(String operation, String host, String username, String folder, String errorMessage, String errorDetails, Map<String, Object> additionalContext) {
            this.timestamp = Instant.now();
            this.operation = operation;
            this.host = host;
            this.username = username;
            this.folder = folder;
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
            map.put("folder", folder);
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
        
        public String getFolder() {
            return folder;
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
     * Records successful email reading.
     * 
     * @param emailCount Number of emails read
     * @param sizeBytes Total size of emails read in bytes
     */
    public void recordSuccess(long emailCount, long sizeBytes) {
        totalEmailsRead.addAndGet(emailCount);
        totalSizeBytes.addAndGet(sizeBytes);
    }
    
    /**
     * Records an error in email reading.
     * 
     * @param operation The operation that failed (e.g., "getMailboxCount", "getMailboxStats")
     * @param host The IMAP host
     * @param username The username
     * @param folder The folder being accessed
     * @param errorMessage The error message
     * @param errorDetails Additional error details
     * @param additionalContext Additional context information
     */
    public void recordError(String operation, String host, String username, String folder, String errorMessage, String errorDetails, Map<String, Object> additionalContext) {
        totalErrors.incrementAndGet();
        
        ErrorInfo errorInfo = new ErrorInfo(operation, host, username, folder, errorMessage, errorDetails, additionalContext);
        
        synchronized (errorLock) {
            errorHistory.addFirst(errorInfo);
            // Keep only last 20 errors
            while (errorHistory.size() > MAX_ERROR_HISTORY) {
                errorHistory.removeLast();
            }
        }
    }
    
    /**
     * Gets the total number of emails read.
     */
    public long getTotalEmailsRead() {
        return totalEmailsRead.get();
    }
    
    /**
     * Gets the total size of emails read in bytes.
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
        map.put("totalEmailsRead", totalEmailsRead.get());
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
        totalEmailsRead.set(0);
        totalSizeBytes.set(0);
        totalErrors.set(0);
        synchronized (errorLock) {
            errorHistory.clear();
        }
    }
}
