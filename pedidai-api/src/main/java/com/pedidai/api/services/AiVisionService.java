package com.pedidai.api.services;

import com.pedidai.api.dto.AiInvoiceDataDTO;

public interface AiVisionService {

    /**
     * Tipus de document que es llegeix: albarà o factura (preus pagats) o tarifa o pressupost (preus oferts).
     * {@code AUTO} només val per llegir: la IA decideix quin dels dos és.
     */
    enum DocumentKind { INVOICE, PRICE_LIST, AUTO }

    default AiInvoiceDataDTO analyzeInvoice(byte[] fileBytes, String mediaType) {
        return analyzeDocument(fileBytes, mediaType, DocumentKind.INVOICE);
    }

    AiInvoiceDataDTO analyzeDocument(byte[] fileBytes, String mediaType, DocumentKind kind);
}
