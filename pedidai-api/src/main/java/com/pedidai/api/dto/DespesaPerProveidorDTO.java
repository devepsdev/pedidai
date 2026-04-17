package com.pedidai.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DespesaPerProveidorDTO {

    private String proveidor;

    private Integer numComandes;

    private BigDecimal despesaTotal;

    private BigDecimal percentatge;
}

