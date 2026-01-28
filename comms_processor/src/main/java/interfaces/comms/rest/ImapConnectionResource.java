package interfaces.comms.rest;

import interfaces.comms.service.ImapConnectionCacheService;
import interfaces.comms.service.ImapConnectionService;
import interfaces.comms.service.EmailProcessingService;
import interfaces.comms.service.EmailProcessor;

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
 * REST resource for IMAPS connection testing and monitoring.
 * Provides endpoints to test IMAPS server connectivity and view connection cache status.
 */
@Path("/imap")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ImapConnectionResource {

    @Inject
    private ImapConnectionService imapConnectionService;
    
    @Inject
    private ImapConnectionCacheService cacheService;
    
    @Inject
    private interfaces.comms.service.EmailReaderStatsService statsService;
    
    @Inject
    private EmailProcessingService emailProcessingService;

    /**
     * Tests IMAPS connection with provided credentials.
     * Does NOT cache the connection. Includes connection timing.
     * 
     * Request body example:
     * {
     *   "host": "imap.gmail.com",
     *   "username": "user@gmail.com",
     *   "password": "app-password"
     * }
     * 
     * @param request Map containing host, username, and password
     * @return Response with success status and connection time
     */
    @POST
    @Path("/test")
    public Response testConnection(Map<String, String> request) {
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
        String password = request.get("password");
        
        // Test the connection (not cached)
        Map<String, Object> result = imapConnectionService.testConnection(host, username, password);
        
        if ((Boolean) result.get("success")) {
            return Response.ok(result).build();
        } else {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(result)
                    .build();
        }
    }
    
    /**
     * Opens a cached IMAPS connection.
     * 
     * Request body example:
     * {
     *   "host": "imap.gmail.com",
     *   "username": "user@gmail.com",
     *   "password": "app-password"
     * }
     * 
     * @param request Map containing host, username, and password
     * @return Response with success status
     */
    @POST
    @Path("/open")
    public Response openConnection(Map<String, String> request) {
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
        String password = request.get("password");
        
        Map<String, Object> result = imapConnectionService.openConnection(host, username, password);
        
        if ((Boolean) result.get("success")) {
            return Response.ok(result).build();
        } else {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(result)
                    .build();
        }
    }
    
    /**
     * Closes a cached IMAPS connection.
     * 
     * Request body example:
     * {
     *   "host": "imap.gmail.com",
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
        
        Map<String, Object> result = imapConnectionService.closeConnection(host, username);
        
        return Response.ok(result).build();
    }
    
    /**
     * Gets the count of emails in a folder.
     * Requires an existing cached connection (use /api/imap/open first).
     * 
     * Request body example:
     * {
     *   "mailboxHost": "imap.gmail.com",
     *   "mailboxUser": "user@gmail.com",
     *   "mailboxFolder": "INBOX"  // optional, defaults to INBOX
     * }
     * 
     * @param request Map containing mailboxHost, mailboxUser, and optional mailboxFolder
     * @return Response with message count
     */
    @POST
    @Path("/mailboxCount")
    public Response getMailboxCount(Map<String, String> request) {
        // Validate request body
        if (request == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Request body is required");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error)
                    .build();
        }
        
        String mailboxHost = request.get("mailboxHost");
        String mailboxUser = request.get("mailboxUser");
        String mailboxFolder = request.get("mailboxFolder"); // Optional, defaults to INBOX
        
        Map<String, Object> result = imapConnectionService.getMailboxCount(mailboxHost, mailboxUser, mailboxFolder);
        
        if ((Boolean) result.get("success")) {
            return Response.ok(result).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(result)
                    .build();
        }
    }
    
    /**
     * Gets detailed statistics for emails in a folder.
     * Requires an existing cached connection (use /api/imap/open first).
     * 
     * Request body example:
     * {
     *   "mailboxHost": "imap.gmail.com",
     *   "mailboxUser": "user@gmail.com",
     *   "mailboxFolder": "INBOX"  // optional, defaults to INBOX
     * }
     * 
     * @param request Map containing mailboxHost, mailboxUser, and optional mailboxFolder
     * @return Response with detailed statistics
     */
    @POST
    @Path("/mailboxStats")
    public Response getMailboxStats(Map<String, String> request) {
        // Validate request body
        if (request == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Request body is required");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error)
                    .build();
        }
        
        String mailboxHost = request.get("mailboxHost");
        String mailboxUser = request.get("mailboxUser");
        String mailboxFolder = request.get("mailboxFolder"); // Optional, defaults to INBOX
        
        Map<String, Object> result = imapConnectionService.getMailboxStats(mailboxHost, mailboxUser, mailboxFolder);
        
        if ((Boolean) result.get("success")) {
            return Response.ok(result).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(result)
                    .build();
        }
    }
    
    /**
     * Gets the oldest message from a folder.
     * Requires an existing cached connection (use /api/imap/open first).
     * 
     * Request body example:
     * {
     *   "mailboxHost": "imap.gmail.com",
     *   "mailboxUser": "user@gmail.com",
     *   "mailboxFolder": "INBOX"  // optional, defaults to INBOX
     * }
     * 
     * @param request Map containing mailboxHost, mailboxUser, and optional mailboxFolder
     * @return Response with oldest message details
     */
    @POST
    @Path("/oldestMessage")
    public Response getOldestMessage(Map<String, String> request) {
        // Validate request body
        if (request == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Request body is required");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error)
                    .build();
        }
        
        String mailboxHost = request.get("mailboxHost");
        String mailboxUser = request.get("mailboxUser");
        String mailboxFolder = request.get("mailboxFolder"); // Optional, defaults to INBOX
        
        Map<String, Object> result = imapConnectionService.getOldestMessage(mailboxHost, mailboxUser, mailboxFolder);
        
        if ((Boolean) result.get("success")) {
            return Response.ok(result).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(result)
                    .build();
        }
    }
    
    /**
     * Gets the newest message from a folder.
     * Requires an existing cached connection (use /api/imap/open first).
     * 
     * Request body example:
     * {
     *   "mailboxHost": "imap.gmail.com",
     *   "mailboxUser": "user@gmail.com",
     *   "mailboxFolder": "INBOX"  // optional, defaults to INBOX
     * }
     * 
     * @param request Map containing mailboxHost, mailboxUser, and optional mailboxFolder
     * @return Response with newest message details
     */
    @POST
    @Path("/newestMessage")
    public Response getNewestMessage(Map<String, String> request) {
        // Validate request body
        if (request == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Request body is required");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error)
                    .build();
        }
        
        String mailboxHost = request.get("mailboxHost");
        String mailboxUser = request.get("mailboxUser");
        String mailboxFolder = request.get("mailboxFolder"); // Optional, defaults to INBOX
        
        Map<String, Object> result = imapConnectionService.getNewestMessage(mailboxHost, mailboxUser, mailboxFolder);
        
        if ((Boolean) result.get("success")) {
            return Response.ok(result).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(result)
                    .build();
        }
    }
    
    /**
     * Gets the status of the IMAPS connection cache.
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
     * Gets email reader statistics.
     * Returns total number of emails read, total size, error count, and recent errors.
     * 
     * @return Response with email reader statistics
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
            errorResponse.put("message", "Failed to retrieve email reader statistics: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(errorResponse)
                          .build();
        }
    }
    
    /**
     * HTML version of IMAP connection status endpoint for browser viewing.
     * 
     * @return Response with HTML page showing IMAP connection status
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
            html.append("    <title>IMAP Connection Status - Communications Processor</title>\n");
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
            html.append("        <h1>IMAP Connection Status</h1>\n");
            
            html.append("        <div class=\"nav-links\">\n");
            html.append("            <a href=\"/comms_processor/\">Home</a>\n");
            html.append("            <a href=\"/comms_processor/api/status.html\">API Status</a>\n");
            html.append("            <a href=\"/comms_processor/api/imap/status\">JSON API</a>\n");
            html.append("            <a href=\"/comms_processor/api/imap/stats.html\">IMAP Stats</a>\n");
            html.append("        </div>\n");
            
            html.append("        <div class=\"info-box\">\n");
            html.append("            <p><strong>Status:</strong> <span class=\"status-ok\">OK</span></p>\n");
            html.append("            <p><strong>Total Connections:</strong> <span class=\"count\">").append(cacheStats.get("totalConnections")).append("</span></p>\n");
            html.append("            <p><strong>Active Connections:</strong> <span class=\"count\">").append(cacheStats.get("activeConnections")).append("</span></p>\n");
            html.append("            <p><strong>Max Connections:</strong> ").append(cacheStats.get("maxConnections")).append("</p>\n");
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
            errorHtml.append("<p>Failed to retrieve IMAP connection status: ").append(escapeHtml(e.getMessage())).append("</p>\n");
            errorHtml.append("</body></html>");
            
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(errorHtml.toString())
                          .build();
        }
    }
    
    /**
     * HTML version of email reader statistics endpoint for browser viewing.
     * 
     * @return Response with HTML page showing email reader statistics
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
            html.append("    <title>IMAP Email Reader Statistics - Communications Processor</title>\n");
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
            html.append("        <h1>IMAP Email Reader Statistics</h1>\n");
            
            html.append("        <div class=\"nav-links\">\n");
            html.append("            <a href=\"/comms_processor/\">Home</a>\n");
            html.append("            <a href=\"/comms_processor/api/status.html\">API Status</a>\n");
            html.append("            <a href=\"/comms_processor/api/imap/stats\">JSON API</a>\n");
            html.append("            <a href=\"/comms_processor/api/imap/status.html\">IMAP Status</a>\n");
            html.append("        </div>\n");
            
            html.append("        <div class=\"info-box\">\n");
            html.append("            <p><strong>Total Emails Read:</strong> <span class=\"stat-value\">").append(stats.get("totalEmailsRead")).append("</span></p>\n");
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
                html.append("                    <th>Folder</th>\n");
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
                    html.append("                    <td>").append(escapeHtml(String.valueOf(error.get("folder")))).append("</td>\n");
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
            errorHtml.append("<p>Failed to retrieve email reader statistics: ").append(escapeHtml(e.getMessage())).append("</p>\n");
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
    
    /**
     * Processes messages from a mailbox using multiple threads.
     * This endpoint requires an EmailProcessor implementation to be provided.
     * 
     * Request body example:
     * {
     *   "mailboxHost": "imap.gmail.com",
     *   "mailboxUser": "user@gmail.com",
     *   "mailboxPassword": "app-password",
     *   "mailboxFolder": "INBOX",
     *   "processorClassName": "com.example.MyEmailProcessor",
     *   "threadCount": 4,
     *   "maxMessages": 100,
     *   "processNewest": false
     * }
     * 
     * Note: The processorClassName must be a fully qualified class name that implements
     * the EmailProcessor interface and is available on the classpath.
     * 
     * @param request Map containing processing parameters
     * @return Response with processing results
     */
    @POST
    @Path("/processMessages")
    public Response processMessages(Map<String, Object> request) {
        // Validate request body
        if (request == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Request body is required");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error)
                    .build();
        }
        
        String mailboxHost = (String) request.get("mailboxHost");
        String mailboxUser = (String) request.get("mailboxUser");
        String mailboxPassword = (String) request.get("mailboxPassword");
        String mailboxFolder = (String) request.get("mailboxFolder");
        String processorClassName = (String) request.get("processorClassName");
        Integer threadCount = request.get("threadCount") != null ? 
                ((Number) request.get("threadCount")).intValue() : null;
        Integer maxMessages = request.get("maxMessages") != null ? 
                ((Number) request.get("maxMessages")).intValue() : null;
        Boolean processNewest = (Boolean) request.get("processNewest");
        Boolean removeMessage = (Boolean) request.get("removeMessage");
        
        // Validate required parameters
        if (processorClassName == null || processorClassName.trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "processorClassName is required - must be a fully qualified class name implementing EmailProcessor");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error)
                    .build();
        }
        
        // Try to instantiate the processor
        EmailProcessor processor;
        try {
            Class<?> processorClass = Class.forName(processorClassName);
            if (!EmailProcessor.class.isAssignableFrom(processorClass)) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "processorClassName must implement EmailProcessor interface");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(error)
                        .build();
            }
            processor = (EmailProcessor) processorClass.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Processor class not found: " + processorClassName);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error)
                    .build();
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to instantiate processor: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error)
                    .build();
        }
        
        // Call the processing service
        Map<String, Object> result = emailProcessingService.processMessages(
                mailboxHost, mailboxUser, mailboxPassword, mailboxFolder,
                processor, threadCount, maxMessages, processNewest, removeMessage);
        
        if ((Boolean) result.get("success")) {
            return Response.ok(result).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(result)
                    .build();
        }
    }
}
