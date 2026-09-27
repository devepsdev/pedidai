package com.pedidai.api.exceptions;

import com.pedidai.api.config.Messages;
import com.pedidai.api.dto.ApiResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converteix les excepcions en respostes JSON amb el missatge traduït a l'idioma de la petició.
 * Els errors inesperats es registren al log però mai s'exposen els detalls interns al client.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final Messages messages;

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        return localized(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleDuplicateResource(DuplicateResourceException ex) {
        return localized(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleBadRequest(BadRequestException ex) {
        return localized(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleForbidden(ForbiddenException ex) {
        return localized(HttpStatus.FORBIDDEN, ex);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleTooManyRequests(TooManyRequestsException ex) {
        return localized(HttpStatus.TOO_MANY_REQUESTS, ex);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleAccessDenied(AccessDeniedException ex) {
        return error(HttpStatus.FORBIDDEN, messages.get("error.forbidden"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = error instanceof FieldError fe ? fe.getField() : error.getObjectName();
            errors.putIfAbsent(fieldName, error.getDefaultMessage());
        });
        // El primer error concret és més útil per a l'usuari que un "error de validació" genèric
        String message = errors.values().stream().findFirst().orElse(messages.get("error.validation"));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDTO.<Map<String, String>>builder()
                        .success(false)
                        .message(message)
                        .data(errors)
                        .build());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponseDTO<Void>> handleMalformedRequest(Exception ex) {
        return error(HttpStatus.BAD_REQUEST, messages.get("error.malformedRequest"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleMediaType(HttpMediaTypeNotSupportedException ex) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, messages.get("error.malformedRequest"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleMethod(HttpRequestMethodNotSupportedException ex) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, messages.get("error.malformedRequest"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleMaxUpload(MaxUploadSizeExceededException ex) {
        return error(HttpStatus.BAD_REQUEST, messages.get("error.file.tooLarge"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleNoResource(NoResourceFoundException ex) {
        return error(HttpStatus.NOT_FOUND, messages.get("error.notFound"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleGenericException(Exception ex) {
        log.error("Error no controlat", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, messages.get("error.internal"));
    }

    private ResponseEntity<ApiResponseDTO<Void>> localized(HttpStatus status, LocalizedException ex) {
        return error(status, messages.get(ex.getMessage(), ex.getArgs()));
    }

    private ResponseEntity<ApiResponseDTO<Void>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiResponseDTO.error(message));
    }
}
