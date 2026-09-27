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
public class ProductRequestDTO {

    @NotBlank(message = "{validation.supplier.required}")
    @Size(max = 255, message = "{validation.uuid.tooLong}")
    private String supplierUuid;

    @Size(max = 255, message = "{validation.category.tooLong}")
    private String category;

    @NotBlank(message = "{validation.productName.required}")
    @Size(max = 255, message = "{validation.name.tooLong}")
    private String name;

    @Size(max = 255, message = "{validation.name.tooLong}")
    private String canonicalName;

    @Size(max = 5000, message = "{validation.description.tooLong}")
    private String description;

    @NotNull(message = "{validation.price.required}")
    @DecimalMin(value = "0.00", inclusive = true, message = "{validation.price.negative}")
    @Digits(integer = 8, fraction = 2, message = "{validation.price.digits}")
    private BigDecimal price;

    @DecimalMin(value = "0.00", inclusive = true, message = "{validation.volume.negative}")
    private BigDecimal volume;

    @Size(max = 50, message = "{validation.unit.tooLong}")
    private String unit;

    @Size(max = 500, message = "{validation.image.tooLong}")
    /*
    - Activar si el camp és una url per comprovar mitjançant patró.
    @Pattern(
            regexp = "^(https?://.*)?$",
            message = "{validation.image.invalidUrl}"
    )*/
    private String imageUrl;
}

