package com.pedidai.api.services;

import com.pedidai.api.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    LoginResponseDTO login(LoginRequestDTO loginDTO);

    void requestPasswordReset(String email);

    void resetPassword(PasswordResetDTO passwordResetDTO);

    void verifyEmail(String token);

    void resendVerificationEmail(String email);

    UserResponseDTO registerUser(UserRegistrationDTO registrationDTO);

    Page<UserResponseDTO> getAllUsersPaginated(Pageable pageable);

    UserResponseDTO getUserByUuid(String uuid);

    Page<UserResponseDTO> searchUsersByText(String text, Pageable pageable);

    Page<UserResponseDTO> searchUsersWithFilters(UserFilterDTO filterDTO, Pageable pageable);

    UserResponseDTO updateUser(String uuid, UserRequestDTO userRequestDTO);

    UserResponseDTO changeUserStatus(String uuid, Boolean isActive);

    void changePassword(String uuid, PasswordChangeDTO passwordChangeDTO);

    void deleteUser(String uuid);
}