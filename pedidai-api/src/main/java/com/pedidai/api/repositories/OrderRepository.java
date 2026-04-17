package com.pedidai.api.repositories;

import com.pedidai.api.entities.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
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

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :since AND o.status NOT IN :excludedStatuses")
    long countRecentOrders(@Param("since") LocalDateTime since, @Param("excludedStatuses") java.util.List<Order.OrderStatus> excludedStatuses);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.createdAt >= :since AND o.status NOT IN :excludedStatuses")
    java.math.BigDecimal sumRecentOrdersAmount(@Param("since") LocalDateTime since, @Param("excludedStatuses") java.util.List<Order.OrderStatus> excludedStatuses);

    @Query("SELECT o.company.uuid, o.company.name, COUNT(o), COALESCE(SUM(o.totalAmount),0) FROM Order o WHERE o.createdAt >= :since AND o.status NOT IN :excludedStatuses GROUP BY o.company.uuid, o.company.name ORDER BY COUNT(o) DESC")
    java.util.List<Object[]> findTopCompaniesByOrderCount(@Param("since") LocalDateTime since, @Param("excludedStatuses") java.util.List<Order.OrderStatus> excludedStatuses, Pageable pageable);

    @Query("SELECT FUNCTION('DATE_FORMAT', o.createdAt, '%Y-%m'), COUNT(o), COALESCE(SUM(o.totalAmount),0) FROM Order o WHERE o.createdAt >= :since GROUP BY FUNCTION('DATE_FORMAT', o.createdAt, '%Y-%m') ORDER BY FUNCTION('DATE_FORMAT', o.createdAt, '%Y-%m') ASC")
    java.util.List<Object[]> findMonthlyOrderStats(@Param("since") LocalDateTime since);

}