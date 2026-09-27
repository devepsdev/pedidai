package com.pedidai.api.services;

import java.util.List;
import java.util.Locale;

public interface EmailService {

    void sendPasswordResetEmail(String to, String token, String userName, Locale locale);

    /** Verificació d'email d'un usuari convidat o reenviament de l'enllaç. */
    void sendEmailVerification(String to, String token, String userName, Locale locale);

    /** Benvinguda a l'administrador d'una empresa nova, amb l'enllaç de verificació. */
    void sendWelcomeVerification(String to, String token, String userName, String companyName, Locale locale);

    /**
     * Envia la comanda al proveïdor. És síncron: si falla, llança excepció perquè la comanda
     * no quedi marcada com a enviada. Les respostes del proveïdor van a {@code replyTo}.
     */
    void sendOrderNotification(OrderEmail order, Locale locale);

    record OrderEmail(String to, String replyTo, String supplierContactName, String companyName,
                      String companyAddress, String companyPhone, String orderName,
                      List<OrderEmailLine> lines, String notes) {}

    record OrderEmailLine(String productName, String quantity, String unit, String notes) {}
}
