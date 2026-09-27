package com.pedidai.api.exceptions;

public class ResourceNotFoundException extends LocalizedException {
    public ResourceNotFoundException(String key, Object... args) {
        super(key, args);
    }
}
