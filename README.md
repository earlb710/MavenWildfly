# MavenWildfly

## Communications Processor Project

A Java EE web application configured for deployment to Wildfly application server with PostgreSQL and Oracle SQL database support.

## Project Structure

```
MavenWildfly/
├── README.md                                     # Project overview and setup guide
├── TESTING.md                                    # Manual API testing instructions
└── comms_processor/
    ├── pom.xml                                   # Maven configuration
    ├── nb-configuration.xml                      # NetBeans project settings
    └── src/
        └── main/
            ├── java/
            │   └── interfaces/comms/
            │       ├── model/
            │       │   ├── ImapConnectionInfo.java   # IMAP connection request model
            │       │   └── SmtpConnectionInfo.java   # SMTP connection request model
            │       ├── rest/
            │       │   ├── RestApplication.java      # JAX-RS application configuration
            │       │   ├── StatusHtmlResource.java   # HTML status page endpoint
            │       │   ├── StatusResource.java       # Status REST endpoints
            │       │   ├── ImapConnectionResource.java # IMAP REST endpoints
            │       │   └── SmtpConnectionResource.java # SMTP REST endpoints
            │       └── service/
            │           ├── WildFlyManagementService.java   # WildFly management operations
            │           ├── ImapConnectionService.java      # IMAP connection processing
            │           ├── ImapConnectionCacheService.java # IMAP cache handling
            │           ├── SmtpConnectionService.java      # SMTP connection processing
            │           └── SmtpConnectionCacheService.java # SMTP cache handling
            ├── resources/
            │   ├── database.properties             # Database configuration
            │   ├── imap-default-settings.json      # Default IMAP request settings
            │   └── smtp-default-settings.json      # Default SMTP request settings
            └── webapp/
                ├── WEB-INF/
                │   ├── web.xml                     # Web application descriptor
                │   └── beans.xml                   # CDI configuration
                └── index.html                      # Welcome page
```

## Prerequisites

- Java 11 or higher
- Maven 3.6 or higher
- Wildfly application server

## Dependencies

- **Jakarta EE API 8.0.0** - Java EE platform APIs (provided by Wildfly)
- **PostgreSQL JDBC Driver 42.7.7** - PostgreSQL database connectivity (patched for security vulnerabilities)
- **Oracle JDBC Driver 21.9.0.0** - Oracle database connectivity
- **Jakarta Mail 2.0.1** - JavaMail API for IMAPS/email connectivity
- **JUnit 4.13.2** - Unit testing framework

## Building the Project

Navigate to the project directory and run:

```bash
cd comms_processor
mvn clean package
```

This will create a WAR file at `target/comms_processor.war`

## Default Mail Settings Templates

The project includes JSON templates in `comms_processor/src/main/resources/` for common mail settings:

- `imap-default-settings.json` - default IMAP connection and mailbox settings
- `smtp-default-settings.json` - default SMTP connection and message settings

## Deploying to Wildfly

### Manual Deployment

1. Copy the WAR file to Wildfly deployment directory:
   ```bash
   cp target/comms_processor.war $WILDFLY_HOME/standalone/deployments/
   ```

### Maven Plugin Deployment

Deploy directly using the Wildfly Maven Plugin:

```bash
mvn wildfly:deploy
```

To redeploy:
```bash
mvn wildfly:redeploy
```

To undeploy:
```bash
mvn wildfly:undeploy
```

## Database Configuration

### PostgreSQL Setup

1. Install PostgreSQL
2. Create database:
   ```sql
   CREATE DATABASE comms_db;
   CREATE USER comms_user WITH PASSWORD 'changeme';
   GRANT ALL PRIVILEGES ON DATABASE comms_db TO comms_user;
   ```

3. Configure datasource in Wildfly (standalone.xml or via CLI)

### Oracle Setup

1. Install Oracle Database
2. Create user and schema as needed
3. Configure datasource in Wildfly

### Wildfly Datasource Configuration

Example CLI commands for configuring datasources in Wildfly:

