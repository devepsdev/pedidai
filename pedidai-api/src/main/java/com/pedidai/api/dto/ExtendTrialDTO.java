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

    @NotNull(message = "El número de mesos és obligatori")
    @Min(value = 1, message = "El mínim és 1 mes")
    @Max(value = 24, message = "El màxim és 24 mesos")
    private Integer months;
}
