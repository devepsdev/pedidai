package com.pedidai.api.security;

import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("SecurityConfig Tests")
class SecurityConfigTest {

    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("PasswordEncoder hauria d'estar configurat amb BCrypt")
    void passwordEncoder_ShouldUseBCrypt() {
        // Given
        SecurityConfig securityConfig = new SecurityConfig();

        // When
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();

        // Then
        assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    @DisplayName("PasswordEncoder hauria d'encriptar contrasenyes correctament")
    void passwordEncoder_ShouldEncryptPasswordsCorrectly() {
        // Given
        SecurityConfig securityConfig = new SecurityConfig();
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        String rawPassword = "password123";

        // When
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // Then
        assertThat(encodedPassword).isNotEqualTo(rawPassword);
        assertThat(passwordEncoder.matches(rawPassword, encodedPassword)).isTrue();
        assertThat(passwordEncoder.matches("wrongpassword", encodedPassword)).isFalse();
    }

    @Test
    @DisplayName("CORS permet només els orígens configurats")
    void corsConfiguration_ShouldAllowSpecifiedDomains() {
        // Given: configuració de producció (sense localhost)
        SecurityConfig securityConfig = new SecurityConfig();
        ReflectionTestUtils.setField(securityConfig, "allowedOrigins",
                List.of("https://pedidai.es", "https://www.pedidai.es", "https://pedidai.com", "https://www.pedidai.com"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");

        // When
        CorsConfigurationSource corsSource = securityConfig.corsConfigurationSource();
        CorsConfiguration corsConfig = corsSource.getCorsConfiguration(request);

        // Then
        assertThat(corsConfig).isNotNull();
        assertThat(corsConfig.getAllowedOriginPatterns()).noneMatch(o -> o.contains("localhost"));
        assertThat(corsConfig.getAllowedOriginPatterns()).contains("https://pedidai.es", "https://www.pedidai.es", "https://pedidai.com", "https://www.pedidai.com");
        assertThat(corsConfig.getAllowedMethods()).contains("GET", "POST", "PUT", "DELETE");
        assertThat(corsConfig.getAllowCredentials()).isTrue();
    }

    @Test
    @DisplayName("CORS hauria de permetre tots els headers")
    void corsConfiguration_ShouldAllowAllHeaders() {
        // Given
        SecurityConfig securityConfig = new SecurityConfig();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");

        // When
        CorsConfigurationSource corsSource = securityConfig.corsConfigurationSource();
        CorsConfiguration corsConfig = corsSource.getCorsConfiguration(request);

        // Then
        assertThat(corsConfig.getAllowedHeaders()).contains("*");
    }

    @Test
    @DisplayName("Contrasenyes diferents haurien de generar hashs diferents")
    void passwordEncoder_ShouldGenerateDifferentHashesForDifferentPasswords() {
        // Given
        SecurityConfig securityConfig = new SecurityConfig();
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();

        // When
        String hash1 = passwordEncoder.encode("password1");
        String hash2 = passwordEncoder.encode("password2");

        // Then
        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("El mateix password hauria de generar hashs diferents cada vegada")
    void passwordEncoder_ShouldGenerateDifferentHashesForSamePassword() {
        // Given
        SecurityConfig securityConfig = new SecurityConfig();
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        String password = "samepassword";

        // When
        String hash1 = passwordEncoder.encode(password);
        String hash2 = passwordEncoder.encode(password);

        // Then
        assertThat(hash1).isNotEqualTo(hash2);
        assertThat(passwordEncoder.matches(password, hash1)).isTrue();
        assertThat(passwordEncoder.matches(password, hash2)).isTrue();
    }
}