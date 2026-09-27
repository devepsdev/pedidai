package com.pedidai.api.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Accés als textos traduïts (src/main/resources/i18n/messages*.properties).
 * Si una clau no existeix, es retorna la clau tal qual, de manera que un text literal
 * també funciona com a missatge.
 */
@Component
@RequiredArgsConstructor
public class Messages {

    private final MessageSource messageSource;

    /** Text en l'idioma de la petició actual. */
    public String get(String key, Object... args) {
        return get(LocaleContextHolder.getLocale(), key, args);
    }

    /** Text en un idioma concret (correus, PDF, processos sense petició HTTP). */
    public String get(Locale locale, String key, Object... args) {
        return messageSource.getMessage(key, args, key, locale);
    }
}
