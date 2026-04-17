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
    private LocalDateTime lastOrderDate;
}
