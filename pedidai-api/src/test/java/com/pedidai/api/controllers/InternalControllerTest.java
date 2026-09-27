package com.pedidai.api.controllers;

import com.pedidai.api.dto.OpsSummaryDTO;
import com.pedidai.api.services.OpsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("InternalController: només accessible des del mateix servidor")
class InternalControllerTest {

    private OpsService opsService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        opsService = mock(OpsService.class);
        when(opsService.getSummary(anyInt())).thenReturn(OpsSummaryDTO.builder().hours(24).totalClients(3)
                .newCompanies(List.of()).build());
        mockMvc = MockMvcBuilders.standaloneSetup(new InternalController(opsService)).build();
    }

    @Test
    @DisplayName("petició local (n8n) → 200 amb el resum")
    void localRequest() throws Exception {
        mockMvc.perform(get("/api/internal/daily-summary").with(r -> { r.setRemoteAddr("127.0.0.1"); return r; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClients").value(3));
        verify(opsService).getSummary(24);
    }

    @Test
    @DisplayName("petició d'una IP externa → 404")
    void externalRequest() throws Exception {
        mockMvc.perform(get("/api/internal/daily-summary").with(r -> { r.setRemoteAddr("83.45.10.2"); return r; }))
                .andExpect(status().isNotFound());
        verifyNoInteractions(opsService);
    }

    @Test
    @DisplayName("petició que arriba per nginx (X-Real-IP) → 404 encara que sembli local")
    void throughProxy() throws Exception {
        mockMvc.perform(get("/api/internal/daily-summary").header("X-Real-IP", "83.45.10.2")
                        .with(r -> { r.setRemoteAddr("127.0.0.1"); return r; }))
                .andExpect(status().isNotFound());
        verifyNoInteractions(opsService);
    }
}
