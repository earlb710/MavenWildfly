package interfaces.comms.service;

import interfaces.comms.model.EmailMessage;
import interfaces.comms.model.EmailProcessingResult;
import interfaces.comms.model.ImapConnectionInfo;
import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.inject.Inject;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Store;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Logger;

/**
 * Service for processing emails from a mailbox using multiple threads.
 * 
 * This service enables parallel processing of emails by dividing messages
 * into chunks and processing each chunk in a separate thread. Each thread
 * gets its own connection to the mailbox folder for thread safety.
 * 
 * Uses WildFly's ManagedExecutorService for proper container-managed threading.
 * This ensures proper context propagation, lifecycle management, and integration
 * with the application server's monitoring and management capabilities.
 */
@Stateless
public class EmailProcessingService {
    
    private static final Logger logger = Logger.getLogger(EmailProcessingService.class.getName());
    
    private static final String IMAPS_PROTOCOL = "imaps";
    private static final int DEFAULT_TIMEOUT = 10000; // 10 seconds
    private static final int DEFAULT_THREAD_COUNT = 4;
    private static final int MAX_THREAD_COUNT = 10;
    
    @Inject
    private ImapConnectionCacheService cacheService;
    
    @Inject
    private EmailReaderStatsService statsService;
    
    /**
     * WildFly's managed executor service for container-managed thread execution.
     * This is injected by the container and provides proper lifecycle management,
     * context propagation, and monitoring integration.
     */
    @Resource
    private ManagedExecutorService executorService;
    
    /**
     * Processes messages from a mailbox using multiple threads.
     * 
     * @param mailboxHost The IMAP server hostname
     * @param mailboxUser The mailbox username
     * @param mailboxPassword The mailbox password
     * @param mailboxFolder The folder to process (default: INBOX)
     * @param processor The EmailProcessor implementation to use
     * @param threadCount Number of threads to use (default: 4, max: 10)
     * @param maxMessages Maximum number of messages to process (0 = all)
     * @param processNewest If true, process newest messages first; otherwise oldest first
     * @param removeMessage If true, delete messages after successful processing (default: false)
     * @return Map containing processing results
     */
    public Map<String, Object> processMessages(
            String mailboxHost,
            String mailboxUser,
            String mailboxPassword,
            String mailboxFolder,
            EmailProcessor processor,
            Integer threadCount,
            Integer maxMessages,
            Boolean processNewest,
            Boolean removeMessage) {
        
        Map<String, Object> result = new HashMap<>();
        
        // Validate inputs
        if (mailboxHost == null || mailboxHost.trim().isEmpty()) {
            result.put("success", false);
            result.put("error", "mailboxHost is required");
            return result;
        }
        
        if (mailboxUser == null || mailboxUser.trim().isEmpty()) {
            result.put("success", false);
            result.put("error", "mailboxUser is required");
            return result;
        }
        
        if (mailboxPassword == null || mailboxPassword.trim().isEmpty()) {
            result.put("success", false);
            result.put("error", "mailboxPassword is required");
            return result;
        }
        
        if (processor == null) {
            result.put("success", false);
            result.put("error", "processor is required");
            return result;
        }
        
        // Set defaults
        if (mailboxFolder == null || mailboxFolder.trim().isEmpty()) {
            mailboxFolder = "INBOX";
        }
        
        if (threadCount == null || threadCount < 1) {
            threadCount = DEFAULT_THREAD_COUNT;
        }
        
        if (threadCount > MAX_THREAD_COUNT) {
            threadCount = MAX_THREAD_COUNT;
            }
        
        if (maxMessages == null || maxMessages < 0) {
            maxMessages = 0; // 0 means all messages
        }
        
        if (processNewest == null) {
            processNewest = false; // Default to oldest first
        }
        
        if (removeMessage == null) {
            removeMessage = false; // Default to not removing messages
        }
        
        logger.info("Starting multi-threaded email processing - host: " + mailboxHost + 
                   ", user: " + mailboxUser + ", folder: " + mailboxFolder + 
                   ", threads: " + threadCount + ", maxMessages: " + maxMessages +
                   ", removeMessage: " + removeMessage);
        
        // Get total message count using a temporary connection
        Folder folder = null;
        Store tempStore = null;
        int totalMessages = 0;
        try {
            Properties props = getImapProperties(mailboxHost);
            Session session = Session.getInstance(props, null);
            tempStore = session.getStore(IMAPS_PROTOCOL);
            tempStore.connect(mailboxHost, mailboxUser, mailboxPassword);
            
            folder = tempStore.getFolder(mailboxFolder);
            folder.open(Folder.READ_ONLY);
            totalMessages = folder.getMessageCount();
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "Failed to connect to mailbox: " + e.getMessage());
            return result;
        } finally {
            try {
                if (folder != null && folder.isOpen()) {
                    folder.close(false);
                }
                if (tempStore != null && tempStore.isConnected()) {
                    tempStore.close();
                }
            } catch (Exception e) {
                logger.warning("Error closing temporary connection: " + e.getMessage());
            }
        }
        
