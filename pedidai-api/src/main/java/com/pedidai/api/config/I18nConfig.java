package com.pedidai.api.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Configuració d'idiomes de l'API: català i castellà.
 * L'idioma de cada petició es pren de la capçalera Accept-Language que envia el frontend;
 * si no n'hi ha o no és compatible, s'usa el castellà.
 */
@Configuration
public class I18nConfig {

    public static final Locale SPANISH = Locale.forLanguageTag("es");
    public static final Locale CATALAN = Locale.forLanguageTag("ca");

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasename("classpath:i18n/messages");
        source.setDefaultEncoding(StandardCharsets.UTF_8.name());
        source.setFallbackToSystemLocale(false);
        return source;
    }

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(List.of(SPANISH, CATALAN));
        resolver.setDefaultLocale(SPANISH);
        return resolver;
    }

    /** Els missatges de les anotacions de validació ({@code {validation.xxx}}) també es tradueixen. */
    @Bean
    public LocalValidatorFactoryBean getValidator(MessageSource messageSource) {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource);
        return bean;
    }

    /** Normalitza qualsevol Locale a "ca" o "es", el format que es desa a la base de dades. */
    public static String languageCode(Locale locale) {
        return locale != null && "ca".equals(locale.getLanguage()) ? "ca" : "es";
    }

    public static Locale localeOf(String languageCode) {
        return "ca".equals(languageCode) ? CATALAN : SPANISH;
    }
}
