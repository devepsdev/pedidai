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

    @NotNull(message = "{validation.days.required}")
    @Min(value = 1, message = "{validation.days.min}")
    @Max(value = 90, message = "{validation.days.max}")
    private Integer days;
}
