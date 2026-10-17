package com.pedidai.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Comanda enviada al proveïdor a la qual probablement correspon un albarà o factura. La web compara les
 * seves línies amb les de l'albarà (quantitats, preus, el que falta i el que sobra) mentre l'usuari el revisa.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceOrderDTO {
    private String uuid;
    private String name;
    private String status;
    private LocalDate date;
    private List<Item> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String productUuid;
        private String productName;
        private BigDecimal quantity;
        private String unit;
        private BigDecimal unitPrice;
    }
}