**PostgreSQL:**
```bash
/subsystem=datasources/jdbc-driver=postgresql:add(driver-name=postgresql,driver-module-name=org.postgresql,driver-class-name=org.postgresql.Driver)

data-source add --name=PostgresDS --jndi-name=java:jboss/datasources/PostgresDS --driver-name=postgresql --connection-url=jdbc:postgresql://localhost:5432/comms_db --user-name=comms_user --password=changeme
```

**Oracle:**
```bash
/subsystem=datasources/jdbc-driver=oracle:add(driver-name=oracle,driver-module-name=com.oracle,driver-class-name=oracle.jdbc.OracleDriver)

data-source add --name=OracleDS --jndi-name=java:jboss/datasources/OracleDS --driver-name=oracle --connection-url=jdbc:oracle:thin:@localhost:1521:ORCL --user-name=comms_user --password=changeme
```

## REST API Endpoints

The application provides REST API endpoints for monitoring and status checking:

### Status Endpoints

Base URL: `http://localhost:8080/comms_processor/api/status`

#### 0. Main API Status Endpoint (NEW)

**Endpoint:** `GET /api/status`

**Description:** Returns a comprehensive overview of all available API endpoints and current system property values. This is the main entry point to discover all available APIs and their configurations.

**Response Example:**
```json
{
  "status": "ok",
  "timestamp": 1706012345678,
  "applicationName": "Communications Processor",
  "version": "1.0.0-SNAPSHOT",
  "totalEndpoints": 17,
  "availableEndpoints": [
    {
      "path": "/api/status/ping",
      "method": "GET",
      "description": "Simple ping endpoint to verify service availability"
    },
    {
      "path": "/api/status/serverStatus",
      "method": "GET",
      "description": "Returns detailed information about all services running on WildFly"
    },
    {
      "path": "/api/imap/test",
      "method": "POST",
      "description": "Tests IMAPS connection with provided credentials"
    },
    {
      "path": "/api/smtp/send",
      "method": "POST",
      "description": "Sends email(s) in .eml format using a cached SMTP connection"
    }
  ],
  "properties": {
    "postgresql": {
      "url": "jdbc***:5432/comms_db",
      "username": "comm***user",
      "driver": "org.postgresql.Driver"
    },
    "oracle": {
      "url": "jdbc***:1521:ORCL",
      "username": "comm***user",
      "driver": "oracle.jdbc.OracleDriver"
    },
    "connectionPool": {
      "min": "5",
      "max": "20"
    },
    "emailSender": {
      "maxBatchSize": "100",
      "minBatchSize": "1",
      "maxConnections": "50",
      "maxPoolSize": "100"
    },
    "emailReader": {
      "maxBatchSize": "100",
      "minBatchSize": "1",
      "maxConnections": "50",
      "maxPoolSize": "100"
    },
    "java": {
      "version": "11.0.20",
      "vendor": "Oracle Corporation",
      "home": "/usr/lib/jvm/java-11-openjdk"
    }
  }
}
```

**Usage:**
```bash
curl http://localhost:8080/comms_processor/api/status
```

**Key Features:**
- **API Discovery**: Lists all available endpoints with their HTTP methods and descriptions
- **Property Values**: Shows current configuration values from database.properties and system properties
- **Security**: Sensitive values (URLs, usernames) are partially masked for security
- **Centralized**: Single endpoint to understand the entire API surface

#### 0b. Main API Status HTML Endpoint (NEW)

**Endpoint:** `GET /api/status.html`

**Description:** HTML version of the main API status endpoint for browser viewing. Provides a user-friendly web interface showing all available API endpoints, their methods, descriptions, and current system property values.

**Usage:**
- Open in browser: http://localhost:8080/comms_processor/api/status.html

**Key Features:**
- **Browser-Friendly**: Fully formatted HTML page with professional styling
- **Complete Overview**: Shows all API endpoints grouped by category (Status, IMAP, SMTP)
- **Visual Design**: Color-coded HTTP methods (GET in green, POST in yellow)
- **Navigation**: Quick links to other HTML status pages
- **System Properties**: Displays database configuration, email settings, and Java environment
- **Responsive**: Mobile-friendly design
- **Security**: Sensitive values are masked for security

