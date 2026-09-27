package com.pedidai.api.services;

public interface DataRetentionService {

    /**
     * Esborra definitivament les empreses que van acabar la prova gratuïta fa més de 30 dies
     * sense contractar, amb totes les seves dades. Retorna quantes empreses s'han esborrat.
     */
    int purgeExpiredTrials();
}
