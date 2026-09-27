package com.pedidai.api.services.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Emmascarament de correus als logs")
class EmailMaskTest {

    @Test
    @DisplayName("Els logs no mostren l'adreça de correu completa")
    void maskEmail_hidesLocalPart() {
        assertThat(EmailServiceImpl.maskEmail("pere@prov.test")).isEqualTo("p***@prov.test");
        assertThat(EmailServiceImpl.maskEmail("sense-arrova")).isEqualTo("***");
        assertThat(EmailServiceImpl.maskEmail(null)).isNull();
    }
}
