package com.pedidai.api.services;

import com.pedidai.api.dto.InvoiceConfirmRequestDTO;
import com.pedidai.api.dto.InvoiceScanResultDTO;
import com.pedidai.api.services.AiVisionService.DocumentKind;
import org.springframework.web.multipart.MultipartFile;

public interface InvoiceService {

    default InvoiceScanResultDTO scanInvoice(MultipartFile file, String supplierUuid) {
        return scanInvoice(file, supplierUuid, DocumentKind.INVOICE);
    }

    /** Llegeix un albarà o factura ({@code INVOICE}) o una tarifa o pressupost ({@code PRICE_LIST}). */
    InvoiceScanResultDTO scanInvoice(MultipartFile file, String supplierUuid, DocumentKind kind);

    InvoiceScanResultDTO confirmInvoice(InvoiceConfirmRequestDTO request);
}
