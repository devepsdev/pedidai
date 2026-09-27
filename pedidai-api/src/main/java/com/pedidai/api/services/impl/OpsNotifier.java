package com.pedidai.api.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidai.api.services.OpsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Avisa n8n de cada registre nou perquè l'equip rebi un correu al moment.
 * Només després de confirmar la transacció i en segon pla: si n8n no respon, el registre no se'n ressent.
 * Sense {@code app.n8n.new-company-webhook} (p. ex. en local) no fa res.
 */
@Slf4j
@Component
public class OpsNotifier {

    private final OpsService opsService;
    private final ObjectMapper objectMapper;
    private final String webhookUrl;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public OpsNotifier(OpsService opsService, ObjectMapper objectMapper,
                       @Value("${app.n8n.new-company-webhook:}") String webhookUrl) {
        this.opsService = opsService;
        this.objectMapper = objectMapper;
        this.webhookUrl = webhookUrl;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCompanyRegistered(CompanyRegisteredEvent event) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }
        try {
            String body = objectMapper.writeValueAsString(opsService.getCompany(event.companyId()));
            HttpRequest request = HttpRequest.newBuilder(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() >= 300) {
                log.warn("n8n ha respost {} a l'avís de registre de l'empresa {}", response.statusCode(), event.companyId());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("No s'ha pogut avisar n8n del registre de l'empresa {}: {}", event.companyId(), e.getMessage());
        }
    }
}
