package com.pedidai.api.dto;

import com.pedidai.api.services.AiVisionService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceScanResultDTO {
    private boolean success;
    private String message;
    private String detectedSupplierName;
    private String detectedSupplierCif;
    private String detectedSupplierPhone;
    private String invoiceNumber;
    private String invoiceDate;
    /** Tipus de document llegit (albarà/factura o tarifa). */
    private AiVisionService.DocumentKind documentKind;
    /** Només en tarifes: fins quan són vàlids els preus. */
    private String validUntil;
    /** El document era massa llarg i només se n'ha llegit una part. */
    private boolean truncated;
    private List<InvoiceProductDTO> products;
    private BigDecimal totalAmount;
    private BigDecimal totalIva;
    private int productsCreated;
    private int productsUpdated;
    private int productsSkipped;
    private int pricesRecorded;
    private int priceIncreases;
    private String matchedSupplierUuid;
    private boolean supplierAutoMatched;
}
