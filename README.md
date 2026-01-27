# MavenWildfly

## Communications Processor Project

A Java EE web application configured for deployment to Wildfly application server with PostgreSQL and Oracle SQL database support.

## Project Structure

```
comms_processor/
├── pom.xml                                 # Maven configuration
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/comms/
│   │   │       ├── rest/
│   │   │       │   ├── RestApplication.java    # JAX-RS Application configuration
│   │   │       │   └── StatusResource.java     # REST endpoints for status
│   │   │       └── service/
│   │   │           └── WildFlyManagementService.java # WildFly management service
│   │   ├── resources/
│   │   │   └── database.properties        # Database configuration
│   │   └── webapp/
│   │       ├── WEB-INF/
│   │       │   ├── web.xml               # Web application descriptor
│   │       │   └── beans.xml             # CDI configuration
│   │       └── index.html                # Welcome page
│   └── test/
│       ├── java/                          # Test source files
│       └── resources/                     # Test resources
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

**Description:** Returns the number of emails in a specified mailbox. Uses cached connections.

**Request Body:**
```json
{
  "host": "imap.gmail.com",
  "username": "user@gmail.com",
  "password": "your-password",
  "mailbox": "INBOX"
}
```

**Note:** The `mailbox` field is optional and defaults to `INBOX` if not provided.

**Success Response (200 OK):**
```json
{
  "success": true,
  "mailbox": "INBOX",
  "messageCount": 42,
  "host": "imap.gmail.com",
  "username": "user@gmail.com"
}
```

**Usage Example:**
```bash
curl -X POST http://localhost:8080/comms_processor/api/imap/mailboxCount \
  -H "Content-Type: application/json" \
  -d '{
    "host": "imap.gmail.com",
    "username": "user@gmail.com",
    "password": "your-password",
    "mailbox": "INBOX"
  }'
```

#### 5. IMAPS Connection Cache Status

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
- **Maximum connections**: 50 (default, configurable)
- **Idle timeout**: 5 minutes - connections not used for 5 minutes are automatically closed
- **Cleanup schedule**: Runs every minute to close idle connections
- **Connection reuse**: Existing connections are reused when the same host/username is requested
- **Eviction policy**: When cache is full, the least recently used connection is closed
- **Statistics tracking**: Usage count and last access time for each connection tracked for the last 24 hours

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