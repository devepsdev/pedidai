package com.pedidai.api.services;

import com.pedidai.api.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {

    ProductResponseDTO createProduct(ProductRequestDTO productRequestDTO);

    ProductResponseDTO getProductByUuid(String uuid);

    ProductResponseDTO updateProduct(String uuid, ProductRequestDTO productRequestDTO);

    ProductResponseDTO deactivateProduct(String uuid);

    Page<ProductResponseDTO> listProductsByCompany(Pageable pageable);

    Page<ProductResponseDTO> searchProducts(ProductSearchDTO dto, Pageable pageable);

    Page<ProductResponseDTO> filterProducts(ProductFilterDTO dto, Pageable pageable);

    String saveProductImage(String productUuid, MultipartFile file);

    /** Valida i desa una imatge (JPEG, PNG o WebP, màx. 5 MB) i en retorna la URL pública. */
    String storeImage(MultipartFile file);

    /** Comparativa de preus entre proveïdors d'un producte, a partir de l'historial d'albarans. */
    List<PriceGroupDTO> comparePrices(String productName, int days);
}
