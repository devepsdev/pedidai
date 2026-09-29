package com.pedidai.api.services.impl;

import com.pedidai.api.config.I18nConfig;
import com.pedidai.api.entities.Company;
import com.pedidai.api.entities.PriceHistory;
import com.pedidai.api.entities.User;
import com.pedidai.api.repositories.CompanyRepository;
import com.pedidai.api.repositories.PriceHistoryRepository;
import com.pedidai.api.repositories.SupplierRepository;
import com.pedidai.api.repositories.UserRepository;
import com.pedidai.api.services.EmailService;
import com.pedidai.api.services.EmailService.TrialEmail;
import com.pedidai.api.services.TrialNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrialNotificationServiceImpl implements TrialNotificationService {

    /** Dies d'antelació de l'avís «la prova acaba aviat». */
    static final int REMINDER_DAYS = 3;
    /** Només s'avisa de les proves acabades fa com a molt aquests dies (no s'envien avisos antics). */
    static final int ENDED_LOOKBACK_DAYS = 7;

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final SupplierRepository supplierRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final EmailService emailService;

    @Override
    @Scheduled(cron = "${app.trial-notifications.cron:0 0 9 * * *}", zone = "Europe/Madrid")
    public int sendTrialNotifications() {
        LocalDateTime now = LocalDateTime.now();
        int sent = 0;

        for (Company company : companyRepository.findClientsWithTrialEndingBetween(now, now.plusDays(REMINDER_DAYS))) {
            if (company.getTrialReminderSentAt() == null && notify(company, true)) {
                company.setTrialReminderSentAt(now);
                companyRepository.save(company);
                sent++;
            }
        }
        for (Company company : companyRepository.findClientsWithTrialEndingBetween(now.minusDays(ENDED_LOOKBACK_DAYS), now)) {
            if (company.getTrialEndNotifiedAt() == null && notify(company, false)) {
                company.setTrialEndNotifiedAt(now);
                companyRepository.save(company);
                sent++;
            }
        }
        if (sent > 0) {
            log.info("Avisos de prova enviats: {}", sent);
        }
        return sent;
    }

    /** Envia l'avís a l'administrador de l'empresa; si no n'hi ha o el correu falla, es tornarà a provar demà. */
    private boolean notify(Company company, boolean endingSoon) {
        if (company.getStatus() == Company.CompanyStatus.SUSPENDED) {
            return false;
        }
        User admin = userRepository.findFirstByCompany_IdAndRoleOrderByIdAsc(company.getId(), User.UserRole.ADMIN)
                .filter(u -> Boolean.TRUE.equals(u.getIsActive()) && !Boolean.TRUE.equals(u.getIsDeleted()))
                .orElse(null);
        if (admin == null) {
            return false;
        }
        TrialEmail mail = new TrialEmail(
                admin.getEmail(),
                admin.getFirstName(),
                company.getName(),
                company.getTrialEndsAt().toLocalDate(),
                company.getTrialEndsAt().plusDays(DataRetentionServiceImpl.RETENTION_DAYS).toLocalDate(),
                priceHistoryRepository.countByCompany_IdAndSource(company.getId(), PriceHistory.Source.INVOICE),
                supplierRepository.countByCompany_IdAndIsActiveTrue(company.getId()),
                companyRepository.countOrdersByCompanyId(company.getId()));
        try {
            if (endingSoon) {
                emailService.sendTrialEndingSoon(mail, I18nConfig.localeOf(admin.getLanguage()));
            } else {
                emailService.sendTrialEnded(mail, I18nConfig.localeOf(admin.getLanguage()));
            }
            return true;
        } catch (RuntimeException e) {
            log.warn("No s'ha pogut enviar l'avís de prova a l'empresa {}: {}", company.getId(), e.getMessage());
            return false;
        }
    }
}
