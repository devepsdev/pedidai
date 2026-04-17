package com.pedidai.api.dto;

import com.pedidai.api.entities.User;
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
public class UserFilterDTO {

    // Filtres de text (cerca parcial, insensible a majúscules)
    private String email;       // Email
    private String firstName;   // Nom
    private String lastName;    // Cognom
    private String phone;       // Telèfon

    // Filtres d'estat
    private Boolean isActive;       // Estat actiu (null = tots, true = actius, false = inactius)
    private Boolean emailVerified;  // Email verificat (null = tots, true = verificats, false = no verificats)
    private User.UserRole role;     // Rol de l'usuari (null = tots, ADMIN o USER)

    // Paginació
    @Min(value = 0, message = "El número de pàgina ha de ser 0 o superior")
    @Builder.Default
    private int page = 0;

    @Min(value = 1, message = "La mida de pàgina ha de ser 1 o superior")
    @Builder.Default
    private int size = 10;

    // Ordenació (camps disponibles: email, firstName, lastName, phone, createdAt, updatedAt)
    @Builder.Default
    private String sortBy = "email";

    @Pattern(regexp = "asc|desc", message = "La direcció d'ordenació ha de ser 'asc' o 'desc'")
    @Builder.Default
    private String sortDir = "asc";

    public boolean hasTextFilters() {
        return (email != null && !email.trim().isEmpty()) ||
                (firstName != null && !firstName.trim().isEmpty()) ||
                (lastName != null && !lastName.trim().isEmpty()) ||
                (phone != null && !phone.trim().isEmpty());
    }

}
