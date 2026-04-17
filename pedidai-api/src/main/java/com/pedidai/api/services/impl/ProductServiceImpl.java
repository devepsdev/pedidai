package com.pedidai.api.services.impl;

import com.pedidai.api.dto.*;
import com.pedidai.api.entities.Order;
import com.pedidai.api.entities.OrderItem;
import com.pedidai.api.entities.Product;
import com.pedidai.api.entities.Supplier;
import com.pedidai.api.entities.User;
import com.pedidai.api.exceptions.ResourceNotFoundException;
import com.pedidai.api.repositories.*;
import com.pedidai.api.services.ProductService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;

    public ProductServiceImpl(ProductRepository productRepository, SupplierRepository supplierRepository,
                              UserRepository userRepository, OrderItemRepository orderItemRepository) {
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.userRepository = userRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    @Transactional
    public ProductResponseDTO createProduct(ProductRequestDTO productRequestDTO) {

        // Validar que el proveïdor existeix
        Supplier supplier = supplierRepository.findByUuid(productRequestDTO.getSupplierUuid())
                .orElseThrow(() -> new IllegalArgumentException("El proveïdor especificat no existeix."));

        // Crear entitat Product a partir del DTO
        Product product = Product.builder()
                .uuid(UUID.randomUUID().toString())
                .supplier(supplier)
                .category(productRequestDTO.getCategory())
                .name(productRequestDTO.getName())
                .description(productRequestDTO.getDescription())
                .volume(productRequestDTO.getVolume())
                .price(productRequestDTO.getPrice())
                .unit(productRequestDTO.getUnit())
                .imageUrl(productRequestDTO.getImageUrl())
                .isActive(true)
                .build();

        // Guardar el producte
        product = productRepository.save(product);

        // Retornar el resultat com a DTO
        return mapToResponseDTO(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDTO getProductByUuid(String uuid) {
        Product product = productRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("No s'ha trobat cap producte amb el UUID: " + uuid));
        return mapToResponseDTO(product);
    }

    @Override
    @Transactional
    public ProductResponseDTO updateProduct(String uuid, ProductRequestDTO productRequestDTO) {

        // Comprovar que el producte existeix
        Product product = productRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("No s'ha trobat cap producte amb el UUID: " + uuid));

        // Actualitzar camps (només els que vénen del DTO)
        product.setCategory(productRequestDTO.getCategory());
        product.setName(productRequestDTO.getName());
        product.setDescription(productRequestDTO.getDescription());
        product.setPrice(productRequestDTO.getPrice());
        product.setVolume(productRequestDTO.getVolume());
        product.setUnit(productRequestDTO.getUnit());
        product.setImageUrl(productRequestDTO.getImageUrl());

        // Guardar canvis
        product = productRepository.save(product);

        // Retornar el DTO de resposta
        return mapToResponseDTO(product);
    }

    @Override
    @Transactional
    public ProductResponseDTO deactivateProduct(String uuid) {

        // Buscar el producte
        Product product = productRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("No s'ha trobat cap producte amb el UUID: " + uuid));

        // Marcar com a inactiu
        product.setIsActive(false);

        // Guardar canvis
        product = productRepository.save(product);

        // Retornar DTO
        return mapToResponseDTO(product);
    }

    @Override
    @Transactional
    public Page<ProductResponseDTO> listProductsByCompany(Pageable pageable){

        // Assignar company i usuari
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuari no trobat: " + username));
        Long companyId = user.getCompany().getId();

        Page<Product> products = productRepository.findProductsByCompanyId(companyId, pageable);

        return products.map(this::mapToResponseDTO);


    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> searchProducts(ProductSearchDTO dto, Pageable pageable){

        Page<Product> products = null;

        if (dto.getSupplierUuid() != null && !dto.getSupplierUuid().isBlank()) {
            // Proveïdor especificat - Validar que el proveïdor existeix
            Supplier supplier = supplierRepository.findByUuid(dto.getSupplierUuid())
                    .orElseThrow(() -> new IllegalArgumentException("El proveïdor especificat no existeix."));
            Long supplierId = supplier.getId();
            products = productRepository.searchProductsBySupplierId(supplierId, dto.getSearchText(), pageable);
        }else{
            // Proveïdor no especificat - cercar company de l'usuari.
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuari no trobat: " + username));
            Long companyId = user.getCompany().getId();
            products = productRepository.searchProductsByCompanyId(companyId, dto.getSearchText(), pageable);
        }
        return products.map(this::mapToResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> filterProducts(ProductFilterDTO dto, Pageable pageable){

        Page<Product> products = null;

        // Si isActive ve nul o buit per defecte mostrem només els actius
        Boolean isActive = dto.getIsActive();
        if (isActive == null) { isActive = true; }

        if (dto.getSupplierUuid() != null && !dto.getSupplierUuid().isBlank()) {
            // Proveïdor especificat - Validar que el proveïdor existeix
            Supplier supplier = supplierRepository.findByUuid(dto.getSupplierUuid())
                    .orElseThrow(() -> new IllegalArgumentException("El proveïdor especificat no existeix."));
            Long supplierId = supplier.getId();
            products = productRepository.filterProductsBySupplierId(supplierId,
                    dto.getName(),
                    dto.getDescription(),
                    dto.getCategory(),
                    dto.getVolume(),
                    dto.getUnit(),
                    dto.getMinPrice(),
                    dto.getMaxPrice(),
                    isActive, pageable);
        }else{
            // Proveïdor no especificat - cercar company de l'usuari.
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuari no trobat: " + username));
            Long companyId = user.getCompany().getId();
            products = productRepository.filterProductsByCompanyId(companyId,
                    dto.getName(),
                    dto.getDescription(),
                    dto.getCategory(),
                    dto.getVolume(),
                    dto.getUnit(),
                    dto.getMinPrice(),
                    dto.getMaxPrice(),
                    isActive, pageable);
        }
        return products.map(this::mapToResponseDTO);

    }

    @Override
    @Transactional
    public String saveProductImage(String productUuid, MultipartFile file) {

        // Validacions bàsiques de l'arxiu
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No s'ha rebut cap imatge.");
        }
        if (!file.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("Només es permeten fitxers d’imatge.");
        }
        if (file.getSize() > 5_000_000) { // 5 MB
            throw new IllegalArgumentException("La imatge no pot superar els 5 MB.");
        }

        // Cercar el producte per UUid
        Product product = productRepository.findByUuid(productUuid)
                .orElseThrow(() -> new IllegalArgumentException("El producte especificat no existeix."));

        try {
            // Crear directori si no existeix
            String uploadDir = "img/productes/";
            Files.createDirectories(Paths.get(uploadDir));

            // Nom únic
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filepath = Paths.get(uploadDir, filename);

            // Guardar imatge
            Files.copy(file.getInputStream(), filepath, StandardCopyOption.REPLACE_EXISTING);

            // Guardar ruta en la BD
            String url = "/img/productes/" + filename;
            product.setImageUrl(url);
            productRepository.save(product);

            return url;

        } catch (IOException e) {
            throw new RuntimeException("Error al pujar la imatge: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PriceComparisonDTO> comparePrices(String productName, int days) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuari no trobat: " + username));
        Long companyId = user.getCompany().getId();

        List<Product> products = productRepository.findActiveByCompanyIdAndNameContaining(companyId, productName);
        if (products.isEmpty()) {
            return List.of();
        }

        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<Order.OrderStatus> activeStatuses = List.of(
                Order.OrderStatus.PENDING,
                Order.OrderStatus.SENT,
                Order.OrderStatus.CONFIRMED,
                Order.OrderStatus.COMPLETED
        );

        List<PriceComparisonDTO> results = products.stream()
                .map(product -> {
                    List<OrderItem> items = orderItemRepository
                            .findByProductIdAndOrderCreatedAfterAndActiveStatus(product.getId(), since, activeStatuses);

                    BigDecimal avgOrderPrice = BigDecimal.ZERO;
                    BigDecimal minOrderPrice = null;
                    BigDecimal maxOrderPrice = null;
                    int orderCount = 0;
                    BigDecimal priceChangePercent = BigDecimal.ZERO;
                    String trend = "STABLE";

                    if (!items.isEmpty()) {
                        orderCount = (int) items.stream()
                                .map(i -> i.getOrder().getId())
                                .distinct()
                                .count();

                        BigDecimal sum = items.stream()
                                .map(OrderItem::getUnitPrice)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        avgOrderPrice = sum.divide(BigDecimal.valueOf(items.size()), 2, RoundingMode.HALF_UP);

                        minOrderPrice = items.stream()
                                .map(OrderItem::getUnitPrice)
                                .min(Comparator.naturalOrder())
                                .orElse(BigDecimal.ZERO);

                        maxOrderPrice = items.stream()
                                .map(OrderItem::getUnitPrice)
                                .max(Comparator.naturalOrder())
                                .orElse(BigDecimal.ZERO);

                        BigDecimal oldestPrice = items.get(0).getUnitPrice();
                        BigDecimal latestPrice = items.get(items.size() - 1).getUnitPrice();

                        if (oldestPrice.compareTo(BigDecimal.ZERO) > 0) {
                            priceChangePercent = latestPrice.subtract(oldestPrice)
                                    .divide(oldestPrice, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100))
                                    .setScale(2, RoundingMode.HALF_UP);

                            if (priceChangePercent.compareTo(new BigDecimal("2")) > 0) {
                                trend = "UP";
                            } else if (priceChangePercent.compareTo(new BigDecimal("-2")) < 0) {
                                trend = "DOWN";
                            }
                        }
                    }

                    Supplier supplier = product.getSupplier();

                    return PriceComparisonDTO.builder()
                            .productUuid(product.getUuid())
                            .productName(product.getName())
                            .category(product.getCategory())
                            .unit(product.getUnit())
                            .volume(product.getVolume())
                            .supplierUuid(supplier != null ? supplier.getUuid() : null)
                            .supplierName(supplier != null ? supplier.getName() : null)
                            .supplierEmail(supplier != null ? supplier.getEmail() : null)
                            .supplierPhone(supplier != null ? supplier.getPhone() : null)
                            .currentPrice(product.getPrice())
                            .avgOrderPrice(avgOrderPrice)
                            .minOrderPrice(minOrderPrice)
                            .maxOrderPrice(maxOrderPrice)
                            .orderCount(orderCount)
                            .priceChangePercent(priceChangePercent)
                            .trend(trend)
                            .build();
                })
                .sorted(Comparator.comparing(PriceComparisonDTO::getCurrentPrice))
                .collect(Collectors.toList());

        results.get(0).setIsCheapest(true);

        return results;
    }

    private ProductResponseDTO mapToResponseDTO(Product product) {
        return ProductResponseDTO.builder()
                .uuid(product.getUuid())
                .supplier(ProductSupplierResponseDTO.builder()
                        .uuid(product.getSupplier().getUuid())
                        .name(product.getSupplier().getName())
                        .build())
                .name(product.getName())
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