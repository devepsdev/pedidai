package com.pedidai.api.entities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class ProductTest {

    // Objectes
    private Company testCompany;
    private Supplier testSupplier;
    private Product testProduct;

    @BeforeEach
    void setUp() {

        // Creació de la companyia
        testCompany = Company.builder().uuid("test-company-uuid").name("Test Companyia 1").taxId("55555555K").email("company1@test.com").phone("666666666").address("Carrer Barcelona").city("Barcelona").postalCode("08080").status(Company.CompanyStatus.ACTIVE).build();

        // Creació del proveedor
        testSupplier = Supplier.builder().uuid("test-supplier-uuid").company(testCompany).name("Test supplier 1").contactName("Antonio").email("user@test.com").phone("444444444").address("Carrer Mallorca").notes("Treball 24/7").isActive(true).build();

        // Creació del producte de prova
        testProduct = Product.builder().uuid("test-product-uuid").supplier(testSupplier).category("Categoria").name("Test Producte 1").description("Descripció Producte 1").price(BigDecimal.valueOf(0.5)).volume(BigDecimal.valueOf(33)).unit("cl").imageUrl("/img/productes/producte1.jpg").isActive(true).build();
    }

    @Test
    @DisplayName("Comprova Creació de Product")
    void testBuilder() {
        assertThat(testProduct.getUuid()).isEqualTo("test-product-uuid");
        assertThat(testProduct.getName()).isEqualTo("Test Producte 1");
        assertThat(testProduct.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(0.5));
        assertThat(testProduct.getUnit()).isEqualTo("cl");
    }

    @Test
    @DisplayName("Comprova les dates al @PrePersist")
    void testPrePersist() {
        testProduct.onCreate();
        assertThat(testProduct.getCreatedAt()).isNotNull();
        assertThat(testProduct.getUpdatedAt()).isNotNull();
        assertThat(testProduct.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Comprova les dates al @PreUpdate")
    void testPreUpdate() {
        LocalDateTime beforeUpdate = LocalDateTime.now().minusMinutes(1);
        testProduct.setUpdatedAt(beforeUpdate);
        testProduct.onUpdate();
        assertThat(testProduct.getUpdatedAt()).isAfter(beforeUpdate);
    }

    @Test
    @DisplayName("Comprova establir uuid manual")
    void testManualUuid() {
        String customUuid = UUID.randomUUID().toString();
        testProduct.setUuid(customUuid);
        assertThat(testProduct.getUuid()).isEqualTo(customUuid);
    }

}