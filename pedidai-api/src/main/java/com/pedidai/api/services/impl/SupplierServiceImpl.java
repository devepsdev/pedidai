package com.pedidai.api.services.impl;

import com.pedidai.api.dto.SupplierFilterDTO;
import com.pedidai.api.dto.SupplierRequestDTO;
import com.pedidai.api.dto.SupplierResponseDTO;
import com.pedidai.api.entities.Company;
import com.pedidai.api.entities.Supplier;
import com.pedidai.api.entities.User;
import com.pedidai.api.exceptions.DuplicateResourceException;
import com.pedidai.api.exceptions.ResourceNotFoundException;
import com.pedidai.api.repositories.CompanyRepository;
import com.pedidai.api.repositories.SupplierRepository;
import com.pedidai.api.repositories.UserRepository;
import com.pedidai.api.services.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
@RequiredArgsConstructor
@Transactional
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    @Override
    public SupplierResponseDTO createSupplier(SupplierRequestDTO supplierRequestDTO) {
        String companyUuid = getCompanyUuidFromAuthenticatedUser();
        // Verificar que l'empresa existeix
        Company company = companyRepository.findByUuid(companyUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Empresa no trobada amb UUID: " + companyUuid));

        // Verificar que no existeix un proveïdor amb el mateix nom a l'empresa
        if (supplierRepository.existsByCompanyUuidAndNameIgnoreCase(
                companyUuid, supplierRequestDTO.getName())) {
            throw new DuplicateResourceException(
                    "Ja existeix un proveïdor amb el nom '" + supplierRequestDTO.getName() +
                            "' a l'empresa especificada");
        }

        // Crear el proveïdor
        Supplier supplier = Supplier.builder()
                .company(company)
                .name(supplierRequestDTO.getName())
                .contactName(supplierRequestDTO.getContactName())
                .email(supplierRequestDTO.getEmail())
                .phone(supplierRequestDTO.getPhone())
                .address(supplierRequestDTO.getAddress())
                .notes(supplierRequestDTO.getNotes())
                .isActive(supplierRequestDTO.getIsActive())
                .build();

        Supplier savedSupplier = supplierRepository.save(supplier);

        return mapToResponseDTO(savedSupplier);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponseDTO getSupplierByUuid(String uuid) {
        Supplier supplier = supplierRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Proveïdor no trobat amb UUID: " + uuid));
        return mapToResponseDTO(supplier);
    }

    @Override
    public SupplierResponseDTO updateSupplier(String uuid, SupplierRequestDTO supplierRequestDTO) {
        Supplier existingSupplier = supplierRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Proveïdor no trobat amb UUID: " + uuid));
        String companyUuid = getCompanyUuidFromAuthenticatedUser();
        // Verificar que l'empresa existeix si s'ha canviat
        if (!existingSupplier.getCompany().getUuid().equals(companyUuid)) {
            Company company = companyRepository.findByUuid(companyUuid)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Empresa no trobada amb UUID: " + companyUuid));
            existingSupplier.setCompany(company);
        }

        // Verificar duplicats de nom si s'ha canviat el nom
        if (!existingSupplier.getName().equalsIgnoreCase(supplierRequestDTO.getName())) {
            if (supplierRepository.existsByCompanyUuidAndNameIgnoreCaseAndUuidNot(
                    companyUuid, supplierRequestDTO.getName(), uuid)) {
                throw new DuplicateResourceException(
                        "Ja existeix un proveïdor amb el nom '" + supplierRequestDTO.getName() +
                                "' a l'empresa especificada");
            }
        }

        // Actualitzar les dades
        existingSupplier.setName(supplierRequestDTO.getName());
        existingSupplier.setContactName(supplierRequestDTO.getContactName());
        existingSupplier.setEmail(supplierRequestDTO.getEmail());
        existingSupplier.setPhone(supplierRequestDTO.getPhone());
        existingSupplier.setAddress(supplierRequestDTO.getAddress());
        existingSupplier.setNotes(supplierRequestDTO.getNotes());
        existingSupplier.setIsActive(supplierRequestDTO.getIsActive());

        Supplier updatedSupplier = supplierRepository.save(existingSupplier);

        return mapToResponseDTO(updatedSupplier);
    }

    @Override
    public SupplierResponseDTO toggleSupplierStatus(String uuid, Boolean isActive) {
        Supplier supplier = supplierRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Proveïdor no trobat amb UUID: " + uuid));

        supplier.setIsActive(isActive);
        Supplier updatedSupplier = supplierRepository.save(supplier);

        return mapToResponseDTO(updatedSupplier);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupplierResponseDTO> getAllSuppliersPaginated(Pageable pageable) {
        String companyUuid = getCompanyUuidFromAuthenticatedUser();

        // Mètode automàtic de Spring Data
        Page<Supplier> suppliersPage = supplierRepository.findByCompanyUuidAndIsActiveTrue(
                companyUuid, pageable);

        return suppliersPage.map(this::mapToResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupplierResponseDTO> searchSuppliersByText(String searchText, Pageable pageable) {
        String companyUuid = getCompanyUuidFromAuthenticatedUser();

        // Verificar que l'empresa existeix
        Company company = companyRepository.findByUuid(companyUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no trobada amb UUID: " + companyUuid));

        // Mètode de cerca en múltiples camps que filtra només actius
        Page<Supplier> suppliers = supplierRepository.findByCompanyIdAndMultipleFieldsContainingActive(
                company.getId(), searchText, pageable);

        return suppliers.map(this::mapToResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupplierResponseDTO> searchSuppliersWithFilters(SupplierFilterDTO filterDTO, Pageable pageable) {
        String companyUuid = getCompanyUuidFromAuthenticatedUser();

        // Verificar que l'empresa existeix
        Company company = companyRepository.findByUuid(companyUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no trobada amb UUID: " + companyUuid));

        // Usar el mètode del repositori amb tots els filtres expandits
        Page<Supplier> suppliers = supplierRepository.findByCompanyIdAndCriteriaActive(
                company.getId(),
                filterDTO.getName(),
                filterDTO.getContactName(),
                filterDTO.getEmail(),
                filterDTO.getPhone(),
                filterDTO.getAddress(),
                pageable
        );

        return suppliers.map(this::mapToResponseDTO);
    }

    private String getCompanyUuidFromAuthenticatedUser() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuari no trobat: " + username));

        if (user.getCompany() == null || user.getCompany().getUuid() == null) {
            throw new ResourceNotFoundException("L'usuari no té empresa assignada");
        }

        return user.getCompany().getUuid();
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