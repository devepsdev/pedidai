package com.pedidai.api.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SuperAdminDashboardDTO {
    private long totalCompanies;
    private long activeCompanies;
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
