package com.pedidai.api.repositories;

import com.pedidai.api.entities.Order;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderSpecifications {

    public static Specification<Order> filterOrders(
            Long orderId,
            Long companyId,
            Long supplierId,
            Long userId,
            String searchText,
            String name,
            String notes,
            Order.OrderStatus status,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            LocalDate deliveryDateFrom,
            LocalDate deliveryDateTo,
            LocalDateTime createdAtFrom,
            LocalDateTime createdAtTo,
            LocalDateTime updatedAtFrom,
            LocalDateTime updatedAtTo
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (orderId != null)
                predicates.add(cb.equal(root.get("id"), orderId));

            if (companyId != null)
                predicates.add(cb.equal(root.get("company").get("id"), companyId));

            if (supplierId != null)
                predicates.add(cb.equal(root.get("supplier").get("id"), supplierId));

            if (userId != null)
                predicates.add(cb.equal(root.get("user").get("id"), userId));

            // -----------------------------
            //  SEARCH TEXT (global)
            //  Compatible amb CLOB/TEXT gràcies a .as(String.class)
            // -----------------------------
            if (searchText != null && !searchText.isBlank()) {
                String like = "%" + searchText.toLowerCase() + "%";
                Predicate nameLike = cb.like(cb.lower(root.get("name")), like);
                Predicate notesLike = cb.like(cb.lower(root.get("notes").as(String.class)), like);
                predicates.add(cb.or(nameLike, notesLike));
            }

            // Filtres individuals
            if (name != null && !name.isBlank())
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));

            if (notes != null && !notes.isBlank())
                predicates.add(cb.like(cb.lower(root.get("notes").as(String.class)), "%" + notes.toLowerCase() + "%"));

            // Status predeterminat
            if (status != null) {
                // Si tenim status, filtrem per aquest
                predicates.add(cb.equal(root.get("status"), status));
            } else {
                // si tenim status, recuperem tots excepte els que estan en deleted.
                predicates.add(cb.notEqual(root.get("status"), Order.OrderStatus.DELETED));
            }

            if (minAmount != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("totalAmount"), minAmount));

            if (maxAmount != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("totalAmount"), maxAmount));

            // -----------------------------
            //  FILTRES DE DATES
            // -----------------------------
            if (deliveryDateFrom != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("deliveryDate"), deliveryDateFrom));

            if (deliveryDateTo != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("deliveryDate"), deliveryDateTo));

            if (createdAtFrom != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdAtFrom));

            if (createdAtTo != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), createdAtTo));

            if (updatedAtFrom != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("updatedAt"), updatedAtFrom));

            if (updatedAtTo != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("updatedAt"), updatedAtTo));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}