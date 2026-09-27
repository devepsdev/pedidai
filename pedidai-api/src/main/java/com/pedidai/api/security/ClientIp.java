package com.pedidai.api.security;

import jakarta.servlet.http.HttpServletRequest;

/** IP real del client darrere de nginx (que fixa X-Real-IP i no es pot falsejar des de fora). */
public final class ClientIp {

    private ClientIp() {
    }

    public static String of(HttpServletRequest request) {
        String realIp = request.getHeader("X-Real-IP");
        return realIp != null && !realIp.isBlank() ? realIp.trim() : request.getRemoteAddr();
    }
}
