package com.pedidai.api.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidai.api.exceptions.BadRequestException;
import com.pedidai.api.services.StripeGateway;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StripeGatewayImpl implements StripeGateway {

    /** Locals per empresa que es poden contractar des de la pàgina de pagament (1 a 5, com diu la web). */
    private static final long MAX_LOCALS = 5;

    private final String secretKey;
    private final String webhookSecret;
    private final String priceId;
    private final String taxRateId;
    private final ObjectMapper objectMapper;

    public StripeGatewayImpl(@Value("${stripe.secret-key:}") String secretKey,
                             @Value("${stripe.webhook-secret:}") String webhookSecret,
                             @Value("${stripe.price-id:}") String priceId,
                             @Value("${stripe.tax-rate-id:}") String taxRateId,
                             ObjectMapper objectMapper) {
        this.secretKey = secretKey;
        this.webhookSecret = webhookSecret;
        this.priceId = priceId;
        this.taxRateId = taxRateId;
        this.objectMapper = objectMapper;
        if (isConfigured()) {
            log.info("Pagament amb Stripe actiu ({})", secretKey.startsWith("sk_live_") ? "mode real" : "mode de proves");
        }
    }

    @Override
    public boolean isConfigured() {
        return !secretKey.isBlank() && !webhookSecret.isBlank() && !priceId.isBlank();
    }

    @Override
    public String createCheckoutSession(CheckoutRequest request) {
        SessionCreateParams.LineItem.Builder lineItem = SessionCreateParams.LineItem.builder()
                .setPrice(priceId)
                .setQuantity(1L)
                .setAdjustableQuantity(SessionCreateParams.LineItem.AdjustableQuantity.builder()
                        .setEnabled(true).setMinimum(1L).setMaximum(MAX_LOCALS).build());
        if (!taxRateId.isBlank()) {
            lineItem.addTaxRate(taxRateId);  // IVA del 21 % sobre el preu sense IVA
        }

        SessionCreateParams.SubscriptionData.Builder subscription = SessionCreateParams.SubscriptionData.builder()
                .putMetadata("company_uuid", request.companyUuid());
        if (request.trialEndEpochSeconds() != null) {
            subscription.setTrialEnd(request.trialEndEpochSeconds());
        }

        SessionCreateParams.Builder params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setClientReferenceId(request.companyUuid())
                .setSuccessUrl(request.successUrl())
                .setCancelUrl(request.cancelUrl())
                .addLineItem(lineItem.build())
                .setSubscriptionData(subscription.build())
                .setBillingAddressCollection(SessionCreateParams.BillingAddressCollection.REQUIRED)
                .setTaxIdCollection(SessionCreateParams.TaxIdCollection.builder().setEnabled(true).build())
                .setAllowPromotionCodes(true)
                .setLocale(SessionCreateParams.Locale.AUTO);
        if (request.existingCustomerId() != null) {
            // Torna a contractar: es reutilitza el client (i les seves factures) i s'actualitzen les dades fiscals
            params.setCustomer(request.existingCustomerId())
                    .setCustomerUpdate(SessionCreateParams.CustomerUpdate.builder()
                            .setAddress(SessionCreateParams.CustomerUpdate.Address.AUTO)
                            .setName(SessionCreateParams.CustomerUpdate.Name.AUTO)
                            .build());
        } else {
            params.setCustomerEmail(request.email());
        }

        try {
            return Session.create(params.build(), options()).getUrl();
        } catch (StripeException e) {
            log.error("Stripe: no s'ha pogut crear la pàgina de pagament: {}", e.getMessage());
            throw new BadRequestException("error.billing.stripe");
        }
    }

    @Override
    public String createPortalSession(String customerId, String returnUrl) {
        var params = com.stripe.param.billingportal.SessionCreateParams.builder()
                .setCustomer(customerId)
                .setReturnUrl(returnUrl)
                .build();
        try {
            return com.stripe.model.billingportal.Session.create(params, options()).getUrl();
        } catch (StripeException e) {
            log.error("Stripe: no s'ha pogut obrir el portal del client: {}", e.getMessage());
            throw new BadRequestException("error.billing.stripe");
        }
    }

    @Override
    public String subscriptionStatus(String subscriptionId) {
        try {
            return Subscription.retrieve(subscriptionId, options()).getStatus();
        } catch (StripeException e) {
            log.error("Stripe: no s'ha pogut llegir la subscripció {}: {}", subscriptionId, e.getMessage());
            throw new BadRequestException("error.billing.stripe");
        }
    }

    @Override
    public JsonNode verifyWebhook(String payload, String signatureHeader) {
        try {
            // Comprova la signatura i que l'avís tingui menys de 5 minuts (evita que es reenviï un avís antic)
            Webhook.constructEvent(payload, signatureHeader, webhookSecret);
            return objectMapper.readTree(payload);
        } catch (SignatureVerificationException | JsonProcessingException e) {
            log.warn("Stripe: avís rebutjat ({})", e.getMessage());
            throw new BadRequestException("error.billing.invalidSignature");
        }
    }

    private RequestOptions options() {
        return RequestOptions.builder().setApiKey(secretKey).build();
    }
}
