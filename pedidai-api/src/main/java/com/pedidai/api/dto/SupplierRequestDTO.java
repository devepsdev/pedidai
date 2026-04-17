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

    @NotBlank(message = "El nom del proveïdor és obligatori")
    @Size(max = 255, message = "El nom no pot superar els 255 caràcters")
    private String name;

    @Size(max = 255, message = "El nom de contacte no pot superar els 255 caràcters")
    private String contactName;

    @Email(message = "L'adreça de correu electrònic ha de tenir un format vàlid")
    @Size(max = 255, message = "L'email no pot superar els 255 caràcters")
    private String email;

    @Size(max = 50, message = "El telèfon no pot superar els 50 caràcters")
    private String phone;

    private String address;

    private String notes;

    @Builder.Default
    private Boolean isActive = true;
}