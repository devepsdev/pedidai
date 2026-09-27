package com.pedidai.api.services.impl;

import com.pedidai.api.entities.*;
import com.pedidai.api.exceptions.BadRequestException;
import com.pedidai.api.services.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl: correu de la comanda al proveïdor")
class NotificationServiceImplTest {

    @Mock private EmailService emailService;
    @InjectMocks private NotificationServiceImpl service;

    private Order order;
    private Supplier supplier;

    @BeforeEach
    void setUp() {
        Company company = Company.builder().id(1L).name("Bar Prova").phone("600000000").build();
        supplier = Supplier.builder().name("Fruites").contactName("Pere").email("pere@prov.test").company(company).build();
        User user = User.builder().email("laura@bar.test").language("ca").company(company).build();
        Product product = Product.builder().name("Tomàquet").unit("kg").supplier(supplier).build();
        OrderItem item = OrderItem.builder().product(product).quantity(new BigDecimal("10.00")).notes("ben madurs").build();
        order = Order.builder().uuid("o1").name("Comanda setmanal").company(company).supplier(supplier).user(user)
                .items(List.of(item)).build();
    }

    @Test
    @DisplayName("envia al proveïdor, amb resposta a l'email del client i en el seu idioma")
    void sendsWithReplyToAndLanguage() {
        service.sendOrderNotification(order);

        ArgumentCaptor<EmailService.OrderEmail> email = ArgumentCaptor.forClass(EmailService.OrderEmail.class);
        verify(emailService).sendOrderNotification(email.capture(), eq(Locale.forLanguageTag("ca")));
        assertThat(email.getValue().to()).isEqualTo("pere@prov.test");
        assertThat(email.getValue().replyTo()).isEqualTo("laura@bar.test");
        assertThat(email.getValue().lines()).singleElement()
                .satisfies(l -> {
                    assertThat(l.quantity()).isEqualTo("10");
                    assertThat(l.unit()).isEqualTo("kg");
                });
    }

    @Test
    @DisplayName("sense email del proveïdor no s'envia i s'explica el motiu")
    void supplierWithoutEmail() {
        supplier.setEmail(" ");

        assertThatThrownBy(() -> service.sendOrderNotification(order))
                .isInstanceOf(BadRequestException.class).hasMessage("error.order.supplierNoEmail");
        verify(emailService, never()).sendOrderNotification(any(), any());
    }
}
