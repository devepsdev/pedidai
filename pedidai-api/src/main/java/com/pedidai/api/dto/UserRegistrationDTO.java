package com.pedidai.api.dto;

import com.pedidai.api.entities.User;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRegistrationDTO {

    @NotBlank(message = "L'email és obligatori")
    @Email(message = "L'email ha de ser vàlid")
    private String email;

    @NotBlank(message = "La contrasenya és obligatòria")
    @Size(min = 8, message = "La contrasenya ha de tenir almenys 8 caràcters")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[^a-zA-Z0-9\\s]).*$",
            message = "La contrasenya ha de contenir un mínim d'una majúscula, una minúscula, un número i un caràcter especial")
    private String password;

    @NotBlank(message = "El nom és obligatori")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Els cognoms són obligatoris")
    @Size(max = 100)
    private String lastName;

    private User.UserRole role;

    @Size(max = 50)
    private String phone;
}