package org.example.application.shared;

/** A requested resource (issue, project, sprint, …) does not exist. Maps to HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
