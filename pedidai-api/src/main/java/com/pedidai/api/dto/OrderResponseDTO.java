package com.pedidai.api.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponseDTO {

    private String uuid;

    private String name;

    private String status;

    private BigDecimal totalAmount;

    private String notes;

    private LocalDate deliveryDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String supplierUuid;

    private List<OrderItemResponseDTO> items;
}
