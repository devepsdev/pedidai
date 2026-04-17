package com.pedidai.api.services.impl;

import com.pedidai.api.dto.*;
import com.pedidai.api.entities.User;
import com.pedidai.api.entities.Company;
import com.pedidai.api.exceptions.BadRequestException;
import com.pedidai.api.exceptions.DuplicateResourceException;
import com.pedidai.api.exceptions.ResourceNotFoundException;
import com.pedidai.api.repositories.CompanyRepository;
import com.pedidai.api.repositories.UserRepository;
import com.pedidai.api.security.JwtUtil;
import com.pedidai.api.services.EmailService;
import com.pedidai.api.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final CompanyRepository companyRepository;

    private final EmailService emailService;

    private final JwtUtil jwtUtil;

    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponseDTO registerUser(UserRegistrationDTO registrationDTO) {
        //Validar que el rol sigui administrador
        if (!isAdminUser()) {
            throw new BadRequestException("L'usuari ha de ser Administrador");
        }

        if (userRepository.existsByEmail(registrationDTO.getEmail())) {
            throw new DuplicateResourceException("Ja existeix un usuari amb l'email: " + registrationDTO.getEmail());
        }

        String companyUuid = getCompanyUuidFromAuthenticatedUser();
        // Verificar que l'empresa existeix
        Company company = companyRepository.findByUuid(companyUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Empresa no trobada amb UUID: " + companyUuid));

        // Generar token de verificació (vàlid per 24 hores)
        String verificationToken = UUID.randomUUID().toString();

        User user = User.builder()
                .company(company)
                .email(registrationDTO.getEmail())
                .password(passwordEncoder.encode(registrationDTO.getPassword()))
                .firstName(registrationDTO.getFirstName())
                .lastName(registrationDTO.getLastName())
                .role(registrationDTO.getRole() != null ? registrationDTO.getRole() : User.UserRole.USER)
                .phone(registrationDTO.getPhone())
                .isActive(true)
                .isDeleted(false)
                .emailVerified(false)
                .emailVerificationToken(verificationToken)
                .emailVerificationExpires(LocalDateTime.now().plusHours(24))
                .build();

        User savedUser = userRepository.save(user);

        // Correu electrònic per a usuaris que s'uneixen a una empresa existent
        emailService.sendEmailVerification(
                savedUser.getEmail(),
                verificationToken,
                savedUser.getFirstName()
        );

        return mapToResponseDTO(savedUser);
    }

    @Override
    public LoginResponseDTO login(LoginRequestDTO loginDTO) {
        User user = userRepository.findByEmail(loginDTO.getEmail())
                .orElseThrow(() -> new BadRequestException("Credencials invàlides"));

        if (!user.getIsActive()) {
            throw new BadRequestException("L'usuari està inactiu");
        }

        if (user.getIsDeleted()) {
            throw new BadRequestException("L'usuari està eliminat");
        }

        if (!user.getEmailVerified()) {
            throw new BadRequestException("Has de verificar el teu correu electrònic abans d'iniciar sessió. Revisa la teva safata d'entrada.");
        }

        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BadRequestException("Credencials invàlides");
        }

        Company company = user.getCompany();
        if (company.getStatus() == Company.CompanyStatus.SUSPENDED) {
            throw new BadRequestException("El compte de l'empresa està suspès. Contacta amb el suport.");
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        return LoginResponseDTO.builder()
                .token(token)
                .type("Bearer")
                .user(mapToResponseDTO(user))
                .build();
    }


    @Override
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuari no trobat amb l'email: " + email));

        String resetToken = UUID.randomUUID().toString();
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetExpires(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        emailService.sendPasswordResetEmail(user.getEmail(), resetToken, user.getFirstName());
    }

    @Override
    public void resetPassword(PasswordResetDTO passwordResetDTO) {
        User user = userRepository.findByValidResetToken(passwordResetDTO.getToken(), LocalDateTime.now())
                .orElseThrow(() -> new BadRequestException("Token invàlid o expirat"));

        user.setPassword(passwordEncoder.encode(passwordResetDTO.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpires(null);
        userRepository.save(user);
    }

    @Override
    public void verifyEmail(String token) {
        User user = userRepository.findByValidVerificationToken(token, LocalDateTime.now())
                .orElseThrow(() -> new BadRequestException("Token de verificació invàlid o expirat"));

        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationExpires(null);

        // Activar l'empresa si l'usuari és administrador
        if (user.getRole() == ADMIN) {
            Company company = user.getCompany();
            if (company != null && company.getStatus() == Company.CompanyStatus.PENDING) {
                company.setStatus(Company.CompanyStatus.ACTIVE);
                companyRepository.save(company);
            }
        }

        userRepository.save(user);
    }

    @Override
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuari no trobat amb l'email: " + email));

        if (user.getEmailVerified()) {
            throw new BadRequestException("Aquest email ja està verificat");
        }

        // Generar nou token de verificació
        String verificationToken = UUID.randomUUID().toString();
        user.setEmailVerificationToken(verificationToken);
        user.setEmailVerificationExpires(LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        // Reenviar email
        emailService.sendEmailVerification(user.getEmail(), verificationToken, user.getFirstName());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDTO> getAllUsersPaginated(Pageable pageable) {
        //Validar que el rol sigui administrador
        if (!isAdminUser()) {
            throw new BadRequestException("L'usuari ha de ser Administrador");
        }
        String companyUuid = getCompanyUuidFromAuthenticatedUser();

        // Mètode automàtic de Spring Data
        Page<User> usersPage = userRepository.findByCompanyUuidAndIsDeletedFalse(companyUuid, pageable);

        return usersPage.map(this::mapToResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserByUuid(String uuid) {
        //Validar que el rol sigui administrador
        if (!isAdminUser()) {
            throw new BadRequestException("L'usuari ha de ser Administrador");
        }
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Usuari no trobat amb UUID: " + uuid));
        return mapToResponseDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDTO> searchUsersByText(String searchText, Pageable pageable) {
        String companyUuid = getCompanyUuidFromAuthenticatedUser();

        // Verificar que l'empresa existeix
        Company company = companyRepository.findByUuid(companyUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no trobada amb UUID: " + companyUuid));

        // Mètode de cerca en múltiples camps
        Page<User> users = userRepository.findByCompanyIdAndMultipleFieldsContainingNoDeleted(
                company.getId(), searchText, pageable);

        return users.map(this::mapToResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDTO> searchUsersWithFilters(UserFilterDTO filterDTO, Pageable pageable) {
        String companyUuid = getCompanyUuidFromAuthenticatedUser();

        // Verificar que l'empresa existeix
        Company company = companyRepository.findByUuid(companyUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no trobada amb UUID: " + companyUuid));

        // Usar el mètode del repositori amb tots els filtres expandits
        Page<User> users = userRepository.findByCompanyIdAndCriteriaActive(
                company.getId(),
                filterDTO.getEmail(),
                filterDTO.getFirstName(),
                filterDTO.getLastName(),
                filterDTO.getPhone(),
                filterDTO.getIsActive(),
                filterDTO.getEmailVerified(),
                filterDTO.getRole(),
                pageable
        );

        return users.map(this::mapToResponseDTO);
    }

    @Override
    public UserResponseDTO updateUser(String uuid, UserRequestDTO userRequestDTO) {
        //Validar que el rol sigui administrador
        if (!isAdminUser()) {
            throw new BadRequestException("L'usuari ha de ser Administrador");
        }
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Usuari no trobat amb UUID: " + uuid));

        if (!user.getEmail().equals(userRequestDTO.getEmail()) && userRepository.existsByEmail(userRequestDTO.getEmail())) {
            throw new DuplicateResourceException("Ja existeix un usuari amb l'email: " + userRequestDTO.getEmail());
        }

        user.setEmail(userRequestDTO.getEmail());
        user.setFirstName(userRequestDTO.getFirstName());
        user.setLastName(userRequestDTO.getLastName());
        user.setPhone(userRequestDTO.getPhone());

        if (userRequestDTO.getRole() != null) {
            user.setRole(userRequestDTO.getRole());
        }

        if (userRequestDTO.getIsActive() != null) {
            user.setIsActive(userRequestDTO.getIsActive());
        }

        User updatedUser = userRepository.save(user);
        return mapToResponseDTO(updatedUser);
    }

    @Override
    public UserResponseDTO changeUserStatus(String uuid, Boolean isActive) {
        //Validar que el rol sigui administrador
        if (!isAdminUser()) {
            throw new BadRequestException("L'usuari ha de ser Administrador");
        }
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Usuari no trobat amb UUID: " + uuid));
        user.setIsActive(isActive);
        User updatedUser = userRepository.save(user);
        return mapToResponseDTO(updatedUser);
    }

    @Override
    public void changePassword(String uuid, PasswordChangeDTO passwordChangeDTO) {
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Usuari no trobat amb UUID: " + uuid));

        if (!passwordEncoder.matches(passwordChangeDTO.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("La contrasenya actual és incorrecta");
        }

        user.setPassword(passwordEncoder.encode(passwordChangeDTO.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    public void deleteUser(String uuid) {
        //Validar que el rol sigui administrador
        if (!isAdminUser()) {
            throw new BadRequestException("L'usuari ha de ser Administrador");
        }
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Usuari no trobat amb UUID: " + uuid));
        user.setIsDeleted(true);
        userRepository.save(user);
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

    private UserResponseDTO mapToResponseDTO(User user) {
        return UserResponseDTO.builder()
                .uuid(user.getUuid())
                .companyUuid(user.getCompany().getUuid())
                .companyName(user.getCompany().getName())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .phone(user.getPhone())
                .isActive(user.getIsActive())
                .isDeleted(user.getIsDeleted())
                .emailVerified(user.getEmailVerified())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}