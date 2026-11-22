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

    // ── Prova acabada i data d'esborrat al llistat ──

    @Test
    @DisplayName("llistat: prova acabada → trialEnded i data d'esborrat")
    void summaryMarksEndedTrialWithDeletionDate() {
        LocalDateTime end = LocalDateTime.now().minusDays(3);
        company(end, Company.CompanyStatus.ACTIVE);
        summaryStubs();

        CompanySummaryDTO result = service.updateCompanyStatus("c1", "ACTIVE");

        assertThat(result.isTrialEnded()).isTrue();
        assertThat(result.getDataDeletionAt())
                .isEqualTo(DataRetentionServiceImpl.deletionTime(end, LocalDateTime.now()));
    }

    @Test
    @DisplayName("llistat: prova en curs o client de pagament → sense data d'esborrat")
    void summaryWithoutDeletionWhenTrialRunningOrPaid() {
        company(LocalDateTime.now().plusDays(3), Company.CompanyStatus.ACTIVE);
        summaryStubs();
        CompanySummaryDTO running = service.updateCompanyStatus("c1", "ACTIVE");
        assertThat(running.isTrialEnded()).isFalse();
        assertThat(running.getDataDeletionAt()).isNull();

        company(null, Company.CompanyStatus.ACTIVE);
        CompanySummaryDTO paid = service.updateCompanyStatus("c1", "ACTIVE");
        assertThat(paid.isTrialEnded()).isFalse();
        assertThat(paid.getDataDeletionAt()).isNull();
    }

    // ── Tauler: només empreses client (mai la de la plataforma) ──

    @Test
    @DisplayName("tauler: separa pagament, prova i prova acabada; MRR = (pagament + prova) × 29")
    void dashboardCountsOnlyClients() {
        when(companyRepository.countClients()).thenReturn(4L);
        when(companyRepository.countActivePaidClients()).thenReturn(1L);
        when(companyRepository.countActiveClientsInTrial(any())).thenReturn(1L);
        when(companyRepository.countActiveClientsWithTrialEnded(any())).thenReturn(1L);
        when(companyRepository.countClientsByStatus(Company.CompanyStatus.INACTIVE)).thenReturn(1L);
        when(userRepository.countByIsDeletedFalseAndRoleNot(User.UserRole.SUPER_ADMIN)).thenReturn(3L);
        when(userRepository.countByRoleAndIsDeletedFalse(User.UserRole.ADMIN)).thenReturn(2L);
        when(userRepository.countByRoleAndIsDeletedFalse(User.UserRole.USER)).thenReturn(1L);
        when(orderRepository.findTopCompaniesByOrderCount(any(), any(), any())).thenReturn(java.util.List.of());

        var d = service.getDashboard();

        assertThat(d.getTotalCompanies()).isEqualTo(4);
        assertThat(d.getPaidCompanies()).isEqualTo(1);
        assertThat(d.getTrialCompanies()).isEqualTo(1);
        assertThat(d.getTrialEndedCompanies()).isEqualTo(1);
        assertThat(d.getActiveCompanies()).isEqualTo(2);
        assertThat(d.getInactiveCompanies()).isEqualTo(1);
        assertThat(d.getPotentialMRR()).isEqualByComparingTo("58");
        assertThat(d.getTotalUsers()).isEqualTo(3);
        assertThat(d.getTotalAdmins()).isEqualTo(2);
        assertThat(d.getTotalRegularUsers()).isEqualTo(1);
    }

    // ── Data d'esborrat: primera neteja (03:30) després de 30 dies ──

    @Test
    @DisplayName("esborrat: la neteja de les 03:30 de l'endemà si el límit cau de dia")
    void deletionTimeNextNightlyRun() {
        LocalDateTime end = LocalDateTime.of(2026, 11, 5, 11, 14);
        assertThat(DataRetentionServiceImpl.deletionTime(end, end.plusDays(1)))
                .isEqualTo(LocalDateTime.of(2026, 12, 6, 3, 30));
    }

    @Test
    @DisplayName("esborrat: el mateix dia si el límit cau abans de les 03:30")
    void deletionTimeSameNight() {
        LocalDateTime end = LocalDateTime.of(2026, 11, 5, 1, 0);
        assertThat(DataRetentionServiceImpl.deletionTime(end, end.plusDays(1)))
                .isEqualTo(LocalDateTime.of(2026, 12, 5, 3, 30));
    }

    @Test
    @DisplayName("esborrat: sense data si la prova no ha acabat o no n'hi ha")
    void deletionTimeNullWhenNotEnded() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 22, 12, 0);
        assertThat(DataRetentionServiceImpl.deletionTime(now.plusDays(1), now)).isNull();
        assertThat(DataRetentionServiceImpl.deletionTime(null, now)).isNull();
    }

    @Test
    @DisplayName("usuaris: marca els de l'empresa amb la prova acabada")
    void usersMarkCompanyTrialEnded() {
        Company ended = Company.builder().id(5L).uuid("c1").name("Bar")
                .status(Company.CompanyStatus.ACTIVE).trialEndsAt(LocalDateTime.now().minusDays(1)).build();
        Company paid = Company.builder().id(6L).uuid("c2").name("Pagat")
                .status(Company.CompanyStatus.ACTIVE).build();
        User a = User.builder().uuid("u1").firstName("A").lastName("B").email("a@test.com").company(ended).build();
        User b = User.builder().uuid("u2").firstName("C").lastName("D").email("c@test.com").company(paid).build();
        when(userRepository.findByAdminFilters(any(), any(), any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(a, b)));

        var users = service.getUsers(null, null, null, org.springframework.data.domain.PageRequest.of(0, 20)).getContent();

        assertThat(users.get(0).isCompanyTrialEnded()).isTrue();
        assertThat(users.get(0).getCompanyStatus()).isEqualTo("ACTIVE");
        assertThat(users.get(1).isCompanyTrialEnded()).isFalse();
    }

    @Test
    @DisplayName("llistat: marca el compte de la plataforma")
    void summaryMarksPlatform() {
        company(null, Company.CompanyStatus.ACTIVE);
        summaryStubs();
        when(userRepository.existsByCompany_IdAndRole(5L, User.UserRole.SUPER_ADMIN)).thenReturn(true);

        assertThat(service.updateCompanyStatus("c1", "ACTIVE").isPlatform()).isTrue();
    }
}
