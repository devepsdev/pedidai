package com.pedidai.api.services.impl;

import com.pedidai.api.dto.*;
import com.pedidai.api.entities.*;
import com.pedidai.api.entities.Order;
import com.pedidai.api.repositories.OrderRepository;
import com.pedidai.api.repositories.ProductRepository;
import com.pedidai.api.repositories.UserRepository;
import com.pedidai.api.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportServiceImpl Tests")
class ReportServiceImplTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    private Company testCompany;
    private User testUser;
    private Supplier testSupplier;
    private Product testProduct;
    private OrderItem testItem;

    @BeforeEach
    void setUp() {
        // Company
        testCompany = Company.builder()
                .id(1L)
                .uuid("company-uuid")
                .name("Test Company")
                .build();

        // User
        testUser = User.builder()
                .uuid("user-uuid")
                .company(testCompany)
                .email("user@test.com")
                .firstName("User")
                .lastName("Test")
                .role(User.UserRole.ADMIN)
                .isActive(true)
                .emailVerified(true)
                .build();

        // Supplier
        testSupplier = Supplier.builder()
                .uuid("supplier-uuid")
                .name("Supplier 1")
                .company(testCompany)
                .build();

        // Product
        testProduct = Product.builder()
                .uuid("product-uuid")
                .name("Product 1")
                .supplier(testSupplier)
                .price(BigDecimal.valueOf(10))
                .build();

        // OrderItem
        testItem = OrderItem.builder()
                .product(testProduct)
                .unitPrice(testProduct.getPrice())
                .quantity(BigDecimal.valueOf(2))
                .build();

        // Mock SecurityContext
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(testUser.getEmail());
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Comprova retorn d'informació dashboard")
    void dashboardInfo_success() {
        // Mock userRepository
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));

        // Crear orders de prova
        Order order1 = Order.builder()
                .uuid("order-1")
                .status(Order.OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(100))
                .build();

        Order order2 = Order.builder()
                .uuid("order-2")
                .status(Order.OrderStatus.COMPLETED)
                .totalAmount(BigDecimal.valueOf(50))
                .build();

        List<Order> orders = List.of(order1, order2);

        // Mock orderRepository
        when(orderRepository.getOrdersByCompanyIdAndPeriodWithoutOrderItems(eq(testCompany.getId()), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(orders);

        // Executar servei
        DashboardResponseDTO dto = reportService.dashboardInfo();

        // Asserts
        assertThat(dto).isNotNull();
        assertThat(dto.getTotalComandes()).isEqualTo(2);
        assertThat(dto.getDespesaComandes()).isEqualByComparingTo(BigDecimal.valueOf(150));
        assertThat(dto.getComandesPendents()).isEqualTo(1);

        // Verificacions
        verify(userRepository).findByEmail(testUser.getEmail());
        verify(orderRepository).getOrdersByCompanyIdAndPeriodWithoutOrderItems(eq(testCompany.getId()), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Comprova retorn d'informació global (informe)")
    void globalInfo_success() {
        // Mock userRepository
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));

        // Crear orders i items
        Order order1 = Order.builder()
                .uuid("order-1")
                .status(Order.OrderStatus.PENDING)
                .supplier(testSupplier)
                .items(List.of(testItem))
                .build();

        Order order2 = Order.builder()
                .uuid("order-2")
                .status(Order.OrderStatus.CONFIRMED)
                .supplier(testSupplier)
                .items(List.of(testItem))
                .build();

        List<Order> orders = List.of(order1, order2);

        PeriodRequestDTO periodDTO = PeriodRequestDTO.builder()
                .dataInicial(LocalDateTime.now().minusDays(30))
                .dataFinal(LocalDateTime.now())
                .build();

        when(orderRepository.getOrdersByCompanyIdAndPeriodWithOrderItems(eq(testCompany.getId()), eq(periodDTO.getDataInicial()), eq(periodDTO.getDataFinal())))
                .thenReturn(orders);

        // Executar servei
        ReportGlobalResponseDTO dto = reportService.globalInfo(periodDTO);

        // Despesa total calculada
        BigDecimal expectedDespesaTotal = testItem.getUnitPrice().multiply(testItem.getQuantity()).multiply(BigDecimal.valueOf(2)).setScale(2);

        assertThat(dto).isNotNull();
        assertThat(dto.getTotalComandes()).isEqualTo(2);
        assertThat(dto.getComandaMitjana()).isEqualByComparingTo(expectedDespesaTotal.divide(BigDecimal.valueOf(2), 2, BigDecimal.ROUND_HALF_UP));
        assertThat(dto.getDespesaTotal()).isEqualByComparingTo(expectedDespesaTotal);
        assertThat(dto.getDespesaProveidors()).hasSize(1);
        assertThat(dto.getTopProductes()).hasSize(1);

        verify(userRepository).findByEmail(testUser.getEmail());
        verify(orderRepository).getOrdersByCompanyIdAndPeriodWithOrderItems(eq(testCompany.getId()), eq(periodDTO.getDataInicial()), eq(periodDTO.getDataFinal()));
    }

    @Test
    @DisplayName("Comprova excepció d'usuari no trobat")
    void globalInfo_userNotFound_throws() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.empty());

        PeriodRequestDTO periodDTO = PeriodRequestDTO.builder()
                .dataInicial(LocalDateTime.now().minusDays(30))
                .dataFinal(LocalDateTime.now())
                .build();

        assertThatThrownBy(() -> reportService.globalInfo(periodDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(testUser.getEmail());

        verify(userRepository).findByEmail(testUser.getEmail());
        verifyNoInteractions(orderRepository);
    }
}
