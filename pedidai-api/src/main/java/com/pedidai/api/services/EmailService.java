package com.pedidai.api.services;

import java.time.LocalDate;
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

    /** Avís al client: la prova gratuïta acaba d'aquí a pocs dies. Síncron: llança excepció si falla. */
    void sendTrialEndingSoon(TrialEmail trial, Locale locale);

    /** Avís al client: la prova ha acabat, el compte queda en pausa i quan s'esborraran les dades. Síncron. */
    void sendTrialEnded(TrialEmail trial, Locale locale);

    /** Dades dels avisos de prova: destinatari, empresa, dates i activitat durant la prova. */
    record TrialEmail(String to, String userName, String companyName, LocalDate trialEnd, LocalDate deletionDate,
                      long invoiceLines, long suppliers, long orders) {}

    record OrderEmail(String to, String replyTo, String supplierContactName, String companyName,
                      String companyAddress, String companyPhone, String orderName,
                      List<OrderEmailLine> lines, String notes) {}

    record OrderEmailLine(String productName, String quantity, String unit, String notes) {}
}
