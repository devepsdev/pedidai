package com.pedidai.api.services.impl;

import com.pedidai.api.dto.SupplierRequestDTO;
import com.pedidai.api.entities.Company;
import com.pedidai.api.entities.Supplier;
import com.pedidai.api.exceptions.DuplicateResourceException;
import com.pedidai.api.exceptions.ResourceNotFoundException;
import com.pedidai.api.repositories.SupplierRepository;
import com.pedidai.api.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SupplierServiceImpl: proveïdors només de l'empresa de l'usuari")
class SupplierServiceImplTest {

    @Mock private SupplierRepository supplierRepository;
    @Mock private CurrentUser currentUser;
    @InjectMocks private SupplierServiceImpl service;

    private Company company;

    @BeforeEach
    void setUp() {
        company = Company.builder().id(1L).uuid("c1").name("Bar").build();
        lenient().when(currentUser.company()).thenReturn(company);
        lenient().when(currentUser.companyId()).thenReturn(1L);
    }

    @Test
    @DisplayName("crea el proveïdor a l'empresa de l'usuari")
    void createsInOwnCompany() {
        when(supplierRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var dto = service.createSupplier(SupplierRequestDTO.builder().name(" Fruites ").email(" ").build());

        assertThat(dto.getCompanyUuid()).isEqualTo("c1");
        assertThat(dto.getName()).isEqualTo("Fruites");
        assertThat(dto.getEmail()).isNull();
    }

    @Test
    @DisplayName("no es pot duplicar el nom d'un proveïdor")
    void duplicateName() {
        when(supplierRepository.existsByCompanyUuidAndNameIgnoreCase("c1", "Fruites")).thenReturn(true);

        assertThatThrownBy(() -> service.createSupplier(SupplierRequestDTO.builder().name("Fruites").build()))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("un proveïdor d'una altra empresa no es pot llegir, editar ni desactivar")
    void otherCompanySupplierIsInvisible() {
        when(supplierRepository.findByUuidAndCompany_Id("s-altre", 1L)).thenReturn(Optional.empty());
        SupplierRequestDTO dto = SupplierRequestDTO.builder().name("Robat").build();

        assertThatThrownBy(() -> service.getSupplierByUuid("s-altre")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.updateSupplier("s-altre", dto)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.toggleSupplierStatus("s-altre", false)).isInstanceOf(ResourceNotFoundException.class);
        verify(supplierRepository, never()).save(any());
    }

    @Test
    @DisplayName("editar un proveïdor no el pot canviar d'empresa")
    void updateKeepsCompany() {
        Supplier supplier = Supplier.builder().id(2L).uuid("s1").name("Fruites").company(company).isActive(true).build();
        when(supplierRepository.findByUuidAndCompany_Id("s1", 1L)).thenReturn(Optional.of(supplier));
        when(supplierRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.updateSupplier("s1", SupplierRequestDTO.builder().name("Fruites Maresme").email("f@prov.test").build());

        assertThat(supplier.getCompany()).isSameAs(company);
        assertThat(supplier.getName()).isEqualTo("Fruites Maresme");
    }
}
