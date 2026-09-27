package com.pedidai.api.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Alta d'una empresa amb el seu administrador. Només es demana el mínim per començar la prova:
 * nom del negoci, nom, email i contrasenya. La resta de dades (CIF, adreça...) es completen després.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyRegistrationDTO {

    @NotBlank(message = "{validation.companyName.required}")
    @Size(max = 255, message = "{validation.name.tooLong}")
    private String companyName;

    @NotBlank(message = "{validation.name.required}")
    @Size(max = 100, message = "{validation.name.tooLong}")
    private String adminFirstName;

    @Size(max = 100, message = "{validation.lastName.tooLong}")
    private String adminLastName;

    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.invalid}")
    @Size(max = 255, message = "{validation.email.tooLong}")
    private String adminEmail;

    @NotBlank(message = "{validation.password.required}")
    @Pattern(regexp = com.pedidai.api.security.PasswordPolicy.REGEX, message = "{validation.password.weak}")
    private String adminPassword;

    @NotNull(message = "{validation.terms.required}")
    @AssertTrue(message = "{validation.terms.required}")
    private Boolean acceptTerms;

    // ───── Opcionals ─────

    @Size(max = 50, message = "{validation.taxId.tooLong}")
    private String taxId;

    @Size(max = 50, message = "{validation.phone.tooLong}")
    private String companyPhone;

    private String companyAddress;

    @Size(max = 100, message = "{validation.city.tooLong}")
    private String companyCity;

    @Size(max = 20, message = "{validation.postalCode.tooLong}")
    private String companyPostalCode;
}
