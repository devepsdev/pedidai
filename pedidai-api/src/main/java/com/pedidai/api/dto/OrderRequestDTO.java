package com.pedidai.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequestDTO {

    @NotBlank(message = "{validation.orderName.required}")
    @Size(max = 255, message = "{validation.name.tooLong}")
    private String name;

    @NotBlank(message = "{validation.supplier.required}")
    private String supplierUuid;

    @Size(max = 2000, message = "{validation.notes.tooLong}")
    private String notes;

    private LocalDate deliveryDate;

    @NotEmpty(message = "{validation.order.itemsRequired}")
    @Size(max = 200, message = "{validation.order.tooManyItems}")
    private List<@Valid OrderItemRequestDTO> items;
}
