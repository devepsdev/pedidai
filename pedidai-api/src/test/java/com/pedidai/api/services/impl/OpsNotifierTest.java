package com.pedidai.api.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pedidai.api.dto.OpsCompanyDTO;
import com.pedidai.api.services.OpsService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@DisplayName("OpsNotifier: avís a n8n de cada registre")
class OpsNotifierTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private HttpServer server;

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("sense URL configurada no fa res")
    void disabledWithoutUrl() {
        OpsService opsService = mock(OpsService.class);
        new OpsNotifier(opsService, objectMapper, "").onCompanyRegistered(new CompanyRegisteredEvent(1L));
        verifyNoInteractions(opsService);
    }

    @Test
    @DisplayName("envia la fitxa de l'empresa al webhook de n8n")
    void postsCompanyToWebhook() throws Exception {
        AtomicReference<String> received = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/webhook/pedidai-nuevo-registro", exchange -> {
            received.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
        OpsService opsService = mock(OpsService.class);
        when(opsService.getCompany(7L)).thenReturn(OpsCompanyDTO.builder().name("Bar Prova").adminEmail("laura@bar.test").build());
        String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/webhook/pedidai-nuevo-registro";

        new OpsNotifier(opsService, objectMapper, url).onCompanyRegistered(new CompanyRegisteredEvent(7L));

        assertThat(received.get()).contains("\"name\":\"Bar Prova\"").contains("laura@bar.test");
    }

    @Test
    @DisplayName("si n8n no respon, no llança cap error")
    void unreachableWebhook() {
        OpsService opsService = mock(OpsService.class);
        when(opsService.getCompany(1L)).thenReturn(OpsCompanyDTO.builder().name("Bar").build());
        OpsNotifier notifier = new OpsNotifier(opsService, objectMapper, "http://127.0.0.1:1/webhook/x");
        assertThatCode(() -> notifier.onCompanyRegistered(new CompanyRegisteredEvent(1L))).doesNotThrowAnyException();
    }
}
