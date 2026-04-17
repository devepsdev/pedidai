package com.pedidai.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportGlobalResponseDTO {

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dataInicial;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dataFinal;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private int totalComandes;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private BigDecimal despesaTotal;

    private BigDecimal comandaMitjana;

    private List<DespesaPerProveidorDTO> despesaProveidors;

    private List<ProducteTopDTO> topProductes;
}

