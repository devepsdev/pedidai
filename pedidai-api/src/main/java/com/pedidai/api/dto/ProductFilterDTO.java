package com.pedidai.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductFilterDTO {

    private String supplierUuid;

    // Filtres de text (cerca parcial, insensible a majúscules
    private String name;        // Nom del producte
    private String description; // Descripció del producte
    private String category; // Categoria del producte

    // Preu mínim del producte (opcional)
    @Min(value = 0, message = "{validation.price.negative}")
    private BigDecimal minPrice;

    // Preu màxim del producte (opcional)
    @Min(value = 0, message = "{validation.price.negative}")
    private BigDecimal maxPrice;

    // Volum del producte (ex: 1, 2, 3)
    private BigDecimal volume;

    // Unitat de mesura del producte (ex: kg, l, cl...)
    private String unit;

    // Filtre d'estat
    private Boolean isActive;   // Estat actiu (true/false/null per tots)

    // Paginació
    @Min(value = 0, message = "{validation.page.number}")
    @Builder.Default
    private int page = 0;

    @Min(value = 1, message = "{validation.page.size}")
    @Builder.Default
    private int size = 10;

    // Ordenació (camps disponibles: name, category, price, unit, createdAt, updatedAt)
    @Builder.Default
    private String sortBy = "name";

    @Pattern(regexp = "asc|desc", message = "{validation.sort.direction}")
    @Builder.Default
    private String sortDir = "asc";

    public boolean hasTextFilters() {
        return (name != null && !name.trim().isEmpty()) ||
                (description != null && !description.trim().isEmpty()) ||
                (category != null && !category.trim().isEmpty());
    }

}