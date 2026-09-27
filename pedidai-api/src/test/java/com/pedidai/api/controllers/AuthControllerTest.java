package com.pedidai.api.controllers;

import com.pedidai.api.dto.*;
import com.pedidai.api.services.UserService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthControllerTest {

    private MockMvc mockMvc;
    private UserService userService;

    @BeforeAll
    void setup() {
        userService = Mockito.mock(UserService.class);
        AuthController authController = new AuthController(userService);
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .build();
    }

    // -------------------
    // LOGIN
    // -------------------
    @Test
    void login_shouldReturnOk() throws Exception {
        LoginResponseDTO loginResponse = LoginResponseDTO.builder()
                .token("jwt.token.aqui")
                .user(UserResponseDTO.builder()
                        .email("user@test.com")
                        .firstName("Joan")
                        .lastName("Garcia")
                        .build())
                .build();

        Mockito.when(userService.login(Mockito.any(), Mockito.any()))
                .thenReturn(loginResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@test.com\",\"password\":\"Password1@\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("success.login"))
                .andExpect(jsonPath("$.data.token").value("jwt.token.aqui"))
                .andExpect(jsonPath("$.data.user.email").value("user@test.com"));
    }

    // -------------------
    // FORGOT PASSWORD
    // -------------------
    @Test
    void forgotPassword_shouldReturnOk() throws Exception {
        Mockito.doNothing().when(userService).requestPasswordReset(Mockito.eq("user@test.com"), Mockito.any());

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@test.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("success.password.resetRequested"));
    }

    // -------------------
    // RESET PASSWORD
    // -------------------
    @Test
    void resetPassword_shouldReturnOk() throws Exception {
        PasswordResetDTO dto = new PasswordResetDTO("abc123", "NovaPass1@");
        Mockito.doNothing().when(userService).resetPassword(dto);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"abc123\",\"newPassword\":\"NovaPass1@\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("success.password.reset"));
    }

    // -------------------
    // VERIFY EMAIL
    // -------------------
    @Test
    void verifyEmail_shouldReturnOk() throws Exception {
        EmailVerificationDTO dto = new EmailVerificationDTO("token123");
        Mockito.doNothing().when(userService).verifyEmail("token123");

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"token123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("success.verification.done"));
    }

    // -------------------
    // RESEND VERIFICATION
    // -------------------
    @Test
    void resendVerification_shouldReturnOk() throws Exception {
        PasswordResetRequestDTO dto = new PasswordResetRequestDTO("user@test.com");
        Mockito.doNothing().when(userService).resendVerificationEmail(Mockito.eq("user@test.com"), Mockito.any());

        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@test.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("success.verification.resent"));
    }
}
