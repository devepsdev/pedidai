package com.pedidai.api.services;

import com.pedidai.api.dto.AiInvoiceDataDTO;

public interface AiVisionService {
    AiInvoiceDataDTO analyzeInvoice(byte[] imageBytes, String mediaType);
}
