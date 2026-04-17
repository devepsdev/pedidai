package com.pedidai.api.controllers;

import com.pedidai.api.dto.*;
import com.pedidai.api.services.SuperAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/superadmin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Super Admin", description = "Endpoints d'administració global de la plataforma PedidAI")
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard global amb estadístiques de tota la plataforma")
    public ResponseEntity<ApiResponseDTO<SuperAdminDashboardDTO>> getDashboard() {
        return ResponseEntity.ok(ApiResponseDTO.success(
                superAdminService.getDashboard(),
                "Dashboard carregat correctament"));
    }

    @GetMapping("/companies")
    @Operation(summary = "Llistat paginat d'empreses amb filtres opcionals")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<CompanySummaryDTO>>> getCompanies(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<CompanySummaryDTO> result = superAdminService.getCompanies(search, status, pageable);
        return ResponseEntity.ok(ApiResponseDTO.success(
                PagedResponseDTO.of(result),
                "Empreses carregades correctament"));
    }

    @GetMapping("/companies/{uuid}")
    @Operation(summary = "Detall complet d'una empresa")
    public ResponseEntity<ApiResponseDTO<CompanyDetailDTO>> getCompany(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponseDTO.success(
                superAdminService.getCompanyDetail(uuid),
                "Empresa trobada correctament"));
    }

    @PatchMapping("/companies/{uuid}/status")
    @Operation(summary = "Canviar l'estat d'una empresa (ACTIVE, INACTIVE, SUSPENDED)")
    public ResponseEntity<ApiResponseDTO<CompanySummaryDTO>> updateCompanyStatus(
            @PathVariable String uuid,
            @Valid @RequestBody CompanyStatusUpdateDTO dto) {
        return ResponseEntity.ok(ApiResponseDTO.success(
                superAdminService.updateCompanyStatus(uuid, dto.getStatus()),
                "Estat de l'empresa actualitzat correctament"));
    }

    @GetMapping("/users")
    @Operation(summary = "Llistat paginat de tots els usuaris de la plataforma")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<UserAdminDTO>>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String companyUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<UserAdminDTO> result = superAdminService.getUsers(search, role, companyUuid, pageable);
        return ResponseEntity.ok(ApiResponseDTO.success(
                PagedResponseDTO.of(result),
                "Usuaris carregats correctament"));
    }

    @GetMapping("/stats/monthly")
    @Operation(summary = "Estadístiques mensuals dels últims 12 mesos")
    public ResponseEntity<ApiResponseDTO<List<MonthlyStatsDTO>>> getMonthlyStats() {
        return ResponseEntity.ok(ApiResponseDTO.success(
                superAdminService.getMonthlyStats(),
                "Estadístiques mensuals carregades correctament"));
    }
}
