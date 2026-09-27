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

    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.invalid}")
    private String email;

    @NotBlank(message = "{validation.password.required}")
    @Size(min = 8, message = "{validation.password.weak}")
    @Pattern(regexp = com.pedidai.api.security.PasswordPolicy.REGEX,
            message = "{validation.password.weak}")
    private String password;

    @NotBlank(message = "{validation.name.required}")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "{validation.lastName.required}")
    @Size(max = 100)
    private String lastName;

    private User.UserRole role;

    @Size(max = 50)
    private String phone;
}