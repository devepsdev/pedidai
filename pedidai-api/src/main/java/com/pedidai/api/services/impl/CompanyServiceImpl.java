package com.pedidai.api.services.impl;

import com.pedidai.api.config.I18nConfig;
import com.pedidai.api.dto.*;
import com.pedidai.api.entities.Company;
import com.pedidai.api.entities.User;
import com.pedidai.api.exceptions.DuplicateResourceException;
import com.pedidai.api.repositories.CompanyRepository;
import com.pedidai.api.repositories.UserRepository;
import com.pedidai.api.security.CurrentUser;
import com.pedidai.api.security.RateLimiter;
import com.pedidai.api.services.CompanyService;
import com.pedidai.api.services.EmailService;
import com.pedidai.api.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    /** Durada de la prova gratuïta. */
    public static final int TRIAL_DAYS = 14;

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final UserService userService;
    private final CurrentUser currentUser;
    private final RateLimiter rateLimiter;

    @Override
    @Transactional
    public LoginResponseDTO registerCompanyWithAdmin(CompanyRegistrationDTO dto, String clientIp) {
        rateLimiter.check("register-ip:" + clientIp, 5, Duration.ofHours(1), "error.tooManyRequests");

        String email = dto.getAdminEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("error.register.emailExists");
        }
        String taxId = blankToNull(dto.getTaxId());
        if (taxId != null && companyRepository.existsByTaxId(taxId)) {
            throw new DuplicateResourceException("error.company.taxIdExists");
        }

        // La prova comença en el moment del registre i l'empresa ja és operativa
        Company company = companyRepository.save(Company.builder()
                .name(dto.getCompanyName().trim())
                .taxId(taxId)
                .email(email)
                .phone(blankToNull(dto.getCompanyPhone()))
                .address(blankToNull(dto.getCompanyAddress()))
                .city(blankToNull(dto.getCompanyCity()))
                .postalCode(blankToNull(dto.getCompanyPostalCode()))
                .status(Company.CompanyStatus.ACTIVE)
                .trialEndsAt(LocalDateTime.now().plusDays(TRIAL_DAYS))
                .build());

        // L'email es verifica després; fins llavors pot fer-ho tot excepte enviar comandes als proveïdors
        String verificationToken = UUID.randomUUID().toString();
        String language = I18nConfig.languageCode(LocaleContextHolder.getLocale());
        User admin = userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString())
                .company(company)
                .email(email)
                .password(passwordEncoder.encode(dto.getAdminPassword()))
                .firstName(dto.getAdminFirstName().trim())
                .lastName(dto.getAdminLastName() != null ? dto.getAdminLastName().trim() : "")
                .role(User.UserRole.ADMIN)
                .language(language)
                .isActive(true)
                .isDeleted(false)
                .emailVerified(false)
                .emailVerificationToken(verificationToken)
                .emailVerificationExpires(LocalDateTime.now().plusHours(48))
                .lastLogin(LocalDateTime.now())
                .build());

        emailService.sendWelcomeVerification(admin.getEmail(), verificationToken, admin.getFirstName(),
                company.getName(), I18nConfig.localeOf(language));

        return userService.issueSession(admin);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyResponseDTO getCompanyByUuid() {
        return mapToResponseDTO(CurrentUser.companyOf(currentUser.requireAdmin()));
    }

    @Override
    @Transactional
    public CompanyResponseDTO updateCompany(CompanyRequestDTO dto) {
        Company company = CurrentUser.companyOf(currentUser.requireAdmin());

        String taxId = blankToNull(dto.getTaxId());
        if (taxId != null && !taxId.equals(company.getTaxId()) && companyRepository.existsByTaxId(taxId)) {
            throw new DuplicateResourceException("error.company.taxIdExists");
        }

        company.setName(dto.getName());
        company.setTaxId(taxId);
        company.setEmail(dto.getEmail());
        company.setPhone(dto.getPhone());
        company.setAddress(dto.getAddress());
        company.setCity(dto.getCity());
        company.setPostalCode(dto.getPostalCode());
        // L'estat de l'empresa (prova, activa, suspesa) només el pot canviar el superadministrador

        return mapToResponseDTO(companyRepository.save(company));
    }

    @Override
    @Transactional(readOnly = true)
    public MyPlanDTO getMyPlan() {
        Company company = currentUser.company();
        return MyPlanDTO.builder()
                .status(company.getStatus().name())
                .trialEndsAt(company.getTrialEndsAt())
                .build();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private CompanyResponseDTO mapToResponseDTO(Company company) {
        return CompanyResponseDTO.builder()
                .uuid(company.getUuid())
                .name(company.getName())
                .taxId(company.getTaxId())
                .email(company.getEmail())
                .phone(company.getPhone())
                .address(company.getAddress())
                .city(company.getCity())
                .postalCode(company.getPostalCode())
                .status(company.getStatus())
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .trialEndsAt(company.getTrialEndsAt())
                .build();
    }
}
