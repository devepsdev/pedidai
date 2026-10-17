package com.pedidai.api.services.impl;

import com.pedidai.api.dto.PriceOverviewDTO;
import com.pedidai.api.entities.Company;
import com.pedidai.api.entities.PriceHistory;
import com.pedidai.api.entities.Product;
import com.pedidai.api.entities.Supplier;
import com.pedidai.api.repositories.PriceHistoryRepository;
import com.pedidai.api.repositories.ProductRepository;
import com.pedidai.api.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PriceServiceImpl: historial, comparativa i pujades de preu")
class PriceServiceImplTest {

    @Mock private PriceHistoryRepository historyRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CurrentUser currentUser;
    @InjectMocks private PriceServiceImpl service;

    private Company company;
    private Supplier garcia;
    private Supplier maresme;
    private long ids = 1;

    @BeforeEach
    void setUp() {
        company = Company.builder().id(1L).build();
        garcia = Supplier.builder().id(10L).uuid("s-garcia").name("García").email("g@prov.test").company(company).isActive(true).build();
        maresme = Supplier.builder().id(11L).uuid("s-maresme").name("Maresme").company(company).isActive(true).build();
    }

    private Product product(long id, String name, String generic, Supplier supplier, String price) {
        return Product.builder().id(id).uuid("p" + id).name(name).canonicalName(generic).unit("kg")
                .price(new BigDecimal(price)).supplier(supplier).isActive(true).build();
    }

    private PriceHistory obs(Product p, String price, String qty, LocalDate date) {
        return PriceHistory.builder().id(ids++).company(company).supplier(p.getSupplier()).product(p)
                .unitPrice(new BigDecimal(price)).unit("kg").quantity(qty == null ? null : new BigDecimal(qty))
                .documentDate(date).source(PriceHistory.Source.INVOICE).build();
    }

