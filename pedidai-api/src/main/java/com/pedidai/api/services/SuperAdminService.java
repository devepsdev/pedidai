package com.pedidai.api.services;

import com.pedidai.api.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface SuperAdminService {
    SuperAdminDashboardDTO getDashboard();
    Page<CompanySummaryDTO> getCompanies(String search, String status, Pageable pageable);
    CompanyDetailDTO getCompanyDetail(String uuid);
    CompanySummaryDTO updateCompanyStatus(String uuid, String status);
    Page<UserAdminDTO> getUsers(String search, String role, String companyUuid, Pageable pageable);
    List<MonthlyStatsDTO> getMonthlyStats();
}
