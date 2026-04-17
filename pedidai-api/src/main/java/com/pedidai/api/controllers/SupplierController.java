package com.pedidai.api.controllers;

import com.pedidai.api.dto.*;
import com.pedidai.api.services.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<SupplierResponseDTO>>> getAllSuppliers(
            @Valid SupplierSearchDTO searchDTO) {
        Sort sort = searchDTO.getSortDir().equalsIgnoreCase("desc") ?
                Sort.by(searchDTO.getSortBy()).descending() :
                Sort.by(searchDTO.getSortBy()).ascending();

        Pageable pageable = PageRequest.of(searchDTO.getPage(), searchDTO.getSize(), sort);

        Page<SupplierResponseDTO> suppliers = supplierService.getAllSuppliersPaginated(pageable);
        // Convertir Page a PagedResponseDTO per evitar warning de serialització
        PagedResponseDTO<SupplierResponseDTO> pagedResponse = PagedResponseDTO.of(suppliers);

        return ResponseEntity.ok(
                ApiResponseDTO.success(pagedResponse, "Proveïdors de l'empresa obtinguts correctament"));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponseDTO<SupplierResponseDTO>> getSupplierByUuid(
            @PathVariable @NotBlank(message = "L'UUID no pot estar buit") String uuid) {
        SupplierResponseDTO supplier = supplierService.getSupplierByUuid(uuid);
        return ResponseEntity.ok(
                ApiResponseDTO.success(supplier, "Proveïdor obtingut correctament"));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<SupplierResponseDTO>>> searchSuppliersByText(
            @Valid SupplierSearchDTO searchDTO) {

        Sort sort = searchDTO.getSortDir().equalsIgnoreCase("desc") ?
                Sort.by(searchDTO.getSortBy()).descending() :
                Sort.by(searchDTO.getSortBy()).ascending();

        Pageable pageable = PageRequest.of(searchDTO.getPage(), searchDTO.getSize(), sort);

        Page<SupplierResponseDTO> suppliers = supplierService.searchSuppliersByText(
                searchDTO.getSearchText(), pageable);

        // Convertir Page a PagedResponseDTO per evitar warning de serialització
        PagedResponseDTO<SupplierResponseDTO> pagedResponse = PagedResponseDTO.of(suppliers);

        return ResponseEntity.ok(
                ApiResponseDTO.success(pagedResponse, "Cerca bàsica de proveïdors completada"));
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<SupplierResponseDTO>>> filterSuppliers(
            @Valid SupplierFilterDTO filterDTO) {

        Sort sort = filterDTO.getSortDir().equalsIgnoreCase("desc") ?
                Sort.by(filterDTO.getSortBy()).descending() :
                Sort.by(filterDTO.getSortBy()).ascending();

        Pageable pageable = PageRequest.of(filterDTO.getPage(), filterDTO.getSize(), sort);

        Page<SupplierResponseDTO> suppliers = supplierService.searchSuppliersWithFilters(
                filterDTO, pageable);

        // Convertir Page a PagedResponseDTO per evitar warning de serialització
        PagedResponseDTO<SupplierResponseDTO> pagedResponse = PagedResponseDTO.of(suppliers);

        String message = String.format("Cerca avançada completada. Filtres aplicats: text=%s",
                filterDTO.hasTextFilters());

        return ResponseEntity.ok(
                ApiResponseDTO.success(pagedResponse, message));
    }

    @PostMapping
    public ResponseEntity<ApiResponseDTO<SupplierResponseDTO>> createSupplier(
            @Valid @RequestBody SupplierRequestDTO supplierRequestDTO) {
        SupplierResponseDTO createdSupplier = supplierService.createSupplier(supplierRequestDTO);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(createdSupplier, "Proveïdor creat correctament"));
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<ApiResponseDTO<SupplierResponseDTO>> updateSupplier(
            @PathVariable @NotBlank(message = "L'UUID no pot estar buit") String uuid,
            @Valid @RequestBody SupplierRequestDTO supplierRequestDTO) {
        SupplierResponseDTO updatedSupplier = supplierService.updateSupplier(uuid, supplierRequestDTO);
        return ResponseEntity.ok(
                ApiResponseDTO.success(updatedSupplier, "Proveïdor actualitzat correctament"));
    }

    @PatchMapping("/{uuid}/status")
    public ResponseEntity<ApiResponseDTO<SupplierResponseDTO>> toggleSupplierStatus(
            @PathVariable @NotBlank(message = "L'UUID no pot estar buit") String uuid,
            @RequestParam Boolean isActive) {
        SupplierResponseDTO updatedSupplier = supplierService.toggleSupplierStatus(uuid, isActive);
        return ResponseEntity.ok(
                ApiResponseDTO.success(updatedSupplier, "Estat del proveïdor actualitzat correctament"));
    }

}