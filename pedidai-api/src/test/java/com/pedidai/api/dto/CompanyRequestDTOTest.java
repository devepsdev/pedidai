package com.pedidai.api.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CompanyRequestDTO Tests")
class CompanyRequestDTOTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("DTO vàlid hauria de passar totes les validacions")
    void validDTO_ShouldPassAllValidations() {
        // Given
        CompanyRequestDTO dto = CompanyRequestDTO.builder()
                .name("Test Company SL")
                .taxId("B12345678")
                .email("test@company.com")
                .phone("123456789")
                .address("Carrer Test 123")
                .city("Barcelona")
                .postalCode("08001")
                .build();

        // When
        Set<ConstraintViolation<CompanyRequestDTO>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Nom buit hauria de fallar la validació")
    void blankName_ShouldFailValidation() {
        // Given
        CompanyRequestDTO dto = CompanyRequestDTO.builder()
                .name("")
                .taxId("B12345678")
                .build();

        // When
        Set<ConstraintViolation<CompanyRequestDTO>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("{validation.name.required}");
    }

    @Test
    @DisplayName("El CIF és opcional (es demana en contractar)")
    void blankTaxId_IsAllowed() {
        // Given
        CompanyRequestDTO dto = CompanyRequestDTO.builder()
                .name("Test Company")
                .taxId("")
                .build();

        // When
        Set<ConstraintViolation<CompanyRequestDTO>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Email invàlid hauria de fallar la validació")
    void invalidEmail_ShouldFailValidation() {
        // Given
        CompanyRequestDTO dto = CompanyRequestDTO.builder()
                .name("Test Company")
                .taxId("B12345678")
                .email("invalid-email")
                .build();

        // When
        Set<ConstraintViolation<CompanyRequestDTO>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("{validation.email.invalid}");
    }

    @Test
    @DisplayName("Nom massa llarg hauria de fallar la validació")
    void tooLongName_ShouldFailValidation() {
        // Given
        String longName = "A".repeat(256); // 256 caràcters
        CompanyRequestDTO dto = CompanyRequestDTO.builder()
                .name(longName)
                .taxId("B12345678")
                .build();

        // When
        Set<ConstraintViolation<CompanyRequestDTO>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("{validation.name.tooLong}");
    }

    @Test
    @DisplayName("TaxId massa llarg hauria de fallar la validació")
    void tooLongTaxId_ShouldFailValidation() {
        // Given
        String longTaxId = "B".repeat(51); // 51 caràcters
        CompanyRequestDTO dto = CompanyRequestDTO.builder()
                .name("Test Company")
                .taxId(longTaxId)
                .build();

        // When
        Set<ConstraintViolation<CompanyRequestDTO>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("{validation.taxId.tooLong}");
    }

    @Test
    @DisplayName("Camps opcionals buits haurien de ser vàlids")
    void optionalEmptyFields_ShouldBeValid() {
        // Given
        CompanyRequestDTO dto = CompanyRequestDTO.builder()
                .name("Test Company")
                .taxId("B12345678")
                .email(null)
                .phone(null)
                .address(null)
                .city(null)
                .postalCode(null)
                .build();

        // When
        Set<ConstraintViolation<CompanyRequestDTO>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Builder pattern hauria de funcionar correctament")
    void builderPattern_ShouldWorkCorrectly() {
        // Given & When
        CompanyRequestDTO dto = CompanyRequestDTO.builder()
                .name("Test Company")
                .taxId("B12345678")
                .email("test@company.com")
                .build();

        // Then
        assertThat(dto.getName()).isEqualTo("Test Company");
        assertThat(dto.getTaxId()).isEqualTo("B12345678");
        assertThat(dto.getEmail()).isEqualTo("test@company.com");
    }

    @Test
    @DisplayName("Múltiples errors haurien de ser reportats")
    void multipleErrors_ShouldBeReported() {
        // Given
        CompanyRequestDTO dto = CompanyRequestDTO.builder()
                .name("") // Error: buit
                .taxId("") // Error: buit
                .email("invalid") // Error: format invàlid
                .build();

        // When
        Set<ConstraintViolation<CompanyRequestDTO>> violations = validator.validate(dto);

        // Then
        assertThat(violations).hasSize(2);
    }

}