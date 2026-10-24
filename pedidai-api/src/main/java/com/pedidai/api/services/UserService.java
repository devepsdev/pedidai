package com.pedidai.api.services;

import com.pedidai.api.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    LoginResponseDTO login(LoginRequestDTO loginDTO, String clientIp);

    /**
     * Comprova email i contrasenya amb els mateixos límits d'intents que el login, sense mirar l'estat
     * de l'empresa ni obrir sessió. El fa servir el pagament d'una empresa amb la prova acabada.
     */
    com.pedidai.api.entities.User verifyCredentials(LoginRequestDTO loginDTO, String clientIp);

    void requestPasswordReset(String email, String clientIp);

    void resetPassword(PasswordResetDTO passwordResetDTO);

    void verifyEmail(String token);

    void resendVerificationEmail(String email, String clientIp);

    /** Reenvia la verificació a l'usuari autenticat. */
    void resendMyVerificationEmail();

    /** Dades de l'usuari autenticat. */
    UserResponseDTO getMe();

    /** Desa l'idioma preferit de l'usuari autenticat ("ca" o "es"). */
    UserResponseDTO updateMyLanguage(String language);

    UserResponseDTO registerUser(UserRegistrationDTO registrationDTO);

    Page<UserResponseDTO> getAllUsersPaginated(Pageable pageable);

    UserResponseDTO getUserByUuid(String uuid);

    Page<UserResponseDTO> searchUsersByText(String text, Pageable pageable);

    Page<UserResponseDTO> searchUsersWithFilters(UserFilterDTO filterDTO, Pageable pageable);

    UserResponseDTO updateUser(String uuid, UserRequestDTO userRequestDTO);

    UserResponseDTO changeUserStatus(String uuid, Boolean isActive);

    /** Canvia la contrasenya, tanca les altres sessions i torna una sessió nova per a aquest dispositiu. */
    LoginResponseDTO changePassword(String uuid, PasswordChangeDTO passwordChangeDTO);

    void deleteUser(String uuid);

    /** Genera el token de sessió per a un usuari ja validat (login automàtic després del registre). */
    LoginResponseDTO issueSession(com.pedidai.api.entities.User user);

    /** Token nou per a l'usuari autenticat: la web el demana cada dia perquè la sessió no caduqui mentre s'usa. */
    LoginResponseDTO renewSession();
}
