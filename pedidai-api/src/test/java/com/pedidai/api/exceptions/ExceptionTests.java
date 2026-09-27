package com.pedidai.api.exceptions;

import com.pedidai.api.config.I18nConfig;
import com.pedidai.api.config.Messages;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler: errors traduïts i sense detalls interns")
class ExceptionTests {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(new Messages(new I18nConfig().messageSource()));

    @AfterEach
    void reset() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    @DisplayName("tradueix la clau a l'idioma de la petició")
    void translatesToRequestLanguage() {
        LocaleContextHolder.setLocale(I18nConfig.CATALAN);
        var ca = handler.handleResourceNotFound(new ResourceNotFoundException("error.order.notFound"));
        LocaleContextHolder.setLocale(I18nConfig.SPANISH);
        var es = handler.handleResourceNotFound(new ResourceNotFoundException("error.order.notFound"));

        assertThat(ca.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ca.getBody().getMessage()).isEqualTo("No s’ha trobat la comanda.");
        assertThat(es.getBody().getMessage()).isEqualTo("Pedido no encontrado.");
    }

    @Test
    @DisplayName("els paràmetres s'insereixen al missatge")
    void insertsArguments() {
        LocaleContextHolder.setLocale(I18nConfig.SPANISH);
        var r = handler.handleDuplicateResource(new DuplicateResourceException("error.supplier.nameExists", "Fruites"));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(r.getBody().getMessage()).isEqualTo("Ya tienes un proveedor llamado Fruites.");
    }

    @Test
    @DisplayName("cada tipus d'error té el seu codi HTTP")
    void statusCodes() {
        assertThat(handler.handleBadRequest(new BadRequestException("error.validation")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(handler.handleForbidden(new ForbiddenException("error.forbidden")).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(handler.handleTooManyRequests(new TooManyRequestsException("error.tooManyRequests")).getStatusCode())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("un error inesperat no exposa el missatge intern")
    void genericErrorHidesDetails() {
        LocaleContextHolder.setLocale(I18nConfig.SPANISH);
        var r = handler.handleGenericException(new RuntimeException("SQL: SELECT * FROM users; password=secret"));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(r.getBody().getMessage()).doesNotContain("SQL").doesNotContain("secret");
    }

    @Test
    @DisplayName("un text que no és una clau es retorna tal qual")
    void literalFallsBack() {
        var r = handler.handleBadRequest(new BadRequestException("Text literal"));
        assertThat(r.getBody().getMessage()).isEqualTo("Text literal");
    }
}
