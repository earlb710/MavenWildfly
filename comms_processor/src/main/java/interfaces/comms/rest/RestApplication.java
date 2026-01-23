package interfaces.comms.rest;

import interfaces.comms.rest.StatusResource;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
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
        return resources;
    }
}
