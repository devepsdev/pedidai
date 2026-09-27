package com.pedidai.api.config;

import com.pedidai.api.dto.ApiResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Tradueix el camp {@code message} de totes les respostes de l'API a l'idioma de la petició.
 * Els controladors hi posen una clau (p. ex. "success.order.created"); si el text no és una clau
 * coneguda es deixa tal qual.
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class ApiMessageTranslationAdvice implements ResponseBodyAdvice<Object> {

    private final Messages messages;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof ApiResponseDTO<?> api && api.getMessage() != null && api.getMessage().startsWith("success.")) {
            api.setMessage(messages.get(api.getMessage()));
        }
        return body;
    }
}
