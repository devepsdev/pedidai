package com.pedidai.api.repositories;

import com.pedidai.api.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUuid(String uuid);

    @Query("SELECT u FROM User u WHERE u.emailVerificationToken = :token AND u.emailVerificationExpires > :now")
    Optional<User> findByValidVerificationToken(@Param("token") String token, @Param("now") LocalDateTime now);

    @Query("SELECT u FROM User u WHERE u.passwordResetToken = :token AND u.passwordResetExpires > :now")
    Optional<User> findByValidResetToken(@Param("token") String token, @Param("now") LocalDateTime now);


    boolean existsByEmail(String email);

    Page<User> findByCompanyUuidAndIsDeletedFalse(String companyUuid, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.company.id = :companyId AND u.isDeleted = false AND " +
            "(:searchText IS NULL OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
            "LOWER(u.phone) LIKE LOWER(CONCAT('%', :searchText, '%')))")
    Page<User> findByCompanyIdAndMultipleFieldsContainingNoDeleted(
            @Param("companyId") Long companyId,
            @Param("searchText") String searchText,
            Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.company.id = :companyId AND u.isDeleted = false AND " +
            "(:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
            "(:firstName IS NULL OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :firstName, '%'))) AND " +
            "(:lastName IS NULL OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :lastName, '%'))) AND " +
            "(:phone IS NULL OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :phone, '%'))) AND " +
            "(:isActive IS NULL OR u.isActive = :isActive) AND " +
            "(:emailVerified IS NULL OR u.emailVerified = :emailVerified) AND " +
            "(:role IS NULL OR u.role = :role)")
    Page<User> findByCompanyIdAndCriteriaActive(
            @Param("companyId") Long companyId,
            @Param("email") String email,
            @Param("firstName") String firstName,
            @Param("lastName") String lastName,
            @Param("phone") String phone,
            @Param("isActive") Boolean isActive,
            @Param("emailVerified") Boolean emailVerified,
            @Param("role") User.UserRole role,
            Pageable pageable);
}