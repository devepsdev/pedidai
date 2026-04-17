package com.pedidai.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumptionAnalysisDTO {

    private int totalOrders;
    private BigDecimal totalSpent;
    private BigDecimal averageOrderAmount;
    private int analyzedDays;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private List<ProductConsumptionDTO> topProducts;
    private List<SupplierSpendDTO> spendBySupplier;
    private List<PriceTrendDTO> priceTrends;
}
