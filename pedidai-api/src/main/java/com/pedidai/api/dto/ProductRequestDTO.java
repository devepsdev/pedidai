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

    @NotBlank(message = "El uuid del proveïdor és obligatori")
    @Size(max = 255, message = "El uuid del proveïdor no pot superar els 255 caràcters")
    private String supplierUuid;

    @Size(max = 255, message = "La categoria no pot superar els 255 caràcters")
    private String category;

    @NotBlank(message = "El nom del producte és obligatori")
    @Size(max = 255, message = "El nom no pot superar els 255 caràcters")
    private String name;

    @Size(max = 5000, message = "La descripció no pot superar els 5000 caràcters")
    private String description;

    @NotNull(message = "El preu és obligatori")
    @DecimalMin(value = "0.00", inclusive = true, message = "El preu no pot ser negatiu")
    @Digits(integer = 8, fraction = 2, message = "El preu ha de tenir com a màxim 8 dígits enters i 2 decimals")
    private BigDecimal price;

    @DecimalMin(value = "0.00", inclusive = true, message = "El volum no pot ser negatiu")
    private BigDecimal volume;

    @Size(max = 50, message = "La unitat no pot superar els 50 caràcters")
    private String unit;

    @Size(max = 500, message = "La ruta o nom de la imatge no pot superar els 500 caràcters")
    /*
    - Activar si el camp és una url per comprovar mitjançant patró.
    @Pattern(
            regexp = "^(https?://.*)?$",
            message = "La URL de la imatge ha de ser un enllaç vàlid (http o https)"
    )*/
    private String imageUrl;
}

