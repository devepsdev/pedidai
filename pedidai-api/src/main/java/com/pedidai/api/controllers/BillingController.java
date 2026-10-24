package com.pedidai.api.controllers;

import com.pedidai.api.dto.ApiResponseDTO;
import com.pedidai.api.dto.BillingUrlDTO;
import com.pedidai.api.dto.LoginRequestDTO;
import com.pedidai.api.security.ClientIp;
import com.pedidai.api.services.BillingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Pagament de la subscripció amb Stripe. */
@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    /** Públic: la pantalla de login l'usa per decidir si ofereix «Contratar ahora» quan la prova ha acabat. */
    @GetMapping("/enabled")
    public ResponseEntity<ApiResponseDTO<Boolean>> isEnabled() {
        return ResponseEntity.ok(ApiResponseDTO.success(billingService.isEnabled(), "success.ok"));
    }

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponseDTO<BillingUrlDTO>> startCheckout() {
        return ResponseEntity.ok(ApiResponseDTO.success(billingService.startCheckout(), "success.ok"));
    }

    /** Públic: l'empresa amb la prova acabada ja no pot iniciar sessió; l'administrador s'identifica aquí. */
    @PostMapping("/checkout-after-trial")
    public ResponseEntity<ApiResponseDTO<BillingUrlDTO>> startCheckoutAfterTrial(
            @Valid @RequestBody LoginRequestDTO credentials, HttpServletRequest request) {
        BillingUrlDTO url = billingService.startCheckoutAfterTrial(credentials, ClientIp.of(request));
        return ResponseEntity.ok(ApiResponseDTO.success(url, "success.ok"));
    }

    @PostMapping("/portal")
    public ResponseEntity<ApiResponseDTO<BillingUrlDTO>> openPortal() {
        return ResponseEntity.ok(ApiResponseDTO.success(billingService.openPortal(), "success.ok"));
    }

    /** Públic: avisos de Stripe. La signatura es comprova amb el secret del webhook abans de fer res. */
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String payload,
                                        @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        billingService.handleWebhook(payload, signature == null ? "" : signature);
        return ResponseEntity.ok().build();
    }
}
