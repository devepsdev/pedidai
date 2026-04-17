package com.pedidai.api.controllers;

import com.pedidai.api.dto.*;
import com.pedidai.api.services.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

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
                .body(ApiResponseDTO.success(createdProduct, "Producte creat correctament"));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponseDTO<ProductResponseDTO>> getProductByUuid(
            @PathVariable @NotBlank(message = "L'UUID no pot estar buit") String uuid) {
        ProductResponseDTO product = productService.getProductByUuid(uuid);
        return ResponseEntity.ok(
                ApiResponseDTO.success(product, "Producte obtingut correctament"));
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<ApiResponseDTO<ProductResponseDTO>> updateProduct(
            @PathVariable @NotBlank(message = "L'UUID no pot estar buit") String uuid,
            @Valid @RequestBody ProductRequestDTO productRequestDTO) {
        ProductResponseDTO updatedProduct = productService.updateProduct(uuid, productRequestDTO);
        return ResponseEntity.ok(
                ApiResponseDTO.success(updatedProduct, "Producte actualitzat correctament"));
    }

    @PatchMapping("/deactivate/{uuid}")
    public ResponseEntity<ApiResponseDTO<ProductResponseDTO>> deactivateProduct(
            @PathVariable @NotBlank(message = "L'UUID no pot estar buit") String uuid) {
        ProductResponseDTO deactivatedProduct = productService.deactivateProduct(uuid);
        return ResponseEntity.ok(
                ApiResponseDTO.success(deactivatedProduct, "Producte eliminat correctament"));
    }

    @GetMapping
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<ProductResponseDTO>>> listProductsByCompany(@Valid ProductSearchDTO searchDTO) {

        // Crear Sort segons el DTO
        Sort sort = searchDTO.getSortDir().equalsIgnoreCase("desc") ?
                Sort.by(searchDTO.getSortBy()).descending() :
                Sort.by(searchDTO.getSortBy()).ascending();

        // Crear Pageable segons el DTO
        Pageable pageable = PageRequest.of(
                searchDTO.getPage(),
                searchDTO.getSize(),
                sort
        );

        // Crida al servei
        Page<ProductResponseDTO> products = productService.listProductsByCompany(pageable);

        // Convertir Page a PagedResponseDTO
        PagedResponseDTO<ProductResponseDTO> pagedResponse = PagedResponseDTO.of(products);

        return ResponseEntity.ok(
                ApiResponseDTO.success(pagedResponse, "Llistat de productes de la companyia completat"));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<ProductResponseDTO>>> searchProducts(@Valid ProductSearchDTO searchDTO) {

        Sort sort = searchDTO.getSortDir().equalsIgnoreCase("desc") ?
                Sort.by(searchDTO.getSortBy()).descending() :
                Sort.by(searchDTO.getSortBy()).ascending();

        Pageable pageable = PageRequest.of(searchDTO.getPage(), searchDTO.getSize(), sort);

        Page<ProductResponseDTO> products = productService.searchProducts(searchDTO, pageable);

        // Convertir Page a PagedResponseDTO per evitar warning de serialització
        PagedResponseDTO<ProductResponseDTO> pagedResponse = PagedResponseDTO.of(products);

        return ResponseEntity.ok(ApiResponseDTO.success(pagedResponse, "Cerca bàsica de productes completada"));
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<ProductResponseDTO>>> filterProducts(@Valid ProductFilterDTO filterDTO) {

        Sort sort = filterDTO.getSortDir().equalsIgnoreCase("desc") ?
                Sort.by(filterDTO.getSortBy()).descending() :
                Sort.by(filterDTO.getSortBy()).ascending();

        Pageable pageable = PageRequest.of(filterDTO.getPage(), filterDTO.getSize(), sort);

        Page<ProductResponseDTO> products = productService.filterProducts(filterDTO, pageable);

        // Convertir Page a PagedResponseDTO per evitar warning de serialització
        PagedResponseDTO<ProductResponseDTO> pagedResponse = PagedResponseDTO.of(products);

        String message = String.format("Cerca avançada completada. Filtres aplicats: text=%s", filterDTO.hasTextFilters());
        return ResponseEntity.ok(ApiResponseDTO.success(pagedResponse, message));
    }

    @PostMapping("/upload/{productUuid}")
    public ResponseEntity<ApiResponseDTO<String>> uploadProductImage(@PathVariable String productUuid, @RequestParam("image") MultipartFile file) {

        String imageUrl = productService.saveProductImage(productUuid, file);

        return ResponseEntity.ok(
                ApiResponseDTO.success(imageUrl, "Imatge pujada correctament")
        );
    }

    @GetMapping("/compare-prices")
    public ResponseEntity<ApiResponseDTO<List<PriceComparisonDTO>>> comparePrices(
            @RequestParam String productName,
            @RequestParam(defaultValue = "90") int days) {

        List<PriceComparisonDTO> result = productService.comparePrices(productName, days);
        String message = result.isEmpty()
                ? "No s'han trobat productes actius amb el nom '" + productName + "'"
                : "Comparativa de preus completada";
        return ResponseEntity.ok(ApiResponseDTO.success(result, message));
    }

    @PostMapping("/upload-temp")
    public ResponseEntity<ApiResponseDTO<String>> uploadTempImage(@RequestParam("image") MultipartFile file) {

        try {
            String uploadDir = "img/productes/";
            Files.createDirectories(Paths.get(uploadDir));

            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filepath = Paths.get(uploadDir, filename);

            Files.copy(file.getInputStream(), filepath, StandardCopyOption.REPLACE_EXISTING);

            String url = "/img/productes/" + filename;

            return ResponseEntity.ok(ApiResponseDTO.success(url, "Imatge pujada correctament"));

        } catch (IOException e) {
            throw new RuntimeException("Error al pujar la imatge: " + e.getMessage(), e);
        }
    }

}

