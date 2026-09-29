package com.pedidai.api.services.impl;

import com.pedidai.api.config.I18nConfig;
import com.pedidai.api.dto.*;
import com.pedidai.api.entities.Company;
import com.pedidai.api.entities.User;
import com.pedidai.api.exceptions.BadRequestException;
import com.pedidai.api.exceptions.DuplicateResourceException;
import com.pedidai.api.exceptions.ForbiddenException;
import com.pedidai.api.exceptions.ResourceNotFoundException;
import com.pedidai.api.repositories.CompanyRepository;
import com.pedidai.api.repositories.UserRepository;
import com.pedidai.api.security.CurrentUser;
import com.pedidai.api.security.JwtUtil;
import com.pedidai.api.security.RateLimiter;
import com.pedidai.api.services.EmailService;
import com.pedidai.api.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private static final int LOGIN_MAX_FAILS_PER_ACCOUNT = 8;
    private static final int LOGIN_MAX_FAILS_PER_IP = 40;
    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUser currentUser;
    private final RateLimiter rateLimiter;

    // ───────────────────────── Sessió ─────────────────────────

    @Override
    public LoginResponseDTO login(LoginRequestDTO loginDTO, String clientIp) {
        String email = loginDTO.getEmail().trim().toLowerCase(Locale.ROOT);
        String accountKey = "login-fail:" + email;
        String ipKey = "login-fail-ip:" + clientIp;

        // Els comptadors només sumen intents fallits; si ja estan al límit, es bloqueja abans de comprovar res
        if (!rateLimiter.tryAcquire(accountKey + ":probe", LOGIN_MAX_FAILS_PER_ACCOUNT, LOGIN_WINDOW)
                | !rateLimiter.tryAcquire(ipKey + ":probe", LOGIN_MAX_FAILS_PER_IP, LOGIN_WINDOW)) {
            throw new com.pedidai.api.exceptions.TooManyRequestsException("error.auth.tooManyAttempts");
        }

        User user = userRepository.findByEmail(email).orElse(null);
        // Mateix missatge si l'usuari no existeix, està esborrat o la contrasenya és incorrecta
        if (user == null || Boolean.TRUE.equals(user.getIsDeleted())
                || !passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BadRequestException("error.auth.invalidCredentials");
        }
        // Login correcte: no compta com a intent fallit
        rateLimiter.reset(accountKey + ":probe");

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new ForbiddenException("error.auth.userInactive");
        }

        if (user.getRole() != User.UserRole.SUPER_ADMIN) {
            checkCompanyCanUseService(user.getCompany());
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        return issueSession(user);
    }

    @Override
    public LoginResponseDTO issueSession(User user) {
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return LoginResponseDTO.builder()
                .token(token)
                .type("Bearer")
                .user(mapToResponseDTO(user))
                .build();
    }

    /** Llança excepció si l'empresa està suspesa o la prova ha acabat sense contractar. */
    private void checkCompanyCanUseService(Company company) {
        if (company.getStatus() == Company.CompanyStatus.SUSPENDED) {
            throw new ForbiddenException("error.auth.companySuspended");
        }
        if (company.getTrialEndsAt() != null && LocalDateTime.now().isAfter(company.getTrialEndsAt())
                && company.getStatus() != Company.CompanyStatus.INACTIVE) {
            company.setStatus(Company.CompanyStatus.INACTIVE);
            companyRepository.save(company);
        }
        if (company.getStatus() == Company.CompanyStatus.INACTIVE) {
            throw new ForbiddenException("error.auth.trialEnded");
        }
    }

    // ───────────────────────── Contrasenya i verificació ─────────────────────────

    @Override
    public void requestPasswordReset(String email, String clientIp) {
        rateLimiter.check("reset-ip:" + clientIp, 10, Duration.ofHours(1), "error.tooManyRequests");
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        // Resposta idèntica tant si el compte existeix com si no, per no revelar quins emails hi ha registrats
        if (!rateLimiter.tryAcquire("reset-email:" + normalized, 3, Duration.ofHours(1))) {
            return;
        }
        userRepository.findByEmail(normalized)
                .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
                .ifPresent(user -> {
                    String resetToken = UUID.randomUUID().toString();
                    user.setPasswordResetToken(resetToken);
                    user.setPasswordResetExpires(LocalDateTime.now().plusHours(1));
                    userRepository.save(user);
                    emailService.sendPasswordResetEmail(user.getEmail(), resetToken, user.getFirstName(),
                            I18nConfig.localeOf(user.getLanguage()));
                });
    }

    @Override
    public void resetPassword(PasswordResetDTO passwordResetDTO) {
        User user = userRepository.findByValidResetToken(passwordResetDTO.getToken(), LocalDateTime.now())
                .orElseThrow(() -> new BadRequestException("error.auth.invalidResetToken"));

        user.setPassword(passwordEncoder.encode(passwordResetDTO.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpires(null);
        // Si ha rebut l'enllaç al correu, l'adreça queda verificada
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    @Override
    public void verifyEmail(String token) {
        User user = userRepository.findByValidVerificationToken(token, LocalDateTime.now())
                .orElseThrow(() -> new BadRequestException("error.auth.invalidVerificationToken"));

        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationExpires(null);

        Company company = user.getCompany();
        if (company != null && company.getStatus() == Company.CompanyStatus.PENDING) {
            company.setStatus(Company.CompanyStatus.ACTIVE);
            companyRepository.save(company);
        }
        userRepository.save(user);
    }

    @Override
    public void resendVerificationEmail(String email, String clientIp) {
        rateLimiter.check("verify-ip:" + clientIp, 10, Duration.ofHours(1), "error.tooManyRequests");
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (!rateLimiter.tryAcquire("verify-email:" + normalized, 3, Duration.ofHours(1))) {
            return;
        }
        userRepository.findByEmail(normalized)
                .filter(u -> !Boolean.TRUE.equals(u.getEmailVerified()) && !Boolean.TRUE.equals(u.getIsDeleted()))
                .ifPresent(this::sendNewVerification);
    }

    @Override
    public void resendMyVerificationEmail() {
        User user = currentUser.get();
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BadRequestException("error.auth.alreadyVerified");
        }
        rateLimiter.check("verify-email:" + user.getEmail(), 3, Duration.ofHours(1), "error.tooManyRequests");
        sendNewVerification(user);
    }

    private void sendNewVerification(User user) {
        String verificationToken = UUID.randomUUID().toString();
        user.setEmailVerificationToken(verificationToken);
        user.setEmailVerificationExpires(LocalDateTime.now().plusHours(48));
        userRepository.save(user);
        emailService.sendEmailVerification(user.getEmail(), verificationToken, user.getFirstName(),
                I18nConfig.localeOf(user.getLanguage()));
    }

    // ───────────────────────── Usuari actual ─────────────────────────

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getMe() {
        return mapToResponseDTO(currentUser.get());
    }

    @Override
    public UserResponseDTO updateMyLanguage(String language) {
        if (!"ca".equals(language) && !"es".equals(language)) {
            throw new BadRequestException("error.user.invalidLanguage");
        }
        User user = currentUser.get();
        user.setLanguage(language);
        return mapToResponseDTO(userRepository.save(user));
    }

    // ───────────────────────── Gestió d'usuaris de l'empresa (només administradors) ─────────────────────────

    @Override
    public UserResponseDTO registerUser(UserRegistrationDTO registrationDTO) {
        User admin = currentUser.requireAdmin();
        String email = registrationDTO.getEmail().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("error.user.emailExists", email);
        }

        String verificationToken = UUID.randomUUID().toString();
        User user = User.builder()
                .uuid(UUID.randomUUID().toString())
                .company(CurrentUser.companyOf(admin))
                .email(email)
                .password(passwordEncoder.encode(registrationDTO.getPassword()))
                .firstName(registrationDTO.getFirstName())
                .lastName(registrationDTO.getLastName() != null ? registrationDTO.getLastName() : "")
                .role(assignableRole(registrationDTO.getRole(), User.UserRole.USER))
                .phone(registrationDTO.getPhone())
                .language(admin.getLanguage())
                .isActive(true)
                .isDeleted(false)
                .emailVerified(false)
                .emailVerificationToken(verificationToken)
                .emailVerificationExpires(LocalDateTime.now().plusHours(48))
                .build();

        User savedUser = userRepository.save(user);
        emailService.sendEmailVerification(savedUser.getEmail(), verificationToken, savedUser.getFirstName(),
                I18nConfig.localeOf(savedUser.getLanguage()));
        return mapToResponseDTO(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDTO> getAllUsersPaginated(Pageable pageable) {
        User admin = currentUser.requireAdmin();
        return userRepository.findByCompanyUuidAndIsDeletedFalse(CurrentUser.companyOf(admin).getUuid(), pageable)
                .map(this::mapToResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserByUuid(String uuid) {
        User admin = currentUser.requireAdmin();
        return mapToResponseDTO(findInCompany(uuid, admin));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDTO> searchUsersByText(String searchText, Pageable pageable) {
        User admin = currentUser.requireAdmin();
        return userRepository.findByCompanyIdAndMultipleFieldsContainingNoDeleted(
                CurrentUser.companyOf(admin).getId(), searchText, pageable).map(this::mapToResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDTO> searchUsersWithFilters(UserFilterDTO filterDTO, Pageable pageable) {
        User admin = currentUser.requireAdmin();
        return userRepository.findByCompanyIdAndCriteriaActive(
                CurrentUser.companyOf(admin).getId(),
                filterDTO.getEmail(),
                filterDTO.getFirstName(),
                filterDTO.getLastName(),
                filterDTO.getPhone(),
                filterDTO.getIsActive(),
                filterDTO.getEmailVerified(),
                filterDTO.getRole(),
                pageable
        ).map(this::mapToResponseDTO);
    }

    @Override
    public UserResponseDTO updateUser(String uuid, UserRequestDTO userRequestDTO) {
        User admin = currentUser.requireAdmin();
        User user = findInCompany(uuid, admin);
        boolean isSelf = user.getId().equals(admin.getId());

        String newEmail = userRequestDTO.getEmail().trim().toLowerCase(Locale.ROOT);
        if (!user.getEmail().equalsIgnoreCase(newEmail)) {
            if (userRepository.existsByEmail(newEmail)) {
                throw new DuplicateResourceException("error.user.emailExists", newEmail);
            }
            // Un email nou s'ha de tornar a verificar abans de poder enviar comandes
            user.setEmail(newEmail);
            user.setEmailVerified(false);
            sendNewVerification(user);
        }

        if (userRequestDTO.getFirstName() != null) {
            user.setFirstName(userRequestDTO.getFirstName());
        }
        if (userRequestDTO.getLastName() != null) {
            user.setLastName(userRequestDTO.getLastName());
        }
        user.setPhone(userRequestDTO.getPhone());

        if (userRequestDTO.getRole() != null && userRequestDTO.getRole() != user.getRole()) {
            if (isSelf) {
                throw new ForbiddenException("error.user.cannotChangeOwnRole");
            }
            user.setRole(assignableRole(userRequestDTO.getRole(), user.getRole()));
        }

        if (userRequestDTO.getIsActive() != null && !userRequestDTO.getIsActive().equals(user.getIsActive())) {
            if (isSelf) {
                throw new ForbiddenException("error.user.cannotDeactivateSelf");
            }
            user.setIsActive(userRequestDTO.getIsActive());
        }

        return mapToResponseDTO(userRepository.save(user));
    }

    @Override
    public UserResponseDTO changeUserStatus(String uuid, Boolean isActive) {
        User admin = currentUser.requireAdmin();
        User user = findInCompany(uuid, admin);
        if (user.getId().equals(admin.getId())) {
            throw new ForbiddenException("error.user.cannotDeactivateSelf");
        }
        user.setIsActive(isActive);
        return mapToResponseDTO(userRepository.save(user));
    }

    @Override
    public void changePassword(String uuid, PasswordChangeDTO passwordChangeDTO) {
        User user = currentUser.get();
        // Cada usuari només pot canviar la seva pròpia contrasenya
        if (!user.getUuid().equals(uuid)) {
            throw new ForbiddenException("error.forbidden");
        }
        if (!passwordEncoder.matches(passwordChangeDTO.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("error.user.wrongCurrentPassword");
        }
        user.setPassword(passwordEncoder.encode(passwordChangeDTO.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    public void deleteUser(String uuid) {
        User admin = currentUser.requireAdmin();
        User user = findInCompany(uuid, admin);
        if (user.getId().equals(admin.getId())) {
            throw new ForbiddenException("error.user.cannotDeleteSelf");
        }
        user.setIsDeleted(true);
        user.setIsActive(false);
        // L'email queda lliure per tornar-lo a donar d'alta i no es conserva una dada personal que ja no cal
        user.setEmail("esborrat-" + user.getUuid() + "@invalid");
        user.setEmailVerificationToken(null);
        user.setPasswordResetToken(null);
        userRepository.save(user);
    }

    // ───────────────────────── Utilitats ─────────────────────────

    private User findInCompany(String uuid, User admin) {
        return userRepository.findByUuidAndCompany_Id(uuid, CurrentUser.companyOf(admin).getId())
                .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("error.user.notFound"));
    }

    /** Un administrador d'empresa només pot assignar els rols ADMIN o USER, mai SUPER_ADMIN. */
    private static User.UserRole assignableRole(User.UserRole requested, User.UserRole fallback) {
        if (requested == null) {
            return fallback;
        }
        if (requested == User.UserRole.SUPER_ADMIN) {
            throw new ForbiddenException("error.user.roleNotAllowed");
        }
        return requested;
    }

    /** Idioma de la petició actual en el format que es desa ("ca"/"es"). */
    public static String requestLanguage() {
        return I18nConfig.languageCode(LocaleContextHolder.getLocale());
    }

    private UserResponseDTO mapToResponseDTO(User user) {
        return UserResponseDTO.builder()
                .uuid(user.getUuid())
                .companyUuid(user.getCompany() != null ? user.getCompany().getUuid() : null)
                .companyName(user.getCompany() != null ? user.getCompany().getName() : null)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .phone(user.getPhone())
                .language(user.getLanguage())
                .isActive(user.getIsActive())
                .isDeleted(user.getIsDeleted())
                .emailVerified(user.getEmailVerified())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
