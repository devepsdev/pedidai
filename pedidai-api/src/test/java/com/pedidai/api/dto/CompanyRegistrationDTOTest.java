package com.pedidai.api.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CompanyRegistrationDTO: registre mínim")
class CompanyRegistrationDTOTest {

    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private static CompanyRegistrationDTO.CompanyRegistrationDTOBuilder valid() {
        return CompanyRegistrationDTO.builder().companyName("Bar Prova").adminFirstName("Laura")
                .adminEmail("laura@bar.test").adminPassword("Bar12345").acceptTerms(true);
    }

    private static Set<String> invalidFields(CompanyRegistrationDTO dto) {
        return validator.validate(dto).stream().map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
    }

    @Test
    @DisplayName("només calen nom del negoci, nom, email, contrasenya i acceptar els termes (CIF opcional)")
    void minimalRegistrationIsValid() {
        assertThat(validator.validate(valid().build())).isEmpty();
    }

    @Test
    @DisplayName("camps obligatoris buits")
    void mandatoryFields() {
        assertThat(invalidFields(new CompanyRegistrationDTO()))
                .containsExactlyInAnyOrder("companyName", "adminFirstName", "adminEmail", "adminPassword", "acceptTerms");
    }

    @Test
    @DisplayName("contrasenya: mínim 8 caràcters amb lletra i número (igual que el formulari)")
    void passwordPolicy() {
        assertThat(invalidFields(valid().adminPassword("abc1").build())).containsExactly("adminPassword");
        assertThat(invalidFields(valid().adminPassword("abcdefgh").build())).containsExactly("adminPassword");
        assertThat(invalidFields(valid().adminPassword("12345678").build())).containsExactly("adminPassword");
        assertThat(invalidFields(valid().adminPassword("restaurante1").build())).isEmpty();
    }

    @Test
    @DisplayName("cal acceptar els termes")
    void termsRequired() {
        assertThat(invalidFields(valid().acceptTerms(false).build())).containsExactly("acceptTerms");
    }

    @Test
    @DisplayName("email mal format")
    void invalidEmail() {
        Set<ConstraintViolation<CompanyRegistrationDTO>> v = validator.validate(valid().adminEmail("no-es-un-email").build());
        assertThat(v).extracting(c -> c.getPropertyPath().toString()).containsExactly("adminEmail");
    }
}
