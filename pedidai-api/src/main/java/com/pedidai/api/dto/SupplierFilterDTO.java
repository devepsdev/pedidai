package com.pedidai.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierFilterDTO {

    // Filtres de text (cerca parcial, insensible a majúscules)
    private String name;        // Nom del proveïdor
    private String contactName; // Nom de contacte
    private String email;       // Email
    private String phone;       // Telèfon
    private String address;     // Adreça

    // Paginació
    @Min(value = 0, message = "{validation.page.number}")
    @Builder.Default
    private int page = 0;

    @Min(value = 1, message = "{validation.page.size}")
    @Builder.Default
    private int size = 10;

    // Ordenació (camps disponibles: name, contactName, email, phone, createdAt, updatedAt)
    @Builder.Default
    private String sortBy = "name";

    @Pattern(regexp = "asc|desc", message = "{validation.sort.direction}")
    @Builder.Default
    private String sortDir = "asc";

    public boolean hasTextFilters() {
        return (name != null && !name.trim().isEmpty()) ||
                (contactName != null && !contactName.trim().isEmpty()) ||
                (email != null && !email.trim().isEmpty()) ||
                (phone != null && !phone.trim().isEmpty()) ||
                (address != null && !address.trim().isEmpty());
    }

}