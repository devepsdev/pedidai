package com.pedidai.api.services.impl;

import com.pedidai.api.dto.ProductRequestDTO;
import com.pedidai.api.entities.Company;
import com.pedidai.api.entities.PriceHistory;
import com.pedidai.api.entities.Product;
import com.pedidai.api.entities.Supplier;
import com.pedidai.api.exceptions.BadRequestException;
import com.pedidai.api.exceptions.ResourceNotFoundException;
import com.pedidai.api.repositories.ProductRepository;
import com.pedidai.api.repositories.SupplierRepository;
import com.pedidai.api.security.CurrentUser;
import com.pedidai.api.services.PriceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductServiceImpl: productes de l'empresa, historial de preus i imatges segures")
class ProductServiceImplTest {

    @Mock private ProductRepository productRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private CurrentUser currentUser;
    @Mock private PriceService priceService;
    @InjectMocks private ProductServiceImpl service;

    private Supplier supplier;

    @BeforeEach
    void setUp() {
        Company company = Company.builder().id(1L).uuid("c1").build();
        supplier = Supplier.builder().id(2L).uuid("s1").name("Fruites").company(company).build();
        lenient().when(currentUser.companyId()).thenReturn(1L);
    }

    @Test
    @DisplayName("crear un producte desa el nom genèric i el primer preu a l'historial")
    void createRecordsPrice() {
        when(supplierRepository.findByUuidAndCompany_Id("s1", 1L)).thenReturn(Optional.of(supplier));
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var dto = service.createProduct(ProductRequestDTO.builder().supplierUuid("s1").name("Tomàquet Pera")
                .price(new BigDecimal("1.55")).unit("kg").build());

        assertThat(dto.getCanonicalName()).isEqualTo("tomàquet pera");
        verify(priceService).recordObservation(any(), eq(new BigDecimal("1.55")), eq("kg"), isNull(), any(),
                eq(PriceHistory.Source.MANUAL), isNull());
    }

    @Test
    @DisplayName("canviar el preu a mà queda a l'historial; si no canvia, no")
    void updateRecordsOnlyPriceChanges() {
        Product product = Product.builder().id(3L).uuid("p1").name("Tomàquet").price(new BigDecimal("1.55")).supplier(supplier).build();
        when(productRepository.findByUuidAndSupplier_Company_Id("p1", 1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.updateProduct("p1", ProductRequestDTO.builder().supplierUuid("s1").name("Tomàquet").price(new BigDecimal("1.55")).build());
        verify(priceService, never()).recordObservation(any(), any(), any(), any(), any(), any(), any());

        service.updateProduct("p1", ProductRequestDTO.builder().supplierUuid("s1").name("Tomàquet").price(new BigDecimal("1.70")).build());
        verify(priceService).recordObservation(eq(product), eq(new BigDecimal("1.70")), any(), any(), any(), eq(PriceHistory.Source.MANUAL), any());
    }

    @Test
    @DisplayName("editar un producte sense imageUrl conserva la imatge; amb \"\" la treu")
    void updateKeepsImageUnlessSent() {
        Product product = Product.builder().id(3L).uuid("p1").name("Tomàquet").price(new BigDecimal("1.55"))
                .imageUrl("/api/img/productes/a.jpg").supplier(supplier).build();
        when(productRepository.findByUuidAndSupplier_Company_Id("p1", 1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.updateProduct("p1", ProductRequestDTO.builder().supplierUuid("s1").name("Tomàquet").price(new BigDecimal("1.55")).build());
        assertThat(product.getImageUrl()).isEqualTo("/api/img/productes/a.jpg");

        service.updateProduct("p1", ProductRequestDTO.builder().supplierUuid("s1").name("Tomàquet").price(new BigDecimal("1.55")).imageUrl("").build());
        assertThat(product.getImageUrl()).isNull();
    }

    @Test
    @DisplayName("els productes d'una altra empresa són invisibles")
    void otherCompanyProduct() {
        when(productRepository.findByUuidAndSupplier_Company_Id("p-altre", 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProductByUuid("p-altre")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.deactivateProduct("p-altre")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("no es pot crear un producte en un proveïdor d'una altra empresa")
    void cannotCreateInForeignSupplier() {
        when(supplierRepository.findByUuidAndCompany_Id("s-altre", 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createProduct(ProductRequestDTO.builder().supplierUuid("s-altre").name("X").price(BigDecimal.ONE).build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("només s'accepten imatges reals JPG/PNG/WebP (no SVG ni fitxers disfressats)")
    void rejectsUnsafeImages() {
        var svg = new MockMultipartFile("image", "../../evil.svg", "image/svg+xml", "<svg onload=alert(1)>".getBytes());
        var fakePng = new MockMultipartFile("image", "x.png", "image/png", "<html>no soc una imatge</html>".getBytes());

        assertThatThrownBy(() -> service.storeImage(svg)).isInstanceOf(BadRequestException.class).hasMessage("error.image.invalidType");
        assertThatThrownBy(() -> service.storeImage(fakePng)).isInstanceOf(BadRequestException.class).hasMessage("error.image.invalidType");
    }

    @Test
    @DisplayName("la URL d'imatge només pot ser una de pròpia o https")
    void rejectsArbitraryImageUrl() {
        when(supplierRepository.findByUuidAndCompany_Id("s1", 1L)).thenReturn(Optional.of(supplier));

        assertThatThrownBy(() -> service.createProduct(ProductRequestDTO.builder().supplierUuid("s1").name("X")
                .price(BigDecimal.ONE).imageUrl("javascript:alert(1)").build()))
                .isInstanceOf(BadRequestException.class).hasMessage("error.image.invalidUrl");
    }
}
