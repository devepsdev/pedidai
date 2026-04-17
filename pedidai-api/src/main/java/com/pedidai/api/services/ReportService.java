package com.pedidai.api.services;

import com.pedidai.api.dto.*;


public interface ReportService {


    DashboardResponseDTO dashboardInfo();

    ReportGlobalResponseDTO globalInfo(PeriodRequestDTO dto);

    byte[] generateGlobalInfoPDF(ReportGlobalResponseDTO dto);

}
