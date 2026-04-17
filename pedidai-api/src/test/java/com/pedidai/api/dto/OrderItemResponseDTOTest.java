package com.pedidai.api.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

@DisplayName("OrderItemResponseDTO Tests")
class OrderItemResponseDTOTest {

    @Test
    @DisplayName("Comprova que Builder crea correctament el DTO")
    void builder_createsDTO() {
        OrderItemResponseDTO dto = OrderItemResponseDTO.builder()
                .uuid("item-1")
                .productUuid("prod-1")
                .productName("Producte A")
                .quantity(new BigDecimal("2"))
                .unitPrice(new BigDecimal("5"))
                .subtotal(new BigDecimal("10"))
                .notes("notes")
                .build();

        assertThat(dto.getUuid()).isEqualTo("item-1");
        assertThat(dto.getProductName()).isEqualTo("Producte A");
        assertThat(dto.getSubtotal()).isEqualTo(new BigDecimal("10"));
    }

    @Test
    @DisplayName("Comprova que equals i hashCode funcionen")
    void equalsHashCode() {
        OrderItemResponseDTO a = OrderItemResponseDTO.builder()
                .uuid("x")
                .build();

        OrderItemResponseDTO b = OrderItemResponseDTO.builder()
                .uuid("x")
                .build();

        assertThat(a).isEqualTo(b);
    }

    @Test
    @DisplayName("Comprova que toString funciona")
    void toString_notEmpty() {
        OrderItemResponseDTO dto = OrderItemResponseDTO.builder()
                .uuid("u")
                .build();

        assertThat(dto.toString()).contains("uuid");
    }
}