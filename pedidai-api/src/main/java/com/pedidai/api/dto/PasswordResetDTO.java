package com.pedidai.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetDTO {

    @NotBlank(message = "{validation.token.required}")
    private String token;

    @NotBlank(message = "{validation.password.required}")
    @Size(min = 8, message = "{validation.password.weak}")
    @Pattern(regexp = com.pedidai.api.security.PasswordPolicy.REGEX,
            message = "{validation.password.weak}")
    private String newPassword;
}