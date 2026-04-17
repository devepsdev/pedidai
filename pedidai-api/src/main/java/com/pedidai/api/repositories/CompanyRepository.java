package com.pedidai.api.repositories;

import com.pedidai.api.entities.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    boolean existsByTaxId(String taxId);

    Optional<Company> findByUuid(String uuid);

    boolean existsByUuid(String uuid);
}