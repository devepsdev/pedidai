package com.pedidai.api.exceptions;

import lombok.Getter;

/**
 * Excepció amb missatge traduïble: {@code getMessage()} és la clau del fitxer
 * i18n/messages*.properties i {@link #getArgs()} els seus paràmetres.
 * El GlobalExceptionHandler la tradueix a l'idioma de la petició.
 */
@Getter
public abstract class LocalizedException extends RuntimeException {

    private final transient Object[] args;

    protected LocalizedException(String key, Object... args) {
        super(key);
        this.args = args;
    }
}
