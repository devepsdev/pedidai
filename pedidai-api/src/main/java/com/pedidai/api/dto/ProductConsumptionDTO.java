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
public class ProductConsumptionDTO {

    private String productUuid;
    private String productName;
    private String supplierName;
    private String category;
    private BigDecimal totalQuantity;
    private String unit;
    private BigDecimal totalSpent;
    private int orderCount;
    private BigDecimal avgQuantityPerOrder;
    private BigDecimal currentPrice;
}
