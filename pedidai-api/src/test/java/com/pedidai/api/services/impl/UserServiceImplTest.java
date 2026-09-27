package com.pedidai.api.services.impl;

import com.pedidai.api.dto.*;
import com.pedidai.api.entities.Company;
import com.pedidai.api.entities.User;
import com.pedidai.api.exceptions.BadRequestException;
import com.pedidai.api.exceptions.ForbiddenException;
import com.pedidai.api.exceptions.ResourceNotFoundException;
import com.pedidai.api.exceptions.TooManyRequestsException;
import com.pedidai.api.repositories.CompanyRepository;
import com.pedidai.api.repositories.UserRepository;
import com.pedidai.api.security.CurrentUser;
import com.pedidai.api.security.JwtUtil;
import com.pedidai.api.security.RateLimiter;
import com.pedidai.api.services.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl: sessió, aïllament entre empreses i rols")
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private EmailService emailService;
    @Mock private JwtUtil jwtUtil;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private CurrentUser currentUser;

    private RateLimiter rateLimiter;
    private UserServiceImpl service;
    private Company company;
    private User admin;

    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiter();
        service = new UserServiceImpl(userRepository, companyRepository, emailService, jwtUtil, passwordEncoder,
                currentUser, rateLimiter);
        company = Company.builder().id(1L).uuid("company-1").name("Bar Prova")
                .status(Company.CompanyStatus.ACTIVE).trialEndsAt(LocalDateTime.now().plusDays(10)).build();
        admin = user(10L, "admin-uuid", "admin@bar.test", User.UserRole.ADMIN);
    }

    private User user(Long id, String uuid, String email, User.UserRole role) {
        return User.builder().id(id).uuid(uuid).email(email).password("hash").firstName("Nom").lastName("")
                .role(role).company(company).isActive(true).isDeleted(false).emailVerified(true).language("ca").build();
    }

    private LoginRequestDTO login(String email, String password) {
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setEmail(email);
        dto.setPassword(password);
        return dto;
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("usuari inexistent i contrasenya incorrecta donen el mateix error")
        void sameErrorForUnknownUserAndWrongPassword() {
            when(userRepository.findByEmail("nobody@bar.test")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("admin@bar.test")).thenReturn(Optional.of(admin));
            when(passwordEncoder.matches("mala", "hash")).thenReturn(false);

            assertThatThrownBy(() -> service.login(login("nobody@bar.test", "mala"), "1.1.1.1"))
                    .isInstanceOf(BadRequestException.class).hasMessage("error.auth.invalidCredentials");
            assertThatThrownBy(() -> service.login(login("admin@bar.test", "mala"), "1.1.1.1"))
                    .isInstanceOf(BadRequestException.class).hasMessage("error.auth.invalidCredentials");
        }

        @Test
        @DisplayName("un usuari esborrat no pot entrar i no es revela que existeix")
        void deletedUserLooksLikeUnknown() {
            admin.setIsDeleted(true);
            when(userRepository.findByEmail("admin@bar.test")).thenReturn(Optional.of(admin));

            assertThatThrownBy(() -> service.login(login("admin@bar.test", "Bar12345"), "1.1.1.1"))
                    .hasMessage("error.auth.invalidCredentials");
        }

        @Test
        @DisplayName("amb la prova acabada, l'empresa passa a inactiva i es bloqueja l'accés")
        void trialEnded() {
            company.setTrialEndsAt(LocalDateTime.now().minusMinutes(1));
            when(userRepository.findByEmail("admin@bar.test")).thenReturn(Optional.of(admin));
            when(passwordEncoder.matches("Bar12345", "hash")).thenReturn(true);

            assertThatThrownBy(() -> service.login(login("admin@bar.test", "Bar12345"), "1.1.1.1"))
                    .isInstanceOf(ForbiddenException.class).hasMessage("error.auth.trialEnded");
            assertThat(company.getStatus()).isEqualTo(Company.CompanyStatus.INACTIVE);
        }

        @Test
        @DisplayName("login correcte retorna token i no cal tenir l'email verificat")
        void okWithoutVerifiedEmail() {
            admin.setEmailVerified(false);
            when(userRepository.findByEmail("admin@bar.test")).thenReturn(Optional.of(admin));
            when(passwordEncoder.matches("Bar12345", "hash")).thenReturn(true);
            when(jwtUtil.generateToken("admin@bar.test", "ADMIN")).thenReturn("jwt");

            LoginResponseDTO response = service.login(login(" Admin@Bar.test ", "Bar12345"), "1.1.1.1");

            assertThat(response.getToken()).isEqualTo("jwt");
            assertThat(admin.getLastLogin()).isNotNull();
        }

        @Test
        @DisplayName("massa intents fallits sobre el mateix compte → 429")
        void bruteForceIsLimited() {
            when(userRepository.findByEmail("admin@bar.test")).thenReturn(Optional.of(admin));
            when(passwordEncoder.matches(anyString(), eq("hash"))).thenReturn(false);
            for (int i = 0; i < 8; i++) {
                assertThatThrownBy(() -> service.login(login("admin@bar.test", "mala"), "1.1.1." + System.nanoTime() % 200))
                        .isInstanceOf(BadRequestException.class);
            }
            assertThatThrownBy(() -> service.login(login("admin@bar.test", "mala"), "2.2.2.2"))
                    .isInstanceOf(TooManyRequestsException.class);
        }
    }

    @Test
    @DisplayName("recuperar contrasenya d'un email desconegut no dona error ni envia res")
    void passwordResetDoesNotRevealAccounts() {
        when(userRepository.findByEmail("nobody@bar.test")).thenReturn(Optional.empty());

        service.requestPasswordReset("nobody@bar.test", "1.1.1.1");

        verifyNoInteractions(emailService);
    }

    @Nested
    @DisplayName("gestió d'usuaris")
    class Management {

        @BeforeEach
        void asAdmin() {
            lenient().when(currentUser.requireAdmin()).thenReturn(admin);
            lenient().when(currentUser.get()).thenReturn(admin);
        }

        @Test
        @DisplayName("no es pot crear un SUPER_ADMIN")
        void cannotCreateSuperAdmin() {
            UserRegistrationDTO dto = UserRegistrationDTO.builder().email("x@bar.test").password("Bar12345")
                    .firstName("X").lastName("Y").role(User.UserRole.SUPER_ADMIN).build();

            assertThatThrownBy(() -> service.registerUser(dto))
                    .isInstanceOf(ForbiddenException.class).hasMessage("error.user.roleNotAllowed");
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("un usuari d'una altra empresa es tracta com a inexistent")
        void otherCompanyUserNotFound() {
            when(userRepository.findByUuidAndCompany_Id("other-uuid", 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateUser("other-uuid", UserRequestDTO.builder().email("a@b.c").build()))
                    .isInstanceOf(ResourceNotFoundException.class);
            assertThatThrownBy(() -> service.deleteUser("other-uuid")).isInstanceOf(ResourceNotFoundException.class);
            assertThatThrownBy(() -> service.getUserByUuid("other-uuid")).isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("l'administrador no es pot canviar el propi rol ni posar-se SUPER_ADMIN")
        void cannotEscalateOwnRole() {
            when(userRepository.findByUuidAndCompany_Id("admin-uuid", 1L)).thenReturn(Optional.of(admin));
            UserRequestDTO dto = UserRequestDTO.builder().email("admin@bar.test").role(User.UserRole.SUPER_ADMIN).build();

            assertThatThrownBy(() -> service.updateUser("admin-uuid", dto)).isInstanceOf(ForbiddenException.class);
            assertThat(admin.getRole()).isEqualTo(User.UserRole.ADMIN);
        }

        @Test
        @DisplayName("canviar l'email d'un usuari obliga a tornar-lo a verificar")
        void emailChangeRequiresVerification() {
            User worker = user(11L, "worker-uuid", "worker@bar.test", User.UserRole.USER);
            when(userRepository.findByUuidAndCompany_Id("worker-uuid", 1L)).thenReturn(Optional.of(worker));
            when(userRepository.existsByEmail("nou@bar.test")).thenReturn(false);
            when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.updateUser("worker-uuid", UserRequestDTO.builder().email("Nou@bar.test").build());

            assertThat(worker.getEmail()).isEqualTo("nou@bar.test");
            assertThat(worker.getEmailVerified()).isFalse();
            verify(emailService).sendEmailVerification(eq("nou@bar.test"), anyString(), eq("Nom"), any());
        }

        @Test
        @DisplayName("ningú pot canviar la contrasenya d'un altre usuari")
        void cannotChangeOthersPassword() {
            PasswordChangeDTO dto = new PasswordChangeDTO();
            dto.setCurrentPassword("Bar12345");
            dto.setNewPassword("Nova12345");

            assertThatThrownBy(() -> service.changePassword("another-uuid", dto)).isInstanceOf(ForbiddenException.class);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("l'administrador no es pot esborrar a si mateix")
        void cannotDeleteSelf() {
            when(userRepository.findByUuidAndCompany_Id("admin-uuid", 1L)).thenReturn(Optional.of(admin));

            assertThatThrownBy(() -> service.deleteUser("admin-uuid")).hasMessage("error.user.cannotDeleteSelf");
        }
    }

    @Test
    @DisplayName("idioma preferit: només ca o es")
    void languageMustBeSupported() {
        when(currentUser.get()).thenReturn(admin);
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertThat(service.updateMyLanguage("es").getLanguage()).isEqualTo("es");
        assertThatThrownBy(() -> service.updateMyLanguage("en")).isInstanceOf(BadRequestException.class);
    }
}
