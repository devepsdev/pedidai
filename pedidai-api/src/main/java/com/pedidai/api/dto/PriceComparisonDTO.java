package com.pedidai.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceComparisonDTO {

    private String productUuid;
    private String productName;
    private String category;
    private String unit;
    private BigDecimal volume;
    private String supplierUuid;
    private String supplierName;
    private String supplierEmail;
    private String supplierPhone;
    private BigDecimal currentPrice;
    private BigDecimal avgOrderPrice;
    private BigDecimal minOrderPrice;
    private BigDecimal maxOrderPrice;
    private int orderCount;
    private BigDecimal priceChangePercent;
    private String trend;
    @Builder.Default
    private Boolean isCheapest = false;
}
