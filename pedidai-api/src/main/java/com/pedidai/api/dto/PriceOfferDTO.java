package com.pedidai.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Preu vigent d'un producte en un proveïdor concret. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceOfferDTO {
    private String productUuid;
    private String productName;
    private String supplierUuid;
    private String supplierName;
    private boolean supplierHasEmail;
    private String unit;
    private BigDecimal latestPrice;
    private LocalDate latestDate;
    private BigDecimal previousPrice;
    /** Variació del darrer preu respecte a l'anterior, en %. Null si només hi ha una observació. */
    private BigDecimal changePercent;
    private boolean cheapest;
}
