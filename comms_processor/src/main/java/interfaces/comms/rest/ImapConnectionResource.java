package interfaces.comms.rest;

import interfaces.comms.service.ImapConnectionCacheService;
import interfaces.comms.service.ImapConnectionService;

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
}
