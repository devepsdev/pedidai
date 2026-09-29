package com.pedidai.api.repositories;

import com.pedidai.api.entities.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    /** Observació més recent d'un producte (per data de document i, en empat, la darrera registrada). */
    Optional<PriceHistory> findFirstByProduct_IdOrderByDocumentDateDescIdDesc(Long productId);

    /** Totes les observacions d'una empresa des d'una data, amb producte i proveïdor carregats. */
    @Query("""
            SELECT h FROM PriceHistory h
            JOIN FETCH h.product p
            JOIN FETCH h.supplier s
            WHERE h.company.id = :companyId
              AND h.documentDate >= :from
              AND p.isActive = true
              AND s.isActive = true
            ORDER BY h.documentDate ASC, h.id ASC
            """)
    List<PriceHistory> findActiveByCompanySince(@Param("companyId") Long companyId, @Param("from") LocalDate from);

    long countByCompany_Id(Long companyId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            DELETE FROM PriceHistory h
            WHERE h.company.id = :companyId
               OR h.product.id IN (SELECT p.id FROM Product p WHERE p.supplier.company.id = :companyId)
            """)
    int deleteByCompanyId(@Param("companyId") Long companyId);

    long countBySourceAndCreatedAtGreaterThanEqual(PriceHistory.Source source, java.time.LocalDateTime since);

    long countByCompany_IdAndSource(Long companyId, PriceHistory.Source source);
}
