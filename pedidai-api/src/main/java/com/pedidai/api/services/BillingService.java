package com.pedidai.api.services;

import com.pedidai.api.dto.BillingUrlDTO;
import com.pedidai.api.dto.LoginRequestDTO;

/** Subscripció de pagament amb Stripe. */
public interface BillingService {

    /** El pagament en línia està configurat. */
    boolean isEnabled();

    /** Pàgina de pagament per a l'empresa de l'administrador connectat (durant la prova o després). */
    BillingUrlDTO startCheckout();

    /**
     * Pàgina de pagament per a una empresa amb la prova acabada, que ja no pot iniciar sessió:
     * l'administrador s'identifica amb email i contrasenya.
     */
    BillingUrlDTO startCheckoutAfterTrial(LoginRequestDTO credentials, String clientIp);

    /** Portal del client de Stripe: targeta, factures i baixa. */
    BillingUrlDTO openPortal();

    /** Aplica un avís (webhook) de Stripe després de comprovar-ne la signatura. */
    void handleWebhook(String payload, String signatureHeader);
}
