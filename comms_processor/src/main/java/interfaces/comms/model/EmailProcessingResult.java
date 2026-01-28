package interfaces.comms.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe class to collect results from multi-threaded email processing.
 * 
 * This class aggregates processing results from multiple threads, tracking
 * success and failure counts, individual message results, and error details.
 */
public class EmailProcessingResult {
    
    private final AtomicInteger processedCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger errorCount = new AtomicInteger(0);
    
    // Thread-safe collections for storing results
    private final Map<String, Map<String, Object>> messageResults = new ConcurrentHashMap<>();
    private final List<Map<String, Object>> errors = new ArrayList<>();
    
    private final long startTime;
    private long endTime;
    
    public EmailProcessingResult() {
        this.startTime = System.currentTimeMillis();
    }
    
    /**
     * Records a successful message processing result.
     * 
     * @param messageId The unique identifier for the message
     * @param result The processing result map
     */
    public void recordSuccess(String messageId, Map<String, Object> result) {
        processedCount.incrementAndGet();
        successCount.incrementAndGet();
        messageResults.put(messageId, result);
    }
    
    /**
     * Records a failed message processing result.
     * 
     * @param messageId The unique identifier for the message (can be null)
     * @param errorMessage The error message
     * @param exception The exception that occurred (can be null)
     */
    public synchronized void recordError(String messageId, String errorMessage, Exception exception) {
        processedCount.incrementAndGet();
        errorCount.incrementAndGet();
        
        Map<String, Object> errorInfo = new ConcurrentHashMap<>();
        errorInfo.put("messageId", messageId);
        errorInfo.put("error", errorMessage);
        if (exception != null) {
            errorInfo.put("exceptionType", exception.getClass().getName());
            errorInfo.put("exceptionMessage", exception.getMessage());
        }
        errorInfo.put("timestamp", System.currentTimeMillis());
        
        errors.add(errorInfo);
    }
    
    /**
     * Marks the processing as complete and records the end time.
     */
    public void markComplete() {
        this.endTime = System.currentTimeMillis();
    }
    
    /**
     * Converts the processing results to a map suitable for JSON response.
     * 
     * @return Map containing all processing results
     */
    public Map<String, Object> toMap() {
        Map<String, Object> result = new ConcurrentHashMap<>();
        result.put("success", errorCount.get() == 0);
        result.put("processedCount", processedCount.get());
        result.put("successCount", successCount.get());
        result.put("errorCount", errorCount.get());
        result.put("processingTimeMs", endTime > 0 ? endTime - startTime : System.currentTimeMillis() - startTime);
        result.put("messageResults", messageResults);
        result.put("errors", errors);
        return result;
    }
    
    // Getters
    public int getProcessedCount() {
        return processedCount.get();
    }
    
    public int getSuccessCount() {
        return successCount.get();
    }
    
    public int getErrorCount() {
        return errorCount.get();
    }
    
    public Map<String, Map<String, Object>> getMessageResults() {
        return messageResults;
    }
    
    public List<Map<String, Object>> getErrors() {
        return errors;
    }
    
    public long getDurationMs() {
        return endTime > 0 ? endTime - startTime : System.currentTimeMillis() - startTime;
    }
}
