package com.pedidai.api.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class CompanyStatusUpdateDTO {
    @NotBlank
    private String status;
}
