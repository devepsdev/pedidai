package com.pedidai.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtendTrialDTO {

    @NotNull(message = "{validation.months.required}")
    @Min(value = 1, message = "{validation.months.min}")
    @Max(value = 24, message = "{validation.months.max}")
    private Integer months;
}