**What's Displayed:**
- Application status and version information
- All available REST API endpoints with descriptions
- Status endpoints (ping, serverStatus, datasources)
- IMAP endpoints (test, open, close, mailboxCount, mailboxStats, status)
- SMTP endpoints (open, close, sendTextMessage, send, status)
- Database configuration (PostgreSQL and Oracle)
- Connection pool settings
- Email sender and reader configuration
- Java environment details

#### 1. Ping Endpoint

**Endpoint:** `GET /api/status/ping`

**Description:** Simple health check endpoint to verify the service is running.

**Response Example:**
```json
{
  "status": "ok",
  "message": "Service is running",
  "timestamp": 1706012345678
}
```

**Usage:**
```bash
curl http://localhost:8080/comms_processor/api/status/ping
```

#### 2. Server Status Endpoint

**Endpoint:** `GET /api/status/serverStatus`

**Description:** Returns detailed information about all services running on the WildFly application server, including deployments, subsystems, and managed resources.

**Response Example:**
```json
{
  "status": "ok",
  "timestamp": 1706012345678,
  "servicesCount": 25,
  "services": [
    {
      "name": "java.lang:type=Runtime",
      "type": "Runtime",
      "domain": "java.lang",
      "state": "RUNNING",
      "objectName": "java.lang:type=Runtime"
    },
    {
      "name": "WildFly Application Server",
      "type": "ApplicationServer",
      "domain": "system",
      "state": "RUNNING"
    },
    ...
  ]
}
```

**Usage:**
```bash
curl http://localhost:8080/comms_processor/api/status/serverStatus
```

#### 3. Datasources Endpoint

**Endpoint:** `GET /api/status/datasources`

**Description:** Returns a list of all available datasources configured in WildFly, including both standard and XA datasources.

**Response Example:**
```json
{
  "status": "ok",
  "timestamp": 1706012345678,
  "datasourcesCount": 2,
  "datasources": [
    {
      "name": "PostgresDS",
      "type": "STANDARD",
      "jndiName": "java:jboss/datasources/PostgresDS",
      "enabled": true,
      "driverName": "postgresql",
      "connectionUrl": "jdbc:postgresql://localhost:5432/comms_db",
      "objectName": "jboss.as:subsystem=datasources,data-source=PostgresDS"
    },
    {
      "name": "OracleDS",
      "type": "STANDARD",
      "jndiName": "java:jboss/datasources/OracleDS",
      "enabled": true,
      "driverName": "oracle",
      "connectionUrl": "jdbc:oracle:thin:@localhost:1521:ORCL",
      "objectName": "jboss.as:subsystem=datasources,data-source=OracleDS"
    }
  ]
}
```

**Usage:**
```bash
curl http://localhost:8080/comms_processor/api/status/datasources
```

### IMAPS Connection Endpoints

Base URL: `http://localhost:8080/comms_processor/api/imap`

#### 1. Test IMAPS Connection

**Endpoint:** `POST /api/imap/test`

**Description:** Tests connectivity to an IMAPS server using provided credentials. Uses TLS 1.2/1.3 encryption for secure connections to IMAPS servers on port 993. **Does NOT cache the connection.**

**Request Body:**
```json
{
  "host": "imap.gmail.com",
  "username": "user@gmail.com",
  "password": "your-password"
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "connectionTimeMs": 1250
}
```

**Failure Response (503 Service Unavailable):**
```json
{
  "success": false,
  "error": "Connection refused",
  "connectionTimeMs": 10050
}
```

**Usage Example:**
```bash
curl -X POST http://localhost:8080/comms_processor/api/imap/test \
  -H "Content-Type: application/json" \
  -d '{
    "host": "imap.gmail.com",
    "username": "user@gmail.com",
    "password": "your-password"
  }'
```

**Notes:**
- All three fields (host, username, password) are required
- Returns connection time in milliseconds
- Supports TLS 1.2 and TLS 1.3 encryption
- Default connection timeout is 10 seconds
- Standard IMAPS port 993 is used
- **Connection is NOT cached** - creates a new connection for each test

#### 2. Open IMAPS Connection (Cached)

