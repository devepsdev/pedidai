package com.pedidai.api.dto;

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
    private String invoiceNumber;
    private String invoiceDate;
    private List<InvoiceProductDTO> products;
    private BigDecimal totalAmount;
    private BigDecimal totalIva;
    private int productsCreated;
    private int productsUpdated;
    private int productsSkipped;
    private String matchedSupplierUuid;
    private boolean supplierAutoMatched;
}
