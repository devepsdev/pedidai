package com.pedidai.api.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Cadena de seguretat real: què és públic i què demana sessió a /api/billing (sense Stripe configurat). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Pagament: rutes públiques i protegides")
class BillingSecurityTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("contractar i obrir el portal demanen sessió")
    void checkoutAndPortalRequireSession() throws Exception {
        mvc.perform(post("/api/billing/checkout")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/billing/portal")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("saber si el pagament està actiu és públic (sense claus: desactivat)")
    void enabledIsPublic() throws Exception {
        mvc.perform(get("/api/billing/enabled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(false));
    }

    @Test
    @DisplayName("el webhook és públic, però sense Stripe configurat no aplica res")
    void webhookIsPublicButInactive() throws Exception {
        mvc.perform(post("/api/billing/webhook").contentType(MediaType.APPLICATION_JSON).content("{}")
                        .header("Stripe-Signature", "t=1,v1=fals"))
                .andExpect(status().isBadRequest());
    }
}
