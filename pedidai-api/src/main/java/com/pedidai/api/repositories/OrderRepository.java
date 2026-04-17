package com.pedidai.api.repositories;

import com.pedidai.api.entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByUuid(String uuid);

    @Query("""
        SELECT DISTINCT o 
        FROM Order o 
        LEFT JOIN FETCH o.items
        WHERE o.company.id = :companyId
          AND o.createdAt BETWEEN :startDate AND :endDate
    """)
    List<Order> getOrdersByCompanyIdAndPeriodWithOrderItems(
            @Param("companyId") Long companyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
        SELECT DISTINCT o 
        FROM Order o 
        WHERE o.company.id = :companyId
          AND o.createdAt BETWEEN :startDate AND :endDate
    """)
    List<Order> getOrdersByCompanyIdAndPeriodWithoutOrderItems(
            @Param("companyId") Long companyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

}