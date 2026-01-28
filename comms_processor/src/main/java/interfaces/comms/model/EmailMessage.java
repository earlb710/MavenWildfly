package interfaces.comms.model;

import java.util.List;

/**
 * Model class representing an email message with metadata and content.
 * This class contains the same fields as the oldestMessage/newestMessage response
 * plus a data field for the email content.
 */
public class EmailMessage {
    private Integer messageNumber;
    private String messageId;
    private String subject;
    private String from;
    private List<String> to;
    private String receivedDate;
    private String sentDate;
    private Integer size;
    private String data; // Email content/body
    
    /**
     * Default constructor.
     */
    public EmailMessage() {
    }
    
    /**
     * Full constructor.
     * 
     * @param messageNumber The message number (position in folder)
     * @param messageId The Message-ID header (unique identifier)
     * @param subject The email subject
     * @param from The sender's email address
     * @param to List of recipient email addresses
     * @param receivedDate The date the message was received
     * @param sentDate The date the message was sent
     * @param size The size of the message in bytes
     * @param data The email content/body
     */
    public EmailMessage(Integer messageNumber, String messageId, String subject, 
                       String from, List<String> to, String receivedDate, 
                       String sentDate, Integer size, String data) {
        this.messageNumber = messageNumber;
        this.messageId = messageId;
        this.subject = subject;
        this.from = from;
        this.to = to;
        this.receivedDate = receivedDate;
        this.sentDate = sentDate;
        this.size = size;
        this.data = data;
    }
    
    // Getters and Setters
    
    public Integer getMessageNumber() {
        return messageNumber;
    }
    
    public void setMessageNumber(Integer messageNumber) {
        this.messageNumber = messageNumber;
    }
    
    public String getMessageId() {
        return messageId;
    }
    
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public String getFrom() {
        return from;
    }
    
    public void setFrom(String from) {
        this.from = from;
    }
    
    public List<String> getTo() {
        return to;
    }
    
    public void setTo(List<String> to) {
        this.to = to;
    }
    
    public String getReceivedDate() {
        return receivedDate;
    }
    
    public void setReceivedDate(String receivedDate) {
        this.receivedDate = receivedDate;
    }
    
    public String getSentDate() {
        return sentDate;
    }
    
    public void setSentDate(String sentDate) {
        this.sentDate = sentDate;
    }
    
    public Integer getSize() {
        return size;
    }
    
    public void setSize(Integer size) {
        this.size = size;
    }
    
    public String getData() {
        return data;
    }
    
    public void setData(String data) {
        this.data = data;
    }
    
    @Override
    public String toString() {
        return "EmailMessage{" +
                "messageNumber=" + messageNumber +
                ", messageId='" + messageId + '\'' +
                ", subject='" + subject + '\'' +
                ", from='" + from + '\'' +
                ", to=" + to +
                ", receivedDate='" + receivedDate + '\'' +
                ", sentDate='" + sentDate + '\'' +
                ", size=" + size +
                ", data='" + (data != null ? data.substring(0, Math.min(50, data.length())) + "..." : null) + '\'' +
                '}';
    }
}
