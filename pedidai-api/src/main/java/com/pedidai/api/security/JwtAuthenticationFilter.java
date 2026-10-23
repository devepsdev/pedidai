package com.pedidai.api.security;

import com.pedidai.api.entities.Company;
import com.pedidai.api.entities.User;
import com.pedidai.api.repositories.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Autentica cada petició amb el token JWT. El rol i l'estat es llegeixen de la base de dades
 * (no del token), de manera que desactivar un usuari, acabar-se la prova o canviar la contrasenya
 * té efecte immediat encara que el token duri 30 dies.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = authHeader.substring(7);
            String email = null;
            try {
                email = jwtUtil.getUsernameFromToken(token);
            } catch (Exception e) {
                log.debug("Token JWT no vàlid: {}", e.getMessage());
            }

            if (email != null && jwtUtil.validateToken(token, email)) {
                Integer tokenVersion = jwtUtil.getTokenVersion(token);
                userRepository.findWithCompanyByEmail(email)
                        .filter(user -> isCurrentSession(tokenVersion, user))
                        .filter(this::canUseService)
                        .ifPresent(user -> {
                            var auth = new UsernamePasswordAuthenticationToken(user.getEmail(), null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
                            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        });
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * El token ha de ser de la sessió vigent: si l'usuari ha canviat la contrasenya, la versió ha pujat.
     * Els tokens sense versió (d'una hora, d'abans d'aquest canvi) valen fins que caduquen.
     */
    private boolean isCurrentSession(Integer tokenVersion, User user) {
        return tokenVersion == null || tokenVersion.equals(user.getTokenVersion());
    }

    private boolean canUseService(User user) {
        if (!Boolean.TRUE.equals(user.getIsActive()) || Boolean.TRUE.equals(user.getIsDeleted())) {
            return false;
        }
        if (user.getRole() == User.UserRole.SUPER_ADMIN) {
            return true;
        }
        Company company = user.getCompany();
        if (company == null) {
            return false;
        }
        boolean trialOver = company.getTrialEndsAt() != null && LocalDateTime.now().isAfter(company.getTrialEndsAt());
        return company.getStatus() != Company.CompanyStatus.SUSPENDED
                && company.getStatus() != Company.CompanyStatus.INACTIVE
                && !trialOver;
    }
}
