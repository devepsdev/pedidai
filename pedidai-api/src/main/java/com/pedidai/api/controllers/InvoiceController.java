package com.pedidai.api.controllers;

import com.pedidai.api.dto.ApiResponseDTO;
import com.pedidai.api.dto.InvoiceConfirmRequestDTO;
import com.pedidai.api.dto.InvoiceScanResultDTO;
import com.pedidai.api.services.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Slf4j
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping(value = "/scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseDTO<InvoiceScanResultDTO>> scanInvoice(
            @RequestPart("image") MultipartFile image,
            @RequestParam(value = "supplierUuid", required = false) String supplierUuid) {

        log.debug("POST /api/invoices/scan - file={}, supplierUuid={}", image.getOriginalFilename(), supplierUuid);
        InvoiceScanResultDTO result = invoiceService.scanInvoice(image, supplierUuid);

        if (!result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(ApiResponseDTO.error(result.getMessage()));
        }

        return ResponseEntity.ok(ApiResponseDTO.success(result, "Factura escaneada correctamente"));
    }

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponseDTO<InvoiceScanResultDTO>> confirmInvoice(
            @RequestBody InvoiceConfirmRequestDTO request) {

        log.debug("POST /api/invoices/confirm - supplierUuid={}, products={}",
                request.getSupplierUuid(),
                request.getProducts() != null ? request.getProducts().size() : 0);

        InvoiceScanResultDTO result = invoiceService.confirmInvoice(request);
        return ResponseEntity.ok(ApiResponseDTO.success(result, result.getMessage()));
    }
}
