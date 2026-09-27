package com.pedidai.api.services;

import com.pedidai.api.dto.PriceGroupDTO;
import com.pedidai.api.dto.PriceOverviewDTO;
import com.pedidai.api.entities.PriceHistory;
import com.pedidai.api.entities.Product;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PriceService {

    /**
     * Registra un preu observat. El preu vigent del producte només s'actualitza si el document
     * és igual o més recent que l'últim registrat (un albarà antic no trepitja el preu actual).
     *
     * @return true si ha canviat el preu vigent del producte
     */
    boolean recordObservation(Product product, BigDecimal unitPrice, String unit, BigDecimal quantity,
                              LocalDate documentDate, PriceHistory.Source source, String documentRef);

    /** Resum de preus de l'empresa de l'usuari en els darrers {@code days} dies. */
    PriceOverviewDTO getOverview(int days);

    /** Grups de productes el nom dels quals conté el text indicat (per al xat i el comparador). */
    List<PriceGroupDTO> compareByName(String productName, int days);
}
