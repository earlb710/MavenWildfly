package interfaces.comms.rest;

import interfaces.comms.service.SmtpConnectionCacheService;
import interfaces.comms.service.SmtpConnectionService;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
     * Sends an email using a cached SMTP connection.
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
    @Path("/send")
    public Response sendEmail(Map<String, String> request) {
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
        
        Map<String, Object> result = smtpConnectionService.sendEmail(
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
}
