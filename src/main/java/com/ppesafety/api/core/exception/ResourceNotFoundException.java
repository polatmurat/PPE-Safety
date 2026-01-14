package com.ppesafety.api.core.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + " not found with id: " + id, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String resourceName, String identifier) {
        super(resourceName + " not found: " + identifier, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }
}
