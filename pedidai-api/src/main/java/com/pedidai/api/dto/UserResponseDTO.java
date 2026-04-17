package com.pedidai.api.dto;

import com.pedidai.api.entities.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDTO {

    private String uuid;

    private String companyUuid;

    private String companyName;

    private String email;

    private String firstName;

    private String lastName;

    private User.UserRole role;

    private String phone;

    private Boolean isActive;

    private Boolean isDeleted;

    private Boolean emailVerified;

    private LocalDateTime lastLogin;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}