package com.pedidai.api.services;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Crides a Stripe. Separades de la lògica de negoci perquè els tests no depenguin de la xarxa.
 * Sense clau secreta configurada ({@code STRIPE_SECRET_KEY}) el pagament en línia queda desactivat.
 */
public interface StripeGateway {

    boolean isConfigured();

    /** Crea una pàgina de pagament (Stripe Checkout) per a la subscripció i en retorna l'adreça. */
    String createCheckoutSession(CheckoutRequest request);

    /** Crea una sessió del portal del client (targeta, factures, baixa) i en retorna l'adreça. */
    String createPortalSession(String customerId, String returnUrl);

    /** Estat actual de la subscripció a Stripe («active», «trialing», «past_due», «canceled»…). */
    String subscriptionStatus(String subscriptionId);

    /**
     * Comprova la signatura de l'avís (capçalera {@code Stripe-Signature}) i en retorna el contingut.
     * Si la signatura no és vàlida o ha caducat, llança {@code BadRequestException}.
     */
    JsonNode verifyWebhook(String payload, String signatureHeader);

    /**
     * @param trialEndEpochSeconds fi de la prova gratuïta que encara queda (el primer cobrament es fa llavors),
     *                             o {@code null} per cobrar ja
     */
    record CheckoutRequest(String companyUuid, String email, String existingCustomerId,
                           Long trialEndEpochSeconds, String successUrl, String cancelUrl) {
    }
}
