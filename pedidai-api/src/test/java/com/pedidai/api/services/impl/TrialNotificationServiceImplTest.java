package com.pedidai.api.services.impl;

import com.pedidai.api.config.I18nConfig;
import com.pedidai.api.entities.Company;
import com.pedidai.api.entities.User;
import com.pedidai.api.repositories.CompanyRepository;
import com.pedidai.api.repositories.UserRepository;
import com.pedidai.api.services.EmailService;
import com.pedidai.api.services.EmailService.TrialEmail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(TrialNotificationServiceImpl.class)
@DisplayName("TrialNotificationService: avisos de la prova al client")
class TrialNotificationServiceImplTest {

    @Autowired private TrialNotificationServiceImpl service;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private UserRepository userRepository;
    @MockitoBean private EmailService emailService;

    @Test
    @DisplayName("avisa 3 dies abans i en acabar, una sola vegada, en l'idioma de l'administrador")
    void remindersAreSentOnce() {
        LocalDateTime now = LocalDateTime.now();
        Company soon = company("Bar Aviat", now.plusDays(2), User.UserRole.ADMIN, "ca");
        Company ended = company("Bar Acabat", now.minusDays(1), User.UserRole.ADMIN, "es");

        assertThat(service.sendTrialNotifications()).isEqualTo(2);

        ArgumentCaptor<TrialEmail> mail = ArgumentCaptor.forClass(TrialEmail.class);
        verify(emailService).sendTrialEndingSoon(mail.capture(), eq(I18nConfig.CATALAN));
        assertThat(mail.getValue().companyName()).isEqualTo("Bar Aviat");
        assertThat(mail.getValue().to()).isEqualTo("bar-aviat@test.com");
        verify(emailService).sendTrialEnded(mail.capture(), any());
        assertThat(mail.getValue().deletionDate()).isEqualTo(ended.getTrialEndsAt().plusDays(30).toLocalDate());

        // Segona execució: ja estan avisades
        assertThat(service.sendTrialNotifications()).isZero();
        verify(emailService, times(1)).sendTrialEndingSoon(any(), any());
        verify(emailService, times(1)).sendTrialEnded(any(), any());
        assertThat(companyRepository.findById(soon.getId()).orElseThrow().getTrialReminderSentAt()).isNotNull();
    }

    @Test
    @DisplayName("no avisa clients de pagament, la plataforma, proves llunyanes ni proves acabades fa temps")
    void skipsCompaniesThatMustNotBeNotified() {
        LocalDateTime now = LocalDateTime.now();
        company("Pagament", null, User.UserRole.ADMIN, "es");
        company("Plataforma", now.plusDays(1), User.UserRole.SUPER_ADMIN, "es");
        company("Prova llarga", now.plusDays(10), User.UserRole.ADMIN, "es");
        company("Caducada fa temps", now.minusDays(20), User.UserRole.ADMIN, "es");

        assertThat(service.sendTrialNotifications()).isZero();
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("si el correu falla, no es marca com a enviat i es torna a provar")
    void failedEmailIsRetried() {
        Company soon = company("Bar Error", LocalDateTime.now().plusDays(2), User.UserRole.ADMIN, "es");
        doThrow(new IllegalStateException("smtp caigut")).when(emailService).sendTrialEndingSoon(any(), any());

        assertThat(service.sendTrialNotifications()).isZero();
        assertThat(companyRepository.findById(soon.getId()).orElseThrow().getTrialReminderSentAt()).isNull();
    }

    private Company company(String name, LocalDateTime trialEndsAt, User.UserRole role, String language) {
        String slug = name.toLowerCase().replace(' ', '-');
        Company company = companyRepository.save(Company.builder().name(name).email(slug + "@test.com")
                .status(Company.CompanyStatus.ACTIVE).trialEndsAt(trialEndsAt).build());
        userRepository.save(User.builder().uuid(UUID.randomUUID().toString()).company(company).email(slug + "@test.com")
                .password("x").firstName("Laura").lastName("Puig").role(role).language(language)
                .isActive(true).isDeleted(false).emailVerified(true).build());
        return company;
    }
}
