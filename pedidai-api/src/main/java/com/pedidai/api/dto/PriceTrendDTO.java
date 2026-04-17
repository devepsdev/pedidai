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
public class PriceTrendDTO {

    private String productUuid;
    private String productName;
    private String supplierName;
    private BigDecimal oldestPrice;
    private BigDecimal latestPrice;
    private BigDecimal changePercent;
    private String trend;
}
