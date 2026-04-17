package com.pedidai.api.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyRegistrationDTO {

    // ========== Dades de l'empresa ==========

    @NotBlank(message = "El nom de l'empresa és obligatori")
    @Size(max = 255, message = "El nom no pot excedir 255 caràcters")
    private String companyName;

    @NotBlank(message = "El CIF/NIF és obligatori")
    @Size(max = 20, message = "El CIF no pot excedir 20 caràcters")
    private String taxId;

    @Email(message = "L'email de l'empresa ha de ser vàlid")
    private String companyEmail;

    @Size(max = 50, message = "El telèfon no pot excedir 50 caràcters")
    private String companyPhone;

    private String companyAddress;

    private String companyCity;

    private String companyPostalCode;

    // ========== Dades de l'administrador ==========

    @NotBlank(message = "L'email de l'administrador és obligatori")
    @Email(message = "L'email ha de ser vàlid")
    private String adminEmail;

    @NotBlank(message = "La contrasenya és obligatòria")
    @Size(min = 8, message = "La contrasenya ha de tenir un mínim de 8 caràcters")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[^a-zA-Z0-9\\s]).*$",
            message = "La contrasenya ha de contenir un mínim d'una majúscula, una minúscula, un número i un caràcter especial")
    private String adminPassword;

    @NotBlank(message = "El nom de l'administrador és obligatori")
    @Size(max = 100, message = "El nom no pot excedir 100 caràcters")
    private String adminFirstName;

    @NotBlank(message = "El cognom és obligatori")
    @Size(max = 100, message = "El cognom no pot excedir 100 caràcters")
    private String adminLastName;

    @Size(max = 50, message = "El telèfon no pot excedir 50 caràcters")
    private String adminPhone;
}