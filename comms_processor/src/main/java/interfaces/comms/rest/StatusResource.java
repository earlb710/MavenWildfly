package interfaces.comms.rest;

import interfaces.comms.service.WildFlyManagementService;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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

    /**
     * HTML version of server status endpoint for browser viewing.
     * 
     * @return Response with HTML page showing server status
     */
    @GET
    @Path("/serverStatus.html")
    @Produces(MediaType.TEXT_HTML)
    public Response serverStatusHtml() {
        try {
            List<Map<String, Object>> services = managementService.getRunningServices();
            
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n");
            html.append("<html lang=\"en\">\n");
            html.append("<head>\n");
            html.append("    <meta charset=\"UTF-8\">\n");
            html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
            html.append("    <title>Server Status - Communications Processor</title>\n");
            html.append("    <style>\n");
            html.append("        body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }\n");
            html.append("        .container { max-width: 1200px; margin: 0 auto; background-color: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n");
            html.append("        h1 { color: #333; border-bottom: 3px solid #007bff; padding-bottom: 10px; }\n");
            html.append("        h2 { color: #555; margin-top: 30px; }\n");
            html.append("        .status-ok { color: #28a745; font-weight: bold; }\n");
            html.append("        .status-error { color: #dc3545; font-weight: bold; }\n");
            html.append("        .info-box { background-color: #e9ecef; padding: 15px; border-radius: 5px; margin-bottom: 20px; }\n");
            html.append("        .info-box p { margin: 5px 0; }\n");
            html.append("        table { width: 100%; border-collapse: collapse; margin-top: 15px; }\n");
            html.append("        th { background-color: #007bff; color: white; padding: 12px; text-align: left; }\n");
            html.append("        td { padding: 10px; border-bottom: 1px solid #ddd; }\n");
            html.append("        tr:hover { background-color: #f8f9fa; }\n");
            html.append("        .timestamp { color: #6c757d; font-size: 0.9em; }\n");
            html.append("        .service-count { font-size: 1.2em; color: #007bff; font-weight: bold; }\n");
            html.append("        .nav-links { margin-bottom: 20px; }\n");
            html.append("        .nav-links a { display: inline-block; padding: 10px 15px; background-color: #007bff; color: white; text-decoration: none; border-radius: 4px; margin-right: 10px; }\n");
            html.append("        .nav-links a:hover { background-color: #0056b3; }\n");
            html.append("    </style>\n");
            html.append("</head>\n");
            html.append("<body>\n");
            html.append("    <div class=\"container\">\n");
            html.append("        <h1>WildFly Server Status</h1>\n");
            
            html.append("        <div class=\"nav-links\">\n");
            html.append("            <a href=\"/comms_processor/\">Home</a>\n");
            html.append("            <a href=\"/comms_processor/api/status/serverStatus\">JSON API</a>\n");
            html.append("            <a href=\"/comms_processor/api/status/datasources.html\">Datasources</a>\n");
            html.append("        </div>\n");
            
            html.append("        <div class=\"info-box\">\n");
            html.append("            <p><strong>Status:</strong> <span class=\"status-ok\">OK</span></p>\n");
            html.append("            <p><strong>Services Count:</strong> <span class=\"service-count\">").append(services.size()).append("</span></p>\n");
            html.append("            <p class=\"timestamp\">Generated: ").append(new java.util.Date()).append("</p>\n");
            html.append("        </div>\n");
            
            html.append("        <h2>Running Services</h2>\n");
            html.append("        <table>\n");
            html.append("            <thead>\n");
            html.append("                <tr>\n");
            html.append("                    <th>Name</th>\n");
            html.append("                    <th>Type</th>\n");
            html.append("                    <th>Domain</th>\n");
            html.append("                    <th>State</th>\n");
            html.append("                </tr>\n");
            html.append("            </thead>\n");
            html.append("            <tbody>\n");
            
            for (Map<String, Object> service : services) {
                html.append("                <tr>\n");
                html.append("                    <td>").append(escapeHtml(String.valueOf(service.get("name")))).append("</td>\n");
                html.append("                    <td>").append(escapeHtml(String.valueOf(service.get("type")))).append("</td>\n");
                html.append("                    <td>").append(escapeHtml(String.valueOf(service.get("domain")))).append("</td>\n");
                html.append("                    <td>").append(escapeHtml(String.valueOf(service.get("state")))).append("</td>\n");
                html.append("                </tr>\n");
            }
            
            html.append("            </tbody>\n");
            html.append("        </table>\n");
            html.append("    </div>\n");
            html.append("</body>\n");
            html.append("</html>");
            
            return Response.ok(html.toString()).build();
        } catch (Exception e) {
            // Log the actual error for debugging
            java.util.logging.Logger.getLogger(StatusResource.class.getName()).severe("Error retrieving server status: " + e.getMessage());
            
            StringBuilder errorHtml = new StringBuilder();
            errorHtml.append("<!DOCTYPE html>\n");
            errorHtml.append("<html><head><title>Error</title></head><body>\n");
            errorHtml.append("<h1>Error</h1>\n");
            errorHtml.append("<p>Failed to retrieve server status. Please check server logs for details.</p>\n");
            errorHtml.append("</body></html>");
            
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(errorHtml.toString())
                          .build();
        }
    }

    /**
     * HTML version of datasources endpoint for browser viewing.
     * 
     * @return Response with HTML page showing datasources
     */
    @GET
    @Path("/datasources.html")
    @Produces(MediaType.TEXT_HTML)
    public Response datasourcesHtml() {
        try {
            List<Map<String, Object>> datasources = managementService.getDatasources();
            
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n");
            html.append("<html lang=\"en\">\n");
            html.append("<head>\n");
            html.append("    <meta charset=\"UTF-8\">\n");
            html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
            html.append("    <title>Datasources - Communications Processor</title>\n");
            html.append("    <style>\n");
            html.append("        body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }\n");
            html.append("        .container { max-width: 1200px; margin: 0 auto; background-color: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n");
            html.append("        h1 { color: #333; border-bottom: 3px solid #007bff; padding-bottom: 10px; }\n");
            html.append("        .status-ok { color: #28a745; font-weight: bold; }\n");
            html.append("        .status-error { color: #dc3545; font-weight: bold; }\n");
            html.append("        .info-box { background-color: #e9ecef; padding: 15px; border-radius: 5px; margin-bottom: 20px; }\n");
            html.append("        .info-box p { margin: 5px 0; }\n");
            html.append("        table { width: 100%; border-collapse: collapse; margin-top: 15px; }\n");
            html.append("        th { background-color: #007bff; color: white; padding: 12px; text-align: left; }\n");
            html.append("        td { padding: 10px; border-bottom: 1px solid #ddd; }\n");
            html.append("        tr:hover { background-color: #f8f9fa; }\n");
            html.append("        .timestamp { color: #6c757d; font-size: 0.9em; }\n");
            html.append("        .datasource-count { font-size: 1.2em; color: #007bff; font-weight: bold; }\n");
            html.append("        .enabled { color: #28a745; }\n");
            html.append("        .disabled { color: #dc3545; }\n");
            html.append("        .nav-links { margin-bottom: 20px; }\n");
            html.append("        .nav-links a { display: inline-block; padding: 10px 15px; background-color: #007bff; color: white; text-decoration: none; border-radius: 4px; margin-right: 10px; }\n");
            html.append("        .nav-links a:hover { background-color: #0056b3; }\n");
            html.append("    </style>\n");
            html.append("</head>\n");
            html.append("<body>\n");
            html.append("    <div class=\"container\">\n");
            html.append("        <h1>WildFly Datasources</h1>\n");
            
            html.append("        <div class=\"nav-links\">\n");
            html.append("            <a href=\"/comms_processor/\">Home</a>\n");
            html.append("            <a href=\"/comms_processor/api/status/serverStatus.html\">Server Status</a>\n");
            html.append("            <a href=\"/comms_processor/api/status/datasources\">JSON API</a>\n");
            html.append("        </div>\n");
            
            html.append("        <div class=\"info-box\">\n");
            html.append("            <p><strong>Status:</strong> <span class=\"status-ok\">OK</span></p>\n");
            html.append("            <p><strong>Datasources Count:</strong> <span class=\"datasource-count\">").append(datasources.size()).append("</span></p>\n");
            html.append("            <p class=\"timestamp\">Generated: ").append(new java.util.Date()).append("</p>\n");
            html.append("        </div>\n");
            
            html.append("        <table>\n");
            html.append("            <thead>\n");
            html.append("                <tr>\n");
            html.append("                    <th>Name</th>\n");
            html.append("                    <th>Type</th>\n");
            html.append("                    <th>JNDI Name</th>\n");
            html.append("                    <th>Driver</th>\n");
            html.append("                    <th>Enabled</th>\n");
            html.append("                    <th>Connection URL</th>\n");
            html.append("                </tr>\n");
            html.append("            </thead>\n");
            html.append("            <tbody>\n");
            
            for (Map<String, Object> ds : datasources) {
                html.append("                <tr>\n");
                html.append("                    <td>").append(escapeHtml(String.valueOf(ds.get("name")))).append("</td>\n");
                html.append("                    <td>").append(escapeHtml(String.valueOf(ds.get("type")))).append("</td>\n");
                html.append("                    <td>").append(escapeHtml(String.valueOf(ds.get("jndiName")))).append("</td>\n");
                html.append("                    <td>").append(escapeHtml(String.valueOf(ds.get("driverName")))).append("</td>\n");
                Object enabled = ds.get("enabled");
                String enabledClass = (enabled != null && enabled.toString().equals("true")) ? "enabled" : "disabled";
                html.append("                    <td class=\"").append(enabledClass).append("\">").append(escapeHtml(String.valueOf(enabled))).append("</td>\n");
                html.append("                    <td>").append(escapeHtml(String.valueOf(ds.get("connectionUrl")))).append("</td>\n");
                html.append("                </tr>\n");
            }
            
            html.append("            </tbody>\n");
            html.append("        </table>\n");
            html.append("    </div>\n");
            html.append("</body>\n");
            html.append("</html>");
            
            return Response.ok(html.toString()).build();
        } catch (Exception e) {
            // Log the actual error for debugging
            java.util.logging.Logger.getLogger(StatusResource.class.getName()).severe("Error retrieving datasources: " + e.getMessage());
            
            StringBuilder errorHtml = new StringBuilder();
            errorHtml.append("<!DOCTYPE html>\n");
            errorHtml.append("<html><head><title>Error</title></head><body>\n");
            errorHtml.append("<h1>Error</h1>\n");
            errorHtml.append("<p>Failed to retrieve datasources. Please check server logs for details.</p>\n");
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
}
