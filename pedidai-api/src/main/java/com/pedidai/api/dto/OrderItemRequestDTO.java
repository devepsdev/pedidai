package com.pedidai.api.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemRequestDTO {

    private String orderItemUuid;

    @NotBlank(message = "{validation.product.required}")
    private String productUuid;

    @NotNull(message = "{validation.quantity.required}")
    @DecimalMin(value = "0.01", message = "{validation.quantity.min}")
    @DecimalMax(value = "100000", message = "{validation.quantity.max}")
    private BigDecimal quantity;

    @Size(max = 500, message = "{validation.notes.tooLong}")
    private String notes;
}
