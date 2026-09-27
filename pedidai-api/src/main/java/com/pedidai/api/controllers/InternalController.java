package com.pedidai.api.controllers;

import com.pedidai.api.dto.OpsSummaryDTO;
import com.pedidai.api.services.OpsService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;

/**
 * Dades per a les automatitzacions internes (n8n, al mateix servidor).
 * Només respon a peticions locals; nginx, a més, bloqueja {@code /api/internal/} des de fora.
 */
@Hidden
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalController {

    private final OpsService opsService;

    @GetMapping("/daily-summary")
    public ResponseEntity<OpsSummaryDTO> dailySummary(@RequestParam(defaultValue = "24") int hours,
                                                      HttpServletRequest request) {
        if (!isLocal(request)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(opsService.getSummary(hours));
    }

    /** Local i sense passar per nginx (que sempre afegeix X-Real-IP a les peticions de fora). */
    private static boolean isLocal(HttpServletRequest request) {
        if (request.getHeader("X-Real-IP") != null) {
            return false;
        }
        try {
            return InetAddress.getByName(request.getRemoteAddr()).isLoopbackAddress();
        } catch (Exception e) {
            return false;
        }
    }
}
