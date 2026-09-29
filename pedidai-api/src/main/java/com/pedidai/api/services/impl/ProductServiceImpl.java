package com.pedidai.api.services.impl;

import com.pedidai.api.dto.*;
import com.pedidai.api.entities.PriceHistory;
import com.pedidai.api.entities.Product;
import com.pedidai.api.entities.Supplier;
import com.pedidai.api.exceptions.BadRequestException;
import com.pedidai.api.exceptions.ResourceNotFoundException;
import com.pedidai.api.repositories.ProductRepository;
import com.pedidai.api.repositories.SupplierRepository;
import com.pedidai.api.security.CurrentUser;
import com.pedidai.api.services.PriceService;
import com.pedidai.api.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    /** Carpeta on es desen les imatges i URL pública (servida per WebConfig, accessible via nginx a /api). */
    public static final String IMAGE_DIR = "img/productes/";
    public static final String IMAGE_URL_PREFIX = "/api/img/productes/";
    private static final long MAX_IMAGE_BYTES = 5_000_000;
    private static final Map<String, String> IMAGE_TYPES = Map.of(
            "image/jpeg", "jpg", "image/png", "png", "image/webp", "webp");
    private static final Pattern ALLOWED_IMAGE_URL =
            Pattern.compile("^(" + Pattern.quote(IMAGE_URL_PREFIX) + "[a-f0-9-]{36}\\.(jpg|png|webp)|https://\\S{1,480})$");

    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final CurrentUser currentUser;
    private final PriceService priceService;

    @Override
    @Transactional
    public ProductResponseDTO createProduct(ProductRequestDTO dto) {
        Supplier supplier = findOwnedSupplier(dto.getSupplierUuid());

        Product product = productRepository.save(Product.builder()
                .uuid(UUID.randomUUID().toString())
                .supplier(supplier)
                .category(dto.getCategory())
                .name(dto.getName().trim())
                .canonicalName(canonicalOf(dto))
                .description(dto.getDescription())
                .volume(dto.getVolume())
                .price(dto.getPrice())
                .unit(dto.getUnit())
                .imageUrl(validImageUrl(dto.getImageUrl()))
                .isActive(true)
                .build());

        priceService.recordObservation(product, dto.getPrice(), dto.getUnit(), null, LocalDate.now(),
                PriceHistory.Source.MANUAL, null);
        return mapToResponseDTO(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDTO getProductByUuid(String uuid) {
        return mapToResponseDTO(findOwned(uuid));
    }

    @Override
    @Transactional
    public ProductResponseDTO updateProduct(String uuid, ProductRequestDTO dto) {
        Product product = findOwned(uuid);
        boolean priceChanged = dto.getPrice() != null
                && (product.getPrice() == null || product.getPrice().compareTo(dto.getPrice()) != 0);

        product.setCategory(dto.getCategory());
        product.setName(dto.getName().trim());
        product.setCanonicalName(canonicalOf(dto));
        product.setDescription(dto.getDescription());
        product.setVolume(dto.getVolume());
        product.setUnit(dto.getUnit());
        // La imatge es puja a part: sense imageUrl a la petició es conserva la que hi havia ("" la treu)
        if (dto.getImageUrl() != null) {
            product.setImageUrl(validImageUrl(dto.getImageUrl()));
        }
        product = productRepository.save(product);

        // Un canvi manual de preu també queda a l'historial i passa a ser el preu vigent
        if (priceChanged) {
            priceService.recordObservation(product, dto.getPrice(), dto.getUnit(), null, LocalDate.now(),
                    PriceHistory.Source.MANUAL, null);
        }
        return mapToResponseDTO(product);
    }

    @Override
    @Transactional
    public ProductResponseDTO deactivateProduct(String uuid) {
        Product product = findOwned(uuid);
        product.setIsActive(false);
        return mapToResponseDTO(productRepository.save(product));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> listProductsByCompany(Pageable pageable) {
        return productRepository.findProductsByCompanyId(currentUser.companyId(), pageable).map(this::mapToResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> searchProducts(ProductSearchDTO dto, Pageable pageable) {
        Page<Product> products;
        if (dto.getSupplierUuid() != null && !dto.getSupplierUuid().isBlank()) {
            Supplier supplier = findOwnedSupplier(dto.getSupplierUuid());
            products = productRepository.searchProductsBySupplierId(supplier.getId(), dto.getSearchText(), pageable);
        } else {
            products = productRepository.searchProductsByCompanyId(currentUser.companyId(), dto.getSearchText(), pageable);
        }
        return products.map(this::mapToResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> filterProducts(ProductFilterDTO dto, Pageable pageable) {
        Boolean isActive = dto.getIsActive() != null ? dto.getIsActive() : Boolean.TRUE;
        Page<Product> products;
        if (dto.getSupplierUuid() != null && !dto.getSupplierUuid().isBlank()) {
            Supplier supplier = findOwnedSupplier(dto.getSupplierUuid());
            products = productRepository.filterProductsBySupplierId(supplier.getId(),
                    dto.getName(), dto.getDescription(), dto.getCategory(), dto.getVolume(), dto.getUnit(),
                    dto.getMinPrice(), dto.getMaxPrice(), isActive, pageable);
        } else {
            products = productRepository.filterProductsByCompanyId(currentUser.companyId(),
                    dto.getName(), dto.getDescription(), dto.getCategory(), dto.getVolume(), dto.getUnit(),
                    dto.getMinPrice(), dto.getMaxPrice(), isActive, pageable);
        }
        return products.map(this::mapToResponseDTO);
    }

    @Override
    @Transactional
    public String saveProductImage(String productUuid, MultipartFile file) {
        Product product = findOwned(productUuid);
        String url = storeImage(file);
        product.setImageUrl(url);
        productRepository.save(product);
        return url;
    }

    @Override
    public String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("error.image.missing");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new BadRequestException("error.image.tooLarge");
        }
        String extension = IMAGE_TYPES.get(file.getContentType());
        if (extension == null || !hasImageSignature(file)) {
            throw new BadRequestException("error.image.invalidType");
        }
        // El nom el decideix el servidor: mai s'usa el nom original del client
        String filename = UUID.randomUUID() + "." + extension;
        try {
            Path dir = Paths.get(IMAGE_DIR).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dir.resolve(filename));
            }
        } catch (IOException e) {
            throw new IllegalStateException("No s'ha pogut desar la imatge", e);
        }
        return IMAGE_URL_PREFIX + filename;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PriceGroupDTO> comparePrices(String productName, int days) {
        return priceService.compareByName(productName, days);
    }

    // ───────────────────────── Utilitats ─────────────────────────

    private Product findOwned(String uuid) {
        return productRepository.findByUuidAndSupplier_Company_Id(uuid, currentUser.companyId())
                .orElseThrow(() -> new ResourceNotFoundException("error.product.notFound"));
    }

    private Supplier findOwnedSupplier(String uuid) {
        return supplierRepository.findByUuidAndCompany_Id(uuid, currentUser.companyId())
                .orElseThrow(() -> new ResourceNotFoundException("error.supplier.notFound"));
    }

    private static String canonicalOf(ProductRequestDTO dto) {
        String source = dto.getCanonicalName() != null && !dto.getCanonicalName().isBlank()
                ? dto.getCanonicalName() : dto.getName();
        return ProductNames.generic(source);
    }

    private static String validImageUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        if (!ALLOWED_IMAGE_URL.matcher(url.trim()).matches()) {
            throw new BadRequestException("error.image.invalidUrl");
        }
        return url.trim();
    }

    /** Comprova els primers bytes del fitxer (JPEG, PNG o WebP) i no només el tipus declarat. */
    private static boolean hasImageSignature(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            byte[] h = in.readNBytes(12);
            if (h.length < 12) {
                return false;
            }
            boolean jpeg = (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
            boolean png = (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G';
            boolean webp = h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                    && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P';
            return jpeg || png || webp;
        } catch (IOException e) {
            return false;
        }
    }

    private ProductResponseDTO mapToResponseDTO(Product product) {
        return ProductResponseDTO.builder()
                .uuid(product.getUuid())
                .supplier(ProductSupplierResponseDTO.builder()
                        .uuid(product.getSupplier().getUuid())
                        .name(product.getSupplier().getName())
                        .build())
                .name(product.getName())
                .canonicalName(product.getCanonicalName())
                .category(product.getCategory())
                .description(product.getDescription())
                .price(product.getPrice())
                .volume(product.getVolume())
                .unit(product.getUnit())
                .imageUrl(product.getImageUrl())
                .isActive(product.getIsActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
