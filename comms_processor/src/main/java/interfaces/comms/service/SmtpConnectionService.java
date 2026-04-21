package interfaces.comms.service;

import interfaces.comms.model.SmtpConnectionInfo;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

/**
 * Service to handle SMTP connections and email sending with TLS encryption.
 * Provides functionality for SMTP server connectivity and email transmission.
 */
@Stateless
public class SmtpConnectionService {

    private static final Logger logger = Logger.getLogger(SmtpConnectionService.class.getName());
    
    private static final String SMTP_PROTOCOL = "smtps";
    private static final int DEFAULT_TIMEOUT = 10000; // 10 seconds
    private static final int DEFAULT_SMTP_PORT = 465; // SMTPS port
    private static final int DEFAULT_MAX_BATCH_SIZE = 100;
    private static final int DEFAULT_MIN_BATCH_SIZE = 1;
    
    private final int maxBatchSize;
    private final int minBatchSize;
    
    @Inject
    private SmtpConnectionCacheService cacheService;
    
    public SmtpConnectionService() {
        // Read system properties for batch control
        maxBatchSize = getSystemPropertyInt("email-sender.maxBatchSize", DEFAULT_MAX_BATCH_SIZE);
        minBatchSize = getSystemPropertyInt("email-sender.minBatchSize", DEFAULT_MIN_BATCH_SIZE);
        logger.info("SmtpConnectionService initialized with maxBatchSize: " + maxBatchSize + 
                    ", minBatchSize: " + minBatchSize);
    }
    
    /**
     * Reads an integer system property with a default value.
     */
    private int getSystemPropertyInt(String propertyName, int defaultValue) {
        try {
            String value = System.getProperty(propertyName);
            if (value != null && !value.trim().isEmpty()) {
                return Integer.parseInt(value.trim());
            }
        } catch (NumberFormatException e) {
            logger.warning("Invalid value for system property " + propertyName + ", using default: " + defaultValue);
        }
        return defaultValue;
    }

    /**
     * Opens a cached SMTP connection.
     * 
     * @param host The SMTP server hostname or IP address
     * @param username The username for authentication
     * @param password The password for authentication
     * @param port The SMTP port (optional, defaults to 465)
     * @return Map containing success status and connection info
     */
    public Map<String, Object> openConnection(String host, String username, String password, Integer port) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            logger.info("Attempting to open SMTP connection - host: " + host + ", username: " + username);
            
            // Validate input parameters
            if (host == null || host.trim().isEmpty()) {
                logger.warning("SMTP connection failed: Host is required");
                result.put("success", false);
                result.put("error", "Host is required");
                return result;
            }
            
            if (username == null || username.trim().isEmpty()) {
                logger.warning("SMTP connection failed: Username is required");
                result.put("success", false);
                result.put("error", "Username is required");
                return result;
            }
            
            if (password == null) {
                logger.warning("SMTP connection failed: Password is required");
                result.put("success", false);
                result.put("error", "Password is required");
                return result;
            }
            
            if (port == null) {
                port = DEFAULT_SMTP_PORT;
            }
            
            // Configure SMTP properties
            Properties props = getSmtpProperties(host, port);
            
            // Get or create cached connection
            SmtpConnectionInfo connectionInfo = cacheService.getOrCreateConnection(host, username, password, props);
            
