package com.pedidai.api.services.impl;

import com.pedidai.api.config.I18nConfig;
import com.pedidai.api.entities.Order;
import com.pedidai.api.entities.OrderItem;
import com.pedidai.api.entities.Supplier;
import com.pedidai.api.exceptions.BadRequestException;
import com.pedidai.api.services.EmailService;
import com.pedidai.api.services.EmailService.OrderEmail;
import com.pedidai.api.services.EmailService.OrderEmailLine;
import com.pedidai.api.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final EmailService emailService;

    @Override
    public void sendOrderNotification(Order order) {
        Supplier supplier = order.getSupplier();
        if (supplier.getEmail() == null || supplier.getEmail().isBlank()) {
            throw new BadRequestException("error.order.supplierNoEmail", supplier.getName());
        }

        List<OrderEmailLine> lines = order.getItems().stream()
                .map(this::toLine)
                .toList();

        // El correu surt en l'idioma de l'usuari que fa la comanda, i les respostes li arriben a ell
        String userEmail = order.getUser() != null ? order.getUser().getEmail() : null;
        String language = order.getUser() != null ? order.getUser().getLanguage() : null;

        emailService.sendOrderNotification(new OrderEmail(
                supplier.getEmail(),
                userEmail,
                supplier.getContactName(),
                order.getCompany().getName(),
                order.getCompany().getAddress(),
                order.getCompany().getPhone(),
                order.getName(),
                lines,
                order.getNotes()
        ), I18nConfig.localeOf(language));

        log.info("Comanda {} enviada per email a {}", order.getUuid(), supplier.getEmail());
    }

    private OrderEmailLine toLine(OrderItem item) {
        String name = item.getProduct() != null ? item.getProduct().getName() : "—";
        String unit = item.getProduct() != null ? item.getProduct().getUnit() : null;
        return new OrderEmailLine(name, formatQuantity(item.getQuantity()), unit, item.getNotes());
    }

    private static String formatQuantity(BigDecimal quantity) {
        return quantity == null ? "" : quantity.stripTrailingZeros().toPlainString();
    }
}
