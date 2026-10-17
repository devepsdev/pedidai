package com.pedidai.api.services;

import com.pedidai.api.dto.AiInvoiceDataDTO;

public interface AiVisionService {

    /** Tipus de document que es llegeix: albarà o factura (preus pagats) o tarifa o pressupost (preus oferts). */
    enum DocumentKind { INVOICE, PRICE_LIST }

    default AiInvoiceDataDTO analyzeInvoice(byte[] fileBytes, String mediaType) {
        return analyzeDocument(fileBytes, mediaType, DocumentKind.INVOICE);
    }

    AiInvoiceDataDTO analyzeDocument(byte[] fileBytes, String mediaType, DocumentKind kind);
}