        if (totalMessages == 0) {
            result.put("success", true);
            result.put("message", "No messages in folder");
            result.put("mailboxHost", mailboxHost);
            result.put("mailboxUser", mailboxUser);
            result.put("mailboxFolder", mailboxFolder);
            result.put("totalMessages", 0);
            result.put("processedCount", 0);
            return result;
        }
        
        // Determine which messages to process
        int messagesToProcess = maxMessages > 0 ? Math.min(maxMessages, totalMessages) : totalMessages;
        
        // Calculate message ranges for each thread
        List<int[]> messageRanges = calculateMessageRanges(
                totalMessages, messagesToProcess, threadCount, processNewest);
        
        logger.info("Processing " + messagesToProcess + " messages from " + totalMessages + 
                   " total using " + messageRanges.size() + " threads");
        
        // Create processing result collector
        EmailProcessingResult processingResult = new EmailProcessingResult();
        
        // Make parameters effectively final for lambda
        final String finalMailboxHost = mailboxHost;
        final String finalMailboxUser = mailboxUser;
        final String finalMailboxPassword = mailboxPassword;
        final String finalMailboxFolder = mailboxFolder;
        final EmailProcessor finalProcessor = processor;
        final Boolean finalRemoveMessage = removeMessage;
        
        // Submit tasks for each message range using WildFly's managed executor service
        List<Future<Void>> futures = new ArrayList<>();
        for (int[] range : messageRanges) {
            final int startMsg = range[0];
            final int endMsg = range[1];
            
            Callable<Void> task = () -> {
                processMessageRange(
                        finalMailboxHost, finalMailboxUser, finalMailboxPassword, finalMailboxFolder,
                        startMsg, endMsg, finalProcessor, processingResult, finalRemoveMessage);
                return null;
            };
            
            futures.add(executorService.submit(task));
        }
        
        // Wait for all tasks to complete
        for (Future<Void> future : futures) {
            try {
                future.get(); // This will throw exception if task failed
            } catch (Exception e) {
                logger.severe("Task execution failed: " + e.getMessage());
                processingResult.recordError(null, "Task execution failed: " + e.getMessage(), e);
            }
        }
        
        // Mark processing as complete
        processingResult.markComplete();
        
        logger.info("Multi-threaded processing completed - processed: " + processingResult.getProcessedCount() +
                   ", success: " + processingResult.getSuccessCount() + 
                   ", errors: " + processingResult.getErrorCount() +
                   ", duration: " + processingResult.getDurationMs() + "ms");
        
        // Build result
        result.put("success", true);
        result.put("mailboxHost", mailboxHost);
        result.put("mailboxUser", mailboxUser);
        result.put("mailboxFolder", mailboxFolder);
        result.put("totalMessages", totalMessages);
        result.put("threadsUsed", messageRanges.size());
        result.putAll(processingResult.toMap());
        
        // Record overall stats
        statsService.recordSuccess(processingResult.getSuccessCount(), 0); // Size not tracked here
        
