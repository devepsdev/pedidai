package com.pedidai.api.services.impl;

import com.pedidai.api.dto.InvoiceConfirmRequestDTO;
import com.pedidai.api.dto.InvoiceProductConfirmDTO;
import com.pedidai.api.entities.Company;
import com.pedidai.api.entities.PriceHistory;
import com.pedidai.api.entities.Product;
import com.pedidai.api.entities.Supplier;
import com.pedidai.api.exceptions.BadRequestException;
import com.pedidai.api.exceptions.ResourceNotFoundException;
import com.pedidai.api.repositories.ProductRepository;
import com.pedidai.api.repositories.SupplierRepository;
import com.pedidai.api.security.CurrentUser;
import com.pedidai.api.security.RateLimiter;
import com.pedidai.api.services.AiVisionService;
import com.pedidai.api.services.PriceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvoiceServiceImpl: confirmació d'albarans")
class InvoiceServiceImplTest {

    @Mock private AiVisionService aiVisionService;
    @Mock private SupplierRepository supplierRepository;
    @Mock private ProductRepository productRepository;
    @Mock private PriceService priceService;
    @Mock private CurrentUser currentUser;

    private InvoiceServiceImpl service;
    private Company company;

    @BeforeEach
    void setUp() {
        service = new InvoiceServiceImpl(aiVisionService, supplierRepository, productRepository, priceService,
                currentUser, new RateLimiter());
        company = Company.builder().id(1L).uuid("c1").build();
        lenient().when(currentUser.company()).thenReturn(company);
    }

    private InvoiceProductConfirmDTO line(String name, String generic, String price, String matched) {
        return InvoiceProductConfirmDTO.builder().name(name).genericName(generic).unit("kg")
                .quantity(BigDecimal.TEN).unitPrice(new BigDecimal(price)).matchedProductUuid(matched).build();
    }

    @Test
    @DisplayName("si el proveïdor no existeix es crea, i cada línia va a l'historial amb la data de l'albarà")
    void createsSupplierAndRecordsPrices() {
        when(supplierRepository.findActiveByCompanyIdAndNameContaining(eq(1L), anyString())).thenReturn(List.of());
        when(supplierRepository.save(any())).thenAnswer(i -> { Supplier s = i.getArgument(0); s.setId(9L); s.setUuid("s-new"); return s; });
        when(productRepository.findBySupplier_IdAndIsActiveTrue(9L)).thenReturn(List.of());
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = service.confirmInvoice(InvoiceConfirmRequestDTO.builder()
                .newSupplier(InvoiceConfirmRequestDTO.NewSupplier.builder().name("Fruites del Maresme S.L.").build())
                .invoiceDate("2026-09-23").invoiceNumber("2026/1543")
                .products(List.of(line("TOMAQUET PERA", "Tomate Pera", "1.55", null))).build());

        ArgumentCaptor<Supplier> supplier = ArgumentCaptor.forClass(Supplier.class);
        verify(supplierRepository).save(supplier.capture());
        assertThat(supplier.getValue().getCompany()).isSameAs(company);
        ArgumentCaptor<Product> product = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(product.capture());
        assertThat(product.getValue().getCanonicalName()).isEqualTo("tomate pera");
        verify(priceService).recordObservation(any(), eq(new BigDecimal("1.55")), eq("kg"), eq(BigDecimal.TEN),
                eq(LocalDate.of(2026, 9, 23)), eq(PriceHistory.Source.INVOICE), eq("2026/1543"));
        assertThat(result.getProductsCreated()).isEqualTo(1);
        assertThat(result.getPricesRecorded()).isEqualTo(1);
    }

    @Test
    @DisplayName("reutilitza un proveïdor existent encara que el nom tingui una altra forma jurídica")
    void reusesSupplierIgnoringLegalForm() {
        Supplier existing = Supplier.builder().id(9L).uuid("s1").name("Fruites del Maresme SL").company(company).build();
        when(supplierRepository.findActiveByCompanyIdAndNameContaining(1L, "fruites")).thenReturn(List.of(existing));
        when(productRepository.findBySupplier_IdAndIsActiveTrue(9L)).thenReturn(new ArrayList<>());
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = service.confirmInvoice(InvoiceConfirmRequestDTO.builder()
                .newSupplier(InvoiceConfirmRequestDTO.NewSupplier.builder().name("FRUITES DEL MARESME, S.L.").build())
                .products(List.of(line("PATATA", "patata", "0.70", null))).build());

        verify(supplierRepository, never()).save(any());
        assertThat(result.getMatchedSupplierUuid()).isEqualTo("s1");
    }

    @Test
    @DisplayName("un producte indicat per UUID que no és d'aquest proveïdor no es modifica")
    void ignoresForeignProductUuid() {
        Supplier supplier = Supplier.builder().id(9L).uuid("s1").name("Fruites").company(company).build();
        when(supplierRepository.findByUuidAndCompany_Id("s1", 1L)).thenReturn(Optional.of(supplier));
        when(productRepository.findBySupplier_IdAndIsActiveTrue(9L)).thenReturn(new ArrayList<>());
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.confirmInvoice(InvoiceConfirmRequestDTO.builder().supplierUuid("s1")
                .products(List.of(line("PATATA", "patata", "0.70", "producte-d-una-altra-empresa"))).build());

        ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(saved.capture());
        assertThat(saved.getValue().getSupplier()).isSameAs(supplier);
    }

    @Test
    @DisplayName("un proveïdor d'una altra empresa no es pot fer servir")
    void foreignSupplierRejected() {
        when(supplierRepository.findByUuidAndCompany_Id("s-altre", 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmInvoice(InvoiceConfirmRequestDTO.builder().supplierUuid("s-altre")
                .products(List.of(line("X", "x", "1", null))).build())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("una data futura o il·legible es substitueix per la d'avui")
    void futureDateBecomesToday() {
        Supplier supplier = Supplier.builder().id(9L).uuid("s1").name("Fruites").company(company).build();
        when(supplierRepository.findByUuidAndCompany_Id("s1", 1L)).thenReturn(Optional.of(supplier));
        when(productRepository.findBySupplier_IdAndIsActiveTrue(9L)).thenReturn(new ArrayList<>());
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.confirmInvoice(InvoiceConfirmRequestDTO.builder().supplierUuid("s1").invoiceDate("2099-01-01")
                .products(List.of(line("X", "x", "1", null))).build());

        verify(priceService).recordObservation(any(), any(), any(), any(), eq(LocalDate.now()), any(), any());
    }

    @Test
    @DisplayName("només s'accepten JPG, PNG, WebP o PDF reals")
    void rejectsOtherFiles() {
        var html = new MockMultipartFile("image", "albara.png", "image/png", "<html></html>".getBytes());

        assertThatThrownBy(() -> service.scanInvoice(html, null))
                .isInstanceOf(BadRequestException.class).hasMessage("error.invoice.invalidFormat");
        verifyNoInteractions(aiVisionService);
    }
}
