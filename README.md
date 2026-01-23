# MavenWildfly

## Communications Processor Project

A Java EE web application configured for deployment to Wildfly application server with PostgreSQL and Oracle SQL database support.

## Project Structure

```
comms_processor/
├── pom.xml                                 # Maven configuration
├── src/
│   ├── main/
│   │   ├── java/                          # Java source files
│   │   ├── resources/
│   │   │   └── database.properties        # Database configuration
│   │   └── webapp/
│   │       ├── WEB-INF/
│   │       │   └── web.xml               # Web application descriptor
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
- **PostgreSQL JDBC Driver 42.7.1** - PostgreSQL database connectivity
- **Oracle JDBC Driver 21.9.0.0** - Oracle database connectivity
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