package com.pedidai.api.dto;

import com.pedidai.api.entities.Company;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyResponseDTO {

    private String uuid;

    private String name;

    private String taxId;

    private String email;

    private String phone;

    private String address;

    private String city;

    private String postalCode;

    private Company.CompanyStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime trialEndsAt;
}