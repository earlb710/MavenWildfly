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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * REST resource for status endpoints.
 * Provides health check and service status information.
 */
@Path("/status")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class StatusResource {

    private static final Logger logger = Logger.getLogger(StatusResource.class.getName());

    @Inject
    private WildFlyManagementService managementService;

    /**
     * Main status endpoint that lists all available API status pages
     * and current property values.
     * 
     * @return Response with all available API endpoints and property values
     */
    @GET
    public Response getApiStatus() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "ok");
            response.put("timestamp", System.currentTimeMillis());
            response.put("applicationName", "Communications Processor");
            response.put("version", "1.0.0-SNAPSHOT");
            
            // List all available status endpoints
            List<Map<String, String>> statusEndpoints = new ArrayList<>();
            
            // Status endpoints
            Map<String, String> ping = new HashMap<>();
            ping.put("path", "/api/status/ping");
            ping.put("method", "GET");
            ping.put("description", "Simple ping endpoint to verify service availability");
            statusEndpoints.add(ping);
            
            Map<String, String> serverStatus = new HashMap<>();
            serverStatus.put("path", "/api/status/serverStatus");
            serverStatus.put("method", "GET");
            serverStatus.put("description", "Returns detailed information about all services running on WildFly");
            statusEndpoints.add(serverStatus);
            
            Map<String, String> serverStatusHtml = new HashMap<>();
            serverStatusHtml.put("path", "/api/status/serverStatus.html");
            serverStatusHtml.put("method", "GET");
            serverStatusHtml.put("description", "HTML version of server status for browser viewing");
            statusEndpoints.add(serverStatusHtml);
            
            Map<String, String> datasources = new HashMap<>();
            datasources.put("path", "/api/status/datasources");
            datasources.put("method", "GET");
            datasources.put("description", "Returns a list of all datasources configured in WildFly");
            statusEndpoints.add(datasources);
            
            Map<String, String> datasourcesHtml = new HashMap<>();
            datasourcesHtml.put("path", "/api/status/datasources.html");
            datasourcesHtml.put("method", "GET");
            datasourcesHtml.put("description", "HTML version of datasources for browser viewing");
            statusEndpoints.add(datasourcesHtml);
            
            // IMAP endpoints
            Map<String, String> imapTest = new HashMap<>();
            imapTest.put("path", "/api/imap/test");
            imapTest.put("method", "POST");
            imapTest.put("description", "Tests IMAPS connection with provided credentials");
            statusEndpoints.add(imapTest);
            
            Map<String, String> imapOpen = new HashMap<>();
            imapOpen.put("path", "/api/imap/open");
            imapOpen.put("method", "POST");
            imapOpen.put("description", "Opens and caches an IMAPS connection");
            statusEndpoints.add(imapOpen);
            
            Map<String, String> imapClose = new HashMap<>();
            imapClose.put("path", "/api/imap/close");
            imapClose.put("method", "POST");
            imapClose.put("description", "Closes a cached IMAPS connection");
            statusEndpoints.add(imapClose);
            
            Map<String, String> imapMailboxCount = new HashMap<>();
            imapMailboxCount.put("path", "/api/imap/mailboxCount");
            imapMailboxCount.put("method", "POST");
            imapMailboxCount.put("description", "Returns the number of emails in a specified folder");
            statusEndpoints.add(imapMailboxCount);
            
            Map<String, String> imapMailboxStats = new HashMap<>();
            imapMailboxStats.put("path", "/api/imap/mailboxStats");
            imapMailboxStats.put("method", "POST");
            imapMailboxStats.put("description", "Returns detailed statistics for emails in a folder");
            statusEndpoints.add(imapMailboxStats);
            
            Map<String, String> imapOldestMessage = new HashMap<>();
            imapOldestMessage.put("path", "/api/imap/oldestMessage");
            imapOldestMessage.put("method", "POST");
            imapOldestMessage.put("description", "Returns the oldest message from a folder by received date");
            statusEndpoints.add(imapOldestMessage);
            
            Map<String, String> imapNewestMessage = new HashMap<>();
            imapNewestMessage.put("path", "/api/imap/newestMessage");
            imapNewestMessage.put("method", "POST");
            imapNewestMessage.put("description", "Returns the newest message from a folder by received date");
            statusEndpoints.add(imapNewestMessage);
            
            Map<String, String> imapStatus = new HashMap<>();
            imapStatus.put("path", "/api/imap/status");
            imapStatus.put("method", "GET");
            imapStatus.put("description", "Returns the status of the IMAPS connection cache");
            statusEndpoints.add(imapStatus);
            
            Map<String, String> imapStats = new HashMap<>();
            imapStats.put("path", "/api/imap/stats");
            imapStats.put("method", "GET");
            imapStats.put("description", "Returns email reader statistics (total emails read, size, errors, and last 20 errors)");
            statusEndpoints.add(imapStats);
            
            Map<String, String> imapStatusHtml = new HashMap<>();
            imapStatusHtml.put("path", "/api/imap/status.html");
            imapStatusHtml.put("method", "GET");
            imapStatusHtml.put("description", "HTML version of IMAPS connection cache status for browser viewing");
            statusEndpoints.add(imapStatusHtml);
            
            Map<String, String> imapStatsHtml = new HashMap<>();
            imapStatsHtml.put("path", "/api/imap/stats.html");
            imapStatsHtml.put("method", "GET");
            imapStatsHtml.put("description", "HTML version of email reader statistics for browser viewing");
            statusEndpoints.add(imapStatsHtml);
            
            // SMTP endpoints
            Map<String, String> smtpOpen = new HashMap<>();
            smtpOpen.put("path", "/api/smtp/open");
            smtpOpen.put("method", "POST");
            smtpOpen.put("description", "Opens and caches an SMTP connection");
            statusEndpoints.add(smtpOpen);
            
            Map<String, String> smtpClose = new HashMap<>();
            smtpClose.put("path", "/api/smtp/close");
            smtpClose.put("method", "POST");
            smtpClose.put("description", "Closes a cached SMTP connection");
            statusEndpoints.add(smtpClose);
            
            Map<String, String> smtpSendText = new HashMap<>();
            smtpSendText.put("path", "/api/smtp/sendTextMessage");
            smtpSendText.put("method", "POST");
            smtpSendText.put("description", "Sends a simple text email using a cached SMTP connection");
            statusEndpoints.add(smtpSendText);
            
            Map<String, String> smtpSend = new HashMap<>();
            smtpSend.put("path", "/api/smtp/send");
            smtpSend.put("method", "POST");
            smtpSend.put("description", "Sends email(s) in .eml format using a cached SMTP connection");
            statusEndpoints.add(smtpSend);
            
            Map<String, String> smtpStatus = new HashMap<>();
            smtpStatus.put("path", "/api/smtp/status");
            smtpStatus.put("method", "GET");
            smtpStatus.put("description", "Returns the status of the SMTP connection cache");
            statusEndpoints.add(smtpStatus);
            
            Map<String, String> smtpStats = new HashMap<>();
            smtpStats.put("path", "/api/smtp/stats");
            smtpStats.put("method", "GET");
            smtpStats.put("description", "Returns email sender statistics (total emails sent, size, errors, and last 20 errors)");
            statusEndpoints.add(smtpStats);
            
            Map<String, String> smtpStatusHtml = new HashMap<>();
            smtpStatusHtml.put("path", "/api/smtp/status.html");
            smtpStatusHtml.put("method", "GET");
            smtpStatusHtml.put("description", "HTML version of SMTP connection cache status for browser viewing");
            statusEndpoints.add(smtpStatusHtml);
            
            Map<String, String> smtpStatsHtml = new HashMap<>();
            smtpStatsHtml.put("path", "/api/smtp/stats.html");
            smtpStatsHtml.put("method", "GET");
            smtpStatsHtml.put("description", "HTML version of email sender statistics for browser viewing");
            statusEndpoints.add(smtpStatsHtml);
            
            response.put("availableEndpoints", statusEndpoints);
            response.put("totalEndpoints", statusEndpoints.size());
            
            // Load and include property values
            Map<String, Object> properties = new HashMap<>();
            
            // Database properties
            Properties dbProps = loadProperties("/database.properties");
            if (dbProps != null && !dbProps.isEmpty()) {
                // PostgreSQL configuration
                properties.put("postgresql", createDbConfigMap(dbProps, "postgresql"));
                
                // Oracle configuration
                properties.put("oracle", createDbConfigMap(dbProps, "oracle"));
                
                // Connection pool settings
                Map<String, String> poolConfig = new HashMap<>();
                poolConfig.put("min", dbProps.getProperty("connection.pool.min"));
                poolConfig.put("max", dbProps.getProperty("connection.pool.max"));
                properties.put("connectionPool", poolConfig);
            }
            
            // System properties related to email processing
            Map<String, String> emailSenderProps = new HashMap<>();
            emailSenderProps.put("maxBatchSize", System.getProperty("email-sender.maxBatchSize", "100"));
            emailSenderProps.put("minBatchSize", System.getProperty("email-sender.minBatchSize", "1"));
            emailSenderProps.put("maxConnections", System.getProperty("email-sender.maxConnections", "50"));
            emailSenderProps.put("maxPoolSize", System.getProperty("email-sender.maxPoolSize", "100"));
            properties.put("emailSender", emailSenderProps);
            
            Map<String, String> emailReaderProps = new HashMap<>();
            emailReaderProps.put("maxBatchSize", System.getProperty("email-reader.maxBatchSize", "100"));
            emailReaderProps.put("minBatchSize", System.getProperty("email-reader.minBatchSize", "1"));
            emailReaderProps.put("maxConnections", System.getProperty("email-reader.maxConnections", "50"));
            emailReaderProps.put("maxPoolSize", System.getProperty("email-reader.maxPoolSize", "100"));
            properties.put("emailReader", emailReaderProps);
            
            // Java system properties
            Map<String, String> javaProps = new HashMap<>();
            javaProps.put("version", System.getProperty("java.version"));
            javaProps.put("vendor", System.getProperty("java.vendor"));
            javaProps.put("home", System.getProperty("java.home"));
            properties.put("java", javaProps);
            
            response.put("properties", properties);
            
            return Response.ok(response).build();
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to retrieve API status: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(errorResponse)
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
     * Helper method to create database configuration map from properties.
     * 
     * @param dbProps The database properties
     * @param prefix The property prefix (e.g., "postgresql", "oracle")
     * @return Map containing URL, username, and driver configuration
     */
    private Map<String, String> createDbConfigMap(Properties dbProps, String prefix) {
        Map<String, String> config = new HashMap<>();
        config.put("url", maskSensitiveValue(dbProps.getProperty(prefix + ".url")));
        config.put("username", maskSensitiveValue(dbProps.getProperty(prefix + ".username")));
        config.put("driver", dbProps.getProperty(prefix + ".driver"));
        return config;
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
