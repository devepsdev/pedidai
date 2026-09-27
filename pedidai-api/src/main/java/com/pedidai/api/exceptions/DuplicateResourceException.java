package com.pedidai.api.exceptions;

public class DuplicateResourceException extends LocalizedException {
    public DuplicateResourceException(String key, Object... args) {
        super(key, args);
    }
}
