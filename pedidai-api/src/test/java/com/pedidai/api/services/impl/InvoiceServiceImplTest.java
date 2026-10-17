package com.pedidai.api.services.impl;

import com.pedidai.api.dto.AiInvoiceDataDTO;
import com.pedidai.api.dto.InvoiceConfirmRequestDTO;
import com.pedidai.api.dto.InvoiceProductConfirmDTO;
import com.pedidai.api.entities.Company;
import com.pedidai.api.entities.Order;
import com.pedidai.api.entities.OrderItem;
import com.pedidai.api.entities.PriceHistory;
import com.pedidai.api.entities.Product;
import com.pedidai.api.entities.Supplier;
import com.pedidai.api.exceptions.BadRequestException;
import com.pedidai.api.exceptions.ResourceNotFoundException;
import com.pedidai.api.repositories.OrderRepository;
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
    @Mock private OrderRepository orderRepository;

    private InvoiceServiceImpl service;
    private Company company;

    @BeforeEach
    void setUp() {
        service = new InvoiceServiceImpl(aiVisionService, supplierRepository, productRepository, priceService,
                currentUser, new RateLimiter(), orderRepository);
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
    @DisplayName("reconeix el proveïdor encara que la IA hi enganxi la paraula «albarán»")
    void reusesSupplierWhenNameIncludesDocumentWord() {
        Supplier existing = Supplier.builder().id(9L).uuid("s1").name("Distribuciones López S.L.").company(company).build();
        when(supplierRepository.findActiveByCompanyIdAndNameContaining(1L, "distribuciones")).thenReturn(List.of(existing));
        when(productRepository.findBySupplier_IdAndIsActiveTrue(9L)).thenReturn(new ArrayList<>());
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = service.confirmInvoice(InvoiceConfirmRequestDTO.builder()
                .newSupplier(InvoiceConfirmRequestDTO.NewSupplier.builder().name("DISTRIBUCIONES LÓPEZ ALBARÁN").build())
                .products(List.of(line("PATATA", "patata", "0.70", null))).build());

        verify(supplierRepository, never()).save(any());
        assertThat(result.getMatchedSupplierUuid()).isEqualTo("s1");
    }

    @Test
    @DisplayName("sense coincidència exacta, accepta l'únic proveïdor que conté el nom sencer")
    void reusesSupplierContainedInDetectedName() {
        Supplier existing = Supplier.builder().id(9L).uuid("s1").name("Distribuciones López").company(company).build();
        Supplier other = Supplier.builder().id(10L).uuid("s2").name("Distribuciones Martí").company(company).build();
        when(supplierRepository.findActiveByCompanyIdAndNameContaining(1L, "distribuciones")).thenReturn(List.of(existing, other));
        when(productRepository.findBySupplier_IdAndIsActiveTrue(9L)).thenReturn(new ArrayList<>());
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = service.confirmInvoice(InvoiceConfirmRequestDTO.builder()
                .newSupplier(InvoiceConfirmRequestDTO.NewSupplier.builder().name("Distribuciones López Hostelería").build())
                .products(List.of(line("PATATA", "patata", "0.70", null))).build());

        assertThat(result.getMatchedSupplierUuid()).isEqualTo("s1");
    }

    @Test
    @DisplayName("si dos proveïdors encaixen parcialment no n'escull cap i en crea un de nou")
    void ambiguousPartialMatchCreatesSupplier() {
        Supplier a = Supplier.builder().id(9L).uuid("s1").name("Distribuciones López").company(company).build();
        Supplier b = Supplier.builder().id(10L).uuid("s2").name("Distribuciones López Hostelería Norte").company(company).build();
        when(supplierRepository.findActiveByCompanyIdAndNameContaining(1L, "distribuciones")).thenReturn(List.of(a, b));
        when(supplierRepository.save(any())).thenAnswer(i -> { Supplier s = i.getArgument(0); s.setId(11L); s.setUuid("s-new"); return s; });
        when(productRepository.findBySupplier_IdAndIsActiveTrue(11L)).thenReturn(new ArrayList<>());
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = service.confirmInvoice(InvoiceConfirmRequestDTO.builder()
                .newSupplier(InvoiceConfirmRequestDTO.NewSupplier.builder().name("Distribuciones López Hostelería").build())
                .products(List.of(line("PATATA", "patata", "0.70", null))).build());

        assertThat(result.getMatchedSupplierUuid()).isEqualTo("s-new");
    }

    @Test
    @DisplayName("una tarifa es desa com a preu ofert, sense quantitat i amb la validesa")
    void priceListIsRecordedAsQuote() {
        Supplier supplier = Supplier.builder().id(9L).uuid("s1").name("Fruites").company(company).build();
        when(supplierRepository.findByUuidAndCompany_Id("s1", 1L)).thenReturn(Optional.of(supplier));
        when(productRepository.findBySupplier_IdAndIsActiveTrue(9L)).thenReturn(new ArrayList<>());
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        LocalDate date = LocalDate.now().minusDays(3);
        LocalDate until = LocalDate.now().plusDays(60);

        service.confirmInvoice(InvoiceConfirmRequestDTO.builder().supplierUuid("s1")
                .documentKind(AiVisionService.DocumentKind.PRICE_LIST)
                .invoiceDate(date.toString()).invoiceNumber("T-2026").validUntil(until.toString())
                .products(List.of(line("TOMAQUET PERA", "tomate pera", "1.55", null))).build());

        verify(priceService).recordObservation(any(), eq(new BigDecimal("1.55")), eq("kg"), isNull(),
                eq(date), eq(PriceHistory.Source.QUOTE), eq("T-2026"), eq(until));
    }

    @Test
    @DisplayName("una validesa anterior a la data de la tarifa s'ignora")
    void priceListIgnoresValidUntilBeforeDate() {
        Supplier supplier = Supplier.builder().id(9L).uuid("s1").name("Fruites").company(company).build();
        when(supplierRepository.findByUuidAndCompany_Id("s1", 1L)).thenReturn(Optional.of(supplier));
        when(productRepository.findBySupplier_IdAndIsActiveTrue(9L)).thenReturn(new ArrayList<>());
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        LocalDate date = LocalDate.now().minusDays(3);

        service.confirmInvoice(InvoiceConfirmRequestDTO.builder().supplierUuid("s1")
                .documentKind(AiVisionService.DocumentKind.PRICE_LIST)
                .invoiceDate(date.toString()).validUntil(date.minusDays(10).toString())
                .products(List.of(line("TOMAQUET PERA", "tomate pera", "1.55", null))).build());

        verify(priceService).recordObservation(any(), any(), any(), isNull(), eq(date),
                eq(PriceHistory.Source.QUOTE), isNull(), isNull());
    }

    @Test
    @DisplayName("accepta tarifes en Excel i CSV, comprovant el contingut i no només l'extensió")
    void acceptsSpreadsheetsAndCsv() {
        var xlsx = new MockMultipartFile("image", "tarifa.xlsx", "application/octet-stream",
                new byte[]{'P', 'K', 3, 4, 0, 0, 0, 0});
        var xls = new MockMultipartFile("image", "TARIFA.XLS", "application/octet-stream",
                new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1});
        var csv = new MockMultipartFile("image", "tarifa.csv", "text/csv",
                "Producto;Precio\nTomate pera;1,55\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var fakeCsv = new MockMultipartFile("image", "tarifa.csv", "text/csv", new byte[]{'M', 'Z', 0, 0, 1, 2});

        assertThat(InvoiceServiceImpl.validateFile(xlsx)).isEqualTo(AiVisionServiceImpl.MEDIA_XLSX);
        assertThat(InvoiceServiceImpl.validateFile(xls)).isEqualTo(AiVisionServiceImpl.MEDIA_XLS);
        assertThat(InvoiceServiceImpl.validateFile(csv)).isEqualTo(AiVisionServiceImpl.MEDIA_CSV);
        assertThatThrownBy(() -> InvoiceServiceImpl.validateFile(fakeCsv))
                .isInstanceOf(BadRequestException.class).hasMessage("error.invoice.invalidFormat");
    }

    @Test
    @DisplayName("en mode AUTO el resultat porta el tipus que ha detectat la IA")
    void autoScanReturnsDetectedKind() {
        AiInvoiceDataDTO ai = new AiInvoiceDataDTO();
        ai.setDocumentType("PRICE_LIST");
        AiInvoiceDataDTO.InvoiceData inv = new AiInvoiceDataDTO.InvoiceData();
        inv.setDate("2026-10-15");
        inv.setValidUntil("2026-11-14");
        ai.setInvoice(inv);
        AiInvoiceDataDTO.ProductData p = new AiInvoiceDataDTO.ProductData();
        p.setName("Tomate pera");
        p.setUnitPrice(new BigDecimal("1.72"));
        ai.setProducts(List.of(p));
        when(aiVisionService.analyzeDocument(any(), eq("application/pdf"), eq(AiVisionService.DocumentKind.AUTO))).thenReturn(ai);
        var pdf = new MockMultipartFile("image", "presupuesto.pdf", "application/pdf", "%PDF-1.4 ...".getBytes());

        var result = service.scanInvoice(pdf, null, AiVisionService.DocumentKind.AUTO);

        assertThat(result.getDocumentKind()).isEqualTo(AiVisionService.DocumentKind.PRICE_LIST);
        assertThat(result.getValidUntil()).isEqualTo("2026-11-14");
    }

    @Test
    @DisplayName("un albarà d'un proveïdor conegut porta l'última comanda enviada per comparar-la")
    void invoiceScanReturnsMatchingOrder() {
        Supplier supplier = Supplier.builder().id(9L).uuid("s1").name("Distribuciones López").company(company).build();
        when(supplierRepository.findByUuidAndCompany_Id("s1", 1L)).thenReturn(Optional.of(supplier));
        Product tomato = Product.builder().id(5L).uuid("p-tomate").name("Tomate pera").unit("kg").supplier(supplier).build();
        when(productRepository.findBySupplier_IdAndIsActiveTrue(9L)).thenReturn(List.of(tomato));
        Order order = Order.builder().uuid("o1").name("Pedido 12/10").status(Order.OrderStatus.SENT)
                .createdAt(LocalDate.now().minusDays(2).atTime(9, 0)).build();
        order.getItems().add(OrderItem.builder().product(tomato).quantity(new BigDecimal("20"))
                .unitPrice(new BigDecimal("1.72")).build());
        when(orderRepository.findBySupplierAndStatusesBetween(eq(1L), eq(9L), any(), any(), any())).thenReturn(List.of(order));
        AiInvoiceDataDTO ai = new AiInvoiceDataDTO();
        AiInvoiceDataDTO.ProductData p = new AiInvoiceDataDTO.ProductData();
        p.setName("Tomate pera");
        p.setQuantity(new BigDecimal("15"));
        p.setUnit("kg");
        p.setUnitPrice(new BigDecimal("1.85"));
        ai.setProducts(List.of(p));
        when(aiVisionService.analyzeDocument(any(), any(), eq(AiVisionService.DocumentKind.INVOICE))).thenReturn(ai);
        var png = new MockMultipartFile("image", "albaran.png", "image/png", new byte[]{(byte) 0x89, 'P', 'N', 'G', 0, 0});

        var result = service.scanInvoice(png, "s1", AiVisionService.DocumentKind.INVOICE);

        assertThat(result.getMatchedOrder().getUuid()).isEqualTo("o1");
        assertThat(result.getMatchedOrder().getItems()).singleElement()
                .satisfies(i -> {
                    assertThat(i.getProductUuid()).isEqualTo("p-tomate");
                    assertThat(i.getQuantity()).isEqualByComparingTo("20");
                    assertThat(i.getUnitPrice()).isEqualByComparingTo("1.72");
                });
        assertThat(result.getProducts().get(0).getMatchedProductUuid()).isEqualTo("p-tomate");
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
