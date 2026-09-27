package com.pedidai.api.repositories;

import com.pedidai.api.entities.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    boolean existsByTaxId(String taxId);

    Optional<Company> findByUuid(String uuid);

    boolean existsByUuid(String uuid);

    long countByStatus(Company.CompanyStatus status);

    @Query("SELECT c FROM Company c WHERE " +
           "(:status IS NULL OR c.status = :status) AND " +
           "(:search IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%',:search,'%')) OR LOWER(c.taxId) LIKE LOWER(CONCAT('%',:search,'%')))")
    Page<Company> findByFilters(@Param("status") Company.CompanyStatus status, @Param("search") String search, Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.company.id = :companyId AND u.isDeleted = false")
    long countUsersByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.company.id = :companyId")
    long countOrdersByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT MAX(o.createdAt) FROM Order o WHERE o.company.id = :companyId")
    java.util.Optional<java.time.LocalDateTime> findLastOrderDateByCompanyId(@Param("companyId") Long companyId);

    /** Empreses amb la prova acabada abans de {@code limit} i sense contractar (mai la del SUPER_ADMIN). */
    @Query("""
            SELECT c.id FROM Company c
            WHERE c.trialEndsAt IS NOT NULL AND c.trialEndsAt < :limit
              AND NOT EXISTS (SELECT u.id FROM User u WHERE u.company = c
                              AND u.role = com.pedidai.api.entities.User.UserRole.SUPER_ADMIN)
            """)
    List<Long> findIdsWithTrialExpiredBefore(@Param("limit") LocalDateTime limit);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Company c WHERE c.id = :companyId")
    int deleteCompanyById(@Param("companyId") Long companyId);

    // ── Alertes internes: només empreses client (mai la del SUPER_ADMIN) ──

    String CLIENT = " NOT EXISTS (SELECT u.id FROM User u WHERE u.company = c"
            + " AND u.role = com.pedidai.api.entities.User.UserRole.SUPER_ADMIN) ";

    @Query("SELECT c FROM Company c WHERE c.createdAt >= :from AND c.createdAt < :to AND" + CLIENT
            + "ORDER BY c.createdAt")
    List<Company> findClientsCreatedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT c FROM Company c WHERE c.trialEndsAt > :from AND c.trialEndsAt <= :to AND" + CLIENT
            + "ORDER BY c.trialEndsAt")
    List<Company> findClientsWithTrialEndingBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(c) FROM Company c WHERE" + CLIENT)
    long countClients();

    @Query("SELECT COUNT(c) FROM Company c WHERE c.trialEndsAt > :now AND" + CLIENT)
    long countClientsInTrial(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(c) FROM Company c WHERE c.trialEndsAt IS NULL AND" + CLIENT)
    long countPaidClients();
}
