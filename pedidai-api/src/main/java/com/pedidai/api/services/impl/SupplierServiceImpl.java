package com.pedidai.api.services.impl;

import com.pedidai.api.dto.SupplierFilterDTO;
import com.pedidai.api.dto.SupplierRequestDTO;
import com.pedidai.api.dto.SupplierResponseDTO;
import com.pedidai.api.entities.Company;
import com.pedidai.api.entities.Supplier;
import com.pedidai.api.exceptions.DuplicateResourceException;
import com.pedidai.api.exceptions.ResourceNotFoundException;
import com.pedidai.api.repositories.SupplierRepository;
import com.pedidai.api.security.CurrentUser;
import com.pedidai.api.services.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final CurrentUser currentUser;

    @Override
    public SupplierResponseDTO createSupplier(SupplierRequestDTO dto) {
        Company company = currentUser.company();

        if (supplierRepository.existsByCompanyUuidAndNameIgnoreCase(company.getUuid(), dto.getName())) {
            throw new DuplicateResourceException("error.supplier.nameExists", dto.getName());
        }

        Supplier supplier = Supplier.builder()
                .company(company)
                .name(dto.getName().trim())
                .contactName(dto.getContactName())
                .email(blankToNull(dto.getEmail()))
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .notes(dto.getNotes())
                .isActive(dto.getIsActive() == null || dto.getIsActive())
                .build();

        return mapToResponseDTO(supplierRepository.save(supplier));
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponseDTO getSupplierByUuid(String uuid) {
        return mapToResponseDTO(findOwned(uuid));
    }

    @Override
    public SupplierResponseDTO updateSupplier(String uuid, SupplierRequestDTO dto) {
        Supplier supplier = findOwned(uuid);
        String companyUuid = supplier.getCompany().getUuid();

        if (!supplier.getName().equalsIgnoreCase(dto.getName())
                && supplierRepository.existsByCompanyUuidAndNameIgnoreCaseAndUuidNot(companyUuid, dto.getName(), uuid)) {
            throw new DuplicateResourceException("error.supplier.nameExists", dto.getName());
        }

        supplier.setName(dto.getName().trim());
        supplier.setContactName(dto.getContactName());
        supplier.setEmail(blankToNull(dto.getEmail()));
        supplier.setPhone(dto.getPhone());
        supplier.setAddress(dto.getAddress());
        supplier.setNotes(dto.getNotes());
        if (dto.getIsActive() != null) {
            supplier.setIsActive(dto.getIsActive());
        }

        return mapToResponseDTO(supplierRepository.save(supplier));
    }

    @Override
    public SupplierResponseDTO toggleSupplierStatus(String uuid, Boolean isActive) {
        Supplier supplier = findOwned(uuid);
        supplier.setIsActive(isActive);
        return mapToResponseDTO(supplierRepository.save(supplier));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupplierResponseDTO> getAllSuppliersPaginated(Pageable pageable) {
        return supplierRepository.findByCompanyUuidAndIsActiveTrue(currentUser.company().getUuid(), pageable)
                .map(this::mapToResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupplierResponseDTO> searchSuppliersByText(String searchText, Pageable pageable) {
        return supplierRepository.findByCompanyIdAndMultipleFieldsContainingActive(
                currentUser.companyId(), searchText, pageable).map(this::mapToResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupplierResponseDTO> searchSuppliersWithFilters(SupplierFilterDTO filterDTO, Pageable pageable) {
        return supplierRepository.findByCompanyIdAndCriteriaActive(
                currentUser.companyId(),
                filterDTO.getName(),
                filterDTO.getContactName(),
                filterDTO.getEmail(),
                filterDTO.getPhone(),
                filterDTO.getAddress(),
                pageable
        ).map(this::mapToResponseDTO);
    }

    /** Proveïdor de l'empresa de l'usuari; si és d'una altra empresa es tracta com a inexistent. */
    private Supplier findOwned(String uuid) {
        return supplierRepository.findByUuidAndCompany_Id(uuid, currentUser.companyId())
                .orElseThrow(() -> new ResourceNotFoundException("error.supplier.notFound"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private SupplierResponseDTO mapToResponseDTO(Supplier supplier) {
        return SupplierResponseDTO.builder()
                .uuid(supplier.getUuid())
                .companyUuid(supplier.getCompany().getUuid())
                .companyName(supplier.getCompany().getName())
                .name(supplier.getName())
                .contactName(supplier.getContactName())
                .email(supplier.getEmail())
                .phone(supplier.getPhone())
                .address(supplier.getAddress())
                .notes(supplier.getNotes())
                .isActive(supplier.getIsActive())
                .createdAt(supplier.getCreatedAt())
                .updatedAt(supplier.getUpdatedAt())
                .build();
    }
}
