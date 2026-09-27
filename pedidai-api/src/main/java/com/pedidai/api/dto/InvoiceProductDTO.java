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
public class InvoiceProductDTO {
    private String name;
    private String genericName;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal unitPrice;
    private BigDecimal ivaPercent;
    private BigDecimal subtotal;
    /** NEW (producte nou), CHANGED (preu diferent de l'últim conegut) o SAME (mateix preu). */
    private String status;
    /** Compatibilitat: CREATED, UPDATED o SKIPPED. */
    private String action;
    private String matchedProductUuid;
    private BigDecimal previousPrice;
    private BigDecimal priceChangePercent;
}
