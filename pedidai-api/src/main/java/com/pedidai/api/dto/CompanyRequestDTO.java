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
public class CompanyRequestDTO {

    @NotBlank(message = "{validation.name.required}")
    @Size(max = 255, message = "{validation.name.tooLong}")
    private String name;
    @Size(max = 50, message = "{validation.taxId.tooLong}")
    private String taxId;

    @Email(message = "{validation.email.invalid}")
    private String email;

    @Size(max = 50, message = "{validation.phone.tooLong}")
    private String phone;

    private String address;

    @Size(max = 100, message = "{validation.city.tooLong}")
    private String city;

    @Size(max = 20, message = "{validation.postalCode.tooLong}")
    private String postalCode;
}