**Endpoint:** `POST /api/imap/open`

**Description:** Opens and caches an IMAPS connection for reuse.

**Request Body:**
```json
{
  "host": "imap.gmail.com",
  "username": "user@gmail.com",
  "password": "your-password"
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "host": "imap.gmail.com",
  "username": "user@gmail.com",
  "cached": true
}
```

**Usage Example:**
```bash
curl -X POST http://localhost:8080/comms_processor/api/imap/open \
  -H "Content-Type: application/json" \
  -d '{
    "host": "imap.gmail.com",
    "username": "user@gmail.com",
    "password": "your-password"
  }'
```

#### 3. Close IMAPS Connection

**Endpoint:** `POST /api/imap/close`

**Description:** Closes a cached IMAPS connection.

**Request Body:**
```json
{
  "host": "imap.gmail.com",
  "username": "user@gmail.com"
}
```

**Success Response (200 OK):**
```json
{
  "success": true
}
```

**Usage Example:**
```bash
curl -X POST http://localhost:8080/comms_processor/api/imap/close \
  -H "Content-Type: application/json" \
  -d '{
    "host": "imap.gmail.com",
    "username": "user@gmail.com"
  }'
```

#### 4. Get Mailbox Count

**Endpoint:** `POST /api/imap/mailboxCount`

**Description:** Returns the number of emails in a specified folder. **Requires an existing cached connection** - use `/api/imap/open` first to establish the connection.

**Request Body:**
```json
{
  "mailboxHost": "imap.gmail.com",
  "mailboxUser": "user@gmail.com",
  "mailboxFolder": "INBOX"
}
```

**Notes:** 
- `mailboxHost` is required - the IMAP server host
- `mailboxUser` is required - the username/email
- `mailboxFolder` is optional and defaults to `INBOX` if not provided
- The connection must already be open/cached, otherwise an error is returned

**Success Response (200 OK):**
```json
{
  "success": true,
  "mailboxHost": "imap.gmail.com",
  "mailboxUser": "user@gmail.com",
  "mailboxFolder": "INBOX",
  "messageCount": 42
}
```

**Error Response (400 Bad Request) - Connection Not Open:**
```json
{
  "success": false,
  "error": "Connection not open. Use /api/imap/open to establish a connection first"
}
```

**Usage Example:**
```bash
# First, open a connection
curl -X POST http://localhost:8080/comms_processor/api/imap/open \
  -H "Content-Type: application/json" \
  -d '{
    "host": "imap.gmail.com",
    "username": "user@gmail.com",
    "password": "your-password"
  }'

# Then, get mailbox count
curl -X POST http://localhost:8080/comms_processor/api/imap/mailboxCount \
  -H "Content-Type: application/json" \
  -d '{
    "mailboxHost": "imap.gmail.com",
    "mailboxUser": "user@gmail.com",
    "mailboxFolder": "INBOX"
  }'
```

#### 5. Get Mailbox Statistics

**Endpoint:** `POST /api/imap/mailboxStats`

**Description:** Returns detailed statistics for emails in a specified folder, including message count, dates, sizes, and total size. **Requires an existing cached connection** - use `/api/imap/open` first to establish the connection.

**Request Body:**
```json
{
  "mailboxHost": "imap.gmail.com",
  "mailboxUser": "user@gmail.com",
  "mailboxFolder": "INBOX"
}
```

**Notes:** 
- `mailboxHost` is required - the IMAP server host
- `mailboxUser` is required - the username/email
- `mailboxFolder` is optional and defaults to `INBOX` if not provided
- The connection must already be open/cached, otherwise an error is returned

**Success Response (200 OK):**
```json
{
  "success": true,
  "mailboxHost": "imap.gmail.com",
  "mailboxUser": "user@gmail.com",
  "mailboxFolder": "INBOX",
  "messageCount": 42,
  "totalSize": 10485760,
  "biggestEmailSize": 5242880,
  "smallestEmailSize": 1024,
  "oldestDate": "Mon Jan 01 10:00:00 UTC 2026",
  "newestDate": "Mon Jan 27 12:00:00 UTC 2026"
}
```