        return result;
    }
    
    /**
     * Processes a range of messages in a single thread.
     */
    private void processMessageRange(
            String mailboxHost,
            String mailboxUser,
            String mailboxPassword,
            String mailboxFolder,
            int startMsg,
            int endMsg,
            EmailProcessor processor,
            EmailProcessingResult processingResult,
            boolean removeMessage) {
        
        Folder folder = null;
        Store store = null;
        
        try {
            // Each thread needs its own Store and Folder connection
            // JavaMail's Folder is not thread-safe
            Properties props = getImapProperties(mailboxHost);
            Session session = Session.getInstance(props, null);
            store = session.getStore(IMAPS_PROTOCOL);
            store.connect(mailboxHost, mailboxUser, mailboxPassword);
            
            folder = store.getFolder(mailboxFolder);
            // Open folder in READ_WRITE mode if we need to delete messages, otherwise READ_ONLY
            int openMode = removeMessage ? Folder.READ_WRITE : Folder.READ_ONLY;
            folder.open(openMode);
            
            logger.info("Thread " + Thread.currentThread().getName() + 
                       " processing messages " + startMsg + " to " + endMsg);
            
            // Get messages in this range
            Message[] messages = folder.getMessages(startMsg, endMsg);
            
            // Fetch metadata in batch for better performance
            jakarta.mail.FetchProfile fetchProfile = new jakarta.mail.FetchProfile();
            fetchProfile.add(jakarta.mail.FetchProfile.Item.ENVELOPE);
            fetchProfile.add(jakarta.mail.FetchProfile.Item.CONTENT_INFO);
            fetchProfile.add(jakarta.mail.FetchProfile.Item.SIZE);
            folder.fetch(messages, fetchProfile);
            
            // Process each message
            for (Message message : messages) {
                String messageId = null;
                try {
                    // Extract message details
                    messageId = extractMessageId(message);
                    EmailMessage emailMessage = convertToEmailMessage(message);
                    
                    // Process the message
                    Map<String, Object> processResult = processor.processEmail(emailMessage);
                    
                    // Check if processing was successful
                    Boolean success = (Boolean) processResult.get("success");
                    
                    // Record success
                    processingResult.recordSuccess(messageId != null ? messageId : "msg-" + message.getMessageNumber(), 
                            processResult);
                    
                    // Delete message if removeMessage is true and processing was successful
                    if (removeMessage && Boolean.TRUE.equals(success)) {
                        try {
                            message.setFlag(jakarta.mail.Flags.Flag.DELETED, true);
                            processingResult.recordDeleted(messageId != null ? messageId : "msg-" + message.getMessageNumber());
                            logger.fine("Marked message " + messageId + " for deletion");
                        } catch (Exception e) {
                            logger.warning("Failed to delete message " + messageId + ": " + e.getMessage());
                        }
                    }
                    
                } catch (Exception e) {
                    logger.warning("Failed to process message " + 
                            (messageId != null ? messageId : message.getMessageNumber()) + 
                            ": " + e.getMessage());
                    processingResult.recordError(
                            messageId != null ? messageId : "msg-" + message.getMessageNumber(),
                            e.getMessage(), e);
                }
            }
            
            logger.info("Thread " + Thread.currentThread().getName() + 
                       " completed processing " + messages.length + " messages");
            
        } catch (Exception e) {
            logger.severe("Thread " + Thread.currentThread().getName() + 
                         " failed: " + e.getMessage());
            processingResult.recordError(null, "Thread processing failed: " + e.getMessage(), e);
        } finally {
            // Close folder and store
            if (folder != null && folder.isOpen()) {
                try {
                    // Expunge to permanently delete marked messages
                    if (removeMessage) {
                        folder.close(true); // true = expunge deleted messages
                    } else {
                        folder.close(false);
                    }
                } catch (Exception e) {
                    logger.warning("Error closing folder: " + e.getMessage());
                }
            }
            if (store != null && store.isConnected()) {
                try {
                    store.close();
                } catch (Exception e) {
                    logger.warning("Error closing store: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Calculates message ranges to distribute across threads.
     */
    private List<int[]> calculateMessageRanges(
            int totalMessages, int messagesToProcess, int threadCount, boolean processNewest) {
        
        List<int[]> ranges = new ArrayList<>();
        
        // Determine starting and ending message numbers
        int startMsgNum, endMsgNum;
        if (processNewest) {
            // Process from the end of the mailbox
            endMsgNum = totalMessages;
            startMsgNum = totalMessages - messagesToProcess + 1;
        } else {
            // Process from the beginning
            startMsgNum = 1;
            endMsgNum = messagesToProcess;
        }
        
        // Divide messages into chunks for threads
        int messagesPerThread = messagesToProcess / threadCount;
        int remainder = messagesToProcess % threadCount;
        
        int currentStart = startMsgNum;
        for (int i = 0; i < threadCount; i++) {
            int chunkSize = messagesPerThread + (i < remainder ? 1 : 0);
            if (chunkSize > 0) {
                int currentEnd = currentStart + chunkSize - 1;
                ranges.add(new int[]{currentStart, currentEnd});
                currentStart = currentEnd + 1;
            }
        }
        
        return ranges;
    }
    
    /**
     * Converts a JavaMail Message to an EmailMessage object.
     */
    private EmailMessage convertToEmailMessage(Message message) throws Exception {
        EmailMessage emailMessage = new EmailMessage();
        
        emailMessage.setMessageNumber(message.getMessageNumber());
        
        // Get Message-ID
        String[] messageIdHeaders = message.getHeader("Message-ID");
        if (messageIdHeaders != null && messageIdHeaders.length > 0) {
            emailMessage.setMessageId(messageIdHeaders[0]);
        }
        
        // Get subject
        emailMessage.setSubject(message.getSubject());
        
        // Get from
        if (message.getFrom() != null && message.getFrom().length > 0) {
            emailMessage.setFrom(message.getFrom()[0].toString());
        }
        
        // Get to
        if (message.getAllRecipients() != null) {
            List<String> toAddresses = new ArrayList<>();
            for (jakarta.mail.Address addr : message.getAllRecipients()) {
                toAddresses.add(addr.toString());
            }
            emailMessage.setTo(toAddresses);
        }
        
        // Get dates
        if (message.getReceivedDate() != null) {
            emailMessage.setReceivedDate(message.getReceivedDate().toString());
        }
        if (message.getSentDate() != null) {
            emailMessage.setSentDate(message.getSentDate().toString());
        }
        
        // Get size
        emailMessage.setSize(message.getSize());
        
        // Get content/data
        Object content = message.getContent();
        if (content != null) {
            emailMessage.setData(content.toString());
        }
        
        return emailMessage;
    }
    
    /**
     * Extracts the Message-ID from a message.
     */
    private String extractMessageId(Message message) {
        try {
            String[] messageIdHeaders = message.getHeader("Message-ID");
            if (messageIdHeaders != null && messageIdHeaders.length > 0) {
                return messageIdHeaders[0];
            }
        } catch (Exception e) {
            logger.fine("Could not extract Message-ID: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Gets IMAP properties configuration.
     */
    private Properties getImapProperties(String host) {
        Properties props = new Properties();
        props.setProperty("mail.store.protocol", IMAPS_PROTOCOL);
        props.setProperty("mail.imaps.host", host);
        props.setProperty("mail.imaps.port", "993");
        props.setProperty("mail.imaps.ssl.enable", "true");
        props.setProperty("mail.imaps.ssl.protocols", "TLSv1.2 TLSv1.3");
        props.setProperty("mail.imaps.ssl.ciphersuites", 
                "TLS_AES_256_GCM_SHA384 TLS_AES_128_GCM_SHA256 " +
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384 TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256");
        props.setProperty("mail.imaps.connectiontimeout", String.valueOf(DEFAULT_TIMEOUT));
        props.setProperty("mail.imaps.timeout", String.valueOf(DEFAULT_TIMEOUT));
        props.setProperty("mail.imaps.ssl.trust", "*");
        return props;
    }
}
