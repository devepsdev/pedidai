package com.pedidai.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Adreça de Stripe (pàgina de pagament o portal del client) on cal enviar l'usuari. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillingUrlDTO {
    private String url;
}
