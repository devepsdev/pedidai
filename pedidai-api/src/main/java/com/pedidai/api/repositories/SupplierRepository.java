package com.pedidai.api.repositories;

import com.pedidai.api.entities.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    @Query("SELECT COUNT(s) > 0 FROM Supplier s WHERE s.company.uuid = :companyUuid AND LOWER(s.name) = LOWER(:name)")
    boolean existsByCompanyUuidAndNameIgnoreCase(@Param("companyUuid") String companyUuid, @Param("name") String name);

    Optional<Supplier> findByUuid(String uuid);

    @Query("SELECT COUNT(s) > 0 FROM Supplier s WHERE s.company.uuid = :companyUuid AND LOWER(s.name) = LOWER(:name) AND s.uuid != :supplierUuid")
    boolean existsByCompanyUuidAndNameIgnoreCaseAndUuidNot(@Param("companyUuid") String companyUuid, @Param("name") String name, @Param("supplierUuid") String supplierUuid);

    @Query("SELECT s FROM Supplier s WHERE s.company.id = :companyId AND s.isActive = true AND " +
            "(:name IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:contactName IS NULL OR LOWER(s.contactName) LIKE LOWER(CONCAT('%', :contactName, '%'))) AND " +
            "(:email IS NULL OR LOWER(s.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
            "(:phone IS NULL OR LOWER(s.phone) LIKE LOWER(CONCAT('%', :phone, '%'))) AND " +
            "(:address IS NULL OR LOWER(s.address) LIKE LOWER(CONCAT('%', :address, '%')))")
    Page<Supplier> findByCompanyIdAndCriteriaActive(
            @Param("companyId") Long companyId,
            @Param("name") String name,
            @Param("contactName") String contactName,
            @Param("email") String email,
            @Param("phone") String phone,
            @Param("address") String address,
            Pageable pageable);

    @Query("SELECT s FROM Supplier s WHERE s.company.id = :companyId AND s.isActive = true AND " +
            "(:searchText IS NULL OR " +
            "LOWER(s.name) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
            "LOWER(s.contactName) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
            "LOWER(s.email) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
            "LOWER(s.phone) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
            "LOWER(s.address) LIKE LOWER(CONCAT('%', :searchText, '%')))")
    Page<Supplier> findByCompanyIdAndMultipleFieldsContainingActive(
            @Param("companyId") Long companyId,
            @Param("searchText") String searchText,
            Pageable pageable);

    Page<Supplier> findByCompanyUuidAndIsActiveTrue(String companyUuid, Pageable pageable);

    @Query("SELECT s FROM Supplier s WHERE s.company.id = :companyId AND s.isActive = true AND LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Supplier> findActiveByCompanyIdAndNameContaining(@Param("companyId") Long companyId, @Param("name") String name);
}