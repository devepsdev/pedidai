package com.pedidai.api.dto;

import com.pedidai.api.entities.Company;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyRequestDTO {

    @NotBlank(message = "El nom és obligatori")
    @Size(max = 255, message = "El nom no pot excedir de 255 caràcters")
    private String name;

    @NotBlank(message = "El NIF/CIF és obligatori")
    @Size(max = 50, message = "El NIF/CIF no pot excedir de 50 caràcters")
    private String taxId;

    @Email(message = "L'email ha de ser vàlid")
    private String email;

    @Size(max = 50, message = "El telèfon no pot excedir de 50 caràcters")
    private String phone;

    private String address;

    @Size(max = 100, message = "La ciutat no pot excedir de 100 caràcters")
    private String city;

    @Size(max = 20, message = "El codi postal no pot excedir de 20 caràcters")
    private String postalCode;

    private Company.CompanyStatus status;
}