package interfaces.comms.rest;

import interfaces.comms.service.ImapConnectionCacheService;
import interfaces.comms.service.ImapConnectionService;

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

    /**
     * Tests IMAPS connection with provided credentials.
     * 
     * Request body example:
     * {
     *   "host": "imap.gmail.com",
     *   "username": "user@gmail.com",
     *   "password": "app-password"
     * }
     * 
     * @param request Map containing host, username, and password
     * @return Response with SUCCESS or FAILED message
     */
    @POST
    @Path("/test")
    public Response testConnection(Map<String, String> request) {
        // Validate request body
        if (request == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("FAILED: Request body is required")
                    .build();
        }
        
        String host = request.get("host");
        String username = request.get("username");
        String password = request.get("password");
        
        // Validate required fields
        if (host == null || host.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("FAILED: Host is required")
                    .build();
        }
        
        if (username == null || username.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("FAILED: Username is required")
                    .build();
        }
        
        if (password == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("FAILED: Password is required")
                    .build();
        }
        
        // Test the connection
        boolean success = imapConnectionService.testConnection(host, username, password);
        
        if (success) {
            return Response.ok("SUCCESS").build();
        } else {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("FAILED")
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
}
