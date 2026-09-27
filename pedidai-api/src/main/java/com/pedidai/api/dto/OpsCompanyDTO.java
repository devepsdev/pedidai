package com.pedidai.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Empresa client tal com la veu l'equip de PedidAI a les alertes internes (n8n). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpsCompanyDTO {
    private String uuid;
    private String name;
    private String city;
    private String adminName;
    private String adminEmail;
    private String phone;
    private String language;
    private boolean emailVerified;
    private LocalDateTime createdAt;
    private LocalDateTime trialEndsAt;
}
