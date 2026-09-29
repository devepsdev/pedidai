package com.pedidai.api.services;

public interface TrialNotificationService {

    /**
     * Envia als clients en prova l'avís «la prova acaba d'aquí a 3 dies» i, quan ja ha acabat,
     * l'avís «la prova ha acabat». Cada avís s'envia una sola vegada. Retorna quants correus s'han enviat.
     */
    int sendTrialNotifications();
}
