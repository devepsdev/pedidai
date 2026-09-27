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
public class UserRequestDTO {

    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.invalid}")
    private String email;

    @Size(max = 100, message = "{validation.name.tooLong}")
    private String firstName;

    @Size(max = 100, message = "{validation.lastName.tooLong}")
    private String lastName;

    private User.UserRole role;

    @Size(max = 50, message = "{validation.phone.tooLong}")
    private String phone;

    private Boolean isActive;
}