**Usage Example:**
```bash
curl -X POST http://localhost:8080/comms_processor/api/imap/mailboxStats \
  -H "Content-Type: application/json" \
  -d '{
    "mailboxHost": "imap.gmail.com",
    "mailboxUser": "user@gmail.com",
    "mailboxFolder": "INBOX"
  }'
```

#### 6. IMAPS Connection Cache Status

**Endpoint:** `GET /api/imap/status`

**Description:** Returns the status of the IMAPS connection cache, including all open connections and their statistics for the last day.

**Response Example:**
```json
{
  "cacheStats": {
    "totalConnections": 5,
    "maxConnections": 50,
    "activeConnections": 5
  },
  "connections": [
    {
      "host": "imap.gmail.com",
      "username": "user@gmail.com",
      "connected": true,
      "createdTime": "2026-01-27T11:00:00Z",
      "lastUsedTime": "2026-01-27T11:05:00Z",
      "idleTimeSeconds": 120,
      "usageCountLastDay": 15
    }
  ],
  "timestamp": 1706012345678
}
```

**Usage Example:**
```bash
curl http://localhost:8080/comms_processor/api/imap/status
```

**Connection Cache Behavior:**
- **Maximum connections**: 50 (default, configurable via `email-reader.maxConnections` system property)
- **Maximum pool size**: 100 (default, configurable via `email-reader.maxPoolSize` system property)
- **Idle timeout**: 5 minutes - connections not used for 5 minutes are automatically closed
- **Cleanup schedule**: Runs every minute to close idle connections
- **Connection reuse**: Existing connections are reused when the same host/username is requested
- **Eviction policy**: When cache is full, the least recently used connection is closed
- **Statistics tracking**: Usage count and last access time for each connection tracked for the last 24 hours
- **Batch processing**: Configurable via `email-reader.minBatchSize` (default: 1) and `email-reader.maxBatchSize` (default: 100)

### SMTP Connection Endpoints

Base URL: `http://localhost:8080/comms_processor/api/smtp`

#### 1. Open SMTP Connection (Cached)

**Endpoint:** `POST /api/smtp/open`

**Description:** Opens and caches an SMTP connection for reuse. Uses TLS 1.2/1.3 encryption for secure connections to SMTPS servers.

**Request Body:**
```json
{
  "host": "smtp.gmail.com",
  "username": "user@gmail.com",
  "password": "app-password",
  "port": 465
}
```

**Notes:**
- `host` is required - the SMTP server hostname
- `username` is required - the username/email
- `password` is required - the password for authentication
- `port` is optional and defaults to 465 (SMTPS port)

**Success Response (200 OK):**
```json
{
  "success": true,
  "host": "smtp.gmail.com",
  "username": "user@gmail.com",
  "port": 465,
  "cached": true
}
```

**Usage Example:**
```bash
curl -X POST http://localhost:8080/comms_processor/api/smtp/open \
  -H "Content-Type: application/json" \
  -d '{
    "host": "smtp.gmail.com",
    "username": "user@gmail.com",
    "password": "app-password",
    "port": 465
  }'
```

#### 2. Close SMTP Connection

**Endpoint:** `POST /api/smtp/close`

**Description:** Closes a cached SMTP connection.

**Request Body:**
```json
{
  "host": "smtp.gmail.com",
  "username": "user@gmail.com"
}
```

**Success Response (200 OK):**
```json
{
  "success": true
}
```

**Usage Example:**
```bash
curl -X POST http://localhost:8080/comms_processor/api/smtp/close \
  -H "Content-Type: application/json" \
  -d '{
    "host": "smtp.gmail.com",
    "username": "user@gmail.com"
  }'
```

#### 3. Send Text Email

**Endpoint:** `POST /api/smtp/sendTextMessage`

**Description:** Sends a simple text email using a cached SMTP connection. **Requires an existing cached connection** - use `/api/smtp/open` first to establish the connection.

**Request Body:**
```json
{
  "smtpHost": "smtp.gmail.com",
  "smtpUser": "user@gmail.com",
  "fromAddress": "user@gmail.com",
  "toAddress": "recipient@example.com",
  "subject": "Test Email",
  "body": "This is a test email."
}
```

