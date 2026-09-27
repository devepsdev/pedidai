package com.pedidai.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/** Resum de preus de l'empresa: comparativa entre proveïdors, pujades i sobrecost estimat. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceOverviewDTO {
    private int days;
    private int observations;
    private int productsTracked;
    private int suppliersTracked;
    /** Grups amb dos o més proveïdors (comparables), del més car al més barat de diferència. */
    private List<PriceGroupDTO> comparable;
    /** Productes que de moment només tenen un proveïdor. */
    private List<PriceGroupDTO> singleSupplier;
    private List<PriceAlertDTO> alerts;
    /**
     * Estimació del que s'ha pagat de més: per a cada línia d'albarà del període,
     * quantitat × (preu pagat − preu vigent més barat del mateix producte).
     */
    private BigDecimal overpaidAmount;
    private int overpaidLines;
}
