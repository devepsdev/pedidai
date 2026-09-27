package com.pedidai.api.services.impl;

import com.pedidai.api.dto.OpsCompanyDTO;
import com.pedidai.api.dto.OpsSummaryDTO;
import com.pedidai.api.entities.Company;
import com.pedidai.api.entities.User;
import com.pedidai.api.repositories.CompanyRepository;
import com.pedidai.api.repositories.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(OpsServiceImpl.class)
@DisplayName("OpsService: resum diari per a l'equip")
class OpsServiceImplTest {

    @Autowired private OpsServiceImpl service;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EntityManager entityManager;

    @Test
    @DisplayName("classifica registres, proves que acaben, proves acabades i esborrats propers")
    void summaryBuckets() {
        LocalDateTime now = LocalDateTime.now();
        Company fresh = company("Bar Nou", now.plusDays(14), User.UserRole.ADMIN, now.minusHours(2));
        Company ending = company("Bar Acaba", now.plusDays(2), User.UserRole.ADMIN, now.minusDays(12));
        Company expired = company("Bar Caducat", now.minusHours(5), User.UserRole.ADMIN, now.minusDays(14));
        Company deleting = company("Bar Esborrat", now.minusDays(25), User.UserRole.ADMIN, now.minusDays(39));
        company("Bar Pagament", null, User.UserRole.ADMIN, now.minusDays(60));
        company("Plataforma", null, User.UserRole.SUPER_ADMIN, now.minusHours(1));

        OpsSummaryDTO summary = service.getSummary(24);

        assertThat(summary.getNewCompanies()).extracting(OpsCompanyDTO::getUuid).containsExactly(fresh.getUuid());
        assertThat(summary.getTrialsEndingSoon()).extracting(OpsCompanyDTO::getUuid).containsExactly(ending.getUuid());
        assertThat(summary.getTrialsExpired()).extracting(OpsCompanyDTO::getUuid).containsExactly(expired.getUuid());
        assertThat(summary.getDeletionSoon()).extracting(OpsCompanyDTO::getUuid).containsExactly(deleting.getUuid());
        assertThat(summary.getTotalClients()).isEqualTo(5);
        assertThat(summary.getInTrial()).isEqualTo(2);
        assertThat(summary.getPaid()).isEqualTo(1);
        assertThat(summary.getTrialOver()).isEqualTo(2);

        OpsCompanyDTO dto = summary.getNewCompanies().getFirst();
        assertThat(dto.getAdminName()).isEqualTo("Laura Puig");
        assertThat(dto.getAdminEmail()).isEqualTo("bar-nou@test.com");
        assertThat(dto.getLanguage()).isEqualTo("ca");
    }

    private Company company(String name, LocalDateTime trialEndsAt, User.UserRole role, LocalDateTime createdAt) {
        String slug = name.toLowerCase().replace(' ', '-');
        Company company = companyRepository.save(Company.builder()
                .name(name).email(slug + "@test.com").status(Company.CompanyStatus.ACTIVE).trialEndsAt(trialEndsAt).build());
        userRepository.save(User.builder()
                .uuid(UUID.randomUUID().toString()).company(company).email(slug + "@test.com").password("x")
                .firstName("Laura").lastName("Puig").role(role).language("ca").isActive(true).emailVerified(false).build());
        // created_at no és actualitzable des de l'entitat: es fixa directament per simular la data de registre
        entityManager.flush();
        entityManager.createNativeQuery("UPDATE companies SET created_at = ?1 WHERE id = ?2")
                .setParameter(1, createdAt).setParameter(2, company.getId()).executeUpdate();
        entityManager.clear();
        return company;
    }
}
