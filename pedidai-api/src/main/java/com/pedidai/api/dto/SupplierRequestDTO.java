package com.pedidai.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierRequestDTO {

    @NotBlank(message = "{validation.supplierName.required}")
    @Size(max = 255, message = "{validation.name.tooLong}")
    private String name;

    @Size(max = 255, message = "{validation.contactName.tooLong}")
    private String contactName;

    @Email(message = "{validation.email.invalid}")
    @Size(max = 255, message = "{validation.email.tooLong}")
    private String email;

    @Size(max = 50, message = "{validation.phone.tooLong}")
    private String phone;

    private String address;

    private String notes;

    @Builder.Default
    private Boolean isActive = true;
}