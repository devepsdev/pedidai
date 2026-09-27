package com.pedidai.api.exceptions;

/** Acció no permesa per a l'usuari autenticat (HTTP 403). */
public class ForbiddenException extends LocalizedException {
    public ForbiddenException(String key, Object... args) {
        super(key, args);
    }
}
