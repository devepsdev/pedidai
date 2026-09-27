package com.pedidai.api.services.impl;

import com.pedidai.api.dto.OpsCompanyDTO;
import com.pedidai.api.dto.OpsSummaryDTO;
import com.pedidai.api.entities.Company;
import com.pedidai.api.entities.PriceHistory;
import com.pedidai.api.entities.User;
import com.pedidai.api.exceptions.ResourceNotFoundException;
import com.pedidai.api.repositories.CompanyRepository;
import com.pedidai.api.repositories.OrderRepository;
import com.pedidai.api.repositories.PriceHistoryRepository;
import com.pedidai.api.repositories.UserRepository;
import com.pedidai.api.services.OpsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OpsServiceImpl implements OpsService {

    /** Dies d'antelació per avisar que una prova s'acaba. */
    static final int ENDING_SOON_DAYS = 3;
    /** Avís quan falta menys d'una setmana per a l'esborrat automàtic. */
    static final int DELETION_WARNING_DAYS = 7;

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    @Override
    @Transactional(readOnly = true)
    public OpsSummaryDTO getSummary(int hours) {
        int window = Math.clamp(hours, 1, 24 * 31);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since = now.minusHours(window);
        LocalDateTime deletionFrom = now.minusDays(DataRetentionServiceImpl.RETENTION_DAYS);

        long total = companyRepository.countClients();
        long inTrial = companyRepository.countClientsInTrial(now);
        long paid = companyRepository.countPaidClients();

        return OpsSummaryDTO.builder()
                .generatedAt(now)
                .hours(window)
                .newCompanies(toDtos(companyRepository.findClientsCreatedBetween(since, now)))
                .trialsEndingSoon(toDtos(companyRepository.findClientsWithTrialEndingBetween(now, now.plusDays(ENDING_SOON_DAYS))))
                .trialsExpired(toDtos(companyRepository.findClientsWithTrialEndingBetween(since, now)))
                .deletionSoon(toDtos(companyRepository.findClientsWithTrialEndingBetween(
                        deletionFrom, deletionFrom.plusDays(DELETION_WARNING_DAYS))))
                .invoiceLinesRead(priceHistoryRepository.countBySourceAndCreatedAtGreaterThanEqual(PriceHistory.Source.INVOICE, since))
                .ordersCreated(orderRepository.countByCreatedAtGreaterThanEqual(since))
                .totalClients(total)
                .inTrial(inTrial)
                .paid(paid)
                .trialOver(Math.max(0, total - inTrial - paid))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OpsCompanyDTO getCompany(Long companyId) {
        return toDto(companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("error.company.notFound")));
    }

    private List<OpsCompanyDTO> toDtos(List<Company> companies) {
        return companies.stream().map(this::toDto).toList();
    }

    private OpsCompanyDTO toDto(Company company) {
        User admin = userRepository.findFirstByCompany_IdAndRoleOrderByIdAsc(company.getId(), User.UserRole.ADMIN)
                .orElse(null);
        return OpsCompanyDTO.builder()
                .uuid(company.getUuid())
                .name(company.getName())
                .city(company.getCity())
                .adminName(admin == null ? null : (admin.getFirstName() + " " + admin.getLastName()).trim())
                .adminEmail(admin == null ? company.getEmail() : admin.getEmail())
                .phone(company.getPhone() != null ? company.getPhone() : admin == null ? null : admin.getPhone())
                .language(admin == null ? null : admin.getLanguage())
                .emailVerified(admin != null && Boolean.TRUE.equals(admin.getEmailVerified()))
                .createdAt(company.getCreatedAt())
                .trialEndsAt(company.getTrialEndsAt())
                .build();
    }
}
