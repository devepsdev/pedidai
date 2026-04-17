package com.pedidai.api.dto;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserSummaryDTO {
    private String uuid;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private boolean active;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
}
