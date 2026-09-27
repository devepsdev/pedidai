package com.pedidai.api.security;

import com.pedidai.api.entities.Company;
import com.pedidai.api.entities.User;
import com.pedidai.api.repositories.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter: el rol i l'estat es llegeixen de la base de dades")
class JwtAuthenticationFilterTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private UserRepository userRepository;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;
    @InjectMocks private JwtAuthenticationFilter filter;

    private User user;
    private Company company;

    @BeforeEach
    void setUp() throws Exception {
        SecurityContextHolder.clearContext();
        company = Company.builder().id(1L).status(Company.CompanyStatus.ACTIVE)
                .trialEndsAt(LocalDateTime.now().plusDays(5)).build();
        user = User.builder().email("u@bar.test").role(User.UserRole.ADMIN).isActive(true).isDeleted(false)
                .company(company).build();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void withValidToken() {
        when(request.getHeader("Authorization")).thenReturn("Bearer tok");
        when(jwtUtil.getUsernameFromToken("tok")).thenReturn("u@bar.test");
        when(jwtUtil.validateToken("tok", "u@bar.test")).thenReturn(true);
        when(userRepository.findWithCompanyByEmail("u@bar.test")).thenReturn(Optional.of(user));
    }

    private boolean authenticated() {
        return SecurityContextHolder.getContext().getAuthentication() != null;
    }

    @Test
    @DisplayName("sense token la petició continua sense autenticar")
    void noToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(authenticated()).isFalse();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("token vàlid: autentica amb el rol de la base de dades")
    void validToken() throws Exception {
        withValidToken();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_ADMIN");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("usuari desactivat: el token deixa de servir immediatament")
    void inactiveUser() throws Exception {
        user.setIsActive(false);
        withValidToken();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(authenticated()).isFalse();
    }

    @Test
    @DisplayName("prova acabada: el token deixa de servir")
    void trialEnded() throws Exception {
        company.setTrialEndsAt(LocalDateTime.now().minusMinutes(1));
        withValidToken();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(authenticated()).isFalse();
    }

    @Test
    @DisplayName("empresa de pagament (sense data de fi de prova) continua tenint accés")
    void paidCompany() throws Exception {
        company.setTrialEndsAt(null);
        withValidToken();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(authenticated()).isTrue();
    }

    @Test
    @DisplayName("token manipulat: no autentica")
    void invalidToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer dolent");
        when(jwtUtil.getUsernameFromToken("dolent")).thenThrow(new RuntimeException("signatura"));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(authenticated()).isFalse();
        verify(filterChain).doFilter(request, response);
    }
}
