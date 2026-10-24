package com.pedidai.api.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidai.api.exceptions.BadRequestException;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("StripeGatewayImpl: configuració i signatura dels avisos")
class StripeGatewayImplTest {

    private static final String WEBHOOK_SECRET = "whsec_test_secret";
    private static final String PAYLOAD = "{\"id\":\"evt_1\",\"object\":\"event\",\"type\":\"checkout.session.completed\"}";

    private static StripeGatewayImpl gateway(String secretKey, String webhookSecret, String priceId) {
        return new StripeGatewayImpl(secretKey, webhookSecret, priceId, "", new ObjectMapper());
    }

    private static String signature(String secret, long timestamp) throws Exception {
        String signed = Webhook.Util.computeHmacSha256(secret, timestamp + "." + PAYLOAD);
        return "t=" + timestamp + ",v1=" + signed;
    }

    @Test
    @DisplayName("sense clau, secret del webhook o preu, el pagament queda desactivat")
    void configured() {
        assertThat(gateway("", "", "").isConfigured()).isFalse();
        assertThat(gateway("sk_test_x", "", "price_1").isConfigured()).isFalse();
        assertThat(gateway("sk_test_x", WEBHOOK_SECRET, "price_1").isConfigured()).isTrue();
    }

    @Test
    @DisplayName("un avís signat amb el secret del webhook s'accepta")
    void validSignature() throws Exception {
        long now = System.currentTimeMillis() / 1000;

        var event = gateway("sk_test_x", WEBHOOK_SECRET, "price_1").verifyWebhook(PAYLOAD, signature(WEBHOOK_SECRET, now));

        assertThat(event.path("id").asText()).isEqualTo("evt_1");
    }

    @Test
    @DisplayName("un avís signat amb un altre secret es rebutja")
    void wrongSecret() throws Exception {
        long now = System.currentTimeMillis() / 1000;
        StripeGatewayImpl gateway = gateway("sk_test_x", WEBHOOK_SECRET, "price_1");

        assertThatThrownBy(() -> gateway.verifyWebhook(PAYLOAD, signature("whsec_un_altre", now)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("error.billing.invalidSignature");
    }

    @Test
    @DisplayName("un avís de fa més de 5 minuts es rebutja (no es pot reenviar un avís antic)")
    void oldEvent() throws Exception {
        long tenMinutesAgo = System.currentTimeMillis() / 1000 - 600;
        StripeGatewayImpl gateway = gateway("sk_test_x", WEBHOOK_SECRET, "price_1");

        assertThatThrownBy(() -> gateway.verifyWebhook(PAYLOAD, signature(WEBHOOK_SECRET, tenMinutesAgo)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("un avís sense signatura es rebutja")
    void missingSignature() {
        assertThatThrownBy(() -> gateway("sk_test_x", WEBHOOK_SECRET, "price_1").verifyWebhook(PAYLOAD, ""))
                .isInstanceOf(BadRequestException.class);
    }
}
