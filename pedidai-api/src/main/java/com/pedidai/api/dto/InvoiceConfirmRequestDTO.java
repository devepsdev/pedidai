package com.pedidai.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Confirmació d'un albarà escanejat. Cal indicar un proveïdor existent ({@code supplierUuid})
 * o les dades per crear-ne un de nou ({@code newSupplier}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceConfirmRequestDTO {

    private String supplierUuid;

    @Valid
    private NewSupplier newSupplier;

    @Size(max = 100, message = "{validation.invoiceNumber.tooLong}")
    private String invoiceNumber;

    /** Data del document en format AAAA-MM-DD; si falta, la d'avui. */
    private String invoiceDate;

    @NotEmpty(message = "{validation.invoice.productsRequired}")
    @Size(max = 300, message = "{validation.order.tooManyItems}")
    private List<@Valid InvoiceProductConfirmDTO> products;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewSupplier {
        @NotBlank(message = "{validation.supplierName.required}")
        @Size(max = 255, message = "{validation.name.tooLong}")
        private String name;

        @Size(max = 50, message = "{validation.phone.tooLong}")
        private String phone;

        @Email(message = "{validation.email.invalid}")
        @Size(max = 255, message = "{validation.email.tooLong}")
        private String email;
    }
}
