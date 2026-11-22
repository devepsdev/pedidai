package com.pedidai.api.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SuperAdminDashboardDTO {
    /** Empreses client (sense la de la plataforma). */
    private long totalCompanies;
    /** De pagament + en prova: les que poden entrar. */
    private long activeCompanies;
    private long paidCompanies;
    private long trialCompanies;
    private long trialEndedCompanies;
    private long inactiveCompanies;
    private long suspendedCompanies;
    private long pendingCompanies;
    private long totalUsers;
    private long totalAdmins;
    private long totalRegularUsers;
    private long totalOrders30d;
    private BigDecimal totalSpent30d;
    private BigDecimal potentialMRR;
    private List<TopCompanyDTO> topCompanies;
}
