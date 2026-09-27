package com.pedidai.api.security;

import com.pedidai.api.entities.Company;
import com.pedidai.api.entities.User;
import com.pedidai.api.exceptions.ForbiddenException;
import com.pedidai.api.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Usuari i empresa de la petició autenticada.
 * Tots els serveis multiempresa han d'obtenir l'empresa d'aquí (mai d'un paràmetre del client)
 * per garantir que cada empresa només accedeix a les seves dades.
 */
@Component
@RequiredArgsConstructor
public class CurrentUser {

    private final UserRepository userRepository;

    public User get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ForbiddenException("error.auth.required");
        }
        return userRepository.findWithCompanyByEmail(auth.getName())
                .orElseThrow(() -> new ForbiddenException("error.auth.required"));
    }

    public Company company() {
        return companyOf(get());
    }

    public Long companyId() {
        return company().getId();
    }

    public static Company companyOf(User user) {
        if (user.getCompany() == null) {
            throw new ForbiddenException("error.user.noCompany");
        }
        return user.getCompany();
    }

    /** Retorna l'usuari actual si és administrador de la seva empresa; si no, 403. */
    public User requireAdmin() {
        User user = get();
        if (user.getRole() != User.UserRole.ADMIN) {
            throw new ForbiddenException("error.user.adminRequired");
        }
        return user;
    }
}
