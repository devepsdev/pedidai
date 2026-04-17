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

    @NotBlank(message = "L'email és obligatori")
    @Email(message = "L'email ha de ser vàlid")
    private String email;

    @Size(max = 100, message = "El nom no pot excedir 100 caràcters")
    private String firstName;

    @Size(max = 100, message = "El cognom no pot excedir 100 caràcters")
    private String lastName;

    private User.UserRole role;

    @Size(max = 50, message = "El telèfon no pot excedir 50 caràcters")
    private String phone;

    private Boolean isActive;
}