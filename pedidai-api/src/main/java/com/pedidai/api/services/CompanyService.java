package com.pedidai.api.services;

import com.pedidai.api.dto.CompanyRegistrationDTO;
import com.pedidai.api.dto.CompanyRequestDTO;
import com.pedidai.api.dto.CompanyResponseDTO;
import com.pedidai.api.dto.MyPlanDTO;

public interface CompanyService {

    CompanyResponseDTO registerCompanyWithAdmin(CompanyRegistrationDTO registrationDTO);

    CompanyResponseDTO getCompanyByUuid();

    CompanyResponseDTO updateCompany(CompanyRequestDTO companyRequestDTO);

    MyPlanDTO getMyPlan();
}