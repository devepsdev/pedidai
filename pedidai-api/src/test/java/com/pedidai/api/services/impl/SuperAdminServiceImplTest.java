package com.pedidai.api.services.impl;

import com.pedidai.api.dto.CompanySummaryDTO;
import com.pedidai.api.entities.Company;
import com.pedidai.api.entities.User;
import com.pedidai.api.exceptions.BadRequestException;
import com.pedidai.api.repositories.CompanyRepository;
import com.pedidai.api.repositories.OrderRepository;
import com.pedidai.api.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SuperAdminServiceImpl: ampliar la prova")
class SuperAdminServiceImplTest {

    @Mock private CompanyRepository companyRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrderRepository orderRepository;

    private SuperAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SuperAdminServiceImpl(companyRepository, userRepository, orderRepository);
    }

    private Company company(LocalDateTime trialEndsAt, Company.CompanyStatus status) {
        Company company = Company.builder().id(5L).uuid("c1").name("Bar").email("bar@test.com")
                .status(status).trialEndsAt(trialEndsAt).build();
        when(companyRepository.findByUuid("c1")).thenReturn(Optional.of(company));
        return company;
    }

    private void summaryStubs() {
        when(companyRepository.findLastOrderDateByCompanyId(5L)).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("prova en curs: suma els dies al final actual")
    void extendsFromCurrentEnd() {
        LocalDateTime end = LocalDateTime.now().plusDays(5);
        Company company = company(end, Company.CompanyStatus.ACTIVE);
        summaryStubs();

        CompanySummaryDTO result = service.extendTrial("c1", 14);

        assertThat(company.getTrialEndsAt()).isEqualTo(end.plusDays(14));
        assertThat(result.getTrialEndsAt()).isEqualTo(end.plusDays(14));
        verify(companyRepository).save(company);
    }

    @Test
    @DisplayName("prova acabada: compta des d'avui i reactiva l'empresa")
    void extendsExpiredTrialFromNow() {
        Company company = company(LocalDateTime.now().minusDays(3), Company.CompanyStatus.INACTIVE);
        summaryStubs();

        service.extendTrial("c1", 14);

        assertThat(company.getTrialEndsAt())
                .isBetween(LocalDateTime.now().plusDays(14).minusMinutes(1), LocalDateTime.now().plusDays(14).plusMinutes(1));
        assertThat(company.getStatus()).isEqualTo(Company.CompanyStatus.ACTIVE);
    }

    @Test
    @DisplayName("client de pagament: no se li crea cap prova")
    void refusesPaidCompany() {
        Company company = company(null, Company.CompanyStatus.ACTIVE);

        assertThatThrownBy(() -> service.extendTrial("c1", 14))
                .isInstanceOf(BadRequestException.class).hasMessage("error.company.alreadyPaid");
        assertThat(company.getTrialEndsAt()).isNull();
        verify(companyRepository, never()).save(any());
    }

    @Test
    @DisplayName("compte de la plataforma: rebutjat")
    void refusesPlatformCompany() {
        company(LocalDateTime.now().plusDays(3), Company.CompanyStatus.ACTIVE);
        when(userRepository.existsByCompany_IdAndRole(5L, User.UserRole.SUPER_ADMIN)).thenReturn(true);

        assertThatThrownBy(() -> service.extendTrial("c1", 14))
                .isInstanceOf(BadRequestException.class).hasMessage("error.company.platformAccount");
        verify(companyRepository, never()).save(any());
    }
}
