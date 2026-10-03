package com.example.user_api.exception;

public class ResourceNotFoundException extends RuntimeException {
    
    // Constructor that accepts a custom error message
    public ResourceNotFoundException(String message) {
        super(message);
    }
}