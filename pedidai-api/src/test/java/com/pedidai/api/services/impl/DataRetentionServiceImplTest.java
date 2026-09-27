package com.pedidai.api.services.impl;

import com.pedidai.api.entities.*;
import com.pedidai.api.repositories.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(DataRetentionServiceImpl.class)
@DisplayName("DataRetentionService Tests")
class DataRetentionServiceImplTest {

    @Autowired private DataRetentionServiceImpl service;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private PriceHistoryRepository priceHistoryRepository;

    @Test
    @DisplayName("Esborra l'empresa amb la prova acabada fa més de 30 dies i totes les seves dades")
    void purge_deletesExpiredCompanyWithAllData() {
        Company expired = companyWithData("expired", LocalDateTime.now().minusDays(31), User.UserRole.ADMIN);

        int deleted = service.purgeExpiredTrials();

        assertThat(deleted).isEqualTo(1);
        assertThat(companyRepository.findById(expired.getId())).isEmpty();
        assertThat(userRepository.count()).isZero();
        assertThat(supplierRepository.count()).isZero();
        assertThat(productRepository.count()).isZero();
        assertThat(orderRepository.count()).isZero();
        assertThat(orderItemRepository.count()).isZero();
        assertThat(priceHistoryRepository.count()).isZero();
    }

    @Test
    @DisplayName("Conserva les empreses dins del termini, les de pagament i la del SUPER_ADMIN")
    void purge_keepsCompaniesThatMustNotBeDeleted() {
        Company expired = companyWithData("expired", LocalDateTime.now().minusDays(45), User.UserRole.ADMIN);
        Company recent = companyWithData("recent", LocalDateTime.now().minusDays(10), User.UserRole.ADMIN);
        Company paid = companyWithData("paid", null, User.UserRole.ADMIN);
        Company platform = companyWithData("platform", LocalDateTime.now().minusDays(400), User.UserRole.SUPER_ADMIN);

        int deleted = service.purgeExpiredTrials();

        assertThat(deleted).isEqualTo(1);
        assertThat(companyRepository.findById(expired.getId())).isEmpty();
        assertThat(companyRepository.findById(recent.getId())).isPresent();
        assertThat(companyRepository.findById(paid.getId())).isPresent();
        assertThat(companyRepository.findById(platform.getId())).isPresent();
        assertThat(userRepository.count()).isEqualTo(3);
        assertThat(supplierRepository.count()).isEqualTo(3);
        assertThat(productRepository.count()).isEqualTo(3);
        assertThat(orderRepository.count()).isEqualTo(3);
        assertThat(orderItemRepository.count()).isEqualTo(3);
        assertThat(priceHistoryRepository.count()).isEqualTo(3);
    }

    @Test
    @DisplayName("No fa res si no hi ha empreses per esborrar")
    void purge_nothingToDelete() {
        companyWithData("recent", LocalDateTime.now().minusDays(29), User.UserRole.ADMIN);

        assertThat(service.purgeExpiredTrials()).isZero();
        assertThat(companyRepository.count()).isEqualTo(1);
    }

    /** Crea una empresa amb usuari, proveïdor, producte, historial de preus i una comanda amb una línia. */
    private Company companyWithData(String name, LocalDateTime trialEndsAt, User.UserRole role) {
        Company company = companyRepository.save(Company.builder()
                .name(name).email(name + "@test.com")
                .status(Company.CompanyStatus.ACTIVE).trialEndsAt(trialEndsAt).build());
        User user = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).company(company).email(name + "-user@test.com")
                .password("pass").firstName("Nom").lastName("Cognom").role(role)
                .isActive(true).emailVerified(true).build());
        Supplier supplier = supplierRepository.save(Supplier.builder()
                .uuid(UUID.randomUUID().toString()).company(company).name(name + " proveïdor").isActive(true).build());
        Product product = productRepository.save(Product.builder()
                .uuid(UUID.randomUUID().toString()).supplier(supplier).name("Tomàquet")
                .price(BigDecimal.ONE).unit("kg").isActive(true).build());
        priceHistoryRepository.save(PriceHistory.builder()
                .company(company).supplier(supplier).product(product).unitPrice(BigDecimal.ONE).unit("kg")
                .documentDate(LocalDate.now()).source(PriceHistory.Source.INVOICE).build());
        Order order = orderRepository.save(Order.builder()
                .uuid(UUID.randomUUID().toString()).company(company).supplier(supplier).user(user)
                .name("Comanda").status(Order.OrderStatus.PENDING).totalAmount(BigDecimal.TEN)
                .items(new ArrayList<>()).build());
        orderItemRepository.save(OrderItem.builder()
                .uuid(UUID.randomUUID().toString()).order(order).product(product)
                .quantity(BigDecimal.TEN).unitPrice(BigDecimal.ONE).subtotal(BigDecimal.TEN)
                .createdAt(LocalDateTime.now()).build());
        return company;
    }
}
