package com.pedidai.api.controllers;

import com.pedidai.api.dto.*;
import com.pedidai.api.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDTO<LoginResponseDTO>> login(@Valid @RequestBody LoginRequestDTO loginDTO) {
        LoginResponseDTO response = userService.login(loginDTO);
        return ResponseEntity.ok(ApiResponseDTO.success(response, "Inici de sessió correcte"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponseDTO<Void>> forgotPassword(@Valid @RequestBody PasswordResetRequestDTO requestDTO) {
        userService.requestPasswordReset(requestDTO.getEmail());
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Correu de recuperació enviat correctament"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponseDTO<Void>> resetPassword(@Valid @RequestBody PasswordResetDTO passwordResetDTO) {
        userService.resetPassword(passwordResetDTO);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Contrasenya restablerta correctament"));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponseDTO<Void>> verifyEmail(@Valid @RequestBody EmailVerificationDTO verificationDTO) {
        userService.verifyEmail(verificationDTO.getToken());
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Correu electrònic verificat correctament. Ja pots iniciar sessió."));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponseDTO<Void>> resendVerification(@Valid @RequestBody PasswordResetRequestDTO requestDTO) {
        userService.resendVerificationEmail(requestDTO.getEmail());
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Correu de verificació reenviat correctament"));
    }
}
