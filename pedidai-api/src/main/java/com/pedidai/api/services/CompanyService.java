package com.pedidai.api.services;

import com.pedidai.api.dto.*;

public interface CompanyService {

    /** Crea l'empresa i el seu administrador, inicia la prova gratuïta i retorna la sessió ja iniciada. */
    LoginResponseDTO registerCompanyWithAdmin(CompanyRegistrationDTO registrationDTO, String clientIp);

    CompanyResponseDTO getCompanyByUuid();

    CompanyResponseDTO updateCompany(CompanyRequestDTO companyRequestDTO);

    MyPlanDTO getMyPlan();
}
