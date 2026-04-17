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
public class SupplierSpendDTO {

    private String supplierUuid;
    private String supplierName;
    private BigDecimal totalSpent;
    private BigDecimal percentage;
    private int orderCount;
}
