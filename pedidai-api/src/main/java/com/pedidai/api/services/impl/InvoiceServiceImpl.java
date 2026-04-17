package com.pedidai.api.services.impl;

import com.pedidai.api.dto.*;
import com.pedidai.api.entities.Product;
import com.pedidai.api.entities.Supplier;
import com.pedidai.api.entities.User;
import com.pedidai.api.exceptions.BadRequestException;
import com.pedidai.api.exceptions.ResourceNotFoundException;
import com.pedidai.api.repositories.ProductRepository;
import com.pedidai.api.repositories.SupplierRepository;
import com.pedidai.api.repositories.UserRepository;
import com.pedidai.api.services.AiVisionService;
import com.pedidai.api.services.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final AiVisionService aiVisionService;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    public InvoiceScanResultDTO scanInvoice(MultipartFile image, String supplierUuid) {
        validateImage(image);

        User currentUser = getCurrentUser();
        Long companyId = currentUser.getCompany().getId();

        byte[] imageBytes;
        String mediaType;
        try {
            imageBytes = image.getBytes();
            mediaType = resolveMediaType(image.getOriginalFilename(), image.getContentType());
        } catch (Exception e) {
            throw new BadRequestException("Error al leer la imagen: " + e.getMessage());
        }

        AiInvoiceDataDTO aiData;
        try {
            aiData = aiVisionService.analyzeInvoice(imageBytes, mediaType);
        } catch (Exception e) {
            log.error("AI Vision scan failed: {}", e.getMessage());
            return InvoiceScanResultDTO.builder()
                    .success(false)
                    .message("Error al analizar la imagen con IA: " + e.getMessage())
                    .build();
        }

        Supplier supplier = null;
        boolean supplierAutoMatched = false;

        if (supplierUuid != null && !supplierUuid.isBlank()) {
            supplier = supplierRepository.findByUuid(supplierUuid)
                    .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado: " + supplierUuid));
        } else if (aiData.getSupplier() != null && aiData.getSupplier().getName() != null
                && !aiData.getSupplier().getName().isBlank()) {
            // The Supplier entity has no CIF field — match by name only
            List<Supplier> nameMatches = supplierRepository.findActiveByCompanyIdAndNameContaining(
                    companyId, aiData.getSupplier().getName());
            if (!nameMatches.isEmpty()) {
                supplier = nameMatches.get(0);
                supplierAutoMatched = true;
                log.debug("Auto-matched supplier '{}' by name", supplier.getName());
            }
        }

        List<InvoiceProductDTO> productDTOs = new ArrayList<>();
        int created = 0, updated = 0, skipped = 0;

        if (aiData.getProducts() != null) {
            for (AiInvoiceDataDTO.ProductData pd : aiData.getProducts()) {
                InvoiceProductDTO dto = supplier != null
                        ? matchProduct(pd, supplier)
                        : buildCreatedProduct(pd);
                productDTOs.add(dto);
                switch (dto.getAction()) {
                    case "CREATED" -> created++;
                    case "UPDATED" -> updated++;
                    case "SKIPPED" -> skipped++;
                }
            }
        }

        AiInvoiceDataDTO.SupplierData supplierData = aiData.getSupplier();
        AiInvoiceDataDTO.InvoiceData invoiceData = aiData.getInvoice();
        AiInvoiceDataDTO.TotalsData totals = aiData.getTotals();

        return InvoiceScanResultDTO.builder()
                .success(true)
                .message("Factura analizada correctamente")
                .detectedSupplierName(supplierData != null ? supplierData.getName() : null)
                .detectedSupplierCif(supplierData != null ? supplierData.getCif() : null)
                .invoiceNumber(invoiceData != null ? invoiceData.getNumber() : null)
                .invoiceDate(invoiceData != null ? invoiceData.getDate() : null)
                .products(productDTOs)
                .totalAmount(totals != null ? totals.getTotal() : null)
                .totalIva(totals != null ? totals.getIva() : null)
                .productsCreated(created)
                .productsUpdated(updated)
                .productsSkipped(skipped)
                .matchedSupplierUuid(supplier != null ? supplier.getUuid() : null)
                .supplierAutoMatched(supplierAutoMatched)
                .build();
    }

    @Override
    @Transactional
    public InvoiceScanResultDTO confirmInvoice(InvoiceConfirmRequestDTO request) {
        if (request.getSupplierUuid() == null || request.getSupplierUuid().isBlank()) {
            throw new BadRequestException("El UUID del proveedor es obligatorio");
        }

        Supplier supplier = supplierRepository.findByUuid(request.getSupplierUuid())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado: " + request.getSupplierUuid()));

        int created = 0, updated = 0, skipped = 0;

        if (request.getProducts() != null) {
            for (InvoiceProductConfirmDTO dto : request.getProducts()) {
                if (dto.getAction() == null) continue;
                switch (dto.getAction()) {
                    case "CREATED" -> {
                        Product newProduct = Product.builder()
                                .uuid(UUID.randomUUID().toString())
                                .supplier(supplier)
                                .name(dto.getName())
                                .price(dto.getUnitPrice() != null ? dto.getUnitPrice() : BigDecimal.ZERO)
                                .unit(dto.getUnit())
                                .isActive(true)
                                .build();
                        productRepository.save(newProduct);
                        created++;
                    }
                    case "UPDATED" -> {
                        if (dto.getMatchedProductUuid() != null && dto.getUnitPrice() != null) {
                            productRepository.findByUuid(dto.getMatchedProductUuid()).ifPresent(existing -> {
                                existing.setPrice(dto.getUnitPrice());
                                productRepository.save(existing);
                            });
                            updated++;
                        }
                    }
                    case "SKIPPED" -> skipped++;
                }
            }
        }

        return InvoiceScanResultDTO.builder()
                .success(true)
                .message(String.format("Cambios aplicados: %d creados, %d actualizados, %d omitidos",
                        created, updated, skipped))
                .productsCreated(created)
                .productsUpdated(updated)
                .productsSkipped(skipped)
                .matchedSupplierUuid(supplier.getUuid())
                .build();
    }

    private InvoiceProductDTO matchProduct(AiInvoiceDataDTO.ProductData pd, Supplier supplier) {
        if (pd.getName() == null || pd.getName().isBlank()) {
            return InvoiceProductDTO.builder().name("(sin nombre)").action("SKIPPED").build();
        }

        List<Product> matches = productRepository.findActiveBySupplierIdAndNameContaining(
                supplier.getId(), pd.getName());

        if (matches.isEmpty()) {
            return buildCreatedProduct(pd);
        }

        Product existing = matches.get(0);
        BigDecimal newPrice = pd.getUnitPrice();

        if (newPrice != null && existing.getPrice().compareTo(newPrice) != 0) {
            return InvoiceProductDTO.builder()
                    .name(pd.getName())
                    .quantity(pd.getQuantity())
                    .unit(pd.getUnit())
                    .unitPrice(newPrice)
                    .ivaPercent(pd.getIvaPercent())
                    .subtotal(pd.getSubtotal())
                    .action("UPDATED")
                    .matchedProductUuid(existing.getUuid())
                    .previousPrice(existing.getPrice())
                    .build();
        }

        return InvoiceProductDTO.builder()
                .name(pd.getName())
                .quantity(pd.getQuantity())
                .unit(pd.getUnit())
                .unitPrice(existing.getPrice())
                .ivaPercent(pd.getIvaPercent())
                .subtotal(pd.getSubtotal())
                .action("SKIPPED")
                .matchedProductUuid(existing.getUuid())
                .previousPrice(existing.getPrice())
                .build();
    }

    private InvoiceProductDTO buildCreatedProduct(AiInvoiceDataDTO.ProductData pd) {
        return InvoiceProductDTO.builder()
                .name(pd.getName())
                .quantity(pd.getQuantity())
                .unit(pd.getUnit())
                .unitPrice(pd.getUnitPrice())
                .ivaPercent(pd.getIvaPercent())
                .subtotal(pd.getSubtotal())
                .action("CREATED")
                .build();
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BadRequestException("La imagen es obligatoria");
        }
        if (image.getSize() > 10 * 1024 * 1024L) {
            throw new BadRequestException("El archivo no puede superar los 10MB");
        }
        String filename = image.getOriginalFilename();
        if (filename != null) {
            String lower = filename.toLowerCase();
            if (!lower.endsWith(".jpg") && !lower.endsWith(".jpeg")
                    && !lower.endsWith(".png") && !lower.endsWith(".pdf")) {
                throw new BadRequestException("Formato no soportado. Use JPG, JPEG, PNG o PDF");
            }
        }
    }

    private String resolveMediaType(String filename, String contentType) {
        if (filename != null) {
            String lower = filename.toLowerCase();
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
            if (lower.endsWith(".png")) return "image/png";
            if (lower.endsWith(".pdf")) return "application/pdf";
        }
        return contentType != null ? contentType : "image/jpeg";
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuari no trobat: " + email));
    }
}
