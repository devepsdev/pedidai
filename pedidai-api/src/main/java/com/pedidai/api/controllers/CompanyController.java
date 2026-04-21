package com.pedidai.api.controllers;

import com.pedidai.api.dto.ApiResponseDTO;
import com.pedidai.api.dto.CompanyRegistrationDTO;
import com.pedidai.api.dto.CompanyRequestDTO;
import com.pedidai.api.dto.CompanyResponseDTO;
import com.pedidai.api.dto.MyPlanDTO;
import com.pedidai.api.services.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> registerCompanyWithAdmin(
            @Valid @RequestBody CompanyRegistrationDTO registrationDTO) {

        CompanyResponseDTO company = companyService.registerCompanyWithAdmin(registrationDTO);

        Map<String, Object> response = new HashMap<>();
        response.put("company", company);
        response.put("message", "Empresa y administrador registrats exitosament. " +
                "Si us plau, verifica l'email de l'administrador per activar el compte.");

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(response, "Registre completat exitosament"));
    }

    @GetMapping
    public ResponseEntity<ApiResponseDTO<CompanyResponseDTO>> getCompanyByUuid() {
        CompanyResponseDTO company = companyService.getCompanyByUuid();
        return ResponseEntity.ok(ApiResponseDTO.success(company, "Empresa recuperada exitosament"));
    }

    @PutMapping
    public ResponseEntity<ApiResponseDTO<CompanyResponseDTO>> updateCompany(
            @Valid @RequestBody CompanyRequestDTO companyRequestDTO) {
        CompanyResponseDTO updated = companyService.updateCompany(companyRequestDTO);
        return ResponseEntity.ok(ApiResponseDTO.success(updated, "Empresa actualitzada exitosament"));
    }

    @GetMapping("/my-plan")
    public ResponseEntity<ApiResponseDTO<MyPlanDTO>> getMyPlan() {
        return ResponseEntity.ok(ApiResponseDTO.success(
                companyService.getMyPlan(), "Pla recuperat correctament"));
    }

}