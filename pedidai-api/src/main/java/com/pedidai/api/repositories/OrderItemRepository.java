package com.pedidai.api.repositories;

import com.pedidai.api.entities.Order;
import com.pedidai.api.entities.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("""
        SELECT oi FROM OrderItem oi
        WHERE oi.product.id = :productId
          AND oi.order.createdAt >= :since
          AND oi.order.status IN :statuses
        ORDER BY oi.order.createdAt ASC
    """)
    List<OrderItem> findByProductIdAndOrderCreatedAfterAndActiveStatus(
            @Param("productId") Long productId,
            @Param("since") LocalDateTime since,
            @Param("statuses") List<Order.OrderStatus> statuses
    );

    /** Línies de les comandes de l'empresa (o que apunten als seus productes, proveïdors o usuaris). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            DELETE FROM OrderItem oi
            WHERE oi.order.id IN (SELECT o.id FROM Order o WHERE o.company.id = :companyId
                   OR o.supplier.id IN (SELECT s.id FROM Supplier s WHERE s.company.id = :companyId)
                   OR o.user.id IN (SELECT u.id FROM User u WHERE u.company.id = :companyId))
               OR oi.product.id IN (SELECT p.id FROM Product p WHERE p.supplier.company.id = :companyId)
            """)
    int deleteByCompanyId(@Param("companyId") Long companyId);
}
