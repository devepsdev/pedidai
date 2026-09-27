package com.pedidai.api.controllers;

import com.pedidai.api.dto.*;
import com.pedidai.api.security.ClientIp;
import com.pedidai.api.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
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
    public ResponseEntity<ApiResponseDTO<LoginResponseDTO>> login(@Valid @RequestBody LoginRequestDTO loginDTO,
                                                                  HttpServletRequest request) {
        LoginResponseDTO response = userService.login(loginDTO, ClientIp.of(request));
        return ResponseEntity.ok(ApiResponseDTO.success(response, "success.login"));
    }

    /** Sempre respon igual, existeixi o no el compte, per no revelar quins emails estan registrats. */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponseDTO<Void>> forgotPassword(@Valid @RequestBody PasswordResetRequestDTO requestDTO,
                                                               HttpServletRequest request) {
        userService.requestPasswordReset(requestDTO.getEmail(), ClientIp.of(request));
        return ResponseEntity.ok(ApiResponseDTO.success(null, "success.password.resetRequested"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponseDTO<Void>> resetPassword(@Valid @RequestBody PasswordResetDTO passwordResetDTO) {
        userService.resetPassword(passwordResetDTO);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "success.password.reset"));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponseDTO<Void>> verifyEmail(@Valid @RequestBody EmailVerificationDTO verificationDTO) {
        userService.verifyEmail(verificationDTO.getToken());
        return ResponseEntity.ok(ApiResponseDTO.success(null, "success.verification.done"));
    }

    /** Públic (sense sessió): mateixa resposta existeixi o no el compte. */
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponseDTO<Void>> resendVerification(@Valid @RequestBody PasswordResetRequestDTO requestDTO,
                                                                   HttpServletRequest request) {
        userService.resendVerificationEmail(requestDTO.getEmail(), ClientIp.of(request));
        return ResponseEntity.ok(ApiResponseDTO.success(null, "success.verification.resent"));
    }
}
