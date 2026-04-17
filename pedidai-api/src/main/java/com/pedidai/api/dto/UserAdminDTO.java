package com.pedidai.api.dto;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserAdminDTO {
    private String uuid;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private boolean active;
    private String companyName;
    private String companyUuid;
    private LocalDateTime createdAt;
}
