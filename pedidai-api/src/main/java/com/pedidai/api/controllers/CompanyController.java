package com.pedidai.api.controllers;

import com.pedidai.api.dto.*;
import com.pedidai.api.security.ClientIp;
import com.pedidai.api.services.CompanyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    /** Alta pública: crea l'empresa, inicia la prova i retorna la sessió ja iniciada. */
    @PostMapping("/register")
    public ResponseEntity<ApiResponseDTO<LoginResponseDTO>> registerCompanyWithAdmin(
            @Valid @RequestBody CompanyRegistrationDTO registrationDTO, HttpServletRequest request) {
        LoginResponseDTO session = companyService.registerCompanyWithAdmin(registrationDTO, ClientIp.of(request));
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(session, "success.register"));
    }

    @GetMapping
    public ResponseEntity<ApiResponseDTO<CompanyResponseDTO>> getCompanyByUuid() {
        return ResponseEntity.ok(ApiResponseDTO.success(companyService.getCompanyByUuid(), "success.ok"));
    }

    @PutMapping
    public ResponseEntity<ApiResponseDTO<CompanyResponseDTO>> updateCompany(
            @Valid @RequestBody CompanyRequestDTO companyRequestDTO) {
        CompanyResponseDTO updated = companyService.updateCompany(companyRequestDTO);
        return ResponseEntity.ok(ApiResponseDTO.success(updated, "success.company.updated"));
    }

    @GetMapping("/my-plan")
    public ResponseEntity<ApiResponseDTO<MyPlanDTO>> getMyPlan() {
        return ResponseEntity.ok(ApiResponseDTO.success(companyService.getMyPlan(), "success.ok"));
    }
}
