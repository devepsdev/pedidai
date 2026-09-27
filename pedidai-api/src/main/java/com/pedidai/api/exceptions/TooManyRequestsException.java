package com.pedidai.api.exceptions;

/** S'ha superat un límit d'ús (HTTP 429). */
public class TooManyRequestsException extends LocalizedException {
    public TooManyRequestsException(String key, Object... args) {
        super(key, args);
    }
}
