package com.pedidai.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequestDTO {

    @NotBlank(message = "El correu electrònic és obligatori")
    @Email(message = "El correu electrònic ha de tenir un format vàlid")
    private String email;

    @NotBlank(message = "La contrasenya és obligatòria")
    private String password;
}
