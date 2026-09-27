package com.pedidai.api.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceProductConfirmDTO {

    @NotBlank(message = "{validation.productName.required}")
    @Size(max = 255, message = "{validation.name.tooLong}")
    private String name;

    /** Nom genèric proposat per la IA per comparar entre proveïdors. */
    @Size(max = 255, message = "{validation.name.tooLong}")
    private String genericName;

    @DecimalMin(value = "0", message = "{validation.quantity.min}")
    private BigDecimal quantity;

    @Size(max = 50, message = "{validation.unit.tooLong}")
    private String unit;

    @NotNull(message = "{validation.price.required}")
    @DecimalMin(value = "0", message = "{validation.price.negative}")
    @Digits(integer = 8, fraction = 4, message = "{validation.price.digits}")
    private BigDecimal unitPrice;

    private BigDecimal ivaPercent;

    private BigDecimal subtotal;

    /** Producte existent al qual correspon la línia (si n'hi ha). */
    private String matchedProductUuid;

    /** Compatibilitat amb versions anteriors del frontend; ja no s'utilitza. */
    private String action;
}
