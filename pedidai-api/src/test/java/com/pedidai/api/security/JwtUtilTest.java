package com.pedidai.api.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtUtil: tokens de 30 dies amb la versió de sessió")
class JwtUtilTest {

    private static final String SECRET = Base64.getEncoder().encodeToString(new byte[64]);

    @Test
    @DisplayName("el token porta l'usuari, el rol i la versió, i dura els dies configurats")
    void tokenWithVersion() {
        JwtUtil jwt = new JwtUtil(SECRET, 30);

        String token = jwt.generateToken("u@bar.test", "ADMIN", 5);

        assertThat(jwt.getUsernameFromToken(token)).isEqualTo("u@bar.test");
        assertThat(jwt.getRoleFromToken(token)).isEqualTo("ADMIN");
        assertThat(jwt.getTokenVersion(token)).isEqualTo(5);
        assertThat(jwt.validateToken(token, "u@bar.test")).isTrue();
        long restant = jwt.getExpirationDateFromToken(token).getTime() - System.currentTimeMillis();
        assertThat(restant).isBetween(Duration.ofDays(29).toMillis(), Duration.ofDays(30).toMillis());
    }

    @Test
    @DisplayName("un token signat amb una altra clau no és vàlid")
    void otherKey() {
        byte[] altra = new byte[64];
        altra[0] = 1;
        String token = new JwtUtil(Base64.getEncoder().encodeToString(altra), 30).generateToken("u@bar.test", "ADMIN", 0);

        assertThat(new JwtUtil(SECRET, 30).validateToken(token, "u@bar.test")).isFalse();
    }

    @Test
    @DisplayName("els tokens emesos amb jjwt 0.11.5 (HS512, caduquen el 2100) continuen sent vàlids")
    void tokenFromPreviousLibraryVersion() {
        String token = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1QGJhci50ZXN0Iiwicm9sZSI6IkFETUlOIiwidHYiOjMsImlhdCI6MTc2MTMwMDAwMCwiZXhwIjo0MTAyNDQ0ODAwfQ"
                + ".eHNWjWp2kS6yShVA7RY1yMUJp-fIpGs8YISoZEoNc22Pp3YHy2dExpusg5CEEpUFdpkjYKj8bGaX7D8INEN9Pg";
        JwtUtil jwt = new JwtUtil(SECRET, 30);

        assertThat(jwt.validateToken(token, "u@bar.test")).isTrue();
        assertThat(jwt.getRoleFromToken(token)).isEqualTo("ADMIN");
        assertThat(jwt.getTokenVersion(token)).isEqualTo(3);
    }
}
