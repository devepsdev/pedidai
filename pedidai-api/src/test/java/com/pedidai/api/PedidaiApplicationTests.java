package com.pedidai.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("PedidaiApplication Tests")
class PedidaiApplicationTests {

    @Test
    @DisplayName("Context de l'aplicació hauria de carregar-se correctament")
    void contextLoads() {
        // Aquest test verifica que el context de Spring Boot es carregui sense errors
        // Si hi ha problemes de configuració, beans mal configurats, etc., aquest test fallarà
    }

    @Test
    @DisplayName("Mètode main hauria d'executar-se sense errors")
    void main_ShouldRunWithoutErrors() {
        // Given & When & Then
        assertThatCode(() -> PedidaiApplication.main(new String[]{}))
                .doesNotThrowAnyException();
    }
}