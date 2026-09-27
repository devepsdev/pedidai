package com.pedidai.api.exceptions;

public class BadRequestException extends LocalizedException {
    public BadRequestException(String key, Object... args) {
        super(key, args);
    }
}
