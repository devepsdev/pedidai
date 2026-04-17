package com.pedidai.api.dto;
import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TopCompanyDTO {
    private String companyUuid;
    private String companyName;
    private long orderCount;
    private BigDecimal totalSpent;
}
