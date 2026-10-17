package com.pedidai.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiInvoiceDataDTO {

    /** Tipus de document detectat per la IA quan es llegeix en mode AUTO: INVOICE o PRICE_LIST. */
    private String documentType;
    private SupplierData supplier;
    private InvoiceData invoice;
    private List<ProductData> products;
    private TotalsData totals;

    /** El text del document era massa llarg i se n'ha llegit només una part (no ve de la IA). */
    @JsonIgnore
    private boolean textTruncated;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SupplierData {
        private String name;
        private String cif;
        private String address;
        private String phone;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InvoiceData {
        private String number;
        private String date;
        /** Només en tarifes: fins quan són vàlids els preus (AAAA-MM-DD). */
        private String validUntil;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProductData {
        private String name;
        /** Nom genèric en castellà, en minúscules i singular ("tomate pera"). */
        private String genericName;
        private BigDecimal quantity;
        private String unit;
        private BigDecimal unitPrice;
        private BigDecimal ivaPercent;
        private BigDecimal subtotal;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TotalsData {
        private BigDecimal base;
        private BigDecimal iva;
        private BigDecimal total;
    }
}
