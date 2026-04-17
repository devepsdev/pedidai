package com.pedidai.api.controllers;

import com.pedidai.api.dto.*;
import com.pedidai.api.exceptions.ResourceNotFoundException;
import com.pedidai.api.services.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("dashboard")
    public ResponseEntity<ApiResponseDTO<DashboardResponseDTO>> dashboardInfo(){

        // Servei
        DashboardResponseDTO dashboard = reportService.dashboardInfo();

        // Retorn
        return ResponseEntity.ok(ApiResponseDTO.success(dashboard, "Informació per dashboard correcta."));
    }

    @GetMapping("global")
    public ResponseEntity<ApiResponseDTO<ReportGlobalResponseDTO>> globalInfo(@RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {

        LocalDateTime start = parseDate(startDate,false);
        LocalDateTime end = parseDate(endDate,true);

        // Creem DTO a partir dels paràmetres
        PeriodRequestDTO dto = PeriodRequestDTO.builder().dataInicial(start).dataFinal(end).build();

        // Servei
        ReportGlobalResponseDTO report = reportService.globalInfo(dto);

        // Retorn
        return ResponseEntity.ok(ApiResponseDTO.success(report, "Informació per el report correcta."));
    }

    @GetMapping("global/pdf")
    public ResponseEntity<byte[]> globalInfoPDF(@RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {

        LocalDateTime start = parseDate(startDate,false);
        LocalDateTime end = parseDate(endDate,true);

        // Creem DTO a partir dels paràmetres
        PeriodRequestDTO dto = PeriodRequestDTO.builder().dataInicial(start).dataFinal(end).build();

        // Servei
        ReportGlobalResponseDTO report = reportService.globalInfo(dto);

        byte[] pdfBytes = reportService.generateGlobalInfoPDF(report);

        // Retornem pdf com resposta
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd");
        String startStr = start.format(formatter);
        String endStr = end.format(formatter);
        String titolPdf = "Report_Global_PedidAI_de_" + startStr + "_a_" + endStr + ".pdf";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.builder("inline")
                .filename(titolPdf).build());

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    private LocalDateTime parseDate(String dateStr, boolean endOfDay) {
        if (dateStr == null || dateStr.isBlank()) {
            throw new ResourceNotFoundException("Format de data no vàlid");
        }

        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,              // 2025-12-07T14:23:59
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"), // 2025-12-07 14:23:59
                DateTimeFormatter.ISO_LOCAL_DATE                    // 2025-12-07
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                if (formatter == DateTimeFormatter.ISO_LOCAL_DATE) {
                    LocalDate date = LocalDate.parse(dateStr, formatter);
                    return endOfDay ? date.atTime(LocalTime.MAX) : date.atStartOfDay();
                }
                return LocalDateTime.parse(dateStr, formatter);
            } catch (DateTimeParseException e) {
                // Provem els següents parsers
            }
        }

        // Si cap parser ha funcionat
        throw new ResourceNotFoundException("Format de data no vàlid");
    }

}