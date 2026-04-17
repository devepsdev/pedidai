package com.pedidai.api.services.impl;

import com.pedidai.api.entities.Order;
import com.pedidai.api.entities.OrderItem;
import com.pedidai.api.entities.Supplier;
import com.pedidai.api.services.EmailService;
import com.pedidai.api.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final EmailService emailService;

    @Override
    public void sendOrderNotification(Order order) {
        log.info("Enviant notificació de comanda {} per email", order.getUuid());

        Supplier supplier = order.getSupplier();

        // Validar que el proveïdor té email
        if (supplier.getEmail() == null || supplier.getEmail().isEmpty()) {
            log.error("El proveïdor {} no té email configurat. No es pot enviar la comanda.",
                    supplier.getName());
            throw new RuntimeException("El proveïdor no té email configurat");
        }

        // Construir els detalls de la comanda
        String orderDetailsHtml = buildOrderDetailsHtml(order);

        try {
            // Enviar email
            emailService.sendOrderNotification(
                    supplier.getEmail(),
                    supplier.getContactName(),
                    order.getCompany().getName(),
                    order.getCompany().getAddress(),
                    order.getCompany().getPhone(),
                    order.getName(),
                    orderDetailsHtml,
                    order.getNotes()
            );

            // Actualitzar l'estat de la comanda a SENT després d'enviar
            order.setStatus(Order.OrderStatus.SENT);
            log.info("Notificació de comanda {} enviada correctament per email a {}",
                    order.getUuid(), supplier.getEmail());

        } catch (Exception e) {
            log.error("Error en enviar notificació de comanda {}: {}",
                    order.getUuid(), e.getMessage(), e);
            throw new RuntimeException("Error en enviar la notificació de la comanda", e);
        }
    }

    private String buildOrderDetailsHtml(Order order) {
        StringBuilder html = new StringBuilder();
        html.append("""
                <table width="100%" cellpadding="0" cellspacing="0" style="border: 1px solid #e0e0e0; border-radius: 6px; overflow: hidden;">
                    <thead>
                        <tr style="background-color: #06b6d4;">
                            <th style="padding: 12px; text-align: left; color: #ffffff; font-size: 14px;">Producte</th>
                            <th style="padding: 12px; text-align: center; color: #ffffff; font-size: 14px;">Quantitat</th>
                            <th style="padding: 12px; text-align: right; color: #ffffff; font-size: 14px;">Volum</th>
                            <th style="padding: 12px; text-align: right; color: #ffffff; font-size: 14px;">Unitat</th>
                        </tr>
                    </thead>
                    <tbody>
                """);

        for (OrderItem item : order.getItems()) {
            String productName = item.getProduct() != null ?
                    item.getProduct().getName() : "Producte desconegut";
            String unit = item.getProduct() != null && item.getProduct().getUnit() != null ?
                    item.getProduct().getUnit() : "";
            String notes = item.getNotes() != null && !item.getNotes().isEmpty() ?
                    "<br><span style=\"font-size: 12px; color: #999; font-style: italic;\">Notes: " +
                            item.getNotes() + "</span>" : "";

            html.append(String.format("""
                            <tr style="border-bottom: 1px solid #eeeeee;">
                                <td style="padding: 15px; color: #333333;">
                                    <strong>%s</strong>%s
                                </td>
                                <td style="padding: 15px; text-align: center; color: #666666;">%s</td>
                                <td style="padding: 15px; text-align: right; color: #666666;">%s</td>
                                <td style="padding: 15px; text-align: right; color: #666666;">%s</td>
                            </tr>
                            """,
                    productName,
                    notes,
                    item.getQuantity(),
                    item.getProduct() != null ? item.getProduct().getVolume() : "",
                    unit
            ));
        }

        html.append("""
                    </tbody>
                </table>
                """);

        return html.toString();
    }

}