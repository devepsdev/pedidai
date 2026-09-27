// Actualitzar el SupplierSearchDTO.java canviant el camp "name" per "searchText":

package com.pedidai.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierSearchDTO {

    private String searchText;

    @Min(value = 0, message = "{validation.page.number}")
    @Builder.Default
    private int page = 0;

    @Min(value = 1, message = "{validation.page.size}")
    @Builder.Default
    private int size = 10;

    @Builder.Default
    private String sortBy = "name";

    @Pattern(regexp = "asc|desc", message = "{validation.sort.direction}")
    @Builder.Default
    private String sortDir = "asc";
}