package com.pedidai.api.services;

import com.pedidai.api.dto.SupplierRequestDTO;
import com.pedidai.api.dto.SupplierResponseDTO;
import com.pedidai.api.dto.SupplierFilterDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SupplierService {

    SupplierResponseDTO createSupplier(SupplierRequestDTO supplierRequestDTO);

    SupplierResponseDTO getSupplierByUuid(String uuid);

    SupplierResponseDTO updateSupplier(String uuid, SupplierRequestDTO supplierRequestDTO);

    SupplierResponseDTO toggleSupplierStatus(String uuid, Boolean isActive);

    Page<SupplierResponseDTO> getAllSuppliersPaginated(Pageable pageable);

    Page<SupplierResponseDTO> searchSuppliersByText(String text, Pageable pageable);

    Page<SupplierResponseDTO> searchSuppliersWithFilters(SupplierFilterDTO filterDTO, Pageable pageable);

}