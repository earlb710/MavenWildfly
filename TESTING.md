# Testing the REST Endpoints

This document provides instructions for manually testing the REST status endpoints after deploying to WildFly.

## Prerequisites

- WildFly server running (default port: 8080)
- Application deployed (`comms_processor.war`)

## Test Endpoints

### 1. Test Ping Endpoint

Using curl:
```bash
curl http://localhost:8080/comms_processor/api/status/ping
```

Expected response:
```json
{
  "status": "ok",
  "message": "Service is running",
  "timestamp": 1706012345678
}
```

Using a browser:
- Navigate to: http://localhost:8080/comms_processor/api/status/ping

### 2. Test Server Status Endpoint

Using curl:
```bash
curl http://localhost:8080/comms_processor/api/status/serverStatus
```

Expected response (example):
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

Using a browser:
- Navigate to: http://localhost:8080/comms_processor/api/status/serverStatus

### 3. Test Datasources Endpoint

Using curl:
```bash
curl http://localhost:8080/comms_processor/api/status/datasources
```

Expected response (example):
```json
{
  "status": "ok",
  "timestamp": 1706012345678,
  "datasourcesCount": 2,
  "datasources": [
    {
      "name": "PostgresDS",
      "jndiName": "java:jboss/datasources/PostgresDS",
      "enabled": true,
      "driverName": "postgresql",
      "connectionUrl": "jdbc:postgresql://localhost:5432/comms_db",
      "objectName": "jboss.as:subsystem=datasources,data-source=PostgresDS"
    },
    {
      "name": "OracleDS",
      "jndiName": "java:jboss/datasources/OracleDS",
      "enabled": true,
      "driverName": "oracle",
      "connectionUrl": "jdbc:oracle:thin:@localhost:1521:ORCL",
      "objectName": "jboss.as:subsystem=datasources,data-source=OracleDS"
    }
  ]
}
```

Using a browser:
- Navigate to: http://localhost:8080/comms_processor/api/status/datasources

## Pretty Print JSON (Optional)

For better readability in the terminal:

```bash
curl http://localhost:8080/comms_processor/api/status/ping | jq .
curl http://localhost:8080/comms_processor/api/status/serverStatus | jq .
curl http://localhost:8080/comms_processor/api/status/datasources | jq .
```

## Testing with Different Tools

### Using HTTPie (if available)
```bash
http GET http://localhost:8080/comms_processor/api/status/ping
http GET http://localhost:8080/comms_processor/api/status/serverStatus
http GET http://localhost:8080/comms_processor/api/status/datasources
```

### Using Postman or Insomnia
- Create a GET request to each endpoint
- No authentication required
- Response format: JSON

## Troubleshooting

If you get a 404 error:
1. Verify WildFly is running: `ps aux | grep wildfly`
2. Check if the application is deployed: `ls $WILDFLY_HOME/standalone/deployments/`
3. Check WildFly logs: `tail -f $WILDFLY_HOME/standalone/log/server.log`

If you get a 500 error:
1. Check WildFly logs for stack traces
2. Verify CDI is enabled (beans.xml should be present)
3. Ensure all services are started properly
