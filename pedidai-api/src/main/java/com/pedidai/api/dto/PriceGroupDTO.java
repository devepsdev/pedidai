package com.pedidai.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/** Un mateix producte (nom genèric + unitat) ofert per un o més proveïdors. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceGroupDTO {
    private String key;
    private String name;
    private String unit;
    private List<PriceOfferDTO> offers;
    private BigDecimal cheapestPrice;
    private BigDecimal highestPrice;
    /** Quant més car és el proveïdor més car respecte al més barat, en %. */
    private BigDecimal spreadPercent;
}
