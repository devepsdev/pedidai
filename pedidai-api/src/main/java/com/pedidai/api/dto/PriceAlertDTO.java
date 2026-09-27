package com.pedidai.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Pujada de preu detectada entre dos albarans del mateix producte i proveïdor. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceAlertDTO {
    private String productUuid;
    private String productName;
    private String supplierName;
    private String unit;
    private BigDecimal previousPrice;
    private LocalDate previousDate;
    private BigDecimal latestPrice;
    private LocalDate latestDate;
    private BigDecimal changePercent;
}
