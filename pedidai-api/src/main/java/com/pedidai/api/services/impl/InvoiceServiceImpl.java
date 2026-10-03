package com.pedidai.api.services.impl;

import com.pedidai.api.dto.*;
import com.pedidai.api.entities.Company;
import com.pedidai.api.entities.PriceHistory;
import com.pedidai.api.entities.Product;
import com.pedidai.api.entities.Supplier;
import com.pedidai.api.exceptions.BadRequestException;
import com.pedidai.api.exceptions.ResourceNotFoundException;
import com.pedidai.api.repositories.ProductRepository;
import com.pedidai.api.repositories.SupplierRepository;
import com.pedidai.api.security.CurrentUser;
import com.pedidai.api.security.RateLimiter;
import com.pedidai.api.services.AiVisionService;
import com.pedidai.api.services.InvoiceService;
import com.pedidai.api.services.PriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Albarans i factures: lectura amb IA ({@link #scanInvoice}) i confirmació, que desa cada línia
 * a l'historial de preus amb la data del document ({@link #confirmInvoice}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
    /** Lectures d'albarans per empresa i dia (limita el cost de la IA). */
    private static final int MAX_SCANS_PER_DAY = 40;

    private final AiVisionService aiVisionService;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final PriceService priceService;
    private final CurrentUser currentUser;
    private final RateLimiter rateLimiter;

    @Override
    @Transactional(readOnly = true)
    public InvoiceScanResultDTO scanInvoice(MultipartFile image, String supplierUuid) {
        Company company = currentUser.company();
        String mediaType = validateFile(image);

        // Si s'ha triat un proveïdor, ha de ser de l'empresa (es valida abans de gastar IA)
        Supplier supplier = supplierUuid != null && !supplierUuid.isBlank() ? findOwnedSupplier(supplierUuid, company.getId()) : null;

        rateLimiter.check("scan:" + company.getId(), MAX_SCANS_PER_DAY, Duration.ofDays(1), "error.invoice.dailyLimit");

        AiInvoiceDataDTO aiData;
        try {
            aiData = aiVisionService.analyzeInvoice(image.getBytes(), mediaType);
        } catch (AiVisionServiceImpl.InvoiceReadException e) {
            throw new BadRequestException(e.getMessage());
        } catch (IOException e) {
            throw new BadRequestException("error.invoice.unreadable");
        }

        boolean autoMatched = false;
        String detectedName = aiData.getSupplier() != null ? aiData.getSupplier().getName() : null;
        if (supplier == null && detectedName != null && !detectedName.isBlank()) {
            supplier = matchSupplierByName(company.getId(), detectedName);
            autoMatched = supplier != null;
        }

        List<Product> supplierProducts = supplier != null
                ? productRepository.findBySupplier_IdAndIsActiveTrue(supplier.getId()) : List.of();

        List<InvoiceProductDTO> lines = new ArrayList<>();
        int increases = 0;
        if (aiData.getProducts() != null) {
            for (AiInvoiceDataDTO.ProductData pd : aiData.getProducts()) {
                if (pd.getName() == null || pd.getName().isBlank()) {
                    continue;
                }
                InvoiceProductDTO line = buildLine(pd, supplierProducts);
                if (line.getPriceChangePercent() != null && line.getPriceChangePercent().signum() > 0) {
                    increases++;
                }
                lines.add(line);
            }
        }
        if (lines.isEmpty()) {
            throw new BadRequestException("error.invoice.noProducts");
        }

        AiInvoiceDataDTO.SupplierData s = aiData.getSupplier();
        AiInvoiceDataDTO.InvoiceData inv = aiData.getInvoice();
        return InvoiceScanResultDTO.builder()
                .success(true)
                .detectedSupplierName(s != null ? s.getName() : null)
                .detectedSupplierCif(s != null ? s.getCif() : null)
                .detectedSupplierPhone(s != null ? s.getPhone() : null)
                .invoiceNumber(inv != null ? inv.getNumber() : null)
                .invoiceDate(inv != null ? inv.getDate() : null)
                .products(lines)
                .totalAmount(aiData.getTotals() != null ? aiData.getTotals().getTotal() : null)
                .totalIva(aiData.getTotals() != null ? aiData.getTotals().getIva() : null)
                .productsCreated((int) lines.stream().filter(l -> "NEW".equals(l.getStatus())).count())
                .productsUpdated((int) lines.stream().filter(l -> "CHANGED".equals(l.getStatus())).count())
                .productsSkipped((int) lines.stream().filter(l -> "SAME".equals(l.getStatus())).count())
                .priceIncreases(increases)
                .matchedSupplierUuid(supplier != null ? supplier.getUuid() : null)
                .supplierAutoMatched(autoMatched)
                .build();
    }

    @Override
    @Transactional
    public InvoiceScanResultDTO confirmInvoice(InvoiceConfirmRequestDTO request) {
        Company company = currentUser.company();
        Supplier supplier = resolveSupplier(request, company);
        LocalDate documentDate = parseDocumentDate(request.getInvoiceDate());
        String documentRef = request.getInvoiceNumber() != null ? request.getInvoiceNumber().trim() : null;

        List<Product> supplierProducts = new ArrayList<>(productRepository.findBySupplier_IdAndIsActiveTrue(supplier.getId()));
        int created = 0;
        int updated = 0;
        int recorded = 0;
        int increases = 0;

        for (InvoiceProductConfirmDTO line : request.getProducts()) {
            if (line.getUnitPrice() == null) {
                continue;
            }
            Product product = findProductForLine(line, supplier, supplierProducts);
            BigDecimal previous = null;
            if (product == null) {
                product = productRepository.save(Product.builder()
                        .uuid(UUID.randomUUID().toString())
                        .supplier(supplier)
                        .name(line.getName().trim())
                        .canonicalName(ProductNames.generic(nonBlank(line.getGenericName(), line.getName())))
                        .unit(line.getUnit())
                        .price(line.getUnitPrice().setScale(2, RoundingMode.HALF_UP))
                        .isActive(true)
                        .build());
                supplierProducts.add(product);
                created++;
            } else {
                previous = product.getPrice();
                if (product.getCanonicalName() == null && line.getGenericName() != null) {
                    product.setCanonicalName(ProductNames.generic(line.getGenericName()));
                }
            }

            if (priceService.recordObservation(product, line.getUnitPrice(), line.getUnit(), line.getQuantity(),
                    documentDate, PriceHistory.Source.INVOICE, documentRef) && previous != null) {
                updated++;
                if (product.getPrice().compareTo(previous) > 0) {
                    increases++;
                }
            }
            recorded++;
        }

        return InvoiceScanResultDTO.builder()
                .success(true)
                .productsCreated(created)
                .productsUpdated(updated)
                .productsSkipped(request.getProducts().size() - recorded)
                .pricesRecorded(recorded)
                .priceIncreases(increases)
                .matchedSupplierUuid(supplier.getUuid())
                .build();
    }

    // ───────────────────────── Proveïdor ─────────────────────────

    private Supplier resolveSupplier(InvoiceConfirmRequestDTO request, Company company) {
        if (request.getSupplierUuid() != null && !request.getSupplierUuid().isBlank()) {
            return findOwnedSupplier(request.getSupplierUuid(), company.getId());
        }
        InvoiceConfirmRequestDTO.NewSupplier ns = request.getNewSupplier();
        if (ns == null || ns.getName() == null || ns.getName().isBlank()) {
            throw new BadRequestException("error.invoice.supplierRequired");
        }
        // Si ja existeix un proveïdor amb aquest nom, es reutilitza en lloc de duplicar-lo
        Supplier existing = matchSupplierByName(company.getId(), ns.getName());
        if (existing != null) {
            return existing;
        }
        return supplierRepository.save(Supplier.builder()
                .company(company)
                .name(ns.getName().trim())
                .phone(blankToNull(ns.getPhone()))
                .email(blankToNull(ns.getEmail()))
                .isActive(true)
                .build());
    }

    private Supplier findOwnedSupplier(String uuid, Long companyId) {
        return supplierRepository.findByUuidAndCompany_Id(uuid, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("error.supplier.notFound"));
    }

    /**
     * Proveïdor actiu de l'empresa amb el mateix nom (ignorant majúscules, accents, forma jurídica i paraules
     * com "albarán"). Si no n'hi ha cap d'idèntic, s'accepta l'únic el nom del qual està contingut sencer al
     * detectat o a l'inrevés ("Distribuciones López" ↔ "Distribuciones López Hostelería").
     */
    private Supplier matchSupplierByName(Long companyId, String name) {
        String wanted = supplierKey(name);
        if (wanted == null) {
            return null;
        }
        String firstWord = wanted.split(" ")[0];
        List<Supplier> candidates = supplierRepository.findActiveByCompanyIdAndNameContaining(companyId, firstWord);
        Optional<Supplier> exact = candidates.stream()
                .filter(s -> wanted.equals(supplierKey(s.getName())))
                .findFirst();
        if (exact.isPresent()) {
            return exact.get();
        }
        List<Supplier> partial = candidates.stream()
                .filter(s -> containsPhrase(wanted, supplierKey(s.getName())))
                .toList();
        return partial.size() == 1 ? partial.get(0) : null;
    }

    static String supplierKey(String name) {
        String c = ProductNames.canonical(name);
        if (c == null) {
            return null;
        }
        String cleaned = c.replaceAll("\\b(s ?l ?u?|s ?a|s ?c ?p|s ?c ?c ?l|c ?b|sll)\\b", " ")
                .replaceAll("\\b(albaran(es)?|albara(ns)?|factura|facturas|factures|tiquet|ticket)\\b", " ")
                .trim().replaceAll("\\s+", " ");
        return cleaned.isEmpty() ? c : cleaned;
    }

    /** Una de les dues claus conté l'altra com a frase sencera, i la més curta té almenys dues paraules. */
    private static boolean containsPhrase(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        String shorter = a.length() <= b.length() ? a : b;
        String longer = shorter == a ? b : a;
        return shorter.split(" ").length >= 2 && (" " + longer + " ").contains(" " + shorter + " ");
    }

    // ───────────────────────── Línies ─────────────────────────

    private InvoiceProductDTO buildLine(AiInvoiceDataDTO.ProductData pd, List<Product> supplierProducts) {
        Product match = findExisting(pd.getName(), pd.getGenericName(), pd.getUnit(), supplierProducts);
        String status = "NEW";
        BigDecimal previous = null;
        BigDecimal change = null;
        if (match != null) {
            previous = match.getPrice();
            boolean samePrice = pd.getUnitPrice() == null || previous == null
                    || previous.compareTo(pd.getUnitPrice().setScale(2, RoundingMode.HALF_UP)) == 0;
            status = samePrice ? "SAME" : "CHANGED";
            if (!samePrice && previous.signum() > 0) {
                change = pd.getUnitPrice().subtract(previous).multiply(BigDecimal.valueOf(100))
                        .divide(previous, 1, RoundingMode.HALF_UP);
            }
        }
        return InvoiceProductDTO.builder()
                .name(pd.getName())
                .genericName(pd.getGenericName())
                .quantity(pd.getQuantity())
                .unit(pd.getUnit())
                .unitPrice(pd.getUnitPrice())
                .ivaPercent(pd.getIvaPercent())
                .subtotal(pd.getSubtotal())
                .status(status)
                .action(switch (status) { case "NEW" -> "CREATED"; case "CHANGED" -> "UPDATED"; default -> "SKIPPED"; })
                .matchedProductUuid(match != null ? match.getUuid() : null)
                .previousPrice(previous)
                .priceChangePercent(change)
                .build();
    }

    private Product findProductForLine(InvoiceProductConfirmDTO line, Supplier supplier, List<Product> supplierProducts) {
        if (line.getMatchedProductUuid() != null && !line.getMatchedProductUuid().isBlank()) {
            Optional<Product> byUuid = supplierProducts.stream()
                    .filter(p -> p.getUuid().equals(line.getMatchedProductUuid()))
                    .findFirst();
            if (byUuid.isPresent()) {
                return byUuid.get();
            }
            // Un UUID que no és d'aquest proveïdor (o d'aquesta empresa) s'ignora
            log.warn("Producte {} no pertany al proveïdor {}", line.getMatchedProductUuid(), supplier.getUuid());
        }
        return findExisting(line.getName(), line.getGenericName(), line.getUnit(), supplierProducts);
    }

    /** Mateix producte del mateix proveïdor: per nom exacte normalitzat o, si no, per nom genèric i unitat. */
    private static Product findExisting(String name, String genericName, String unit, List<Product> supplierProducts) {
        String byName = ProductNames.canonical(name);
        for (Product p : supplierProducts) {
            if (byName != null && byName.equals(ProductNames.canonical(p.getName()))) {
                return p;
            }
        }
        String byGeneric = ProductNames.canonical(genericName);
        String wantedUnit = ProductNames.unit(unit);
        if (byGeneric != null) {
            for (Product p : supplierProducts) {
                if (byGeneric.equals(ProductNames.canonical(p.getCanonicalName())) && wantedUnit.equals(ProductNames.unit(p.getUnit()))) {
                    return p;
                }
            }
        }
        return null;
    }

    // ───────────────────────── Validacions ─────────────────────────

    /** Accepta JPG, PNG, WebP o PDF de fins a 10 MB, comprovant la signatura del fitxer. */
    private static String validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("error.invoice.fileRequired");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new BadRequestException("error.file.tooLarge");
        }
        try (InputStream in = file.getInputStream()) {
            byte[] h = in.readNBytes(12);
            if (h.length >= 4 && h[0] == '%' && h[1] == 'P' && h[2] == 'D' && h[3] == 'F') {
                return "application/pdf";
            }
            if (h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) {
                return "image/jpeg";
            }
            if (h.length >= 4 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') {
                return "image/png";
            }
            if (h.length >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F' && h[8] == 'W' && h[9] == 'E') {
                return "image/webp";
            }
        } catch (IOException e) {
            throw new BadRequestException("error.invoice.unreadable");
        }
        throw new BadRequestException("error.invoice.invalidFormat");
    }

    /** Data del document; si no es pot llegir o és futura, s'usa la d'avui. */
    private static LocalDate parseDocumentDate(String value) {
        if (value == null || value.isBlank()) {
            return LocalDate.now();
        }
        try {
            LocalDate date = LocalDate.parse(value.trim());
            return date.isAfter(LocalDate.now()) || date.isBefore(LocalDate.now().minusYears(5)) ? LocalDate.now() : date;
        } catch (DateTimeParseException e) {
            return LocalDate.now();
        }
    }

    private static String nonBlank(String preferred, String fallback) {
        return preferred != null && !preferred.isBlank() ? preferred : fallback;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
