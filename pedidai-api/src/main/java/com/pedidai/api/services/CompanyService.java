package com.pedidai.api.services;

import com.pedidai.api.dto.CompanyRegistrationDTO;
import com.pedidai.api.dto.CompanyRequestDTO;
import com.pedidai.api.dto.CompanyResponseDTO;

public interface CompanyService {

    CompanyResponseDTO registerCompanyWithAdmin(CompanyRegistrationDTO registrationDTO);

    CompanyResponseDTO getCompanyByUuid();

    CompanyResponseDTO updateCompany(CompanyRequestDTO companyRequestDTO);
}