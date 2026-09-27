package com.pedidai.api.services.impl;

import com.pedidai.api.dto.OrderItemRequestDTO;
import com.pedidai.api.dto.OrderRequestDTO;
import com.pedidai.api.entities.*;
import com.pedidai.api.exceptions.BadRequestException;
import com.pedidai.api.exceptions.ForbiddenException;
import com.pedidai.api.exceptions.ResourceNotFoundException;
import com.pedidai.api.exceptions.TooManyRequestsException;
import com.pedidai.api.repositories.*;
import com.pedidai.api.security.CurrentUser;
import com.pedidai.api.services.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl: comandes només de l'empresa i enviament segur")
class OrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProductRepository productRepository;
    @Mock private NotificationService notificationService;
    @Mock private CurrentUser currentUser;

    @InjectMocks private OrderServiceImpl service;

    private Company company;
    private User user;
    private Supplier supplier;
    private Product tomato;

    @BeforeEach
    void setUp() {
        company = Company.builder().id(1L).uuid("c1").name("Bar").build();
        user = User.builder().id(5L).email("u@bar.test").company(company).emailVerified(true).build();
        supplier = Supplier.builder().id(20L).uuid("s1").name("Fruites").email("f@prov.test").company(company).build();
        tomato = Product.builder().id(30L).uuid("p1").name("Tomate").price(new BigDecimal("1.55")).supplier(supplier).build();
        lenient().when(currentUser.get()).thenReturn(user);
        lenient().when(currentUser.companyId()).thenReturn(1L);
    }

    private OrderRequestDTO request(String supplierUuid, String productUuid) {
        return OrderRequestDTO.builder().name("Comanda").supplierUuid(supplierUuid)
                .items(List.of(OrderItemRequestDTO.builder().productUuid(productUuid).quantity(new BigDecimal("10")).build()))
                .build();
    }

    private Order pendingOrder() {
        Order order = new Order();
        order.setUuid("o1");
        order.setName("Comanda");
        order.setStatus(Order.OrderStatus.PENDING);
        order.setCompany(company);
        order.setSupplier(supplier);
        order.setUser(user);
        order.setItems(new ArrayList<>());
        order.setTotalAmount(BigDecimal.ZERO);
        return order;
    }

    @Test
    @DisplayName("crea la comanda amb el preu vigent del producte")
    void createsOrder() {
        when(supplierRepository.findByUuidAndCompany_Id("s1", 1L)).thenReturn(Optional.of(supplier));
        when(productRepository.findByUuidAndSupplier_Company_Id("p1", 1L)).thenReturn(Optional.of(tomato));

        var dto = service.createOrder(request("s1", "p1"));

        assertThat(dto.getStatus()).isEqualTo("PENDING");
        assertThat(dto.getTotalAmount()).isEqualByComparingTo("15.50");
        assertThat(dto.getSupplierName()).isEqualTo("Fruites");
    }

    @Test
    @DisplayName("no es pot demanar a un proveïdor d'una altra empresa")
    void supplierFromOtherCompany() {
        when(supplierRepository.findByUuidAndCompany_Id("s-altre", 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createOrder(request("s-altre", "p1"))).isInstanceOf(ResourceNotFoundException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("tots els productes han de ser del proveïdor de la comanda")
    void productFromAnotherSupplier() {
        Supplier other = Supplier.builder().id(21L).uuid("s2").name("Altre").company(company).build();
        Product water = Product.builder().id(31L).uuid("p2").name("Aigua").price(BigDecimal.ONE).supplier(other).build();
        when(supplierRepository.findByUuidAndCompany_Id("s1", 1L)).thenReturn(Optional.of(supplier));
        when(productRepository.findByUuidAndSupplier_Company_Id("p2", 1L)).thenReturn(Optional.of(water));

        assertThatThrownBy(() -> service.createOrder(request("s1", "p2")))
                .isInstanceOf(BadRequestException.class).hasMessage("error.order.productFromOtherSupplier");
    }

    @Test
    @DisplayName("sense email verificat no es poden enviar comandes")
    void sendRequiresVerifiedEmail() {
        user.setEmailVerified(false);
        when(orderRepository.findByUuidAndCompany_Id("o1", 1L)).thenReturn(Optional.of(pendingOrder()));

        assertThatThrownBy(() -> service.sendOrder("o1")).isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("límit diari de comandes enviades")
    void sendDailyLimit() {
        when(orderRepository.findByUuidAndCompany_Id("o1", 1L)).thenReturn(Optional.of(pendingOrder()));
        when(orderRepository.countByCompany_IdAndStatusAndUpdatedAtAfter(eq(1L), eq(Order.OrderStatus.SENT), any()))
                .thenReturn((long) OrderServiceImpl.MAX_SENT_PER_DAY);

        assertThatThrownBy(() -> service.sendOrder("o1")).isInstanceOf(TooManyRequestsException.class);
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("enviar una comanda pendent la marca com a enviada")
    void sendsPendingOrder() {
        Order order = pendingOrder();
        when(orderRepository.findByUuidAndCompany_Id("o1", 1L)).thenReturn(Optional.of(order));

        assertThat(service.sendOrder("o1").getStatus()).isEqualTo("SENT");
        verify(notificationService).sendOrderNotification(order);
    }

    @Test
    @DisplayName("una comanda ja enviada no es pot tornar a enviar ni modificar")
    void sentOrderIsLocked() {
        Order order = pendingOrder();
        order.setStatus(Order.OrderStatus.SENT);
        when(orderRepository.findByUuidAndCompany_Id("o1", 1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.sendOrder("o1")).hasMessage("error.order.notPending");
        assertThatThrownBy(() -> service.updateOrder("o1", request("s1", "p1"))).hasMessage("error.order.notEditable");
    }

    @Test
    @DisplayName("una comanda d'una altra empresa és invisible")
    void orderFromOtherCompany() {
        when(orderRepository.findByUuidAndCompany_Id("o-altre", 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOrderByUuid("o-altre")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.sendOrder("o-altre")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.deleteOrder("o-altre")).isInstanceOf(ResourceNotFoundException.class);
    }
}
