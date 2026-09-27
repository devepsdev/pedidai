package com.pedidai.api.services.impl;

import com.pedidai.api.dto.CompanyRegistrationDTO;
import com.pedidai.api.dto.CompanyRequestDTO;
import com.pedidai.api.dto.LoginResponseDTO;
import com.pedidai.api.entities.Company;
import com.pedidai.api.entities.User;
import com.pedidai.api.exceptions.DuplicateResourceException;
import com.pedidai.api.exceptions.ForbiddenException;
import com.pedidai.api.exceptions.TooManyRequestsException;
import com.pedidai.api.repositories.CompanyRepository;
import com.pedidai.api.repositories.UserRepository;
import com.pedidai.api.security.CurrentUser;
import com.pedidai.api.security.RateLimiter;
import com.pedidai.api.services.EmailService;
import com.pedidai.api.services.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Locale;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompanyServiceImpl: alta amb prova de 14 dies")
class CompanyServiceImplTest {

    @Mock private CompanyRepository companyRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private UserService userService;
    @Mock private CurrentUser currentUser;

    private CompanyServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CompanyServiceImpl(companyRepository, userRepository, passwordEncoder, emailService,
                userService, currentUser, new RateLimiter());
    }

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    private CompanyRegistrationDTO registration(String email) {
        return CompanyRegistrationDTO.builder().companyName(" Bar Prova ").adminFirstName("Laura")
                .adminEmail(email).adminPassword("Bar12345").acceptTerms(true).build();
    }

    @Test
    @DisplayName("crea empresa activa amb 14 dies de prova, admin amb l'idioma de la petició i sessió iniciada")
    void registersAndStartsTrial() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("ca-ES"));
        when(userRepository.existsByEmail("laura@bar.test")).thenReturn(false);
        when(companyRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(passwordEncoder.encode("Bar12345")).thenReturn("hash");
        LoginResponseDTO session = LoginResponseDTO.builder().token("jwt").build();
        when(userService.issueSession(any())).thenReturn(session);

        assertThat(service.registerCompanyWithAdmin(registration("Laura@Bar.test"), "1.1.1.1")).isSameAs(session);

        ArgumentCaptor<Company> company = ArgumentCaptor.forClass(Company.class);
        verify(companyRepository).save(company.capture());
        assertThat(company.getValue().getName()).isEqualTo("Bar Prova");
        assertThat(company.getValue().getStatus()).isEqualTo(Company.CompanyStatus.ACTIVE);
        assertThat(company.getValue().getTaxId()).isNull();
        assertThat(company.getValue().getTrialEndsAt())
                .isBetween(LocalDateTime.now().plusDays(13).plusHours(23), LocalDateTime.now().plusDays(14).plusMinutes(1));

        ArgumentCaptor<User> admin = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(admin.capture());
        assertThat(admin.getValue().getRole()).isEqualTo(User.UserRole.ADMIN);
        assertThat(admin.getValue().getEmail()).isEqualTo("laura@bar.test");
        assertThat(admin.getValue().getLanguage()).isEqualTo("ca");
        assertThat(admin.getValue().getEmailVerified()).isFalse();
        verify(emailService).sendWelcomeVerification(eq("laura@bar.test"), anyString(), eq("Laura"), eq("Bar Prova"), any());
    }

    @Test
    @DisplayName("email ja registrat → 409")
    void duplicateEmail() {
        when(userRepository.existsByEmail("laura@bar.test")).thenReturn(true);

        assertThatThrownBy(() -> service.registerCompanyWithAdmin(registration("laura@bar.test"), "1.1.1.1"))
                .isInstanceOf(DuplicateResourceException.class).hasMessage("error.register.emailExists");
        verify(companyRepository, never()).save(any());
    }

    @Test
    @DisplayName("més de 5 registres per hora des de la mateixa IP → 429")
    void registrationRateLimited() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);
        for (int i = 0; i < 5; i++) {
            int n = i;
            assertThatThrownBy(() -> service.registerCompanyWithAdmin(registration("u" + n + "@bar.test"), "9.9.9.9"))
                    .isInstanceOf(DuplicateResourceException.class);
        }
        assertThatThrownBy(() -> service.registerCompanyWithAdmin(registration("u6@bar.test"), "9.9.9.9"))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    @DisplayName("l'administrador no pot canviar l'estat de la seva empresa")
    void updateIgnoresStatus() {
        Company company = Company.builder().id(1L).uuid("c1").name("Bar").status(Company.CompanyStatus.INACTIVE).build();
        User admin = User.builder().role(User.UserRole.ADMIN).company(company).build();
        when(currentUser.requireAdmin()).thenReturn(admin);
        when(companyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.updateCompany(CompanyRequestDTO.builder().name("Bar Nou").email("bar@bar.test").build());

        assertThat(company.getName()).isEqualTo("Bar Nou");
        assertThat(company.getStatus()).isEqualTo(Company.CompanyStatus.INACTIVE);
    }

    @Test
    @DisplayName("un usuari que no és administrador no pot editar l'empresa")
    void onlyAdminCanUpdate() {
        when(currentUser.requireAdmin()).thenThrow(new ForbiddenException("error.user.adminRequired"));

        assertThatThrownBy(() -> service.updateCompany(CompanyRequestDTO.builder().name("X").build()))
                .isInstanceOf(ForbiddenException.class);
    }
}
