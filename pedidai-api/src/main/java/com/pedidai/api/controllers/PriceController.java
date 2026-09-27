package com.pedidai.api.controllers;

import com.pedidai.api.dto.ApiResponseDTO;
import com.pedidai.api.dto.PriceOverviewDTO;
import com.pedidai.api.services.PriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/prices")
@RequiredArgsConstructor
public class PriceController {

    private final PriceService priceService;

    /** Comparativa entre proveïdors, pujades de preu i sobrecost estimat dels darrers dies. */
    @GetMapping("/overview")
    public ResponseEntity<ApiResponseDTO<PriceOverviewDTO>> overview(@RequestParam(defaultValue = "90") int days) {
        return ResponseEntity.ok(ApiResponseDTO.success(priceService.getOverview(days), "success.ok"));
    }
}
