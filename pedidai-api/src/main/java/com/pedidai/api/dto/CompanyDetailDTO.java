package com.pedidai.api.dto;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CompanyDetailDTO {
    private String uuid;
    private String name;
    private String taxId;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String postalCode;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime trialEndsAt;
    private List<UserSummaryDTO> users;
    private long supplierCount;
    private long productCount;
    private long orderCount;
    private List<RecentOrderDTO> recentOrders;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RecentOrderDTO {
        private String uuid;
        private String supplierName;
        private java.math.BigDecimal total;
        private String status;
        private LocalDateTime createdAt;
    }
}
