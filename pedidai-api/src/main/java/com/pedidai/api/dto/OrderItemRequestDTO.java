package com.pedidai.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemRequestDTO {

    private String orderItemUuid;

    @NotBlank
    private String productUuid;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal quantity;

    private String notes;

}
