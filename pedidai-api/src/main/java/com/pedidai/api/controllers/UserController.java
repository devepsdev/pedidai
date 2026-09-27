package com.pedidai.api.controllers;

import com.pedidai.api.dto.*;
import com.pedidai.api.services.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<UserResponseDTO>>> getAllUsers(
            @Valid UserSearchDTO searchDTO) {
        Sort sort = searchDTO.getSortDir().equalsIgnoreCase("desc") ?
                Sort.by(searchDTO.getSortBy()).descending() :
                Sort.by(searchDTO.getSortBy()).ascending();

        Pageable pageable = PageRequest.of(searchDTO.getPage(), searchDTO.getSize(), sort);

        Page<UserResponseDTO> users = userService.getAllUsersPaginated(pageable);
        // Convertir Page a PagedResponseDTO per evitar warning de serialització
        PagedResponseDTO<UserResponseDTO> pagedResponse = PagedResponseDTO.of(users);

        return ResponseEntity.ok(
                ApiResponseDTO.success(pagedResponse, "success.ok"));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<UserResponseDTO>>> searchUsersByText(
            @Valid UserSearchDTO searchDTO) {

        Sort sort = searchDTO.getSortDir().equalsIgnoreCase("desc") ?
                Sort.by(searchDTO.getSortBy()).descending() :
                Sort.by(searchDTO.getSortBy()).ascending();

        Pageable pageable = PageRequest.of(searchDTO.getPage(), searchDTO.getSize(), sort);

        Page<UserResponseDTO> users = userService.searchUsersByText(
                searchDTO.getSearchText(), pageable);

        // Convertir Page a PagedResponseDTO per evitar warning de serialització
        PagedResponseDTO<UserResponseDTO> pagedResponse = PagedResponseDTO.of(users);

        return ResponseEntity.ok(
                ApiResponseDTO.success(pagedResponse, "success.search.done"));
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<UserResponseDTO>>> filterUsers(
            @Valid UserFilterDTO filterDTO) {

        Sort sort = filterDTO.getSortDir().equalsIgnoreCase("desc") ?
                Sort.by(filterDTO.getSortBy()).descending() :
                Sort.by(filterDTO.getSortBy()).ascending();

        Pageable pageable = PageRequest.of(filterDTO.getPage(), filterDTO.getSize(), sort);

        Page<UserResponseDTO> users = userService.searchUsersWithFilters(
                filterDTO, pageable);

        // Convertir Page a PagedResponseDTO per evitar warning de serialització
        PagedResponseDTO<UserResponseDTO> pagedResponse = PagedResponseDTO.of(users);

        String message = "success.search.done";

        return ResponseEntity.ok(
                ApiResponseDTO.success(pagedResponse, message));
    }

    /** Dades de l'usuari autenticat (inclou si ha verificat l'email i el seu idioma). */
    @GetMapping("/me")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> getMe() {
        return ResponseEntity.ok(ApiResponseDTO.success(userService.getMe(), "success.ok"));
    }

    @PatchMapping("/me/language")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> updateMyLanguage(@RequestParam String language) {
        return ResponseEntity.ok(ApiResponseDTO.success(userService.updateMyLanguage(language), "success.ok"));
    }

    @PostMapping("/me/resend-verification")
    public ResponseEntity<ApiResponseDTO<Void>> resendMyVerification() {
        userService.resendMyVerificationEmail();
        return ResponseEntity.ok(ApiResponseDTO.success(null, "success.verification.resent"));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> getUserByUuid(
            @PathVariable @NotBlank(message = "{validation.uuid.required}") String uuid) {
        UserResponseDTO user = userService.getUserByUuid(uuid);
        return ResponseEntity.ok(ApiResponseDTO.success(user, "success.ok"));
    }

    @PostMapping()
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> registerUser(
            @Valid @RequestBody UserRegistrationDTO registrationDTO) {
        UserResponseDTO created = userService.registerUser(registrationDTO);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(created, "success.user.created"));
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> updateUser(
            @PathVariable @NotBlank(message = "{validation.uuid.required}") String uuid,
            @Valid @RequestBody UserRequestDTO userRequestDTO) {
        UserResponseDTO updated = userService.updateUser(uuid, userRequestDTO);
        return ResponseEntity.ok(ApiResponseDTO.success(updated, "success.user.updated"));
    }

    @PatchMapping("/{uuid}/status")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> changeUserStatus(
            @PathVariable @NotBlank(message = "{validation.uuid.required}") String uuid,
            @RequestParam Boolean isActive) {
        UserResponseDTO updated = userService.changeUserStatus(uuid, isActive);
        return ResponseEntity.ok(ApiResponseDTO.success(updated, "success.user.statusUpdated"));
    }

    @PatchMapping("/{uuid}/change-password")
    public ResponseEntity<ApiResponseDTO<Void>> changePassword(
            @PathVariable @NotBlank(message = "{validation.uuid.required}") String uuid,
            @Valid @RequestBody PasswordChangeDTO passwordChangeDTO) {
        userService.changePassword(uuid, passwordChangeDTO);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "success.password.changed"));
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteUser(
            @PathVariable @NotBlank(message = "{validation.uuid.required}") String uuid) {
        userService.deleteUser(uuid);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "success.user.deleted"));
    }
}