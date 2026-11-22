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
    /** Estat de l'empresa: si no és ACTIVE, l'usuari no pot entrar. */
    private String companyStatus;
    /** La prova de l'empresa ha acabat sense contractar: l'usuari no pot entrar. */
    private boolean companyTrialEnded;
    private LocalDateTime createdAt;
}