    @Test
    @DisplayName("un albarà més antic que l'últim registrat no canvia el preu vigent")
    void olderDocumentDoesNotOverwrite() {
        Product tomato = product(1, "Tomate pera", "tomate pera", garcia, "1.85");
        when(historyRepository.findFirstByProduct_IdOrderByDocumentDateDescIdDesc(1L))
                .thenReturn(Optional.of(obs(tomato, "1.85", "15", LocalDate.of(2026, 9, 21))));

        boolean changed = service.recordObservation(tomato, new BigDecimal("1.60"), "kg", BigDecimal.TEN,
                LocalDate.of(2026, 8, 12), PriceHistory.Source.INVOICE, "A-1");

        assertThat(changed).isFalse();
        assertThat(tomato.getPrice()).isEqualByComparingTo("1.85");
        verify(historyRepository).save(any());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("un albarà més recent actualitza el preu vigent")
    void newerDocumentUpdatesPrice() {
        Product tomato = product(1, "Tomate pera", "tomate pera", garcia, "1.60");
        when(historyRepository.findFirstByProduct_IdOrderByDocumentDateDescIdDesc(1L))
                .thenReturn(Optional.of(obs(tomato, "1.60", "15", LocalDate.of(2026, 8, 12))));

        assertThat(service.recordObservation(tomato, new BigDecimal("1.854"), "kg", null,
                LocalDate.of(2026, 9, 21), PriceHistory.Source.INVOICE, null)).isTrue();
        assertThat(tomato.getPrice()).isEqualByComparingTo("1.85");
    }

    @Test
    @DisplayName("el resum marca el proveïdor més barat, les pujades i el sobrecost")
    void overviewComparesSuppliers() {
        LocalDate today = LocalDate.now();
        Product tomatoGarcia = product(1, "TOMATE PERA CAT.1", "tomate pera", garcia, "1.85");
        Product tomatoMaresme = product(2, "TOMAQUET PERA", "tomate pera", maresme, "1.55");
        Product lemon = product(3, "LIMON", "limón", garcia, "1.90");
        when(currentUser.companyId()).thenReturn(1L);
        when(historyRepository.findActiveByCompanySince(eq(1L), any())).thenReturn(new java.util.ArrayList<>(List.of(
                obs(tomatoGarcia, "1.60", "15", today.minusDays(40)),
                obs(tomatoGarcia, "1.85", "15", today.minusDays(6)),
                obs(tomatoMaresme, "1.55", "20", today.minusDays(4)),
                obs(lemon, "1.90", "5", today.minusDays(6)))));

        PriceOverviewDTO overview = service.getOverview(90);

        assertThat(overview.getComparable()).hasSize(1);
        var group = overview.getComparable().get(0);
        assertThat(group.getName()).isEqualTo("Tomate pera");
        assertThat(group.getOffers().get(0).getSupplierName()).isEqualTo("Maresme");
        assertThat(group.getOffers().get(0).isCheapest()).isTrue();
        assertThat(group.getSpreadPercent()).isEqualByComparingTo("19.4");
        assertThat(overview.getSingleSupplier()).extracting("name").containsExactly("Limón");
        assertThat(overview.getAlerts()).hasSize(1);
        assertThat(overview.getAlerts().get(0).getChangePercent()).isEqualByComparingTo("15.6");
        // (1.60-1.55)*15 + (1.85-1.55)*15 = 0.75 + 4.50
        assertThat(overview.getOverpaidAmount()).isEqualByComparingTo("5.25");
        assertThat(overview.getSuppliersTracked()).isEqualTo(2);
    }

    private PriceHistory quote(Product p, String price, LocalDate date, LocalDate validUntil) {
        return PriceHistory.builder().id(ids++).company(company).supplier(p.getSupplier()).product(p)
                .unitPrice(new BigDecimal(price)).unit("kg").documentDate(date).validUntil(validUntil)
                .source(PriceHistory.Source.QUOTE).build();
    }

    @Test
    @DisplayName("una tarifa d'un altre proveïdor es compara, però no compta com a pagat de més")
    void quoteIsComparedButNotOverpaid() {
        LocalDate today = LocalDate.now();
        Product tomatoGarcia = product(1, "TOMATE PERA", "tomate pera", garcia, "1.85");
        Product tomatoMaresme = product(2, "TOMAQUET PERA", "tomate pera", maresme, "1.55");
        when(currentUser.companyId()).thenReturn(1L);
        when(historyRepository.findActiveByCompanySince(eq(1L), any())).thenReturn(new java.util.ArrayList<>(List.of(
                obs(tomatoGarcia, "1.60", "15", today.minusDays(40)),
                obs(tomatoGarcia, "1.85", "15", today.minusDays(6)),
                quote(tomatoMaresme, "1.55", today.minusDays(2), null))));

        PriceOverviewDTO overview = service.getOverview(90);

        assertThat(overview.getComparable()).hasSize(1);
        var offers = overview.getComparable().get(0).getOffers();
        assertThat(offers.get(0).getSupplierName()).isEqualTo("Maresme");
        assertThat(offers.get(0).isQuoted()).isTrue();
        assertThat(offers.get(0).isCheapest()).isTrue();
        assertThat(offers.get(0).getValidUntil()).isEqualTo(today.minusDays(2).plusDays(PriceServiceImpl.QUOTE_DEFAULT_DAYS));
        assertThat(offers.get(1).isQuoted()).isFalse();
        // La pujada de García (preus pagats) sí que surt; el sobrecost només es calcula entre preus pagats
        assertThat(overview.getAlerts()).extracting("supplierName").containsExactly("García");
        assertThat(overview.getOverpaidAmount()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("una tarifa nova del mateix proveïdor no genera una alerta de pujada")
    void quoteDoesNotRaiseAlert() {
        LocalDate today = LocalDate.now();
        Product tomato = product(1, "TOMATE PERA", "tomate pera", garcia, "1.80");
        when(currentUser.companyId()).thenReturn(1L);
        when(historyRepository.findActiveByCompanySince(eq(1L), any())).thenReturn(new java.util.ArrayList<>(List.of(
                obs(tomato, "1.60", "15", today.minusDays(10)),
                quote(tomato, "1.80", today.minusDays(2), today.plusDays(30)))));

        PriceOverviewDTO overview = service.getOverview(90);

        assertThat(overview.getAlerts()).isEmpty();
        var offer = overview.getSingleSupplier().get(0).getOffers().get(0);
        assertThat(offer.isQuoted()).isTrue();
        assertThat(offer.getPreviousPrice()).isEqualByComparingTo("1.60");
        assertThat(offer.getChangePercent()).isEqualByComparingTo("12.5");
        assertThat(offer.getValidUntil()).isEqualTo(today.plusDays(30));
    }

    @Test
    @DisplayName("una tarifa caducada no compta: queda l'últim preu pagat")
    void expiredQuoteIsIgnored() {
        LocalDate today = LocalDate.now();
        Product tomato = product(1, "TOMATE PERA", "tomate pera", garcia, "1.40");
        Product tomatoMaresme = product(2, "TOMAQUET PERA", "tomate pera", maresme, "1.30");
        when(currentUser.companyId()).thenReturn(1L);
        when(historyRepository.findActiveByCompanySince(eq(1L), any())).thenReturn(new java.util.ArrayList<>(List.of(
                obs(tomato, "1.60", "15", today.minusDays(60)),
                quote(tomato, "1.40", today.minusDays(40), today.minusDays(1)),
                quote(tomatoMaresme, "1.30", today.minusDays(40), today.minusDays(1)))));

        PriceOverviewDTO overview = service.getOverview(90);

        assertThat(overview.getComparable()).isEmpty();
        var offer = overview.getSingleSupplier().get(0).getOffers().get(0);
        assertThat(offer.isQuoted()).isFalse();
        assertThat(offer.getLatestPrice()).isEqualByComparingTo("1.60");
    }

    @Test
    @DisplayName("una tarifa desa la validesa; un albarà no")
    void recordsValidUntilOnlyForQuotes() {
        Product tomato = product(1, "Tomate pera", "tomate pera", garcia, "1.60");
        when(historyRepository.findFirstByProduct_IdOrderByDocumentDateDescIdDesc(1L)).thenReturn(Optional.empty());
        LocalDate until = LocalDate.now().plusDays(30);

        service.recordObservation(tomato, new BigDecimal("1.50"), "kg", null, LocalDate.now(),
                PriceHistory.Source.QUOTE, "T-1", until);
        service.recordObservation(tomato, new BigDecimal("1.50"), "kg", BigDecimal.ONE, LocalDate.now(),
                PriceHistory.Source.INVOICE, "A-1", until);

        var saved = org.mockito.ArgumentCaptor.forClass(PriceHistory.class);
        verify(historyRepository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues().get(0).getValidUntil()).isEqualTo(until);
        assertThat(saved.getAllValues().get(1).getValidUntil()).isNull();
    }

    @Test
    @DisplayName("la cerca per nom troba plurals i ignora accents")
    void compareByNameMatchesPlurals() {
        Product tomato = product(1, "TOMAQUET PERA", "tomate pera", maresme, "1.55");
        when(currentUser.companyId()).thenReturn(1L);
        when(historyRepository.findActiveByCompanySince(eq(1L), any()))
                .thenReturn(new java.util.ArrayList<>(List.of(obs(tomato, "1.55", "20", LocalDate.now()))));

        assertThat(service.compareByName("Tomates", 90)).hasSize(1);
        assertThat(service.compareByName("patatas", 90)).isEmpty();
    }
}
