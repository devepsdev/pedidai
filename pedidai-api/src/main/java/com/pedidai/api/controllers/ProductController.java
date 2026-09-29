package com.pedidai.api.controllers;

import com.pedidai.api.dto.*;
import com.pedidai.api.services.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponseDTO<ProductResponseDTO>> createProduct(
            @Valid @RequestBody ProductRequestDTO productRequestDTO) {
        ProductResponseDTO createdProduct = productService.createProduct(productRequestDTO);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(createdProduct, "success.product.created"));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponseDTO<ProductResponseDTO>> getProductByUuid(
            @PathVariable @NotBlank(message = "{validation.uuid.required}") String uuid) {
        ProductResponseDTO product = productService.getProductByUuid(uuid);
        return ResponseEntity.ok(
                ApiResponseDTO.success(product, "success.ok"));
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<ApiResponseDTO<ProductResponseDTO>> updateProduct(
            @PathVariable @NotBlank(message = "{validation.uuid.required}") String uuid,
            @Valid @RequestBody ProductRequestDTO productRequestDTO) {
        ProductResponseDTO updatedProduct = productService.updateProduct(uuid, productRequestDTO);
        return ResponseEntity.ok(
                ApiResponseDTO.success(updatedProduct, "success.product.updated"));
    }

    @PatchMapping("/deactivate/{uuid}")
    public ResponseEntity<ApiResponseDTO<ProductResponseDTO>> deactivateProduct(
            @PathVariable @NotBlank(message = "{validation.uuid.required}") String uuid) {
        ProductResponseDTO deactivatedProduct = productService.deactivateProduct(uuid);
        return ResponseEntity.ok(
                ApiResponseDTO.success(deactivatedProduct, "success.product.deleted"));
    }

    @GetMapping
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<ProductResponseDTO>>> listProductsByCompany(@Valid ProductSearchDTO searchDTO) {

        // Crear Sort segons el DTO
        Sort sort = searchDTO.getSortDir().equalsIgnoreCase("desc") ?
                Sort.by(searchDTO.getSortBy()).descending() :
                Sort.by(searchDTO.getSortBy()).ascending();

        // Crear Pageable segons el DTO
        Pageable pageable = Pages.of(
                searchDTO.getPage(),
                searchDTO.getSize(),
                sort
        );

        // Crida al servei
        Page<ProductResponseDTO> products = productService.listProductsByCompany(pageable);

        // Convertir Page a PagedResponseDTO
        PagedResponseDTO<ProductResponseDTO> pagedResponse = PagedResponseDTO.of(products);

        return ResponseEntity.ok(
                ApiResponseDTO.success(pagedResponse, "success.ok"));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<ProductResponseDTO>>> searchProducts(@Valid ProductSearchDTO searchDTO) {

        Sort sort = searchDTO.getSortDir().equalsIgnoreCase("desc") ?
                Sort.by(searchDTO.getSortBy()).descending() :
                Sort.by(searchDTO.getSortBy()).ascending();

        Pageable pageable = Pages.of(searchDTO.getPage(), searchDTO.getSize(), sort);

        Page<ProductResponseDTO> products = productService.searchProducts(searchDTO, pageable);

        // Convertir Page a PagedResponseDTO per evitar warning de serialització
        PagedResponseDTO<ProductResponseDTO> pagedResponse = PagedResponseDTO.of(products);

        return ResponseEntity.ok(ApiResponseDTO.success(pagedResponse, "success.search.done"));
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<ProductResponseDTO>>> filterProducts(@Valid ProductFilterDTO filterDTO) {

        Sort sort = filterDTO.getSortDir().equalsIgnoreCase("desc") ?
                Sort.by(filterDTO.getSortBy()).descending() :
                Sort.by(filterDTO.getSortBy()).ascending();

        Pageable pageable = Pages.of(filterDTO.getPage(), filterDTO.getSize(), sort);

        Page<ProductResponseDTO> products = productService.filterProducts(filterDTO, pageable);

        // Convertir Page a PagedResponseDTO per evitar warning de serialització
        PagedResponseDTO<ProductResponseDTO> pagedResponse = PagedResponseDTO.of(products);

        String message = "success.search.done";
        return ResponseEntity.ok(ApiResponseDTO.success(pagedResponse, message));
    }

    @PostMapping("/upload/{productUuid}")
    public ResponseEntity<ApiResponseDTO<String>> uploadProductImage(@PathVariable String productUuid, @RequestParam("image") MultipartFile file) {

        String imageUrl = productService.saveProductImage(productUuid, file);

        return ResponseEntity.ok(
                ApiResponseDTO.success(imageUrl, "success.image.uploaded")
        );
    }

    /** Comparativa de preus entre proveïdors (a partir dels albarans) dels productes que coincideixen amb el nom. */
    @GetMapping("/compare-prices")
    public ResponseEntity<ApiResponseDTO<List<PriceGroupDTO>>> comparePrices(
            @RequestParam String productName,
            @RequestParam(defaultValue = "90") int days) {
        return ResponseEntity.ok(ApiResponseDTO.success(productService.comparePrices(productName, days), "success.ok"));
    }

    @PostMapping("/upload-temp")
    public ResponseEntity<ApiResponseDTO<String>> uploadTempImage(@RequestParam("image") MultipartFile file) {
        return ResponseEntity.ok(ApiResponseDTO.success(productService.storeImage(file), "success.image.uploaded"));
    }
}
