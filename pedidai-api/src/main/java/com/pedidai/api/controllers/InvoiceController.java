package com.pedidai.api.controllers;

import com.pedidai.api.dto.ApiResponseDTO;
import com.pedidai.api.dto.InvoiceConfirmRequestDTO;
import com.pedidai.api.dto.InvoiceScanResultDTO;
import com.pedidai.api.services.AiVisionService;
import com.pedidai.api.services.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    /**
     * Llegeix amb IA un albarà o factura ({@code kind=INVOICE}, per defecte), una tarifa o pressupost
     * ({@code kind=PRICE_LIST}, també en Excel o CSV) o un document del qual la IA decideix el tipus
     * ({@code kind=AUTO}, el que fa servir la web) i proposa les línies; encara no desa res.
     * El tipus llegit torna a {@code documentKind}.
     */
    @PostMapping(value = "/scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseDTO<InvoiceScanResultDTO>> scanInvoice(
            @RequestPart("image") MultipartFile image,
            @RequestParam(value = "supplierUuid", required = false) String supplierUuid,
            @RequestParam(value = "kind", defaultValue = "INVOICE") AiVisionService.DocumentKind kind) {
        return ResponseEntity.ok(ApiResponseDTO.success(invoiceService.scanInvoice(image, supplierUuid, kind),
                "success.invoice.scanned"));
    }

    /** Desa les línies confirmades: productes nous, preus i historial amb la data del document. */
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponseDTO<InvoiceScanResultDTO>> confirmInvoice(
            @Valid @RequestBody InvoiceConfirmRequestDTO request) {
        return ResponseEntity.ok(ApiResponseDTO.success(invoiceService.confirmInvoice(request),
                "success.invoice.confirmed"));
    }
}
