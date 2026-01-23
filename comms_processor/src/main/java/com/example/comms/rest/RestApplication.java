package com.example.comms.rest;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * JAX-RS Application configuration class.
 * Configures the base path for REST endpoints.
 */
@ApplicationPath("/api")
public class RestApplication extends Application {
    // JAX-RS will automatically discover and register resource classes
}
