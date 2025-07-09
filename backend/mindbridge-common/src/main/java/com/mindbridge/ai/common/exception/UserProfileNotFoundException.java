package com.mindbridge.ai.common.exception;

public class UserProfileNotFoundException extends RuntimeException {
    public UserProfileNotFoundException(String userId) {
        super("User not found with ID: " + userId);
    }
}
