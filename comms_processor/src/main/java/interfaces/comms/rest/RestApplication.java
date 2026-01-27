package interfaces.comms.rest;

import interfaces.comms.rest.StatusResource;
import interfaces.comms.rest.StatusHtmlResource;
import interfaces.comms.rest.ImapConnectionResource;
import interfaces.comms.rest.SmtpConnectionResource;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * JAX-RS Application configuration class.
 * Configures the base path for REST endpoints.
 */
@ApplicationPath("/api")
public class RestApplication extends Application {
    
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> resources = new HashSet<>();
        resources.add(StatusResource.class);
        resources.add(StatusHtmlResource.class);
        resources.add(ImapConnectionResource.class);
        resources.add(SmtpConnectionResource.class);
        return resources;
    }
}
