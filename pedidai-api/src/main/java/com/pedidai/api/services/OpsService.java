package com.pedidai.api.services;

import com.pedidai.api.dto.OpsCompanyDTO;
import com.pedidai.api.dto.OpsSummaryDTO;

/** Dades per a les alertes internes de l'equip de PedidAI (les envia n8n per correu). */
public interface OpsService {

    /** Resum dels registres, proves i activitat de les darreres {@code hours} hores. */
    OpsSummaryDTO getSummary(int hours);

    /** Fitxa d'una empresa client per a l'avís de registre nou. */
    OpsCompanyDTO getCompany(Long companyId);
}
