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
public class PasswordResetRequestDTO {

    @NotBlank(message = "No pot ser null, buit o contenir només espais en blanc")
    @Email(message = "Ha de tenir un format d'email vàlid (exemple: user@domain.com)")
    private String email;
}