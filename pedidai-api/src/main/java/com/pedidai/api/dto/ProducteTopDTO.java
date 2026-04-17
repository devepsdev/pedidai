package com.pedidai.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ProducteTopDTO {

    private String nomProducte;

    private BigDecimal quantitatTotal;

    private BigDecimal despesaTotal;
}