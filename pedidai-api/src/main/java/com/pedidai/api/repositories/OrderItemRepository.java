package com.pedidai.api.repositories;

import com.pedidai.api.entities.Order;
import com.pedidai.api.entities.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
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

}