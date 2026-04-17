package com.pedidai.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderFilterDTO {

    private String orderUuid;

    private String supplierUuid;

    private String userUuid;

    private String searchText;
    private String name;
    private String notes;

    private String status;

    @Min(value = 0, message = "El preu mínim de la comandano pot ser negatiu")
    private BigDecimal minAmount;

    @Min(value = 0, message = "El preu màxim de la comanda no pot ser negatiu")
    private BigDecimal maxAmount;

    // --- Rangs de dates ---
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate deliveryDateFrom;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate deliveryDateTo;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAtFrom;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAtTo;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAtFrom;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAtTo;

    @Min(value = 0, message = "El número de pàgina ha de ser 0 o superior")
    @Builder.Default
    private int page = 0;

    @Min(value = 1, message = "La mida de pàgina ha de ser 1 o superior")
    @Builder.Default
    private int size = 10;

    @Builder.Default
    private String sortBy = "name";

    @Pattern(regexp = "asc|desc", message = "La direcció d'ordenació ha de ser 'asc' o 'desc'")
    @Builder.Default
    private String sortDir = "asc";

    public boolean hasTextFilters() {
        return (name != null && !name.trim().isEmpty()) || (notes != null && !notes.trim().isEmpty());
    }

}