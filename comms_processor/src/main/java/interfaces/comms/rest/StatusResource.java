package interfaces.comms.rest;

import interfaces.comms.service.WildFlyManagementService;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST resource for status endpoints.
 * Provides health check and service status information.
 */
@Path("/status")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class StatusResource {

    @Inject
    private WildFlyManagementService managementService;

    /**
     * Simple ping endpoint to verify service availability.
     * 
     * @return Response with ping status
     */
    @GET
    @Path("/ping")
    public Response ping() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("message", "Service is running");
        response.put("timestamp", System.currentTimeMillis());
        
        return Response.ok(response).build();
    }

    /**
     * Server status endpoint that lists all services running on WildFly.
     * 
     * @return Response with list of running services
     */
    @GET
    @Path("/serverStatus")
    public Response serverStatus() {
        try {
            List<Map<String, Object>> services = managementService.getRunningServices();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "ok");
            response.put("timestamp", System.currentTimeMillis());
            response.put("servicesCount", services.size());
            response.put("services", services);
            
            return Response.ok(response).build();
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to retrieve server status: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(errorResponse)
                          .build();
        }
    }

    /**
     * Datasources endpoint that lists all available datasources configured in WildFly.
     * 
     * @return Response with list of datasources
     */
    @GET
    @Path("/datasources")
    public Response datasources() {
        try {
            List<Map<String, Object>> datasources = managementService.getDatasources();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "ok");
            response.put("timestamp", System.currentTimeMillis());
            response.put("datasourcesCount", datasources.size());
            response.put("datasources", datasources);
            
            return Response.ok(response).build();
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to retrieve datasources: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(errorResponse)
                          .build();
        }
    }
}
