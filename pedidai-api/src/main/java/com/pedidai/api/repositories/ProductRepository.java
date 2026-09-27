package com.pedidai.api.repositories;

import com.pedidai.api.entities.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByUuid(String uuid);

    /** Producte només si el seu proveïdor pertany a l'empresa indicada. */
    Optional<Product> findByUuidAndSupplier_Company_Id(String uuid, Long companyId);

    List<Product> findBySupplier_Company_IdAndIsActiveTrue(Long companyId);

    List<Product> findBySupplier_IdAndIsActiveTrue(Long supplierId);

    @Query("SELECT p FROM Product p " +
            "JOIN p.supplier s " +
            "WHERE s.company.id = :companyId AND p.isActive = true")
    Page<Product> findProductsByCompanyId(@Param("companyId") Long companyId, Pageable pageable);

    @Query("""
            SELECT p FROM Product p
            WHERE p.supplier.id = :supplierId
            AND (:searchText IS NULL OR :searchText = '' 
            OR LOWER(p.name) LIKE LOWER(CONCAT('%', :searchText, '%'))
            OR LOWER(p.description) LIKE LOWER(CONCAT('%', :searchText, '%'))
            OR LOWER(p.category) LIKE LOWER(CONCAT('%', :searchText, '%')))
            AND p.isActive = true
           """)
    Page<Product> searchProductsBySupplierId(Long supplierId, String searchText, Pageable pageable);

    @Query("""
            SELECT p FROM Product p
            WHERE p.supplier.company.id = :companyId
            AND (:searchText IS NULL OR :searchText = '' 
            OR LOWER(p.name) LIKE LOWER(CONCAT('%', :searchText, '%'))    
            OR LOWER(p.description) LIKE LOWER(CONCAT('%', :searchText, '%'))
            OR LOWER(p.category) LIKE LOWER(CONCAT('%', :searchText, '%')))
            AND p.isActive = true
           """)
    Page<Product> searchProductsByCompanyId(Long companyId, String searchText, Pageable pageable);

    @Query("""
   SELECT p FROM Product p
   WHERE p.supplier.id = :supplierId
     AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))
     AND (:description IS NULL OR LOWER(p.description) LIKE LOWER(CONCAT('%', :description, '%')))
     AND (:category IS NULL OR LOWER(p.category) LIKE LOWER(CONCAT('%', :category, '%')))
     AND (:volume IS NULL OR p.volume = :volume)
     AND (:unit IS NULL OR LOWER(p.unit) = LOWER(:unit))
     AND (:minPrice IS NULL OR p.price >= :minPrice)
     AND (:maxPrice IS NULL OR p.price <= :maxPrice)
     AND (:isActive IS NULL OR p.isActive = :isActive)
    """)
    Page<Product> filterProductsBySupplierId(@Param("supplierId") Long supplierId,
                                             @Param("name") String name,
                                             @Param("description") String description,
                                             @Param("category") String category,
                                             @Param("volume") BigDecimal volume,
                                             @Param("unit") String unit,
                                             @Param("minPrice") BigDecimal minPrice,
                                             @Param("maxPrice") BigDecimal maxPrice,
                                             @Param("isActive") Boolean isActive,
                                             Pageable pageable);

    @Query("""
    SELECT p FROM Product p
    WHERE p.supplier.company.id = :companyId
     AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))
     AND (:description IS NULL OR LOWER(p.description) LIKE LOWER(CONCAT('%', :description, '%')))
     AND (:category IS NULL OR LOWER(p.category) LIKE LOWER(CONCAT('%', :category, '%')))
     AND (:volume IS NULL OR p.volume = :volume)
     AND (:unit IS NULL OR LOWER(p.unit) = LOWER(:unit))
     AND (:minPrice IS NULL OR p.price >= :minPrice)
     AND (:maxPrice IS NULL OR p.price <= :maxPrice)
     AND (:isActive IS NULL OR p.isActive = :isActive)
    """)
    Page<Product> filterProductsByCompanyId(@Param("companyId") Long companyId,
                                            @Param("name") String name,
                                            @Param("description") String description,
                                            @Param("category") String category,
                                            @Param("volume") BigDecimal volume,
                                            @Param("unit") String unit,
                                            @Param("minPrice") BigDecimal minPrice,
                                            @Param("maxPrice") BigDecimal maxPrice,
                                            @Param("isActive") Boolean isActive,
                                            Pageable pageable);

    @Query("""
        SELECT p FROM Product p
        WHERE p.supplier.company.id = :companyId
          AND LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))
          AND p.isActive = true
    """)
    List<Product> findActiveByCompanyIdAndNameContaining(
            @Param("companyId") Long companyId,
            @Param("name") String name
    );

    @Query("""
        SELECT p FROM Product p
        WHERE p.supplier.id = :supplierId
          AND LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))
          AND p.isActive = true
    """)
    List<Product> findActiveBySupplierIdAndNameContaining(
            @Param("supplierId") Long supplierId,
            @Param("name") String name
    );

    @Query("SELECT p.imageUrl FROM Product p WHERE p.supplier.company.id = :companyId AND p.imageUrl IS NOT NULL")
    List<String> findImageUrlsByCompanyId(@Param("companyId") Long companyId);

    boolean existsByImageUrl(String imageUrl);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Product p WHERE p.supplier.id IN (SELECT s.id FROM Supplier s WHERE s.company.id = :companyId)")
    int deleteByCompanyId(@Param("companyId") Long companyId);
}
