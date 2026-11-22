package com.pedidai.api.dto;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CompanySummaryDTO {
    private String uuid;
    private String name;
    private String taxId;
    private String email;
    private String city;
    private String status;
    private long userCount;
    private long orderCount;
    private LocalDateTime createdAt;
    private LocalDateTime trialEndsAt;
    /** Compte de la plataforma (la del SUPER_ADMIN), no és cap client. */
    private boolean platform;
    /** La prova ha acabat sense contractar: ja no pot entrar. */
    private boolean trialEnded;
    /** Quan s'esborraran les seves dades (nul si no ha d'esborrar-se). */
    private LocalDateTime dataDeletionAt;
    private LocalDateTime lastOrderDate;
}
