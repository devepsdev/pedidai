package com.pedidai.api.dto;
import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MonthlyStatsDTO {
    private String month;
    private long orderCount;
    private BigDecimal totalSpent;
}
