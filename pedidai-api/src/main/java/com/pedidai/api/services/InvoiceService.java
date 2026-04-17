package com.pedidai.api.services;

import com.pedidai.api.dto.InvoiceConfirmRequestDTO;
import com.pedidai.api.dto.InvoiceScanResultDTO;
import org.springframework.web.multipart.MultipartFile;

public interface InvoiceService {
    InvoiceScanResultDTO scanInvoice(MultipartFile image, String supplierUuid);
    InvoiceScanResultDTO confirmInvoice(InvoiceConfirmRequestDTO request);
}
