package interfaces.comms.rest;

import interfaces.comms.service.WildFlyManagementService;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * REST resource for status.html endpoint.
 * Provides HTML view of API status and configuration.
 */
@Path("/status.html")
@RequestScoped
public class StatusHtmlResource {

    private static final Logger logger = Logger.getLogger(StatusHtmlResource.class.getName());

    @Inject
    private WildFlyManagementService managementService;

    /**
     * HTML version of the main API status endpoint for browser viewing.
     * 
     * @return Response with HTML page showing API status, endpoints, and properties
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response getApiStatusHtml() {
        try {
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n");
            html.append("<html lang=\"en\">\n");
            html.append("<head>\n");
            html.append("    <meta charset=\"UTF-8\">\n");
            html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
            html.append("    <title>API Status - Communications Processor</title>\n");
            html.append("    <style>\n");
            html.append("        body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }\n");
            html.append("        .container { max-width: 1200px; margin: 0 auto; background-color: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n");
            html.append("        h1 { color: #333; border-bottom: 3px solid #007bff; padding-bottom: 10px; }\n");
            html.append("        h2 { color: #555; margin-top: 30px; border-bottom: 2px solid #6c757d; padding-bottom: 5px; }\n");
            html.append("        h3 { color: #666; margin-top: 20px; }\n");
            html.append("        .status-ok { color: #28a745; font-weight: bold; }\n");
            html.append("        .info-box { background-color: #e9ecef; padding: 15px; border-radius: 5px; margin-bottom: 20px; }\n");
            html.append("        .info-box p { margin: 5px 0; }\n");
            html.append("        table { width: 100%; border-collapse: collapse; margin-top: 15px; margin-bottom: 20px; }\n");
            html.append("        th { background-color: #007bff; color: white; padding: 12px; text-align: left; }\n");
            html.append("        td { padding: 10px; border-bottom: 1px solid #ddd; }\n");
            html.append("        tr:hover { background-color: #f8f9fa; }\n");
            html.append("        .timestamp { color: #6c757d; font-size: 0.9em; }\n");
            html.append("        .nav-links { margin-bottom: 20px; }\n");
            html.append("        .nav-links a { display: inline-block; padding: 10px 15px; background-color: #007bff; color: white; text-decoration: none; border-radius: 4px; margin-right: 10px; margin-bottom: 10px; }\n");
            html.append("        .nav-links a:hover { background-color: #0056b3; }\n");
            html.append("        .endpoint-card { background-color: #f8f9fa; border-left: 4px solid #007bff; padding: 10px; margin-bottom: 10px; }\n");
            html.append("        .endpoint-path { font-weight: bold; color: #007bff; }\n");
            html.append("        .endpoint-method { display: inline-block; background-color: #28a745; color: white; padding: 2px 8px; border-radius: 3px; font-size: 0.85em; margin-right: 10px; }\n");
            html.append("        .endpoint-method.post { background-color: #ffc107; color: #000; }\n");
            html.append("        .endpoint-desc { color: #666; font-size: 0.9em; margin-top: 5px; }\n");
            html.append("        .property-section { background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin-bottom: 15px; }\n");
            html.append("        .property-key { font-weight: bold; color: #495057; }\n");
            html.append("        .property-value { color: #6c757d; }\n");
            html.append("    </style>\n");
            html.append("</head>\n");
            html.append("<body>\n");
            html.append("    <div class=\"container\">\n");
            html.append("        <h1>Communications Processor - API Status</h1>\n");
            
            html.append("        <div class=\"nav-links\">\n");
            html.append("            <a href=\"/comms_processor/\">Home</a>\n");
            html.append("            <a href=\"/comms_processor/api/status\">JSON API</a>\n");
            html.append("            <a href=\"/comms_processor/api/status/serverStatus.html\">Server Status</a>\n");
            html.append("            <a href=\"/comms_processor/api/status/datasources.html\">Datasources</a>\n");
            html.append("        </div>\n");
            
            html.append("        <div class=\"info-box\">\n");
            html.append("            <p><strong>Status:</strong> <span class=\"status-ok\">OK</span></p>\n");
            html.append("            <p><strong>Application:</strong> Communications Processor</p>\n");
            html.append("            <p><strong>Version:</strong> 1.0.0-SNAPSHOT</p>\n");
            html.append("            <p class=\"timestamp\">Generated: ").append(new java.util.Date()).append("</p>\n");
            html.append("        </div>\n");
            
            // Available Endpoints Section
            html.append("        <h2>Available API Endpoints</h2>\n");
            
            html.append("        <h3>Status Endpoints</h3>\n");
            html.append("        <div class=\"endpoint-card\">\n");
            html.append("            <span class=\"endpoint-method\">GET</span>\n");
            html.append("            <span class=\"endpoint-path\">/api/status/ping</span>\n");
            html.append("            <div class=\"endpoint-desc\">Simple ping endpoint to verify service availability</div>\n");
            html.append("        </div>\n");
            
            html.append("        <div class=\"endpoint-card\">\n");
            html.append("            <span class=\"endpoint-method\">GET</span>\n");
            html.append("            <span class=\"endpoint-path\">/api/status/serverStatus</span>\n");
            html.append("            <div class=\"endpoint-desc\">Returns detailed information about all services running on WildFly</div>\n");
            html.append("        </div>\n");
            
            html.append("        <div class=\"endpoint-card\">\n");
            html.append("            <span class=\"endpoint-method\">GET</span>\n");
            html.append("            <span class=\"endpoint-path\">/api/status/serverStatus.html</span>\n");
            html.append("            <div class=\"endpoint-desc\">HTML version of server status for browser viewing</div>\n");
            html.append("        </div>\n");
            
            html.append("        <div class=\"endpoint-card\">\n");
            html.append("            <span class=\"endpoint-method\">GET</span>\n");
            html.append("            <span class=\"endpoint-path\">/api/status/datasources</span>\n");
            html.append("            <div class=\"endpoint-desc\">Returns a list of all datasources configured in WildFly</div>\n");
            html.append("        </div>\n");
            
            html.append("        <div class=\"endpoint-card\">\n");
            html.append("            <span class=\"endpoint-method\">GET</span>\n");
            html.append("            <span class=\"endpoint-path\">/api/status/datasources.html</span>\n");
            html.append("            <div class=\"endpoint-desc\">HTML version of datasources for browser viewing</div>\n");
            html.append("        </div>\n");
            
            html.append("        <h3>IMAP Endpoints</h3>\n");
            html.append("        <div class=\"endpoint-card\">\n");
            html.append("            <span class=\"endpoint-method post\">POST</span>\n");
            html.append("            <span class=\"endpoint-path\">/api/imap/test</span>\n");
            html.append("            <div class=\"endpoint-desc\">Tests IMAPS connection with provided credentials</div>\n");
            html.append("        </div>\n");
            
            html.append("        <div class=\"endpoint-card\">\n");
            html.append("            <span class=\"endpoint-method post\">POST</span>\n");
            html.append("            <span class=\"endpoint-path\">/api/imap/open</span>\n");
            html.append("            <div class=\"endpoint-desc\">Opens and caches an IMAPS connection</div>\n");
            html.append("        </div>\n");
            
            html.append("        <div class=\"endpoint-card\">\n");
            html.append("            <span class=\"endpoint-method post\">POST</span>\n");
            html.append("            <span class=\"endpoint-path\">/api/imap/close</span>\n");
            html.append("            <div class=\"endpoint-desc\">Closes a cached IMAPS connection</div>\n");
            html.append("        </div>\n");
            
            html.append("        <div class=\"endpoint-card\">\n");
            html.append("            <span class=\"endpoint-method post\">POST</span>\n");
            html.append("            <span class=\"endpoint-path\">/api/imap/mailboxCount</span>\n");
            html.append("            <div class=\"endpoint-desc\">Returns the number of emails in a specified folder</div>\n");
            html.append("        </div>\n");
            
            html.append("        <div class=\"endpoint-card\">\n");
            html.append("            <span class=\"endpoint-method post\">POST</span>\n");
            html.append("            <span class=\"endpoint-path\">/api/imap/mailboxStats</span>\n");
            html.append("            <div class=\"endpoint-desc\">Returns detailed statistics for emails in a folder</div>\n");
            html.append("        </div>\n");
            
            html.append("        <div class=\"endpoint-card\">\n");
            html.append("            <span class=\"endpoint-method\">GET</span>\n");
            html.append("            <span class=\"endpoint-path\">/api/imap/status</span>\n");
            html.append("            <div class=\"endpoint-desc\">Returns the status of the IMAPS connection cache</div>\n");
            html.append("        </div>\n");
            
            html.append("        <h3>SMTP Endpoints</h3>\n");
            html.append("        <div class=\"endpoint-card\">\n");
            html.append("            <span class=\"endpoint-method post\">POST</span>\n");
            html.append("            <span class=\"endpoint-path\">/api/smtp/open</span>\n");
            html.append("            <div class=\"endpoint-desc\">Opens and caches an SMTP connection</div>\n");
            html.append("        </div>\n");
            
            html.append("        <div class=\"endpoint-card\">\n");
            html.append("            <span class=\"endpoint-method post\">POST</span>\n");
            html.append("            <span class=\"endpoint-path\">/api/smtp/close</span>\n");
            html.append("            <div class=\"endpoint-desc\">Closes a cached SMTP connection</div>\n");
            html.append("        </div>\n");
            
            html.append("        <div class=\"endpoint-card\">\n");
            html.append("            <span class=\"endpoint-method post\">POST</span>\n");
            html.append("            <span class=\"endpoint-path\">/api/smtp/sendTextMessage</span>\n");
            html.append("            <div class=\"endpoint-desc\">Sends a simple text email using a cached SMTP connection</div>\n");
            html.append("        </div>\n");
            
            html.append("        <div class=\"endpoint-card\">\n");
            html.append("            <span class=\"endpoint-method post\">POST</span>\n");
            html.append("            <span class=\"endpoint-path\">/api/smtp/send</span>\n");
            html.append("            <div class=\"endpoint-desc\">Sends email(s) in .eml format using a cached SMTP connection</div>\n");
            html.append("        </div>\n");
            
            html.append("        <div class=\"endpoint-card\">\n");
            html.append("            <span class=\"endpoint-method\">GET</span>\n");
            html.append("            <span class=\"endpoint-path\">/api/smtp/status</span>\n");
            html.append("            <div class=\"endpoint-desc\">Returns the status of the SMTP connection cache</div>\n");
            html.append("        </div>\n");
            
            // Properties Section
            html.append("        <h2>System Properties</h2>\n");
            
            // Load and display properties
            Properties dbProps = loadProperties("/database.properties");
            if (dbProps != null && !dbProps.isEmpty()) {
                html.append("        <h3>Database Configuration</h3>\n");
                
                // PostgreSQL
                html.append("        <div class=\"property-section\">\n");
                html.append("            <h4>PostgreSQL</h4>\n");
                html.append("            <p><span class=\"property-key\">URL:</span> <span class=\"property-value\">")
                    .append(escapeHtml(maskSensitiveValue(dbProps.getProperty("postgresql.url")))).append("</span></p>\n");
                html.append("            <p><span class=\"property-key\">Username:</span> <span class=\"property-value\">")
                    .append(escapeHtml(maskSensitiveValue(dbProps.getProperty("postgresql.username")))).append("</span></p>\n");
                html.append("            <p><span class=\"property-key\">Driver:</span> <span class=\"property-value\">")
                    .append(escapeHtml(dbProps.getProperty("postgresql.driver"))).append("</span></p>\n");
                html.append("        </div>\n");
                
                // Oracle
                html.append("        <div class=\"property-section\">\n");
                html.append("            <h4>Oracle</h4>\n");
                html.append("            <p><span class=\"property-key\">URL:</span> <span class=\"property-value\">")
                    .append(escapeHtml(maskSensitiveValue(dbProps.getProperty("oracle.url")))).append("</span></p>\n");
                html.append("            <p><span class=\"property-key\">Username:</span> <span class=\"property-value\">")
                    .append(escapeHtml(maskSensitiveValue(dbProps.getProperty("oracle.username")))).append("</span></p>\n");
                html.append("            <p><span class=\"property-key\">Driver:</span> <span class=\"property-value\">")
                    .append(escapeHtml(dbProps.getProperty("oracle.driver"))).append("</span></p>\n");
                html.append("        </div>\n");
                
                // Connection Pool
                html.append("        <div class=\"property-section\">\n");
                html.append("            <h4>Connection Pool</h4>\n");
                html.append("            <p><span class=\"property-key\">Min:</span> <span class=\"property-value\">")
                    .append(escapeHtml(dbProps.getProperty("connection.pool.min"))).append("</span></p>\n");
                html.append("            <p><span class=\"property-key\">Max:</span> <span class=\"property-value\">")
                    .append(escapeHtml(dbProps.getProperty("connection.pool.max"))).append("</span></p>\n");
                html.append("        </div>\n");
            }
            
            // Email Sender Properties
            html.append("        <h3>Email Configuration</h3>\n");
            html.append("        <div class=\"property-section\">\n");
            html.append("            <h4>Email Sender</h4>\n");
            html.append("            <p><span class=\"property-key\">Max Batch Size:</span> <span class=\"property-value\">")
                .append(System.getProperty("email-sender.maxBatchSize", "100")).append("</span></p>\n");
            html.append("            <p><span class=\"property-key\">Min Batch Size:</span> <span class=\"property-value\">")
                .append(System.getProperty("email-sender.minBatchSize", "1")).append("</span></p>\n");
            html.append("            <p><span class=\"property-key\">Max Connections:</span> <span class=\"property-value\">")
                .append(System.getProperty("email-sender.maxConnections", "50")).append("</span></p>\n");
            html.append("            <p><span class=\"property-key\">Max Pool Size:</span> <span class=\"property-value\">")
                .append(System.getProperty("email-sender.maxPoolSize", "100")).append("</span></p>\n");
            html.append("        </div>\n");
            
            html.append("        <div class=\"property-section\">\n");
            html.append("            <h4>Email Reader</h4>\n");
            html.append("            <p><span class=\"property-key\">Max Batch Size:</span> <span class=\"property-value\">")
                .append(System.getProperty("email-reader.maxBatchSize", "100")).append("</span></p>\n");
            html.append("            <p><span class=\"property-key\">Min Batch Size:</span> <span class=\"property-value\">")
                .append(System.getProperty("email-reader.minBatchSize", "1")).append("</span></p>\n");
            html.append("            <p><span class=\"property-key\">Max Connections:</span> <span class=\"property-value\">")
                .append(System.getProperty("email-reader.maxConnections", "50")).append("</span></p>\n");
            html.append("            <p><span class=\"property-key\">Max Pool Size:</span> <span class=\"property-value\">")
                .append(System.getProperty("email-reader.maxPoolSize", "100")).append("</span></p>\n");
            html.append("        </div>\n");
            
            // Java Properties
            html.append("        <div class=\"property-section\">\n");
            html.append("            <h4>Java Environment</h4>\n");
            html.append("            <p><span class=\"property-key\">Version:</span> <span class=\"property-value\">")
                .append(escapeHtml(System.getProperty("java.version"))).append("</span></p>\n");
            html.append("            <p><span class=\"property-key\">Vendor:</span> <span class=\"property-value\">")
                .append(escapeHtml(System.getProperty("java.vendor"))).append("</span></p>\n");
            html.append("            <p><span class=\"property-key\">Home:</span> <span class=\"property-value\">")
                .append(escapeHtml(System.getProperty("java.home"))).append("</span></p>\n");
            html.append("        </div>\n");
            
            html.append("    </div>\n");
            html.append("</body>\n");
            html.append("</html>");
            
            return Response.ok(html.toString()).build();
        } catch (Exception e) {
            logger.severe("Error generating API status HTML: " + e.getMessage());
            
            StringBuilder errorHtml = new StringBuilder();
            errorHtml.append("<!DOCTYPE html>\n");
            errorHtml.append("<html><head><title>Error</title></head><body>\n");
            errorHtml.append("<h1>Error</h1>\n");
            errorHtml.append("<p>Failed to generate API status page. Please check server logs for details.</p>\n");
            errorHtml.append("</body></html>");
            
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(errorHtml.toString())
                          .build();
        }
    }

    /**
     * Helper method to load properties from a resource file.
     */
    private Properties loadProperties(String resourcePath) {
        Properties props = new Properties();
        try (InputStream input = getClass().getResourceAsStream(resourcePath)) {
            if (input != null) {
                props.load(input);
            }
        } catch (Exception e) {
            // Log but don't fail - properties might not be available
            logger.warning("Could not load properties from " + resourcePath + ": " + e.getMessage());
        }
        return props;
    }

    /**
     * Helper method to mask sensitive values in property values.
     * Consistently masks all values to prevent information disclosure.
     * For values longer than 8 characters, shows first 4 and last 4 characters.
     * For shorter values, completely masks them.
     */
    private String maskSensitiveValue(String value) {
        if (value == null || value.length() <= 8) {
            return "***";
        }
        // Show first 4 and last 4 characters, mask the middle
        int len = value.length();
        return value.substring(0, 4) + "***" + value.substring(len - 4);
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