**Notes:**
- `smtpHost` is required - the SMTP server host
- `smtpUser` is required - the username/email used to open the connection
- `fromAddress` is required - the sender's email address
- `toAddress` is required - the recipient's email address
- `subject` is optional - email subject line
- `body` is optional - email body text
- **Connection must already be open/cached** - Returns error if connection not found

**Success Response (200 OK):**
```json
{
  "success": true,
  "smtpHost": "smtp.gmail.com",
  "smtpUser": "user@gmail.com",
  "from": "user@gmail.com",
  "to": "recipient@example.com",
  "sendTimeMs": 850
}
```

**Error Response (400 Bad Request) - Connection Not Open:**
```json
{
  "success": false,
  "error": "Connection not open. Use /api/smtp/open to establish a connection first"
}
```

**Usage Example:**
```bash
# First, open a connection
curl -X POST http://localhost:8080/comms_processor/api/smtp/open \
  -H "Content-Type: application/json" \
  -d '{
    "host": "smtp.gmail.com",
    "username": "user@gmail.com",
    "password": "app-password"
  }'

# Then, send a text email
curl -X POST http://localhost:8080/comms_processor/api/smtp/sendTextMessage \
  -H "Content-Type: application/json" \
  -d '{
    "smtpHost": "smtp.gmail.com",
    "smtpUser": "user@gmail.com",
    "fromAddress": "user@gmail.com",
    "toAddress": "recipient@example.com",
    "subject": "Hello from API",
    "body": "This email was sent via the SMTP API."
  }'
```

#### 4. Send Email (.eml format)

**Endpoint:** `POST /api/smtp/send`

**Description:** Sends one or more emails in .eml format using a cached SMTP connection. Accepts base64 encoded (optionally gzipped) .eml format data. **Requires an existing cached connection** - use `/api/smtp/open` first to establish the connection.

**Request Body (Single Email):**
```json
{
  "smtpHost": "smtp.gmail.com",
  "smtpUser": "user@gmail.com",
  "data": "base64-encoded-eml-data"
}
```

**Request Body (Multiple Emails):**
```json
{
  "smtpHost": "smtp.gmail.com",
  "smtpUser": "user@gmail.com",
  "data": [
    "base64-encoded-eml-data-1",
    "base64-encoded-eml-data-2",
    "base64-encoded-eml-data-3"
  ]
}
```

**Notes:**
- `smtpHost` is required - the SMTP server host
- `smtpUser` is required - the username/email used to open the connection
- `data` is required - can be:
  - A single string containing base64 encoded .eml format email
  - An array of strings, each containing base64 encoded .eml format email
- Data can be optionally gzipped before base64 encoding (automatically detected)
- **Connection must already be open/cached** - Returns error if connection not found
- **Automatic Reconnection**: When sending multiple emails (array), if the `maxBatchSize` limit is reached during processing, the connection automatically disconnects and reconnects. The client does not need to manage reconnections manually.

**Success Response (200 OK) - Single Email:**
```json
{
  "success": true,
  "smtpHost": "smtp.gmail.com",
  "smtpUser": "user@gmail.com",
  "sendTimeMs": 920,
  "dataSize": 4096
}
```

**Success Response (200 OK) - Multiple Emails (All Successful):**
```json
{
  "success": true,
  "smtpHost": "smtp.gmail.com",
  "smtpUser": "user@gmail.com",
  "sendTimeMs": 2750,
  "totalEmails": 3,
  "successCount": 3,
  "failureCount": 0,
  "totalDataSize": 12288,
  "emailsSentSinceConnect": 3,
  "maxBatchSize": 100,
  "results": [
    {
      "index": 0,
      "success": true,
      "dataSize": 4096
    },
    {
      "index": 1,
      "success": true,
      "dataSize": 4096
    },
    {
      "index": 2,
      "success": true,
      "dataSize": 4096
    }
  ]
}
```