            if (connectionInfo.isConnected()) {
                logger.info("SMTP connection opened successfully - host: " + host + ", username: " + username + ", port: " + port);
                result.put("success", true);
                result.put("host", host);
                result.put("username", username);
                result.put("port", port);
                result.put("cached", true);
            } else {
                logger.severe("SMTP connection failed: Connection not established - host: " + host + ", username: " + username);
                result.put("success", false);
                result.put("error", "Connection not established");
            }
            
        } catch (Exception e) {
            logger.severe("Failed to open cached SMTP connection - host: " + host + ", username: " + username + ", error: " + e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Closes a cached SMTP connection.
     * 
     * @param host The SMTP server hostname or IP address
     * @param username The username
     * @return Map containing success status
     */
    public Map<String, Object> closeConnection(String host, String username) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            logger.info("Attempting to close SMTP connection - host: " + host + ", username: " + username);
            boolean closed = cacheService.closeConnection(host, username);
            result.put("success", closed);
            if (!closed) {
                logger.warning("SMTP connection not found in cache - host: " + host + ", username: " + username);
                result.put("message", "Connection not found in cache");
            } else {
                logger.info("SMTP connection closed successfully - host: " + host + ", username: " + username);
            }
        } catch (Exception e) {
            logger.severe("Failed to close SMTP connection - host: " + host + ", username: " + username + ", error: " + e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Sends a text email using a cached SMTP connection.
     * 
     * @param smtpHost The SMTP server host
     * @param smtpUser The SMTP username
     * @param fromAddress From email address
     * @param toAddress To email address
     * @param subject Email subject
     * @param body Email body
     * @return Map containing success status
     */
    public Map<String, Object> sendTextMessage(String smtpHost, String smtpUser, String fromAddress, 
                                         String toAddress, String subject, String body) {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("Attempting to send text email - from: " + fromAddress + ", to: " + toAddress + ", subject: " + subject);
            
            // Validate required fields
            if (smtpHost == null || smtpHost.trim().isEmpty()) {
                logger.warning("Send text email failed: smtpHost is required");
                result.put("success", false);
                result.put("error", "smtpHost is required");
                return result;
            }
            
            if (smtpUser == null || smtpUser.trim().isEmpty()) {
                logger.warning("Send text email failed: smtpUser is required");
                result.put("success", false);
                result.put("error", "smtpUser is required");
                return result;
            }
            
            if (fromAddress == null || fromAddress.trim().isEmpty()) {
                logger.warning("Send text email failed: fromAddress is required");
                result.put("success", false);
                result.put("error", "fromAddress is required");
                return result;
            }
            
            if (toAddress == null || toAddress.trim().isEmpty()) {
                logger.warning("Send text email failed: toAddress is required");
                result.put("success", false);
                result.put("error", "toAddress is required");
                return result;
            }
            
            if (subject == null) {
                subject = "";
            }
            
            if (body == null) {
                body = "";
            }
            
            // Get existing cached connection (do NOT create new one)
            SmtpConnectionInfo connectionInfo = cacheService.getExistingConnection(smtpHost, smtpUser);
            
            if (connectionInfo == null || !connectionInfo.isConnected()) {
                logger.severe("Send text email failed: Connection not open - host: " + smtpHost + ", user: " + smtpUser);
                result.put("success", false);
                result.put("error", "Connection not open. Use /api/smtp/open to establish a connection first");
                return result;
            }

            connectionInfo = reconnectIfThresholdReached(smtpHost, smtpUser, connectionInfo);
             
            // Create message
            Properties props = getSmtpProperties(smtpHost, DEFAULT_SMTP_PORT);
            Session session = Session.getInstance(props, null);
            MimeMessage message = new MimeMessage(session);
            
            message.setFrom(new InternetAddress(fromAddress));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(toAddress));
            message.setSubject(subject);
            message.setText(body);
            
            // Send message using cached transport
            Transport transport = connectionInfo.getTransport();
            transport.sendMessage(message, message.getAllRecipients());
             
            // Increment email counter
            connectionInfo.incrementEmailsSent();
            SmtpConnectionInfo activeConnection = reconnectIfThresholdReached(smtpHost, smtpUser, connectionInfo);
            boolean autoReconnected = activeConnection != connectionInfo;
            connectionInfo = activeConnection;
             
            long sendTime = System.currentTimeMillis() - startTime;
             
            result.put("success", true);
            result.put("smtpHost", smtpHost);
            result.put("smtpUser", smtpUser);
            result.put("from", fromAddress);
            result.put("to", toAddress);
            result.put("sendTimeMs", sendTime);
            result.put("emailsSentSinceConnect", connectionInfo.getEmailsSentSinceConnect());
            result.put("maxBatchSize", maxBatchSize);

            if (autoReconnected) {
                result.put("reconnectCount", 1);
                result.put("message", "SMTP connection automatically reconnected after reaching max batch size (" + maxBatchSize + ")");
            }
             
            logger.info("Text email sent successfully - from: " + fromAddress + ", to: " + toAddress + ", sendTime: " + sendTime + "ms, emailsSentSinceConnect: " + connectionInfo.getEmailsSentSinceConnect());

            
        } catch (Exception e) {
            logger.severe("Failed to send text email - from: " + fromAddress + ", to: " + toAddress + ", error: " + e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Sends an email in .eml format using a cached SMTP connection.
     * Accepts base64 encoded (optionally gzipped) .eml format data.
     * 
     * @param smtpHost The SMTP server host
     * @param smtpUser The SMTP username
     * @param data Base64 encoded .eml data (possibly gzipped)
     * @return Map containing success status
     */
    public Map<String, Object> sendEmail(String smtpHost, String smtpUser, String data) {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("Attempting to send .eml format email - host: " + smtpHost + ", user: " + smtpUser);
            
            // Validate required fields
            if (smtpHost == null || smtpHost.trim().isEmpty()) {
                logger.warning("Send .eml email failed: smtpHost is required");
                result.put("success", false);
                result.put("error", "smtpHost is required");
                return result;
            }
            
            if (smtpUser == null || smtpUser.trim().isEmpty()) {
                logger.warning("Send .eml email failed: smtpUser is required");
                result.put("success", false);
                result.put("error", "smtpUser is required");
                return result;
            }
            
            if (data == null || data.trim().isEmpty()) {
                logger.warning("Send .eml email failed: data is required");
                result.put("success", false);
                result.put("error", "data is required");
                return result;
            }
            
            // Get existing cached connection (do NOT create new one)
            SmtpConnectionInfo connectionInfo = cacheService.getExistingConnection(smtpHost, smtpUser);
            
            if (connectionInfo == null || !connectionInfo.isConnected()) {
                logger.severe("Send .eml email failed: Connection not open - host: " + smtpHost + ", user: " + smtpUser);
                result.put("success", false);
                result.put("error", "Connection not open. Use /api/smtp/open to establish a connection first");
                return result;
            }

            connectionInfo = reconnectIfThresholdReached(smtpHost, smtpUser, connectionInfo);
             
            // Process and send the email
            Map<String, Object> sendResult = processAndSendEmail(smtpHost, connectionInfo, data);
            if (!(Boolean) sendResult.get("success")) {
                return sendResult;
            }
             
            // Increment email counter
            connectionInfo.incrementEmailsSent();
            SmtpConnectionInfo activeConnection = reconnectIfThresholdReached(smtpHost, smtpUser, connectionInfo);
            boolean autoReconnected = activeConnection != connectionInfo;
            connectionInfo = activeConnection;
             
            long sendTime = System.currentTimeMillis() - startTime;
             
            result.put("success", true);
            result.put("smtpHost", smtpHost);
            result.put("smtpUser", smtpUser);
            result.put("sendTimeMs", sendTime);
            result.put("dataSize", sendResult.get("dataSize"));
            result.put("emailsSentSinceConnect", connectionInfo.getEmailsSentSinceConnect());
            result.put("maxBatchSize", maxBatchSize);

            if (autoReconnected) {
                result.put("reconnectCount", 1);
                result.put("message", "SMTP connection automatically reconnected after reaching max batch size (" + maxBatchSize + ")");
            }
             
            logger.info(".eml email sent successfully - host: " + smtpHost + ", user: " + smtpUser + ", dataSize: " + sendResult.get("dataSize") + " bytes, sendTime: " + sendTime + "ms, emailsSentSinceConnect: " + connectionInfo.getEmailsSentSinceConnect());

            
        } catch (Exception e) {
            logger.severe("Failed to send .eml email - host: " + smtpHost + ", user: " + smtpUser + ", error: " + e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Sends multiple emails in .eml format using a cached SMTP connection.
     * Accepts an array of base64 encoded (optionally gzipped) .eml format data.
     * Automatically reconnects when maxBatchSize is reached.
     * 
     * @param smtpHost The SMTP server host
     * @param smtpUser The SMTP username
     * @param dataArray Array of base64 encoded .eml data (possibly gzipped)
     * @return Map containing success status and results for each email
     */
    public Map<String, Object> sendEmails(String smtpHost, String smtpUser, java.util.List<String> dataArray) {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("Attempting to send batch of " + (dataArray != null ? dataArray.size() : 0) + " .eml emails - host: " + smtpHost + ", user: " + smtpUser);
            
            // Validate required fields
            if (smtpHost == null || smtpHost.trim().isEmpty()) {
                logger.warning("Send batch emails failed: smtpHost is required");
                result.put("success", false);
                result.put("error", "smtpHost is required");
                return result;
            }
            
            if (smtpUser == null || smtpUser.trim().isEmpty()) {
                logger.warning("Send batch emails failed: smtpUser is required");
                result.put("success", false);
                result.put("error", "smtpUser is required");
                return result;
            }
            
            if (dataArray == null || dataArray.isEmpty()) {
                logger.warning("Send batch emails failed: data array is required and must not be empty");
                result.put("success", false);
                result.put("error", "data array is required and must not be empty");
                return result;
            }
            
            // Check batch size constraints
            if (dataArray.size() < minBatchSize) {
                logger.warning("Send batch emails failed: Batch size (" + dataArray.size() + ") is below minimum (" + minBatchSize + ")");
                result.put("success", false);
                result.put("error", "Batch size (" + dataArray.size() + ") is below minimum (" + minBatchSize + ")");
                return result;
            }
            
            // Get existing cached connection (do NOT create new one)
            SmtpConnectionInfo connectionInfo = cacheService.getExistingConnection(smtpHost, smtpUser);
            
            if (connectionInfo == null || !connectionInfo.isConnected()) {
                logger.severe("Send batch emails failed: Connection not open - host: " + smtpHost + ", user: " + smtpUser);
                result.put("success", false);
                result.put("error", "Connection not open. Use /api/smtp/open to establish a connection first");
                return result;
            }
             
            // Process and send each email
            java.util.List<Map<String, Object>> results = new java.util.ArrayList<>();
            int successCount = 0;
            int failureCount = 0;
            long totalDataSize = 0;
            int reconnectCount = 0;
            
            for (int i = 0; i < dataArray.size(); i++) {
                String data = dataArray.get(i);
                Map<String, Object> emailResult = new HashMap<>();
                emailResult.put("index", i);

                try {
                    SmtpConnectionInfo activeConnection = reconnectIfThresholdReached(smtpHost, smtpUser, connectionInfo);
                    if (activeConnection != connectionInfo) {
                        reconnectCount++;
                    }
                    connectionInfo = activeConnection;
                } catch (Exception e) {
                    logger.severe("Failed to auto-reconnect before batch send: " + e.getMessage());
                    emailResult.put("success", false);
                    emailResult.put("error", "Auto-reconnect failed: " + e.getMessage());
                    failureCount++;
                    results.add(emailResult);
                    continue;
                }
                 
                if (data == null || data.trim().isEmpty()) {
                    logger.warning("Email at index " + i + " has empty data - skipping");
                    emailResult.put("success", false);
                    emailResult.put("error", "data at index " + i + " is empty");
                    failureCount++;
                } else {
                    Map<String, Object> sendResult = processAndSendEmail(smtpHost, connectionInfo, data);
                    emailResult.put("success", sendResult.get("success"));
                    
                    if ((Boolean) sendResult.get("success")) {
                        Object dataSizeObj = sendResult.get("dataSize");
                        int dataSize = dataSizeObj instanceof Number ? ((Number) dataSizeObj).intValue() : 0;
                        emailResult.put("dataSize", dataSize);
                        totalDataSize += dataSize;
                        successCount++;
                        
                        // Increment email counter (thread-safe)
                        connectionInfo.incrementEmailsSent();
                        SmtpConnectionInfo activeConnection = reconnectIfThresholdReached(smtpHost, smtpUser, connectionInfo);
                        if (activeConnection != connectionInfo) {
                            reconnectCount++;
                        }
                        connectionInfo = activeConnection;
                        logger.fine("Email at index " + i + " sent successfully - dataSize: " + dataSize + " bytes");
                    } else {
                        emailResult.put("error", sendResult.get("error"));
                        failureCount++;
                        logger.warning("Email at index " + i + " failed - error: " + sendResult.get("error"));
                    }
                }
                
                results.add(emailResult);
            }
            
            long sendTime = System.currentTimeMillis() - startTime;
            
            result.put("success", failureCount == 0);
            result.put("smtpHost", smtpHost);
            result.put("smtpUser", smtpUser);
            result.put("sendTimeMs", sendTime);
            result.put("totalEmails", dataArray.size());
            result.put("successCount", successCount);
            result.put("failureCount", failureCount);
            result.put("totalDataSize", totalDataSize);
            result.put("emailsSentSinceConnect", connectionInfo.getEmailsSentSinceConnect());
            result.put("maxBatchSize", maxBatchSize);
            if (reconnectCount > 0) {
                result.put("reconnectCount", reconnectCount);
                result.put("message", "Auto-reconnected " + reconnectCount + " time(s) during batch processing");
            }
            result.put("results", results);
            
            logger.info("Batch send completed - total: " + dataArray.size() + ", success: " + successCount + ", failed: " + failureCount + 
                       ", totalDataSize: " + totalDataSize + " bytes, sendTime: " + sendTime + "ms" +
                       (reconnectCount > 0 ? ", auto-reconnected: " + reconnectCount + " times" : "") + 
                       ", emailsSentSinceConnect: " + connectionInfo.getEmailsSentSinceConnect());

            
        } catch (Exception e) {
            logger.severe("Failed to send batch of emails - host: " + smtpHost + ", user: " + smtpUser + ", error: " + e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Helper method to process and send a single email.
     * Handles base64 decoding, gzip decompression, and .eml parsing.
     * 
     * @param smtpHost The SMTP server host
     * @param connectionInfo The cached connection info
     * @param data Base64 encoded .eml data
     * @return Map containing success status and data size
     */
    private Map<String, Object> processAndSendEmail(String smtpHost, SmtpConnectionInfo connectionInfo, String data) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Decode base64 data
            byte[] decodedData;
            try {
                decodedData = Base64.getDecoder().decode(data);
            } catch (IllegalArgumentException e) {
                result.put("success", false);
                result.put("error", "Invalid base64 encoding: " + e.getMessage());
                return result;
            }
            
            // Try to detect if data is gzipped (check magic number)
            byte[] emlData;
            if (decodedData.length >= 2 && decodedData[0] == (byte) 0x1f && decodedData[1] == (byte) 0x8b) {
                // Data is gzipped, decompress it
                logger.fine("Detected gzipped data, decompressing...");
                try (ByteArrayInputStream bais = new ByteArrayInputStream(decodedData);
                     GZIPInputStream gzis = new GZIPInputStream(bais);
                     ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = gzis.read(buffer)) > 0) {
                        baos.write(buffer, 0, len);
                    }
                    emlData = baos.toByteArray();
                } catch (Exception e) {
                    result.put("success", false);
                    result.put("error", "Failed to decompress gzipped data: " + e.getMessage());
                    return result;
                }
            } else {
                // Data is not gzipped, use as-is
                emlData = decodedData;
            }
            
            // Parse .eml data as MimeMessage
            Properties props = getSmtpProperties(smtpHost, DEFAULT_SMTP_PORT);
            Session session = Session.getInstance(props, null);
            MimeMessage message;
            
            try (ByteArrayInputStream bais = new ByteArrayInputStream(emlData)) {
                message = new MimeMessage(session, bais);
            } catch (Exception e) {
                result.put("success", false);
                result.put("error", "Failed to parse .eml data: " + e.getMessage());
                return result;
            }
            
            // Send message using cached transport
            Transport transport = connectionInfo.getTransport();
            transport.sendMessage(message, message.getAllRecipients());
            
            result.put("success", true);
            result.put("dataSize", emlData.length);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * Reconnects the SMTP transport when the configured send threshold has been reached.
     */
    private SmtpConnectionInfo reconnectIfThresholdReached(String smtpHost, String smtpUser, SmtpConnectionInfo connectionInfo) throws Exception {
        if (connectionInfo == null || maxBatchSize <= 0 || connectionInfo.getEmailsSentSinceConnect() < maxBatchSize) {
            return connectionInfo;
        }

        String password = connectionInfo.getPassword();
        if (password == null || password.isEmpty()) {
            throw new IllegalStateException("Password not available for automatic reconnection. Connection may have expired.");
        }

        int port = connectionInfo.getPort() > 0 ? connectionInfo.getPort() : DEFAULT_SMTP_PORT;
        logger.info("SMTP send threshold reached for host: " + smtpHost + ", user: " + smtpUser +
                ". Disconnecting and reconnecting after " + connectionInfo.getEmailsSentSinceConnect() + " emails.");
        return cacheService.reconnect(smtpHost, smtpUser, password, getSmtpProperties(smtpHost, port));
    }
    
    /**
     * Creates SMTP connection properties with TLS encryption settings.
     * 
     * @param host The SMTP server hostname
     * @param port The SMTP port
     * @return Properties configured for SMTP connection
     */
    private Properties getSmtpProperties(String host, int port) {
        Properties props = new Properties();
        
        // Enable SMTP protocol
        props.setProperty("mail.transport.protocol", SMTP_PROTOCOL);
        
        // Configure connection settings
        props.setProperty("mail.smtps.host", host);
        props.setProperty("mail.smtps.port", String.valueOf(port));
        props.setProperty("mail.smtps.connectiontimeout", String.valueOf(DEFAULT_TIMEOUT));
        props.setProperty("mail.smtps.timeout", String.valueOf(DEFAULT_TIMEOUT));
        props.setProperty("mail.smtps.auth", "true");
        
        // Enable TLS/SSL with latest encryption
        props.setProperty("mail.smtps.ssl.enable", "true");
        props.setProperty("mail.smtps.ssl.protocols", "TLSv1.2 TLSv1.3"); // Use TLS 1.2 and 1.3
        props.setProperty("mail.smtps.ssl.checkserveridentity", "true");
        // Note: In production, configure proper certificate trust store instead of trusting all
        // For testing purposes only - this accepts all certificates
        props.setProperty("mail.smtps.ssl.trust", "*");
        
        return props;
    }
}
