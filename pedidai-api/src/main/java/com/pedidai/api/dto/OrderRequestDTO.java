package com.pedidai.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import jakarta.validation.constraints.*;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequestDTO {

    @NotBlank
    private String name;

    @NotBlank
    private String supplierUuid;

    private String notes;

    private LocalDate deliveryDate;

    @NotNull
    private List<OrderItemRequestDTO> items;
}
