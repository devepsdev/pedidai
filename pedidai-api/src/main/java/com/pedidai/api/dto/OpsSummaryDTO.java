package com.pedidai.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** Resum d'activitat per a l'equip de PedidAI (correu diari generat amb n8n). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpsSummaryDTO {
    private LocalDateTime generatedAt;
    private int hours;

    /** Registres de les darreres {@code hours} hores. */
    private List<OpsCompanyDTO> newCompanies;
    /** Proves que acaben en els propers 3 dies: bon moment per trucar. */
    private List<OpsCompanyDTO> trialsEndingSoon;
    /** Proves que han acabat en les darreres {@code hours} hores sense contractar. */
    private List<OpsCompanyDTO> trialsExpired;
    /** Proves acabades fa més de 23 dies: les dades s'esborraran en menys d'una setmana. */
    private List<OpsCompanyDTO> deletionSoon;

    private long invoiceLinesRead;
    private long ordersCreated;

    private long totalClients;
    private long inTrial;
    private long paid;
    private long trialOver;
}
