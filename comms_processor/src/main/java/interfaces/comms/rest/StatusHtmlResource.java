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
            html.append("        .endpoint-path a { color: #007bff; text-decoration: none; }\n");
            html.append("        .endpoint-path a:hover { text-decoration: underline; }\n");
            html.append("        .endpoint-method { display: inline-block; background-color: #28a745; color: white; padding: 2px 8px; border-radius: 3px; font-size: 0.85em; margin-right: 10px; }\n");
            html.append("        .endpoint-method.post { background-color: #ffc107; color: #000; }\n");
            html.append("        .endpoint-desc { color: #666; font-size: 0.9em; margin-top: 5px; }\n");
            html.append("        .expand-btn { display: inline-block; margin-left: 10px; padding: 4px 10px; background-color: #6c757d; color: white; border: none; border-radius: 3px; cursor: pointer; font-size: 0.85em; }\n");
            html.append("        .expand-btn:hover { background-color: #5a6268; }\n");
            html.append("        .test-btn { display: inline-block; margin-left: 5px; padding: 4px 10px; background-color: #17a2b8; color: white; border: none; border-radius: 3px; cursor: pointer; font-size: 0.85em; }\n");
            html.append("        .test-btn:hover { background-color: #138496; }\n");
            html.append("        .modal { display: none; position: fixed; z-index: 1000; left: 0; top: 0; width: 100%; height: 100%; overflow: auto; background-color: rgba(0,0,0,0.5); }\n");
            html.append("        .modal-content { background-color: #fefefe; margin: 5% auto; padding: 20px; border: 1px solid #888; border-radius: 8px; width: 80%; max-width: 800px; }\n");
            html.append("        .modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; border-bottom: 2px solid #007bff; padding-bottom: 10px; }\n");
            html.append("        .modal-header h2 { margin: 0; color: #333; }\n");
            html.append("        .close-btn { color: #aaa; font-size: 28px; font-weight: bold; cursor: pointer; }\n");
            html.append("        .close-btn:hover, .close-btn:focus { color: #000; }\n");
            html.append("        .modal-body { margin-bottom: 15px; }\n");
            html.append("        .modal-body label { display: block; font-weight: bold; margin-bottom: 5px; color: #495057; }\n");
            html.append("        .modal-body textarea { width: 100%; min-height: 200px; padding: 10px; font-family: 'Courier New', monospace; font-size: 0.9em; border: 1px solid #dee2e6; border-radius: 4px; }\n");
            html.append("        .modal-footer { display: flex; justify-content: space-between; align-items: center; }\n");
            html.append("        .post-btn { padding: 8px 20px; background-color: #28a745; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 1em; }\n");
            html.append("        .post-btn:hover { background-color: #218838; }\n");
            html.append("        .response-area { margin-top: 15px; padding: 10px; background-color: #f8f9fa; border: 1px solid #dee2e6; border-radius: 4px; min-height: 100px; font-family: 'Courier New', monospace; font-size: 0.9em; white-space: pre-wrap; word-wrap: break-word; }\n");
            html.append("        .response-area.success { background-color: #d4edda; border-color: #c3e6cb; }\n");
            html.append("        .response-area.error { background-color: #f8d7da; border-color: #f5c6cb; }\n");
            html.append("        .json-example { display: none; margin-top: 10px; padding: 10px; background-color: #f8f9fa; border: 1px solid #dee2e6; border-radius: 4px; font-family: 'Courier New', monospace; font-size: 0.9em; overflow-x: auto; }\n");
            html.append("        .json-example.show { display: block; }\n");
            html.append("        .json-example pre { margin: 0; white-space: pre-wrap; word-wrap: break-word; }\n");
            html.append("        .json-section { margin-bottom: 15px; }\n");
            html.append("        .json-section:last-child { margin-bottom: 0; }\n");
            html.append("        .json-label { font-weight: bold; color: #495057; margin-bottom: 5px; }\n");
            html.append("        .property-section { background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin-bottom: 15px; }\n");
            html.append("        .property-key { font-weight: bold; color: #495057; }\n");
            html.append("        .property-value { color: #6c757d; }\n");
            html.append("        .endpoint-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 20px; }\n");
            html.append("        .endpoint-grid-single { grid-column: 1 / -1; }\n");
            html.append("        .column-header { font-weight: bold; color: #007bff; padding: 5px 10px; background-color: #e7f3ff; border-radius: 4px; margin-bottom: 5px; text-align: center; }\n");
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
            html.append("        <div class=\"endpoint-card endpoint-grid-single\">\n");
            html.append("            <span class=\"endpoint-method\">GET</span>\n");
            html.append("            <span class=\"endpoint-path\"><a href=\"/comms_processor/api/status/ping\">/api/status/ping</a></span>\n");
            html.append("            <div class=\"endpoint-desc\">Simple ping endpoint to verify service availability</div>\n");
            html.append("        </div>\n");
            
            html.append("        <div class=\"endpoint-grid\">\n");
            html.append("            <div>\n");
            html.append("                <div class=\"column-header\">JSON API</div>\n");
            html.append("                <div class=\"endpoint-card\">\n");
            html.append("                    <span class=\"endpoint-method\">GET</span>\n");
            html.append("                    <span class=\"endpoint-path\"><a href=\"/comms_processor/api/status/serverStatus\">/api/status/serverStatus</a></span>\n");
            html.append("                    <div class=\"endpoint-desc\">Returns detailed information about all services running on WildFly</div>\n");
            html.append("                </div>\n");
            html.append("                <div class=\"endpoint-card\">\n");
            html.append("                    <span class=\"endpoint-method\">GET</span>\n");
            html.append("                    <span class=\"endpoint-path\"><a href=\"/comms_processor/api/status/datasources\">/api/status/datasources</a></span>\n");
            html.append("                    <div class=\"endpoint-desc\">Returns a list of all datasources configured in WildFly</div>\n");
            html.append("                </div>\n");
            html.append("            </div>\n");
            html.append("            <div>\n");
            html.append("                <div class=\"column-header\">HTML View</div>\n");
            html.append("                <div class=\"endpoint-card\">\n");
            html.append("                    <span class=\"endpoint-method\">GET</span>\n");
            html.append("                    <span class=\"endpoint-path\"><a href=\"/comms_processor/api/status/serverStatus.html\">/api/status/serverStatus.html</a></span>\n");
            html.append("                    <div class=\"endpoint-desc\">HTML version of server status for browser viewing</div>\n");
            html.append("                </div>\n");
            html.append("                <div class=\"endpoint-card\">\n");
            html.append("                    <span class=\"endpoint-method\">GET</span>\n");
            html.append("                    <span class=\"endpoint-path\"><a href=\"/comms_processor/api/status/datasources.html\">/api/status/datasources.html</a></span>\n");
            html.append("                    <div class=\"endpoint-desc\">HTML version of datasources for browser viewing</div>\n");
            html.append("                </div>\n");
            html.append("            </div>\n");
            html.append("        </div>\n");
            
            html.append("        <h3>IMAP Endpoints</h3>\n");
            html.append("        <div class=\"endpoint-card\">\n");
            html.append("            <span class=\"endpoint-method post\">POST</span>\n");
            html.append("            <span class=\"endpoint-path\">/api/imap/test</span>\n");
            html.append("            <button class=\"expand-btn\" onclick=\"toggleJson('json-imap-test')\">Show JSON</button>\n");
            html.append("            <button class=\"test-btn\" onclick='openTestModal(\"/api/imap/test\", \"{\\n  \\\"host\\\": \\\"imap.example.com\\\",\\n  \\\"port\\\": 993,\\n  \\\"username\\\": \\\"user@example.com\\\",\\n  \\\"password\\\": \\\"your-password\\\",\\n  \\\"protocol\\\": \\\"imaps\\\"\\n}\")'>Test</button>\n");
            html.append("            <div class=\"endpoint-desc\">Tests IMAPS connection with provided credentials</div>\n");
            html.append("            <div id=\"json-imap-test\" class=\"json-example\">\n");
            html.append("                <div class=\"json-section\">\n");
            html.append("                    <div class=\"json-label\">Request JSON:</div>\n");
            html.append("                    <pre>{\n");
            html.append("  \"host\": \"imap.example.com\",\n");
            html.append("  \"port\": 993,\n");
            html.append("  \"username\": \"user@example.com\",\n");
            html.append("  \"password\": \"your-password\",\n");
            html.append("  \"protocol\": \"imaps\"\n");
            html.append("}</pre>\n");
            html.append("                </div>\n");
            html.append("                <div class=\"json-section\">\n");
            html.append("                    <div class=\"json-label\">Response JSON (Success):</div>\n");
            html.append("                    <pre>{\n");
            html.append("  \"status\": \"success\",\n");
            html.append("  \"message\": \"IMAP connection test successful\",\n");
            html.append("  \"connected\": true\n");
            html.append("}</pre>\n");
            html.append("                </div>\n");
            html.append("                <div class=\"json-section\">\n");
            html.append("                    <div class=\"json-label\">Response JSON (Error):</div>\n");
            html.append("                    <pre>{\n");
            html.append("  \"status\": \"error\",\n");
            html.append("  \"message\": \"Connection failed: Authentication error\",\n");
            html.append("  \"connected\": false\n");
            html.append("}</pre>\n");
            html.append("                </div>\n");
            html.append("            </div>\n");
            html.append("        </div>\n");
            
            html.append("        <div class=\"endpoint-card\">\n");
            html.append("            <span class=\"endpoint-method post\">POST</span>\n");
            html.append("            <span class=\"endpoint-path\">/api/imap/open</span>\n");
            html.append("            <button class=\"expand-btn\" onclick=\"toggleJson('json-imap-open')\">Show JSON</button>\n");
            html.append("            <button class=\"test-btn\" onclick='openTestModal(\"/api/imap/open\", \"{\\n  \\\"sessionId\\\": \\\"test-session-123\\\",\\n  \\\"host\\\": \\\"imap.example.com\\\",\\n  \\\"port\\\": 993,\\n  \\\"username\\\": \\\"user@example.com\\\",\\n  \\\"password\\\": \\\"your-password\\\",\\n  \\\"protocol\\\": \\\"imaps\\\"\\n}\")'>Test</button>\n");
            html.append("            <div class=\"endpoint-desc\">Opens and caches an IMAPS connection</div>\n");
            html.append("            <div id=\"json-imap-open\" class=\"json-example\">\n");
            html.append("                <div class=\"json-section\">\n");
            html.append("                    <div class=\"json-label\">Request JSON:</div>\n");
            html.append("                    <pre>{\n");
            html.append("  \"sessionId\": \"unique-session-id\",\n");
            html.append("  \"host\": \"imap.example.com\",\n");
            html.append("  \"port\": 993,\n");
            html.append("  \"username\": \"user@example.com\",\n");
            html.append("  \"password\": \"your-password\",\n");
            html.append("  \"protocol\": \"imaps\"\n");
            html.append("}</pre>\n");
            html.append("                </div>\n");
            html.append("                <div class=\"json-section\">\n");
            html.append("                    <div class=\"json-label\">Response JSON (Success):</div>\n");
            html.append("                    <pre>{\n");
            html.append("  \"status\": \"success\",\n");
            html.append("  \"message\": \"IMAP connection opened and cached\",\n");
            html.append("  \"sessionId\": \"unique-session-id\"\n");
            html.append("}</pre>\n");
            html.append("                </div>\n");
            html.append("                <div class=\"json-section\">\n");
            html.append("                    <div class=\"json-label\">Response JSON (Error):</div>\n");
            html.append("                    <pre>{\n");
            html.append("  \"status\": \"error\",\n");
            html.append("  \"message\": \"Failed to open connection: Host unreachable\"\n");
            html.append("}</pre>\n");
            html.append("                </div>\n");
            html.append("            </div>\n");
            html.append("        </div>\n");
            
            html.append("        <div class=\"endpoint-card\">\n");
            html.append("            <span class=\"endpoint-method post\">POST</span>\n");
            html.append("            <span class=\"endpoint-path\">/api/imap/close</span>\n");
            html.append("            <button class=\"expand-btn\" onclick=\"toggleJson('json-imap-close')\">Show JSON</button>\n");
            html.append("            <button class=\"test-btn\" onclick='openTestModal(\"/api/imap/close\", \"{\\n  \\\"sessionId\\\": \\\"test-session-123\\\"\\n}\")'>Test</button>\n");
            html.append("            <div class=\"endpoint-desc\">Closes a cached IMAPS connection</div>\n");
            html.append("            <div id=\"json-imap-close\" class=\"json-example\">\n");
            html.append("                <div class=\"json-section\">\n");
            html.append("                    <div class=\"json-label\">Request JSON:</div>\n");
            html.append("                    <pre>{\n");
            html.append("  \"sessionId\": \"unique-session-id\"\n");
            html.append("}</pre>\n");
            html.append("                </div>\n");
            html.append("                <div class=\"json-section\">\n");
            html.append("                    <div class=\"json-label\">Response JSON (Success):</div>\n");
            html.append("                    <pre>{\n");
            html.append("  \"status\": \"success\",\n");
            html.append("  \"message\": \"IMAP connection closed\",\n");
            html.append("  \"sessionId\": \"unique-session-id\"\n");
            html.append("}</pre>\n");
            html.append("                </div>\n");
            html.append("                <div class=\"json-section\">\n");
            html.append("                    <div class=\"json-label\">Response JSON (Error):</div>\n");
            html.append("                    <pre>{\n");
            html.append("  \"status\": \"error\",\n");
            html.append("  \"message\": \"Session not found\"\n");
            html.append("}</pre>\n");
            html.append("                </div>\n");
            html.append("            </div>\n");
            html.append("        </div>\n");
            
            html.append("        <div class=\"endpoint-card\">\n");
            html.append("            <span class=\"endpoint-method post\">POST</span>\n");
            html.append("            <span class=\"endpoint-path\">/api/imap/mailboxCount</span>\n");
            html.append("            <button class=\"expand-btn\" onclick=\"toggleJson('json-imap-count')\">Show JSON</button>\n");
            html.append("            <button class=\"test-btn\" onclick='openTestModal(\"/api/imap/mailboxCount\", \"{\\n  \\\"sessionId\\\": \\\"test-session-123\\\",\\n  \\\"folder\\\": \\\"INBOX\\\"\\n}\")'>Test</button>\n");
            html.append("            <div class=\"endpoint-desc\">Returns the number of emails in a specified folder</div>\n");
            html.append("            <div id=\"json-imap-count\" class=\"json-example\">\n");
            html.append("                <div class=\"json-section\">\n");
            html.append("                    <div class=\"json-label\">Request JSON:</div>\n");
            html.append("                    <pre>{\n");
            html.append("  \"sessionId\": \"unique-session-id\",\n");
            html.append("  \"folder\": \"INBOX\"\n");
            html.append("}</pre>\n");
            html.append("                </div>\n");
            html.append("                <div class=\"json-section\">\n");
            html.append("                    <div class=\"json-label\">Response JSON (Success):</div>\n");
            html.append("                    <pre>{\n");
            html.append("  \"status\": \"success\",\n");
            html.append("  \"folder\": \"INBOX\",\n");
            html.append("  \"messageCount\": 42,\n");
            html.append("  \"unreadCount\": 5\n");
            html.append("}</pre>\n");
            html.append("                </div>\n");
            html.append("                <div class=\"json-section\">\n");
            html.append("                    <div class=\"json-label\">Response JSON (Error):</div>\n");
            html.append("                    <pre>{\n");
            html.append("  \"status\": \"error\",\n");
            html.append("  \"message\": \"Folder not found: INBOX\"\n");
            html.append("}</pre>\n");
            html.append("                </div>\n");
            html.append("            </div>\n");
            html.append("        </div>\n");
            
            html.append("        <div class=\"endpoint-card\">\n");
            html.append("            <span class=\"endpoint-method post\">POST</span>\n");
            html.append("            <span class=\"endpoint-path\">/api/imap/mailboxStats</span>\n");
            html.append("            <button class=\"expand-btn\" onclick=\"toggleJson('json-imap-stats')\">Show JSON</button>\n");
            html.append("            <button class=\"test-btn\" onclick='openTestModal(\"/api/imap/mailboxStats\", \"{\\n  \\\"sessionId\\\": \\\"test-session-123\\\",\\n  \\\"folder\\\": \\\"INBOX\\\",\\n  \\\"includeUnread\\\": true\\n}\")'>Test</button>\n");
            html.append("            <div class=\"endpoint-desc\">Returns detailed statistics for emails in a folder</div>\n");
            html.append("            <div id=\"json-imap-stats\" class=\"json-example\">\n");
            html.append("                <div class=\"json-section\">\n");
            html.append("                    <div class=\"json-label\">Request JSON:</div>\n");
            html.append("                    <pre>{\n");
            html.append("  \"sessionId\": \"unique-session-id\",\n");
            html.append("  \"folder\": \"INBOX\",\n");
            html.append("  \"includeUnread\": true\n");
            html.append("}</pre>\n");
            html.append("                </div>\n");
            html.append("                <div class=\"json-section\">\n");
            html.append("                    <div class=\"json-label\">Response JSON (Success):</div>\n");
            html.append("                    <pre>{\n");
            html.append("  \"status\": \"success\",\n");
            html.append("  \"folder\": \"INBOX\",\n");
            html.append("  \"totalMessages\": 42,\n");
            html.append("  \"unreadMessages\": 5,\n");
            html.append("  \"recentMessages\": 3,\n");
            html.append("  \"folderSize\": 5242880\n");
            html.append("}</pre>\n");
            html.append("                </div>\n");
            html.append("                <div class=\"json-section\">\n");
            html.append("                    <div class=\"json-label\">Response JSON (Error):</div>\n");
            html.append("                    <pre>{\n");
            html.append("  \"status\": \"error\",\n");
            html.append("  \"message\": \"Failed to retrieve mailbox statistics\"\n");
            html.append("}</pre>\n");
            html.append("                </div>\n");
            html.append("            </div>\n");
            html.append("        </div>\n");
            
            html.append("        <div class=\"endpoint-card\">\n");
            html.append("            <span class=\"endpoint-method post\">POST</span>\n");
            html.append("            <span class=\"endpoint-path\">/api/imap/oldestMessage</span>\n");
            html.append("            <button class=\"expand-btn\" onclick=\"toggleJson('json-imap-oldest')\">Show JSON</button>\n");
            html.append("            <button class=\"test-btn\" onclick='openTestModal(\"/api/imap/oldestMessage\", \"{\\n  \\\"mailboxHost\\\": \\\"imap.gmail.com\\\",\\n  \\\"mailboxUser\\\": \\\"user@gmail.com\\\",\\n  \\\"mailboxFolder\\\": \\\"INBOX\\\"\\n}\")'>Test</button>\n");
            html.append("            <div class=\"endpoint-desc\">Returns the oldest message from a folder by received date</div>\n");
            html.append("            <div id=\"json-imap-oldest\" class=\"json-example\">\n");
            html.append("                <div class=\"json-section\">\n");
            html.append("                    <div class=\"json-label\">Request JSON:</div>\n");
            html.append("                    <pre>{\n");
            html.append("  \"mailboxHost\": \"imap.gmail.com\",\n");
            html.append("  \"mailboxUser\": \"user@gmail.com\",\n");
            html.append("  \"mailboxFolder\": \"INBOX\"\n");
            html.append("}</pre>\n");
            html.append("                </div>\n");
            html.append("                <div class=\"json-section\">\n");
            html.append("                    <div class=\"json-label\">Response JSON (Success):</div>\n");
            html.append("                    <pre>{\n");
            html.append("  \"success\": true,\n");
            html.append("  \"mailboxHost\": \"imap.gmail.com\",\n");
            html.append("  \"mailboxUser\": \"user@gmail.com\",\n");
            html.append("  \"mailboxFolder\": \"INBOX\",\n");
            html.append("  \"messageCount\": 150,\n");
            html.append("  \"oldestMessage\": {\n");
            html.append("    \"messageNumber\": 1,\n");
            html.append("    \"messageId\": \"<welcome-123@example.com>\",\n");
            html.append("    \"subject\": \"Welcome Email\",\n");
            html.append("    \"from\": \"admin@example.com\",\n");
            html.append("    \"to\": [\"user@gmail.com\"],\n");
            html.append("    \"receivedDate\": \"Mon Jan 01 10:30:00 UTC 2024\",\n");
            html.append("    \"sentDate\": \"Mon Jan 01 10:29:45 UTC 2024\",\n");
            html.append("    \"size\": 4567\n");
            html.append("  }\n");
            html.append("}</pre>\n");
            html.append("                </div>\n");
            html.append("                <div class=\"json-section\">\n");
            html.append("                    <div class=\"json-label\">Response JSON (Error):</div>\n");
            html.append("                    <pre>{\n");
            html.append("  \"success\": false,\n");
            html.append("  \"error\": \"Connection not open. Use /api/imap/open to establish a connection first\"\n");
            html.append("}</pre>\n");
            html.append("                </div>\n");
            html.append("            </div>\n");
            html.append("        </div>\n");
            
            html.append("        <div class=\"endpoint-card\">\n");
            html.append("            <span class=\"endpoint-method post\">POST</span>\n");
            html.append("            <span class=\"endpoint-path\">/api/imap/newestMessage</span>\n");
            html.append("            <button class=\"expand-btn\" onclick=\"toggleJson('json-imap-newest')\">Show JSON</button>\n");
            html.append("            <button class=\"test-btn\" onclick='openTestModal(\"/api/imap/newestMessage\", \"{\\n  \\\"mailboxHost\\\": \\\"imap.gmail.com\\\",\\n  \\\"mailboxUser\\\": \\\"user@gmail.com\\\",\\n  \\\"mailboxFolder\\\": \\\"INBOX\\\"\\n}\")'>Test</button>\n");
            html.append("            <div class=\"endpoint-desc\">Returns the newest message from a folder by received date</div>\n");
            html.append("            <div id=\"json-imap-newest\" class=\"json-example\">\n");
            html.append("                <div class=\"json-section\">\n");
            html.append("                    <div class=\"json-label\">Request JSON:</div>\n");
            html.append("                    <pre>{\n");
            html.append("  \"mailboxHost\": \"imap.gmail.com\",\n");
            html.append("  \"mailboxUser\": \"user@gmail.com\",\n");
            html.append("  \"mailboxFolder\": \"INBOX\"\n");
            html.append("}</pre>\n");
            html.append("                </div>\n");
            html.append("                <div class=\"json-section\">\n");
            html.append("                    <div class=\"json-label\">Response JSON (Success):</div>\n");
            html.append("                    <pre>{\n");
            html.append("  \"success\": true,\n");
            html.append("  \"mailboxHost\": \"imap.gmail.com\",\n");
            html.append("  \"mailboxUser\": \"user@gmail.com\",\n");
            html.append("  \"mailboxFolder\": \"INBOX\",\n");
            html.append("  \"messageCount\": 150,\n");
            html.append("  \"newestMessage\": {\n");
            html.append("    \"messageNumber\": 150,\n");
            html.append("    \"messageId\": \"<update-456@example.com>\",\n");
            html.append("    \"subject\": \"Latest Update\",\n");
            html.append("    \"from\": \"notifications@example.com\",\n");
            html.append("    \"to\": [\"user@gmail.com\"],\n");
            html.append("    \"receivedDate\": \"Mon Jan 28 09:15:00 UTC 2026\",\n");
            html.append("    \"sentDate\": \"Mon Jan 28 09:14:30 UTC 2026\",\n");
            html.append("    \"size\": 8901\n");
            html.append("  }\n");
            html.append("}</pre>\n");
            html.append("                </div>\n");
            html.append("                <div class=\"json-section\">\n");
            html.append("                    <div class=\"json-label\">Response JSON (Error):</div>\n");
            html.append("                    <pre>{\n");
            html.append("  \"success\": false,\n");
            html.append("  \"error\": \"Connection not open. Use /api/imap/open to establish a connection first\"\n");
            html.append("}</pre>\n");
            html.append("                </div>\n");
            html.append("            </div>\n");
            html.append("        </div>\n");
            
            html.append("        <div class=\"endpoint-card\">\n");
            html.append("            <span class=\"endpoint-method post\">POST</span>\n");
            html.append("            <span class=\"endpoint-path\">/api/imap/processMessages</span>\n");
            html.append("            <button class=\"expand-btn\" onclick=\"toggleJson('json-imap-process')\">Show JSON</button>\n");
            html.append("            <button class=\"test-btn\" onclick='openTestModal(\"/api/imap/processMessages\", \"{\\n  \\\"mailboxHost\\\": \\\"imap.gmail.com\\\",\\n  \\\"mailboxUser\\\": \\\"user@gmail.com\\\",\\n  \\\"mailboxPassword\\\": \\\"app-password\\\",\\n  \\\"mailboxFolder\\\": \\\"INBOX\\\",\\n  \\\"processorClassName\\\": \\\"interfaces.comms.examples.LoggingEmailProcessor\\\",\\n  \\\"threadCount\\\": 4,\\n  \\\"maxMessages\\\": 100,\\n  \\\"processNewest\\\": false,\\n  \\\"removeMessage\\\": false\\n}\")'>Test</button>\n");
            html.append("            <div class=\"endpoint-desc\">Process messages from a mailbox using multiple threads and a custom EmailProcessor implementation</div>\n");
            html.append("            <div id=\"json-imap-process\" class=\"json-example\">\n");
            html.append("                <div class=\"json-section\">\n");
            html.append("                    <div class=\"json-label\">Request JSON:</div>\n");
            html.append("                    <pre>{\n");
            html.append("  \"mailboxHost\": \"imap.gmail.com\",\n");
            html.append("  \"mailboxUser\": \"user@gmail.com\",\n");
            html.append("  \"mailboxPassword\": \"app-password\",\n");
            html.append("  \"mailboxFolder\": \"INBOX\",\n");
            html.append("  \"processorClassName\": \"interfaces.comms.examples.LoggingEmailProcessor\",\n");
            html.append("  \"threadCount\": 4,          // Optional, default: 4, max: 10\n");
            html.append("  \"maxMessages\": 100,        // Optional, processes all if not specified\n");
            html.append("  \"processNewest\": false,    // Optional, default: false\n");
            html.append("  \"removeMessage\": false     // Optional, default: false, deletes on success if true\n");
            html.append("}</pre>\n");
            html.append("                </div>\n");
            html.append("                <div class=\"json-section\">\n");
            html.append("                    <div class=\"json-label\">Response JSON (Success):</div>\n");
            html.append("                    <pre>{\n");
            html.append("  \"success\": true,\n");
            html.append("  \"mailboxHost\": \"imap.gmail.com\",\n");
            html.append("  \"mailboxUser\": \"user@gmail.com\",\n");
            html.append("  \"mailboxFolder\": \"INBOX\",\n");
            html.append("  \"totalMessages\": 150,\n");
            html.append("  \"threadsUsed\": 4,\n");
            html.append("  \"processedCount\": 100,\n");
            html.append("  \"successCount\": 98,\n");
            html.append("  \"errorCount\": 2,\n");
            html.append("  \"deletedCount\": 0,\n");
            html.append("  \"deletedMessageIds\": [],\n");
            html.append("  \"processingTimeMs\": 5234,\n");
            html.append("  \"messageResults\": {\n");
            html.append("    \"&lt;msg1@example.com&gt;\": {\"success\": true, \"message\": \"Processed successfully\"},\n");
            html.append("    \"&lt;msg2@example.com&gt;\": {\"success\": true, \"message\": \"Processed successfully\"}\n");
            html.append("  },\n");
            html.append("  \"errors\": [\n");
            html.append("    {\n");
            html.append("      \"messageId\": \"&lt;msg50@example.com&gt;\",\n");
            html.append("      \"error\": \"Processing failed\",\n");
            html.append("      \"exceptionType\": \"java.lang.Exception\",\n");
            html.append("      \"exceptionMessage\": \"Invalid format\",\n");
            html.append("      \"timestamp\": 1234567890\n");
            html.append("    }\n");
            html.append("  ]\n");
            html.append("}</pre>\n");
            html.append("                </div>\n");
            html.append("                <div class=\"json-section\">\n");
            html.append("                    <div class=\"json-label\">Response JSON (Error):</div>\n");
            html.append("                    <pre>{\n");
            html.append("  \"success\": false,\n");
            html.append("  \"error\": \"processorClassName is required\"\n");
            html.append("}</pre>\n");
            html.append("                </div>\n");
            html.append("            </div>\n");
            html.append("        </div>\n");
            
            html.append("        <div class=\"endpoint-grid\">\n");
            html.append("            <div>\n");
            html.append("                <div class=\"column-header\">JSON API</div>\n");
            html.append("                <div class=\"endpoint-card\">\n");
            html.append("                    <span class=\"endpoint-method\">GET</span>\n");
            html.append("                    <span class=\"endpoint-path\"><a href=\"/comms_processor/api/imap/status\">/api/imap/status</a></span>\n");
            html.append("                    <div class=\"endpoint-desc\">Returns the status of the IMAPS connection cache</div>\n");
            html.append("                </div>\n");
            html.append("                <div class=\"endpoint-card\">\n");
            html.append("                    <span class=\"endpoint-method\">GET</span>\n");
            html.append("                    <span class=\"endpoint-path\"><a href=\"/comms_processor/api/imap/stats\">/api/imap/stats</a></span>\n");
            html.append("                    <div class=\"endpoint-desc\">Returns email reader statistics (total emails read, size, errors, and last 20 errors)</div>\n");
            html.append("                </div>\n");
            html.append("            </div>\n");
            html.append("            <div>\n");
            html.append("                <div class=\"column-header\">HTML View</div>\n");
            html.append("                <div class=\"endpoint-card\">\n");
            html.append("                    <span class=\"endpoint-method\">GET</span>\n");
            html.append("                    <span class=\"endpoint-path\"><a href=\"/comms_processor/api/imap/status.html\">/api/imap/status.html</a></span>\n");
            html.append("                    <div class=\"endpoint-desc\">HTML version of IMAPS connection cache status for browser viewing</div>\n");
            html.append("                </div>\n");
            html.append("                <div class=\"endpoint-card\">\n");
            html.append("                    <span class=\"endpoint-method\">GET</span>\n");
            html.append("                    <span class=\"endpoint-path\"><a href=\"/comms_processor/api/imap/stats.html\">/api/imap/stats.html</a></span>\n");
            html.append("                    <div class=\"endpoint-desc\">HTML version of email reader statistics for browser viewing</div>\n");
            html.append("                </div>\n");
            html.append("            </div>\n");
            html.append("        </div>\n");
            
            html.append("        <h3>SMTP Endpoints</h3>\n");
            html.append("        <div class=\"endpoint-card\">\n");
            html.append("            <span class=\"endpoint-method post\">POST</span>\n");
            html.append("            <span class=\"endpoint-path\">/api/smtp/open</span>\n");
            html.append("            <button class=\"expand-btn\" onclick=\"toggleJson('json-smtp-open')\">Show JSON</button>\n");
            html.append("            <button class=\"test-btn\" onclick='openTestModal(\"/api/smtp/open\", \"{\\n  \\\"host\\\": \\\"smtp.gmail.com\\\",\\n  \\\"username\\\": \\\"user@gmail.com\\\",\\n  \\\"password\\\": \\\"your-password\\\",\\n  \\\"port\\\": 465\\n}\")'>Test</button>\n");
            html.append("            <div class=\"endpoint-desc\">Opens and caches an SMTP connection</div>\n");
            html.append("            <div id=\"json-smtp-open\" class=\"json-example\">\n");
            html.append("                <div class=\"json-section\">\n");
            html.append("                    <div class=\"json-label\">Request JSON:</div>\n");
            html.append("                    <pre>{\n");
            html.append("  \"host\": \"smtp.gmail.com\",\n");
            html.append("  \"username\": \"user@gmail.com\",\n");
            html.append("  \"password\": \"your-password\",\n");
            html.append("  \"port\": 465\n");
            html.append("}</pre>\n");
            html.append("                </div>\n");
            html.append("                <div class=\"json-section\">\n");
            html.append("                    <div class=\"json-label\">Response JSON (Success):</div>\n");
            html.append("                    <pre>{\n");
            html.append("  \"success\": true,\n");
            html.append("  \"message\": \"SMTP connection opened and cached\"\n");
            html.append("}</pre>\n");
            html.append("                </div>\n");
            html.append("                <div class=\"json-section\">\n");
            html.append("                    <div class=\"json-label\">Response JSON (Error):</div>\n");
            html.append("                    <pre>{\n");
            html.append("  \"success\": false,\n");
            html.append("  \"error\": \"Failed to connect to SMTP server\"\n");
            html.append("}</pre>\n");
            html.append("                </div>\n");
            html.append("            </div>\n");
            html.append("        </div>\n");
            
            html.append("        <div class=\"endpoint-card\">\n");
            html.append("            <span class=\"endpoint-method post\">POST</span>\n");
            html.append("            <span class=\"endpoint-path\">/api/smtp/close</span>\n");
            html.append("            <button class=\"expand-btn\" onclick=\"toggleJson('json-smtp-close')\">Show JSON</button>\n");
            html.append("            <button class=\"test-btn\" onclick='openTestModal(\"/api/smtp/close\", \"{\\n  \\\"host\\\": \\\"smtp.gmail.com\\\",\\n  \\\"username\\\": \\\"user@gmail.com\\\"\\n}\")'>Test</button>\n");
            html.append("            <div class=\"endpoint-desc\">Closes a cached SMTP connection</div>\n");
            html.append("            <div id=\"json-smtp-close\" class=\"json-example\">\n");
            html.append("                <div class=\"json-section\">\n");
            html.append("                    <div class=\"json-label\">Request JSON:</div>\n");
            html.append("                    <pre>{\n");
            html.append("  \"host\": \"smtp.gmail.com\",\n");
            html.append("  \"username\": \"user@gmail.com\"\n");
            html.append("}</pre>\n");
            html.append("                </div>\n");
            html.append("                <div class=\"json-section\">\n");
            html.append("                    <div class=\"json-label\">Response JSON (Success):</div>\n");
            html.append("                    <pre>{\n");
            html.append("  \"success\": true,\n");
            html.append("  \"message\": \"SMTP connection closed\"\n");
            html.append("}</pre>\n");
            html.append("                </div>\n");
            html.append("                <div class=\"json-section\">\n");
            html.append("                    <div class=\"json-label\">Response JSON (Error):</div>\n");
            html.append("                    <pre>{\n");
            html.append("  \"status\": \"error\",\n");
            html.append("  \"message\": \"Session not found\"\n");
            html.append("}</pre>\n");
            html.append("                </div>\n");
            html.append("            </div>\n");
            html.append("        </div>\n");
            
            html.append("        <div class=\"endpoint-card\">\n");
            html.append("            <span class=\"endpoint-method post\">POST</span>\n");
            html.append("            <span class=\"endpoint-path\">/api/smtp/sendTextMessage</span>\n");
            html.append("            <button class=\"expand-btn\" onclick=\"toggleJson('json-smtp-text')\">Show JSON</button>\n");
            html.append("            <button class=\"test-btn\" onclick='openTestModal(\"/api/smtp/sendTextMessage\", \"{\\n  \\\"smtpHost\\\": \\\"smtp.gmail.com\\\",\\n  \\\"smtpUser\\\": \\\"user@gmail.com\\\",\\n  \\\"fromAddress\\\": \\\"sender@example.com\\\",\\n  \\\"toAddress\\\": \\\"recipient@example.com\\\",\\n  \\\"subject\\\": \\\"Test Email\\\",\\n  \\\"body\\\": \\\"This is a test message\\\"\\n}\")'>Test</button>\n");
            html.append("            <div class=\"endpoint-desc\">Sends a simple text email using a cached SMTP connection</div>\n");
            html.append("            <div id=\"json-smtp-text\" class=\"json-example\">\n");
            html.append("                <div class=\"json-section\">\n");
            html.append("                    <div class=\"json-label\">Request JSON:</div>\n");
            html.append("                    <pre>{\n");
            html.append("  \"smtpHost\": \"smtp.gmail.com\",\n");
            html.append("  \"smtpUser\": \"user@gmail.com\",\n");
            html.append("  \"fromAddress\": \"sender@example.com\",\n");
            html.append("  \"toAddress\": \"recipient@example.com\",\n");
            html.append("  \"subject\": \"Email Subject\",\n");
            html.append("  \"body\": \"Email body content\"\n");
            html.append("}</pre>\n");
            html.append("                </div>\n");
            html.append("                <div class=\"json-section\">\n");
            html.append("                    <div class=\"json-label\">Response JSON (Success):</div>\n");
            html.append("                    <pre>{\n");
            html.append("  \"status\": \"success\",\n");
            html.append("  \"message\": \"Email sent successfully\",\n");
            html.append("  \"messageId\": \"abc123xyz\",\n");
            html.append("  \"recipients\": 1\n");
            html.append("}</pre>\n");
            html.append("                </div>\n");
            html.append("                <div class=\"json-section\">\n");
            html.append("                    <div class=\"json-label\">Response JSON (Error):</div>\n");
            html.append("                    <pre>{\n");
            html.append("  \"status\": \"error\",\n");
            html.append("  \"message\": \"Failed to send email: Invalid recipient address\"\n");
            html.append("}</pre>\n");
            html.append("                </div>\n");
            html.append("            </div>\n");
            html.append("        </div>\n");
            
            html.append("        <div class=\"endpoint-card\">\n");
            html.append("            <span class=\"endpoint-method post\">POST</span>\n");
            html.append("            <span class=\"endpoint-path\">/api/smtp/send</span>\n");
            html.append("            <button class=\"expand-btn\" onclick=\"toggleJson('json-smtp-send')\">Show JSON</button>\n");
            html.append("            <button class=\"test-btn\" onclick='openTestModal(\"/api/smtp/send\", \"{\\n  \\\"smtpHost\\\": \\\"smtp.gmail.com\\\",\\n  \\\"smtpUser\\\": \\\"user@gmail.com\\\",\\n  \\\"data\\\": \\\"VGVzdCBFTUwgY29udGVudCBlbmNvZGVkIGluIEJhc2U2NA==\\\"\\n}\")'>Test</button>\n");
            html.append("            <div class=\"endpoint-desc\">Sends email(s) in .eml format using a cached SMTP connection</div>\n");
            html.append("            <div id=\"json-smtp-send\" class=\"json-example\">\n");
            html.append("                <div class=\"json-section\">\n");
            html.append("                    <div class=\"json-label\">Request JSON:</div>\n");
            html.append("                    <pre>{\n");
            html.append("  \"smtpHost\": \"smtp.gmail.com\",\n");
            html.append("  \"smtpUser\": \"user@gmail.com\",\n");
            html.append("  \"data\": \"Base64 encoded .eml file content\"\n");
            html.append("}</pre>\n");
            html.append("                </div>\n");
            html.append("                <div class=\"json-section\">\n");
            html.append("                    <div class=\"json-label\">Response JSON (Success):</div>\n");
            html.append("                    <pre>{\n");
            html.append("  \"status\": \"success\",\n");
            html.append("  \"message\": \"Email sent successfully\",\n");
            html.append("  \"messageId\": \"xyz789abc\",\n");
            html.append("  \"emailsSent\": 1\n");
            html.append("}</pre>\n");
            html.append("                </div>\n");
            html.append("                <div class=\"json-section\">\n");
            html.append("                    <div class=\"json-label\">Response JSON (Error):</div>\n");
            html.append("                    <pre>{\n");
            html.append("  \"status\": \"error\",\n");
            html.append("  \"message\": \"Failed to parse .eml content\"\n");
            html.append("}</pre>\n");
            html.append("                </div>\n");
            html.append("            </div>\n");
            html.append("        </div>\n");
            
            html.append("        <div class=\"endpoint-grid\">\n");
            html.append("            <div>\n");
            html.append("                <div class=\"column-header\">JSON API</div>\n");
            html.append("                <div class=\"endpoint-card\">\n");
            html.append("                    <span class=\"endpoint-method\">GET</span>\n");
            html.append("                    <span class=\"endpoint-path\"><a href=\"/comms_processor/api/smtp/status\">/api/smtp/status</a></span>\n");
            html.append("                    <div class=\"endpoint-desc\">Returns the status of the SMTP connection cache</div>\n");
            html.append("                </div>\n");
            html.append("                <div class=\"endpoint-card\">\n");
            html.append("                    <span class=\"endpoint-method\">GET</span>\n");
            html.append("                    <span class=\"endpoint-path\"><a href=\"/comms_processor/api/smtp/stats\">/api/smtp/stats</a></span>\n");
            html.append("                    <div class=\"endpoint-desc\">Returns email sender statistics (total emails sent, size, errors, and last 20 errors)</div>\n");
            html.append("                </div>\n");
            html.append("            </div>\n");
            html.append("            <div>\n");
            html.append("                <div class=\"column-header\">HTML View</div>\n");
            html.append("                <div class=\"endpoint-card\">\n");
            html.append("                    <span class=\"endpoint-method\">GET</span>\n");
            html.append("                    <span class=\"endpoint-path\"><a href=\"/comms_processor/api/smtp/status.html\">/api/smtp/status.html</a></span>\n");
            html.append("                    <div class=\"endpoint-desc\">HTML version of SMTP connection cache status for browser viewing</div>\n");
            html.append("                </div>\n");
            html.append("                <div class=\"endpoint-card\">\n");
            html.append("                    <span class=\"endpoint-method\">GET</span>\n");
            html.append("                    <span class=\"endpoint-path\"><a href=\"/comms_processor/api/smtp/stats.html\">/api/smtp/stats.html</a></span>\n");
            html.append("                    <div class=\"endpoint-desc\">HTML version of email sender statistics for browser viewing</div>\n");
            html.append("                </div>\n");
            html.append("            </div>\n");
            html.append("        </div>\n");
            
            // Properties Section
            html.append("        <h2>System Properties</h2>\n");
            
            // Load and display properties
            Properties dbProps = loadProperties("/database.properties");
            if (dbProps != null && !dbProps.isEmpty()) {
                html.append("        <h3>Database Configuration</h3>\n");
                html.append("        <table>\n");
                html.append("            <thead>\n");
                html.append("                <tr>\n");
                html.append("                    <th>Description</th>\n");
                html.append("                    <th>Name</th>\n");
                html.append("                    <th>Value</th>\n");
                html.append("                </tr>\n");
                html.append("            </thead>\n");
                html.append("            <tbody>\n");
                
                // PostgreSQL
                html.append("                <tr>\n");
                html.append("                    <td>PostgreSQL connection URL</td>\n");
                html.append("                    <td>postgresql.url</td>\n");
                html.append("                    <td>").append(escapeHtml(maskSensitiveValue(dbProps.getProperty("postgresql.url")))).append("</td>\n");
                html.append("                </tr>\n");
                html.append("                <tr>\n");
                html.append("                    <td>PostgreSQL username</td>\n");
                html.append("                    <td>postgresql.username</td>\n");
                html.append("                    <td>").append(escapeHtml(maskSensitiveValue(dbProps.getProperty("postgresql.username")))).append("</td>\n");
                html.append("                </tr>\n");
                html.append("                <tr>\n");
                html.append("                    <td>PostgreSQL driver class</td>\n");
                html.append("                    <td>postgresql.driver</td>\n");
                html.append("                    <td>").append(escapeHtml(dbProps.getProperty("postgresql.driver"))).append("</td>\n");
                html.append("                </tr>\n");
                
                // Oracle
                html.append("                <tr>\n");
                html.append("                    <td>Oracle connection URL</td>\n");
                html.append("                    <td>oracle.url</td>\n");
                html.append("                    <td>").append(escapeHtml(maskSensitiveValue(dbProps.getProperty("oracle.url")))).append("</td>\n");
                html.append("                </tr>\n");
                html.append("                <tr>\n");
                html.append("                    <td>Oracle username</td>\n");
                html.append("                    <td>oracle.username</td>\n");
                html.append("                    <td>").append(escapeHtml(maskSensitiveValue(dbProps.getProperty("oracle.username")))).append("</td>\n");
                html.append("                </tr>\n");
                html.append("                <tr>\n");
                html.append("                    <td>Oracle driver class</td>\n");
                html.append("                    <td>oracle.driver</td>\n");
                html.append("                    <td>").append(escapeHtml(dbProps.getProperty("oracle.driver"))).append("</td>\n");
                html.append("                </tr>\n");
                
                // Connection Pool
                html.append("                <tr>\n");
                html.append("                    <td>Minimum connection pool size</td>\n");
                html.append("                    <td>connection.pool.min</td>\n");
                html.append("                    <td>").append(escapeHtml(dbProps.getProperty("connection.pool.min"))).append("</td>\n");
                html.append("                </tr>\n");
                html.append("                <tr>\n");
                html.append("                    <td>Maximum connection pool size</td>\n");
                html.append("                    <td>connection.pool.max</td>\n");
                html.append("                    <td>").append(escapeHtml(dbProps.getProperty("connection.pool.max"))).append("</td>\n");
                html.append("                </tr>\n");
                
                html.append("            </tbody>\n");
                html.append("        </table>\n");
            }
            
            // Email Configuration
            html.append("        <h3>Email Configuration</h3>\n");
            html.append("        <table>\n");
            html.append("            <thead>\n");
            html.append("                <tr>\n");
            html.append("                    <th>Description</th>\n");
            html.append("                    <th>Name</th>\n");
            html.append("                    <th>Value</th>\n");
            html.append("                </tr>\n");
            html.append("            </thead>\n");
            html.append("            <tbody>\n");
            
            // Email Sender Properties
            html.append("                <tr>\n");
            html.append("                    <td>Email sender maximum batch size</td>\n");
            html.append("                    <td>email-sender.maxBatchSize</td>\n");
            html.append("                    <td>").append(System.getProperty("email-sender.maxBatchSize", "100")).append("</td>\n");
            html.append("                </tr>\n");
            html.append("                <tr>\n");
            html.append("                    <td>Email sender minimum batch size</td>\n");
            html.append("                    <td>email-sender.minBatchSize</td>\n");
            html.append("                    <td>").append(System.getProperty("email-sender.minBatchSize", "1")).append("</td>\n");
            html.append("                </tr>\n");
            html.append("                <tr>\n");
            html.append("                    <td>Email sender maximum connections</td>\n");
            html.append("                    <td>email-sender.maxConnections</td>\n");
            html.append("                    <td>").append(System.getProperty("email-sender.maxConnections", "50")).append("</td>\n");
            html.append("                </tr>\n");
            html.append("                <tr>\n");
            html.append("                    <td>Email sender maximum pool size</td>\n");
            html.append("                    <td>email-sender.maxPoolSize</td>\n");
            html.append("                    <td>").append(System.getProperty("email-sender.maxPoolSize", "100")).append("</td>\n");
            html.append("                </tr>\n");
            
            // Email Reader Properties
            html.append("                <tr>\n");
            html.append("                    <td>Email reader maximum batch size</td>\n");
            html.append("                    <td>email-reader.maxBatchSize</td>\n");
            html.append("                    <td>").append(System.getProperty("email-reader.maxBatchSize", "100")).append("</td>\n");
            html.append("                </tr>\n");
            html.append("                <tr>\n");
            html.append("                    <td>Email reader minimum batch size</td>\n");
            html.append("                    <td>email-reader.minBatchSize</td>\n");
            html.append("                    <td>").append(System.getProperty("email-reader.minBatchSize", "1")).append("</td>\n");
            html.append("                </tr>\n");
            html.append("                <tr>\n");
            html.append("                    <td>Email reader maximum connections</td>\n");
            html.append("                    <td>email-reader.maxConnections</td>\n");
            html.append("                    <td>").append(System.getProperty("email-reader.maxConnections", "50")).append("</td>\n");
            html.append("                </tr>\n");
            html.append("                <tr>\n");
            html.append("                    <td>Email reader maximum pool size</td>\n");
            html.append("                    <td>email-reader.maxPoolSize</td>\n");
            html.append("                    <td>").append(System.getProperty("email-reader.maxPoolSize", "100")).append("</td>\n");
            html.append("                </tr>\n");
            
            html.append("            </tbody>\n");
            html.append("        </table>\n");
            
            // Java Environment
            html.append("        <h3>Java Environment</h3>\n");
            html.append("        <table>\n");
            html.append("            <thead>\n");
            html.append("                <tr>\n");
            html.append("                    <th>Description</th>\n");
            html.append("                    <th>Name</th>\n");
            html.append("                    <th>Value</th>\n");
            html.append("                </tr>\n");
            html.append("            </thead>\n");
            html.append("            <tbody>\n");
            html.append("                <tr>\n");
            html.append("                    <td>Java version</td>\n");
            html.append("                    <td>java.version</td>\n");
            html.append("                    <td>").append(escapeHtml(System.getProperty("java.version"))).append("</td>\n");
            html.append("                </tr>\n");
            html.append("                <tr>\n");
            html.append("                    <td>Java vendor</td>\n");
            html.append("                    <td>java.vendor</td>\n");
            html.append("                    <td>").append(escapeHtml(System.getProperty("java.vendor"))).append("</td>\n");
            html.append("                </tr>\n");
            html.append("                <tr>\n");
            html.append("                    <td>Java home directory</td>\n");
            html.append("                    <td>java.home</td>\n");
            html.append("                    <td>").append(escapeHtml(System.getProperty("java.home"))).append("</td>\n");
            html.append("                </tr>\n");
            html.append("            </tbody>\n");
            html.append("        </table>\n");
            
            html.append("    </div>\n");
            
            // Add modal HTML structure
            html.append("    <!-- Test Modal -->\n");
            html.append("    <div id=\"testModal\" class=\"modal\">\n");
            html.append("        <div class=\"modal-content\">\n");
            html.append("            <div class=\"modal-header\">\n");
            html.append("                <h2 id=\"modalTitle\">Test API Endpoint</h2>\n");
            html.append("                <span class=\"close-btn\" onclick=\"closeTestModal()\">&times;</span>\n");
            html.append("            </div>\n");
            html.append("            <div class=\"modal-body\">\n");
            html.append("                <label for=\"requestJson\">Request JSON:</label>\n");
            html.append("                <textarea id=\"requestJson\" placeholder=\"Enter your JSON here...\"></textarea>\n");
            html.append("            </div>\n");
            html.append("            <div class=\"modal-footer\">\n");
            html.append("                <button class=\"post-btn\" onclick=\"sendTestRequest()\">POST Request</button>\n");
            html.append("            </div>\n");
            html.append("            <div id=\"responseArea\" class=\"response-area\" style=\"display:none;\"></div>\n");
            html.append("        </div>\n");
            html.append("    </div>\n");
            
            // Add JavaScript for expand/collapse functionality
            html.append("    <script>\n");
            html.append("        var currentEndpoint = '';\n");
            html.append("        \n");
            html.append("        function toggleJson(id) {\n");
            html.append("            var element = document.getElementById(id);\n");
            html.append("            var button = event.target;\n");
            html.append("            if (element.classList.contains('show')) {\n");
            html.append("                element.classList.remove('show');\n");
            html.append("                button.textContent = 'Show JSON';\n");
            html.append("            } else {\n");
            html.append("                element.classList.add('show');\n");
            html.append("                button.textContent = 'Hide JSON';\n");
            html.append("            }\n");
            html.append("        }\n");
            html.append("        \n");
            html.append("        function openTestModal(endpoint, jsonData) {\n");
            html.append("            currentEndpoint = endpoint;\n");
            html.append("            document.getElementById('modalTitle').textContent = 'Test: ' + endpoint;\n");
            html.append("            document.getElementById('requestJson').value = jsonData;\n");
            html.append("            document.getElementById('testModal').style.display = 'block';\n");
            html.append("            document.getElementById('responseArea').style.display = 'none';\n");
            html.append("            document.getElementById('responseArea').className = 'response-area';\n");
            html.append("        }\n");
            html.append("        \n");
            html.append("        function closeTestModal() {\n");
            html.append("            document.getElementById('testModal').style.display = 'none';\n");
            html.append("        }\n");
            html.append("        \n");
            html.append("        window.onclick = function(event) {\n");
            html.append("            var modal = document.getElementById('testModal');\n");
            html.append("            if (event.target == modal) {\n");
            html.append("                closeTestModal();\n");
            html.append("            }\n");
            html.append("        }\n");
            html.append("        \n");
            html.append("        function sendTestRequest() {\n");
            html.append("            var jsonData = document.getElementById('requestJson').value;\n");
            html.append("            var responseArea = document.getElementById('responseArea');\n");
            html.append("            \n");
            html.append("            try {\n");
            html.append("                JSON.parse(jsonData);\n");
            html.append("            } catch (e) {\n");
            html.append("                responseArea.style.display = 'block';\n");
            html.append("                responseArea.className = 'response-area error';\n");
            html.append("                responseArea.textContent = 'Invalid JSON: ' + e.message;\n");
            html.append("                return;\n");
            html.append("            }\n");
            html.append("            \n");
            html.append("            responseArea.style.display = 'block';\n");
            html.append("            responseArea.className = 'response-area';\n");
            html.append("            responseArea.textContent = 'Sending request...';\n");
            html.append("            \n");
            html.append("            fetch('/comms_processor' + currentEndpoint, {\n");
            html.append("                method: 'POST',\n");
            html.append("                headers: {\n");
            html.append("                    'Content-Type': 'application/json'\n");
            html.append("                },\n");
            html.append("                body: jsonData\n");
            html.append("            })\n");
            html.append("            .then(response => {\n");
            html.append("                return response.text().then(text => ({\n");
            html.append("                    status: response.status,\n");
            html.append("                    statusText: response.statusText,\n");
            html.append("                    body: text,\n");
            html.append("                    ok: response.ok\n");
            html.append("                }));\n");
            html.append("            })\n");
            html.append("            .then(result => {\n");
            html.append("                var displayText = 'Status: ' + result.status + ' ' + result.statusText + '\\n\\n';\n");
            html.append("                try {\n");
            html.append("                    var jsonResponse = JSON.parse(result.body);\n");
            html.append("                    displayText += JSON.stringify(jsonResponse, null, 2);\n");
            html.append("                } catch (e) {\n");
            html.append("                    displayText += result.body;\n");
            html.append("                }\n");
            html.append("                \n");
            html.append("                responseArea.textContent = displayText;\n");
            html.append("                if (result.ok) {\n");
            html.append("                    responseArea.className = 'response-area success';\n");
            html.append("                } else {\n");
            html.append("                    responseArea.className = 'response-area error';\n");
            html.append("                }\n");
            html.append("            })\n");
            html.append("            .catch(error => {\n");
            html.append("                responseArea.className = 'response-area error';\n");
            html.append("                responseArea.textContent = 'Error: ' + error.message;\n");
            html.append("            });\n");
            html.append("        }\n");
            html.append("    </script>\n");
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
