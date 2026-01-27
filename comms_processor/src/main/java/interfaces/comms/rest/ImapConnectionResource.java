package interfaces.comms.rest;

import interfaces.comms.service.ImapConnectionService;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

/**
 * REST resource for IMAPS connection testing.
 * Provides an endpoint to test IMAPS server connectivity.
 */
@Path("/imap")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ImapConnectionResource {

    @Inject
    private ImapConnectionService imapConnectionService;

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
}
