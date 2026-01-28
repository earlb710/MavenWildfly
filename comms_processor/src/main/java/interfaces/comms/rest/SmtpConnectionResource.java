package interfaces.comms.rest;

import interfaces.comms.service.SmtpConnectionCacheService;
import interfaces.comms.service.SmtpConnectionService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST resource for SMTP connection management and email sending.
 * Provides endpoints for SMTP connection caching and email transmission.
 */
@Path("/smtp")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SmtpConnectionResource {

    @Inject
    private SmtpConnectionService smtpConnectionService;
    
    @Inject
    private SmtpConnectionCacheService cacheService;
    
    @Inject
    private interfaces.comms.service.EmailSenderStatsService statsService;

    /**
     * Opens a cached SMTP connection.
     * 
     * Request body example:
     * {
     *   "host": "smtp.gmail.com",
     *   "username": "user@gmail.com",
     *   "password": "app-password",
     *   "port": 465  // optional, defaults to 465
     * }
     * 
     * @param request Map containing host, username, password, and optional port
     * @return Response with success status
     */
    @POST
    @Path("/open")
    public Response openConnection(Map<String, Object> request) {
        // Validate request body
        if (request == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Request body is required");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error)
                    .build();
        }
        
        String host = (String) request.get("host");
        String username = (String) request.get("username");
        String password = (String) request.get("password");
        Integer port = request.get("port") != null ? ((Number) request.get("port")).intValue() : null;
        
        Map<String, Object> result = smtpConnectionService.openConnection(host, username, password, port);
        
        if ((Boolean) result.get("success")) {
            return Response.ok(result).build();
        } else {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(result)
                    .build();
        }
    }
    
    /**
     * Closes a cached SMTP connection.
     * 
     * Request body example:
     * {
     *   "host": "smtp.gmail.com",
     *   "username": "user@gmail.com"
     * }
     * 
     * @param request Map containing host and username
     * @return Response with success status
     */
    @POST
    @Path("/close")
    public Response closeConnection(Map<String, String> request) {
        // Validate request body
        if (request == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Request body is required");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error)
                    .build();
        }
        
        String host = request.get("host");
        String username = request.get("username");
        
        Map<String, Object> result = smtpConnectionService.closeConnection(host, username);
        
        return Response.ok(result).build();
    }
    
    /**
     * Sends a text email using a cached SMTP connection.
     * Requires an existing cached connection (use /api/smtp/open first).
     * 
     * Request body example:
     * {
     *   "smtpHost": "smtp.gmail.com",
     *   "smtpUser": "user@gmail.com",
     *   "fromAddress": "user@gmail.com",
     *   "toAddress": "recipient@example.com",
     *   "subject": "Test Email",
     *   "body": "This is a test email."
     * }
     * 
     * @param request Map containing email parameters
     * @return Response with success status and send time
     */
    @POST
    @Path("/sendTextMessage")
    public Response sendTextMessage(Map<String, String> request) {
        // Validate request body
        if (request == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Request body is required");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error)
                    .build();
        }
        
        String smtpHost = request.get("smtpHost");
        String smtpUser = request.get("smtpUser");
        String fromAddress = request.get("fromAddress");
        String toAddress = request.get("toAddress");
        String subject = request.get("subject");
        String body = request.get("body");
        
        Map<String, Object> result = smtpConnectionService.sendTextMessage(
            smtpHost, smtpUser, fromAddress, toAddress, subject, body);
        
        if ((Boolean) result.get("success")) {
            return Response.ok(result).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(result)
                    .build();
        }
    }
    
    /**
     * Sends an email in .eml format using a cached SMTP connection.
     * Requires an existing cached connection (use /api/smtp/open first).
     * 
     * Request body example (single email):
     * {
     *   "smtpHost": "smtp.gmail.com",
     *   "smtpUser": "user@gmail.com",
     *   "data": "base64-encoded-eml-data"
     * }
     * 
     * Request body example (multiple emails):
     * {
     *   "smtpHost": "smtp.gmail.com",
     *   "smtpUser": "user@gmail.com",
     *   "data": ["base64-encoded-eml-data-1", "base64-encoded-eml-data-2"]
     * }
     * 
     * The data field can be a single string or an array of strings.
     * Each data item should contain base64 encoded .eml format email.
     * Data can be optionally gzipped before base64 encoding.
     * 
     * @param request Map containing email parameters
     * @return Response with success status and send time
     */
    @POST
    @Path("/send")
    public Response sendEmail(Map<String, Object> request) {
        // Validate request body
        if (request == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Request body is required");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error)
                    .build();
        }
        
        String smtpHost = (String) request.get("smtpHost");
        String smtpUser = (String) request.get("smtpUser");
        Object dataField = request.get("data");
        
        Map<String, Object> result;
        
        // Check if data is an array or a single string
        if (dataField instanceof java.util.List) {
            // Handle array of data - validate all elements are strings
            @SuppressWarnings("unchecked")
            java.util.List<Object> rawList = (java.util.List<Object>) dataField;
            
            // Validate all elements are strings
            java.util.List<String> dataArray = new java.util.ArrayList<>();
            for (int i = 0; i < rawList.size(); i++) {
                Object element = rawList.get(i);
                if (!(element instanceof String)) {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("error", "data array element at index " + i + " must be a string");
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(error)
                            .build();
                }
                dataArray.add((String) element);
            }
            
            result = smtpConnectionService.sendEmails(smtpHost, smtpUser, dataArray);
        } else if (dataField instanceof String) {
            // Handle single data string
            String data = (String) dataField;
            result = smtpConnectionService.sendEmail(smtpHost, smtpUser, data);
        } else {
            // Invalid data type
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "data field must be a string or an array of strings");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error)
                    .build();
        }
        
        if ((Boolean) result.get("success")) {
            return Response.ok(result).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(result)
                    .build();
        }
    }
    
    /**
     * Gets the status of the SMTP connection cache.
     * Shows all open connections and their statistics for the last day.
     * 
     * @return Response with connection cache status and statistics
     */
    @GET
    @Path("/status")
    public Response getConnectionStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            
            // Get cache statistics
            Map<String, Object> cacheStats = cacheService.getCacheStats();
            status.put("cacheStats", cacheStats);
            
            // Get all connections
            List<Map<String, Object>> connections = cacheService.getAllConnections();
            status.put("connections", connections);
            
            status.put("timestamp", System.currentTimeMillis());
            
            return Response.ok(status).build();
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to retrieve connection status: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(errorResponse)
                          .build();
        }
    }
    
    /**
     * Gets email sender statistics.
     * Returns total number of emails sent, total size, error count, and recent errors.
     * 
     * @return Response with email sender statistics
     */
    @GET
    @Path("/stats")
    public Response getStats() {
        try {
            Map<String, Object> stats = statsService.getStatsMap();
            stats.put("timestamp", System.currentTimeMillis());
            
            return Response.ok(stats).build();
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to retrieve email sender statistics: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(errorResponse)
                          .build();
        }
    }
    
    /**
     * HTML version of SMTP connection status endpoint for browser viewing.
     * 
     * @return Response with HTML page showing SMTP connection status
     */
    @GET
    @Path("/status.html")
    @Produces(MediaType.TEXT_HTML)
    public Response getConnectionStatusHtml() {
        try {
            Map<String, Object> cacheStats = cacheService.getCacheStats();
            List<Map<String, Object>> connections = cacheService.getAllConnections();
            
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n");
            html.append("<html lang=\"en\">\n");
            html.append("<head>\n");
            html.append("    <meta charset=\"UTF-8\">\n");
            html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
            html.append("    <title>SMTP Connection Status - Communications Processor</title>\n");
            html.append("    <style>\n");
            html.append("        body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }\n");
            html.append("        .container { max-width: 1200px; margin: 0 auto; background-color: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n");
            html.append("        h1 { color: #333; border-bottom: 3px solid #007bff; padding-bottom: 10px; }\n");
            html.append("        h2 { color: #555; margin-top: 30px; }\n");
            html.append("        .status-ok { color: #28a745; font-weight: bold; }\n");
            html.append("        .info-box { background-color: #e9ecef; padding: 15px; border-radius: 5px; margin-bottom: 20px; }\n");
            html.append("        .info-box p { margin: 5px 0; }\n");
            html.append("        table { width: 100%; border-collapse: collapse; margin-top: 15px; }\n");
            html.append("        th { background-color: #007bff; color: white; padding: 12px; text-align: left; }\n");
            html.append("        td { padding: 10px; border-bottom: 1px solid #ddd; }\n");
            html.append("        tr:hover { background-color: #f8f9fa; }\n");
            html.append("        .timestamp { color: #6c757d; font-size: 0.9em; }\n");
            html.append("        .count { font-size: 1.2em; color: #007bff; font-weight: bold; }\n");
            html.append("        .nav-links { margin-bottom: 20px; }\n");
            html.append("        .nav-links a { display: inline-block; padding: 10px 15px; background-color: #007bff; color: white; text-decoration: none; border-radius: 4px; margin-right: 10px; }\n");
            html.append("        .nav-links a:hover { background-color: #0056b3; }\n");
            html.append("        .connected { color: #28a745; }\n");
            html.append("        .disconnected { color: #dc3545; }\n");
            html.append("    </style>\n");
            html.append("</head>\n");
            html.append("<body>\n");
            html.append("    <div class=\"container\">\n");
            html.append("        <h1>SMTP Connection Status</h1>\n");
            
            html.append("        <div class=\"nav-links\">\n");
            html.append("            <a href=\"/comms_processor/\">Home</a>\n");
            html.append("            <a href=\"/comms_processor/api/status.html\">API Status</a>\n");
            html.append("            <a href=\"/comms_processor/api/smtp/status\">JSON API</a>\n");
            html.append("            <a href=\"/comms_processor/api/smtp/stats.html\">SMTP Stats</a>\n");
            html.append("        </div>\n");
            
            html.append("        <div class=\"info-box\">\n");
            html.append("            <p><strong>Status:</strong> <span class=\"status-ok\">OK</span></p>\n");
            html.append("            <p><strong>Total Connections:</strong> <span class=\"count\">").append(cacheStats.get("totalConnections")).append("</span></p>\n");
            html.append("            <p><strong>Active Connections:</strong> <span class=\"count\">").append(cacheStats.get("activeConnections")).append("</span></p>\n");
            html.append("            <p><strong>Max Connections:</strong> ").append(cacheStats.get("maxConnections")).append("</p>\n");
            if (cacheStats.containsKey("readyPoolSize")) {
                html.append("            <p><strong>Ready Pool Size:</strong> ").append(cacheStats.get("readyPoolSize")).append("</p>\n");
            }
            html.append("            <p class=\"timestamp\">Generated: ").append(new java.util.Date()).append("</p>\n");
            html.append("        </div>\n");
            
            if (connections != null && !connections.isEmpty()) {
                html.append("        <h2>Active Connections</h2>\n");
                html.append("        <table>\n");
                html.append("            <thead>\n");
                html.append("                <tr>\n");
                html.append("                    <th>Host</th>\n");
                html.append("                    <th>Username</th>\n");
                html.append("                    <th>Connected</th>\n");
                html.append("                    <th>Created Time</th>\n");
                html.append("                    <th>Last Used</th>\n");
                html.append("                    <th>Idle Time (seconds)</th>\n");
                html.append("                    <th>Usage Count (24h)</th>\n");
                html.append("                </tr>\n");
                html.append("            </thead>\n");
                html.append("            <tbody>\n");
                
                for (Map<String, Object> conn : connections) {
                    html.append("                <tr>\n");
                    html.append("                    <td>").append(escapeHtml(String.valueOf(conn.get("host")))).append("</td>\n");
                    html.append("                    <td>").append(escapeHtml(String.valueOf(conn.get("username")))).append("</td>\n");
                    Object connected = conn.get("connected");
                    String connClass = (connected != null && connected.toString().equals("true")) ? "connected" : "disconnected";
                    html.append("                    <td class=\"").append(connClass).append("\">").append(escapeHtml(String.valueOf(connected))).append("</td>\n");
                    html.append("                    <td>").append(escapeHtml(String.valueOf(conn.get("createdTime")))).append("</td>\n");
                    html.append("                    <td>").append(escapeHtml(String.valueOf(conn.get("lastUsedTime")))).append("</td>\n");
                    html.append("                    <td>").append(escapeHtml(String.valueOf(conn.get("idleTimeSeconds")))).append("</td>\n");
                    html.append("                    <td>").append(escapeHtml(String.valueOf(conn.get("usageCountLastDay")))).append("</td>\n");
                    html.append("                </tr>\n");
                }
                
                html.append("            </tbody>\n");
                html.append("        </table>\n");
            } else {
                html.append("        <p>No active connections.</p>\n");
            }
            
            html.append("    </div>\n");
            html.append("</body>\n");
            html.append("</html>");
            
            return Response.ok(html.toString()).build();
        } catch (Exception e) {
            StringBuilder errorHtml = new StringBuilder();
            errorHtml.append("<!DOCTYPE html>\n");
            errorHtml.append("<html><head><title>Error</title></head><body>\n");
            errorHtml.append("<h1>Error</h1>\n");
            errorHtml.append("<p>Failed to retrieve SMTP connection status: ").append(escapeHtml(e.getMessage())).append("</p>\n");
            errorHtml.append("</body></html>");
            
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(errorHtml.toString())
                          .build();
        }
    }
    
    /**
     * HTML version of email sender statistics endpoint for browser viewing.
     * 
     * @return Response with HTML page showing email sender statistics
     */
    @GET
    @Path("/stats.html")
    @Produces(MediaType.TEXT_HTML)
    public Response getStatsHtml() {
        try {
            Map<String, Object> stats = statsService.getStatsMap();
            
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n");
            html.append("<html lang=\"en\">\n");
            html.append("<head>\n");
            html.append("    <meta charset=\"UTF-8\">\n");
            html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
            html.append("    <title>SMTP Email Sender Statistics - Communications Processor</title>\n");
            html.append("    <style>\n");
            html.append("        body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }\n");
            html.append("        .container { max-width: 1200px; margin: 0 auto; background-color: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n");
            html.append("        h1 { color: #333; border-bottom: 3px solid #007bff; padding-bottom: 10px; }\n");
            html.append("        h2 { color: #555; margin-top: 30px; }\n");
            html.append("        .status-ok { color: #28a745; font-weight: bold; }\n");
            html.append("        .info-box { background-color: #e9ecef; padding: 15px; border-radius: 5px; margin-bottom: 20px; }\n");
            html.append("        .info-box p { margin: 5px 0; }\n");
            html.append("        table { width: 100%; border-collapse: collapse; margin-top: 15px; }\n");
            html.append("        th { background-color: #007bff; color: white; padding: 12px; text-align: left; }\n");
            html.append("        td { padding: 10px; border-bottom: 1px solid #ddd; }\n");
            html.append("        tr:hover { background-color: #f8f9fa; }\n");
            html.append("        .timestamp { color: #6c757d; font-size: 0.9em; }\n");
            html.append("        .stat-value { font-size: 1.5em; color: #007bff; font-weight: bold; }\n");
            html.append("        .error-count { font-size: 1.5em; color: #dc3545; font-weight: bold; }\n");
            html.append("        .nav-links { margin-bottom: 20px; }\n");
            html.append("        .nav-links a { display: inline-block; padding: 10px 15px; background-color: #007bff; color: white; text-decoration: none; border-radius: 4px; margin-right: 10px; }\n");
            html.append("        .nav-links a:hover { background-color: #0056b3; }\n");
            html.append("        .error-details { font-size: 0.9em; color: #666; white-space: pre-wrap; }\n");
            html.append("    </style>\n");
            html.append("</head>\n");
            html.append("<body>\n");
            html.append("    <div class=\"container\">\n");
            html.append("        <h1>SMTP Email Sender Statistics</h1>\n");
            
            html.append("        <div class=\"nav-links\">\n");
            html.append("            <a href=\"/comms_processor/\">Home</a>\n");
            html.append("            <a href=\"/comms_processor/api/status.html\">API Status</a>\n");
            html.append("            <a href=\"/comms_processor/api/smtp/stats\">JSON API</a>\n");
            html.append("            <a href=\"/comms_processor/api/smtp/status.html\">SMTP Status</a>\n");
            html.append("        </div>\n");
            
            html.append("        <div class=\"info-box\">\n");
            html.append("            <p><strong>Total Emails Sent:</strong> <span class=\"stat-value\">").append(stats.get("totalEmailsSent")).append("</span></p>\n");
            html.append("            <p><strong>Total Size:</strong> ").append(formatBytes(((Number) stats.get("totalSizeBytes")).longValue())).append("</p>\n");
            html.append("            <p><strong>Total Errors:</strong> <span class=\"error-count\">").append(stats.get("totalErrors")).append("</span></p>\n");
            html.append("            <p class=\"timestamp\">Generated: ").append(new java.util.Date()).append("</p>\n");
            html.append("        </div>\n");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> recentErrors = (List<Map<String, Object>>) stats.get("recentErrors");
            if (recentErrors != null && !recentErrors.isEmpty()) {
                html.append("        <h2>Recent Errors (Last ").append(recentErrors.size()).append(")</h2>\n");
                html.append("        <table>\n");
                html.append("            <thead>\n");
                html.append("                <tr>\n");
                html.append("                    <th>Timestamp</th>\n");
                html.append("                    <th>Operation</th>\n");
                html.append("                    <th>Host</th>\n");
                html.append("                    <th>Username</th>\n");
                html.append("                    <th>Error Message</th>\n");
                html.append("                </tr>\n");
                html.append("            </thead>\n");
                html.append("            <tbody>\n");
                
                for (Map<String, Object> error : recentErrors) {
                    html.append("                <tr>\n");
                    html.append("                    <td>").append(escapeHtml(String.valueOf(error.get("timestamp")))).append("</td>\n");
                    html.append("                    <td>").append(escapeHtml(String.valueOf(error.get("operation")))).append("</td>\n");
                    html.append("                    <td>").append(escapeHtml(String.valueOf(error.get("host")))).append("</td>\n");
                    html.append("                    <td>").append(escapeHtml(String.valueOf(error.get("username")))).append("</td>\n");
                    html.append("                    <td>").append(escapeHtml(String.valueOf(error.get("errorMessage")))).append("</td>\n");
                    html.append("                </tr>\n");
                }
                
                html.append("            </tbody>\n");
                html.append("        </table>\n");
            } else {
                html.append("        <p>No recent errors.</p>\n");
            }
            
            html.append("    </div>\n");
            html.append("</body>\n");
            html.append("</html>");
            
            return Response.ok(html.toString()).build();
        } catch (Exception e) {
            StringBuilder errorHtml = new StringBuilder();
            errorHtml.append("<!DOCTYPE html>\n");
            errorHtml.append("<html><head><title>Error</title></head><body>\n");
            errorHtml.append("<h1>Error</h1>\n");
            errorHtml.append("<p>Failed to retrieve email sender statistics: ").append(escapeHtml(e.getMessage())).append("</p>\n");
            errorHtml.append("</body></html>");
            
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(errorHtml.toString())
                          .build();
        }
    }
    
    /**
     * Helper method to escape HTML special characters to prevent XSS.
     */
    private String escapeHtml(String input) {
        if (input == null) {
            return "null";
        }
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
    }
    
    /**
     * Helper method to format bytes into human-readable format.
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " bytes";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}
