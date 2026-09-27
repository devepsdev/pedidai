package com.pedidai.api.security;

/**
 * Regla única de contrasenyes (la mateixa que valida el frontend):
 * mínim 8 caràcters amb almenys una lletra i un número.
 */
public final class PasswordPolicy {

    public static final String REGEX = "^(?=.*\\p{L})(?=.*\\d).{8,100}$";

    private PasswordPolicy() {
    }
}
