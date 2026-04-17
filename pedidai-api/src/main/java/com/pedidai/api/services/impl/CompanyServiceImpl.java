package com.pedidai.api.services.impl;

import com.pedidai.api.dto.CompanyRegistrationDTO;
import com.pedidai.api.dto.CompanyRequestDTO;
import com.pedidai.api.dto.CompanyResponseDTO;
import com.pedidai.api.entities.Company;
import com.pedidai.api.entities.User;
import com.pedidai.api.exceptions.BadRequestException;
import com.pedidai.api.exceptions.DuplicateResourceException;
import com.pedidai.api.exceptions.ResourceNotFoundException;
import com.pedidai.api.repositories.CompanyRepository;
import com.pedidai.api.repositories.UserRepository;
import com.pedidai.api.services.CompanyService;
import com.pedidai.api.services.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.pedidai.api.entities.User.UserRole.ADMIN;

@Service
@RequiredArgsConstructor
@Transactional
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Override
    @Transactional
    public CompanyResponseDTO registerCompanyWithAdmin(CompanyRegistrationDTO registrationDTO) {

        // Validar que el taxId no existeixi
        if (companyRepository.existsByTaxId(registrationDTO.getTaxId())) {
            throw new DuplicateResourceException("El CIF/NIF ja està registrat");
        }

        // Validar que l'email del admin no existeixi
        if (userRepository.existsByEmail(registrationDTO.getAdminEmail())) {
            throw new DuplicateResourceException("L'email de l'administrador ja està registrat");
        }

        // 1. Crear l'empresa
        Company company = Company.builder()
                .name(registrationDTO.getCompanyName())
                .taxId(registrationDTO.getTaxId())
                .email(registrationDTO.getCompanyEmail())
                .phone(registrationDTO.getCompanyPhone())
                .address(registrationDTO.getCompanyAddress())
                .city(registrationDTO.getCompanyCity())
                .postalCode(registrationDTO.getCompanyPostalCode())
                .status(Company.CompanyStatus.PENDING)
                .build();

        company = companyRepository.save(company);

        // 2. Crear l'usuari administrador
        String verificationToken = UUID.randomUUID().toString();

        User admin = User.builder()
                .company(company)
                .email(registrationDTO.getAdminEmail())
                .password(passwordEncoder.encode(registrationDTO.getAdminPassword()))
                .firstName(registrationDTO.getAdminFirstName())
                .lastName(registrationDTO.getAdminLastName())
                .phone(registrationDTO.getAdminPhone())
                .role(User.UserRole.ADMIN)
                .isActive(true)
                .emailVerified(false)
                .emailVerificationToken(verificationToken)
                .emailVerificationExpires(LocalDateTime.now().plusHours(24))
                .build();

        admin = userRepository.save(admin);

        // 3. Enviar email de verificació
        emailService.sendCompanyAdminVerification(
                admin.getEmail(),
                verificationToken,
                admin.getFirstName(),
                company.getName()
        );

        // 4. Retornar la empresa creada
        return mapToResponseDTO(company);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyResponseDTO getCompanyByUuid() {
        //Validar que el rol sigui administrador
        if (!isAdminUser()) {
            throw new BadRequestException("L'usuari ha de ser Administrador");
        }
        String companyUuid = getCompanyUuidFromAuthenticatedUser();

        // Verificar que l'empresa existeix
        Company company = companyRepository.findByUuid(companyUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no trobada amb UUID: " + companyUuid));
        return mapToResponseDTO(company);
    }

    @Override
    @Transactional
    public CompanyResponseDTO updateCompany(CompanyRequestDTO companyRequestDTO) {
        //Validar que el rol sigui administrador
        if (!isAdminUser()) {
            throw new BadRequestException("L'usuari ha de ser Administrador");
        }
        String companyUuid = getCompanyUuidFromAuthenticatedUser();

        // Verificar que l'empresa existeix
        Company company = companyRepository.findByUuid(companyUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no trobada amb UUID: " + companyUuid));

        if (!company.getTaxId().equals(companyRequestDTO.getTaxId()) &&
                companyRepository.existsByTaxId(companyRequestDTO.getTaxId())) {
            throw new DuplicateResourceException("Ja existeix una empresa amb el NIF/CIF: " + companyRequestDTO.getTaxId());
        }

        company.setName(companyRequestDTO.getName());
        company.setTaxId(companyRequestDTO.getTaxId());
        company.setEmail(companyRequestDTO.getEmail());
        company.setPhone(companyRequestDTO.getPhone());
        company.setAddress(companyRequestDTO.getAddress());
        company.setCity(companyRequestDTO.getCity());
        company.setPostalCode(companyRequestDTO.getPostalCode());

        if (companyRequestDTO.getStatus() != null) {
            company.setStatus(companyRequestDTO.getStatus());
        }

        Company updatedCompany = companyRepository.save(company);
        return mapToResponseDTO(updatedCompany);
    }

    private String getCompanyUuidFromAuthenticatedUser() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuari no trobat: " + username));

        if (user.getCompany() == null || user.getCompany().getUuid() == null) {
            throw new ResourceNotFoundException("L'usuari no té empresa assignada");
        }

        return user.getCompany().getUuid();
    }

    private Boolean isAdminUser() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuari no trobat: " + username));

        return user.getRole() == ADMIN;
    }

    private CompanyResponseDTO mapToResponseDTO(Company company) {
        return CompanyResponseDTO.builder()
                .uuid(company.getUuid())
                .name(company.getName())
                .taxId(company.getTaxId())
                .email(company.getEmail())
                .phone(company.getPhone())
                .address(company.getAddress())
                .city(company.getCity())
                .postalCode(company.getPostalCode())
                .status(company.getStatus())
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .build();
    }

}