**Success Response with Auto-Reconnection:**
```json
{
  "success": true,
  "smtpHost": "smtp.gmail.com",
  "smtpUser": "user@gmail.com",
  "sendTimeMs": 5200,
  "totalEmails": 150,
  "successCount": 150,
  "failureCount": 0,
  "totalDataSize": 614400,
  "emailsSentSinceConnect": 50,
  "maxBatchSize": 100,
  "reconnectCount": 1,
  "message": "Auto-reconnected 1 time(s) during batch processing",
  "results": [...]
}
```

**Partial Success Response (200 OK) - Some Emails Failed:**
```json
{
  "success": false,
  "smtpHost": "smtp.gmail.com",
  "smtpUser": "user@gmail.com",
  "sendTimeMs": 1850,
  "totalEmails": 3,
  "successCount": 2,
  "failureCount": 1,
  "totalDataSize": 8192,
  "results": [
    {
      "index": 0,
      "success": true,
      "dataSize": 4096
    },
    {
      "index": 1,
      "success": false,
      "error": "Failed to parse .eml data: Invalid format"
    },
    {
      "index": 2,
      "success": true,
      "dataSize": 4096
    }
  ]
}
```

**Error Response (400 Bad Request) - Connection Not Open:**
```json
{
  "success": false,
  "error": "Connection not open. Use /api/smtp/open to establish a connection first"
}
```

**Usage Example (Single Email):**
```bash
# First, open a connection
curl -X POST http://localhost:8080/comms_processor/api/smtp/open \
  -H "Content-Type: application/json" \
  -d '{
    "host": "smtp.gmail.com",
    "username": "user@gmail.com",
    "password": "app-password"
  }'

# Then, send an email from .eml file
# First, encode the .eml file to base64
EML_DATA=$(base64 -w 0 email.eml)

curl -X POST http://localhost:8080/comms_processor/api/smtp/send \
  -H "Content-Type: application/json" \
  -d "{
    \"smtpHost\": \"smtp.gmail.com\",
    \"smtpUser\": \"user@gmail.com\",
    \"data\": \"$EML_DATA\"
  }"
```

**Usage Example (Multiple Emails):**
```bash
# Encode multiple .eml files
EML_DATA_1=$(base64 -w 0 email1.eml)
EML_DATA_2=$(base64 -w 0 email2.eml)
EML_DATA_3=$(base64 -w 0 email3.eml)

# Send all emails in one request
curl -X POST http://localhost:8080/comms_processor/api/smtp/send \
  -H "Content-Type: application/json" \
  -d "{
    \"smtpHost\": \"smtp.gmail.com\",
    \"smtpUser\": \"user@gmail.com\",
    \"data\": [\"$EML_DATA_1\", \"$EML_DATA_2\", \"$EML_DATA_3\"]
  }"
```

#### 5. SMTP Connection Cache Status

**Endpoint:** `GET /api/smtp/status`

**Description:** Returns the status of the SMTP connection cache, including all open connections and their statistics for the last day.

**Response Example:**
```json
{
  "cacheStats": {
    "totalConnections": 3,
    "maxConnections": 50,
    "activeConnections": 3
  },
  "connections": [
    {
      "host": "smtp.gmail.com",
      "username": "user@gmail.com",
      "connected": true,
      "createdTime": "2026-01-27T12:00:00Z",
      "lastUsedTime": "2026-01-27T12:30:00Z",
      "idleTimeSeconds": 60,
      "usageCountLastDay": 8
    }
  ],
  "timestamp": 1706015345678
}
```

**Usage Example:**
```bash
curl http://localhost:8080/comms_processor/api/smtp/status
```

**Connection Cache Behavior:**
- **Maximum connections**: 50 (default, configurable via `email-sender.maxConnections` system property)
- **Idle timeout**: 5 minutes - connections not used for 5 minutes are automatically closed
- **Cleanup schedule**: Runs every minute to close idle connections
- **Connection reuse**: Existing connections are reused when the same host/username is requested
- **Eviction policy**: When cache is full, the least recently used connection is closed
- **Statistics tracking**: Usage count and last access time for each connection tracked for the last 24 hours

## System Properties

The following system properties can be configured to control email sending behavior:

### email-sender.maxBatchSize
- **Description**: Maximum number of emails that can be sent before requiring a reconnection
- **Default**: 100
- **Purpose**: Some SMTP servers block connections after sending too many emails. This setting helps prevent that by tracking emails sent since the last connection.
- **Usage**: When the limit is reached, a warning is included in the response suggesting reconnection.
- **Example**: `-Demail-sender.maxBatchSize=50`

### email-sender.minBatchSize
- **Description**: Minimum number of emails required for batch sending
- **Default**: 1
- **Purpose**: Enforces a minimum batch size when sending multiple emails
- **Example**: `-Demail-sender.minBatchSize=10`

### email-sender.maxConnections
- **Description**: Maximum number of concurrent SMTP connections to maintain in the cache (per host/user combination)
- **Default**: 50
- **Purpose**: Limits memory usage and prevents resource exhaustion
- **Example**: `-Demail-sender.maxConnections=100`

### email-sender.maxPoolSize
- **Description**: Maximum size of the connection pool
- **Default**: 100
- **Purpose**: Controls the overall pool size for connection management
- **Example**: `-Demail-sender.maxPoolSize=200`

### email-reader.maxConnections
- **Description**: Maximum number of concurrent IMAP connections to maintain in the cache (per host/user combination)
- **Default**: 50
- **Purpose**: Limits memory usage and prevents resource exhaustion for IMAP connections
- **Example**: `-Demail-reader.maxConnections=100`

### email-reader.maxPoolSize
- **Description**: Maximum size of the IMAP connection pool
- **Default**: 100
- **Purpose**: Controls the overall pool size for IMAP connection management
- **Example**: `-Demail-reader.maxPoolSize=200`

### email-reader.minBatchSize
- **Description**: Minimum number of messages required for batch processing
- **Default**: 1
- **Purpose**: Enforces a minimum batch size when processing multiple emails
- **Example**: `-Demail-reader.minBatchSize=10`

### email-reader.maxBatchSize
- **Description**: Maximum number of messages to process in a single batch
- **Default**: 100
- **Purpose**: Controls batch processing size for IMAP operations like mailboxStats
- **Example**: `-Demail-reader.maxBatchSize=200`

### Setting System Properties

System properties can be set when starting WildFly:

```bash
./standalone.sh -Demail-sender.maxBatchSize=50 -Demail-sender.maxConnections=100 -Demail-reader.maxConnections=75
```

Or added to `standalone.xml`:

```xml
<system-properties>
    <property name="email-sender.maxBatchSize" value="50"/>
    <property name="email-sender.minBatchSize" value="1"/>
    <property name="email-sender.maxConnections" value="100"/>
    <property name="email-sender.maxPoolSize" value="200"/>
    <property name="email-reader.maxConnections" value="75"/>
    <property name="email-reader.maxPoolSize" value="150"/>
    <property name="email-reader.minBatchSize" value="1"/>
    <property name="email-reader.maxBatchSize" value="200"/>
</system-properties>
```

### Email Sending Response Fields

When sending emails, the response now includes:
- `emailsSentSinceConnect`: Number of emails sent since the connection was established
- `maxBatchSize`: The configured maximum batch size
- `warning`: (if applicable) Warning message when max batch size is reached

**Example Response:**
```json
{
  "success": true,
  "smtpHost": "smtp.gmail.com",
  "smtpUser": "user@gmail.com",
  "sendTimeMs": 920,
  "dataSize": 4096,
  "emailsSentSinceConnect": 45,
  "maxBatchSize": 100
}
```

When the limit is reached:
```json
{
  "success": true,
  "emailsSentSinceConnect": 100,
  "maxBatchSize": 100,
  "warning": "Max batch size (100) reached. Consider reconnecting."
}
```

To reconnect after reaching the limit, call `/api/smtp/close` followed by `/api/smtp/open` with the same credentials.

## Testing

Run tests with:
```bash
mvn test
```

## Project Information

- **Group ID:** com.example
- **Artifact ID:** comms_processor
- **Version:** 1.0.0-SNAPSHOT
- **Packaging:** WAR
- **Java Version:** 11
