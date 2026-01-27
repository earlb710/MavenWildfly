package interfaces.comms.service;

import interfaces.comms.model.SmtpConnectionInfo;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import javax.ejb.Stateless;
import javax.inject.Inject;
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
    
    @Inject
    private SmtpConnectionCacheService cacheService;

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
            // Validate input parameters
            if (host == null || host.trim().isEmpty()) {
                result.put("success", false);
                result.put("error", "Host is required");
                return result;
            }
            
            if (username == null || username.trim().isEmpty()) {
                result.put("success", false);
                result.put("error", "Username is required");
                return result;
            }
            
            if (password == null) {
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
                result.put("success", true);
                result.put("host", host);
                result.put("username", username);
                result.put("port", port);
                result.put("cached", true);
            } else {
                result.put("success", false);
                result.put("error", "Connection not established");
            }
            
        } catch (Exception e) {
            logger.severe("Failed to open cached SMTP connection: " + e.getMessage());
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
            boolean closed = cacheService.closeConnection(host, username);
            result.put("success", closed);
            if (!closed) {
                result.put("message", "Connection not found in cache");
            }
        } catch (Exception e) {
            logger.severe("Failed to close SMTP connection: " + e.getMessage());
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
            // Validate required fields
            if (smtpHost == null || smtpHost.trim().isEmpty()) {
                result.put("success", false);
                result.put("error", "smtpHost is required");
                return result;
            }
            
            if (smtpUser == null || smtpUser.trim().isEmpty()) {
                result.put("success", false);
                result.put("error", "smtpUser is required");
                return result;
            }
            
            if (fromAddress == null || fromAddress.trim().isEmpty()) {
                result.put("success", false);
                result.put("error", "fromAddress is required");
                return result;
            }
            
            if (toAddress == null || toAddress.trim().isEmpty()) {
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
                result.put("success", false);
                result.put("error", "Connection not open. Use /api/smtp/open to establish a connection first");
                return result;
            }
            
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
            
            long sendTime = System.currentTimeMillis() - startTime;
            
            result.put("success", true);
            result.put("smtpHost", smtpHost);
            result.put("smtpUser", smtpUser);
            result.put("from", fromAddress);
            result.put("to", toAddress);
            result.put("sendTimeMs", sendTime);
            
            logger.info("Email sent successfully from " + fromAddress + " to " + toAddress + " in " + sendTime + "ms");
            
        } catch (Exception e) {
            logger.severe("Failed to send email: " + e.getMessage());
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
            // Validate required fields
            if (smtpHost == null || smtpHost.trim().isEmpty()) {
                result.put("success", false);
                result.put("error", "smtpHost is required");
                return result;
            }
            
            if (smtpUser == null || smtpUser.trim().isEmpty()) {
                result.put("success", false);
                result.put("error", "smtpUser is required");
                return result;
            }
            
            if (data == null || data.trim().isEmpty()) {
                result.put("success", false);
                result.put("error", "data is required");
                return result;
            }
            
            // Get existing cached connection (do NOT create new one)
            SmtpConnectionInfo connectionInfo = cacheService.getExistingConnection(smtpHost, smtpUser);
            
            if (connectionInfo == null || !connectionInfo.isConnected()) {
                result.put("success", false);
                result.put("error", "Connection not open. Use /api/smtp/open to establish a connection first");
                return result;
            }
            
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
            
            long sendTime = System.currentTimeMillis() - startTime;
            
            result.put("success", true);
            result.put("smtpHost", smtpHost);
            result.put("smtpUser", smtpUser);
            result.put("sendTimeMs", sendTime);
            result.put("dataSize", emlData.length);
            
            logger.info("Email sent successfully via .eml data (" + emlData.length + " bytes) in " + sendTime + "ms");
            
        } catch (Exception e) {
            logger.severe("Failed to send email from .eml data: " + e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
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
