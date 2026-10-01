package com.pedidai.api.services.impl;

import com.pedidai.api.config.I18nConfig;
import com.pedidai.api.config.Messages;
import com.pedidai.api.services.EmailService;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailServiceImpl: correus bilingües i sense HTML injectat")
class EmailServiceImplTest {

    @Mock private JavaMailSender mailSender;
    private EmailServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EmailServiceImpl(mailSender, new Messages(new I18nConfig().messageSource()));
        ReflectionTestUtils.setField(service, "fromEmail", "noreply@pedidai.test");
        ReflectionTestUtils.setField(service, "frontendUrl", "https://pedidai.test");
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
    }

    private MimeMessage sent() {
        ArgumentCaptor<MimeMessage> message = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(message.capture());
        return message.getValue();
    }

    private static String html(MimeMessage message) throws Exception {
        // El cos és multipart: es busca la part HTML recursivament
        return findHtml(message.getContent());
    }

    private static String findHtml(Object content) throws Exception {
        if (content instanceof String s) {
            return s;
        }
        if (content instanceof jakarta.mail.Multipart mp) {
            for (int i = 0; i < mp.getCount(); i++) {
                String found = findHtml(mp.getBodyPart(i).getContent());
                if (found != null && found.contains("<html")) {
                    return found;
                }
            }
        }
        return null;
    }

    @Test
    @DisplayName("la comanda escapa el text de l'usuari i les respostes van al client")
    void orderEmailIsEscaped() throws Exception {
        var order = new EmailService.OrderEmail("pere@prov.test", "laura@bar.test", "Pere", "Bar <b>Prova</b>",
                null, null, "Setmanal", List.of(new EmailService.OrderEmailLine("<script>alert(1)</script>", "10", "kg", null)),
                "<a href=\"http://phishing.test\">clica</a>");

        service.sendOrderNotification(order, I18nConfig.CATALAN);

        MimeMessage message = sent();
        String body = html(message);
        assertThat(body).doesNotContain("<script>").doesNotContain("<a href=\"http://phishing.test\">");
        assertThat(body).contains("&lt;script&gt;");
        assertThat(((InternetAddress) message.getReplyTo()[0]).getAddress()).isEqualTo("laura@bar.test");
        assertThat(message.getSubject()).startsWith("Nova comanda");
    }

    @Test
    @DisplayName("el correu de benvinguda surt en l'idioma de l'usuari")
    void welcomeEmailLanguage() throws Exception {
        service.sendWelcomeVerification("laura@bar.test", "token-1", "Laura", "Bar Prova", I18nConfig.SPANISH);

        MimeMessage message = sent();
        assertThat(message.getSubject()).contains("prueba de 14 días");
        assertThat(html(message)).contains("https://pedidai.test/verify-email?token=token-1");
    }

    @Test
    @DisplayName("si el correu de compte falla, no es propaga l'error (l'usuari pot demanar-ne un altre)")
    void accountEmailFailureIsSwallowed() {
        doThrow(new org.springframework.mail.MailSendException("smtp caigut")).when(mailSender).send(any(MimeMessage.class));

        assertThatCode(() -> service.sendEmailVerification("a@b.test", "t", "A", I18nConfig.CATALAN)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("avís de fi de prova: data en l'idioma, nom escapat i respostes a PedidAI")
    void trialEndingSoonEmail() throws Exception {
        var trial = new EmailService.TrialEmail("laura@bar.test", "Laura", "Bar <b>Prova</b>",
                java.time.LocalDate.of(2026, 10, 12), java.time.LocalDate.of(2026, 11, 11), 12, 3, 2);

        service.sendTrialEndingSoon(trial, I18nConfig.CATALAN);

        MimeMessage message = sent();
        assertThat(message.getSubject()).contains("12 d’octubre del 2026");
        assertThat(message.getReplyTo()[0].toString()).isEqualTo("hola@pedidai.es");
        String body = html(message);
        assertThat(body).contains("Bar &lt;b&gt;Prova&lt;/b&gt;").doesNotContain("<b>Prova</b>");
        assertThat(body).contains("Línies d’albarà llegides: 12").contains("mailto:hola@pedidai.es");
    }

    @Test
    @DisplayName("avís de prova acabada en castellà amb la data d'esborrat")
    void trialEndedEmail() throws Exception {
        var trial = new EmailService.TrialEmail("laura@bar.test", "Laura", "Bar Prova",
                java.time.LocalDate.of(2026, 10, 12), java.time.LocalDate.of(2026, 11, 11), 0, 0, 0);

        service.sendTrialEnded(trial, I18nConfig.SPANISH);

        String body = html(sent());
        assertThat(body).contains("11 de noviembre de 2026").contains("no se ha cobrado nada");
    }

    @Test
    @DisplayName("el remitent porta el nom de PedidAI")
    void senderHasDisplayName() throws Exception {
        ReflectionTestUtils.setField(service, "fromEmail", "hola@pedidai.test");
        ReflectionTestUtils.setField(service, "fromName", "PedidAI");
        var trial = new EmailService.TrialEmail("laura@bar.test", "Laura", "Bar Prova",
                java.time.LocalDate.of(2026, 10, 12), java.time.LocalDate.of(2026, 11, 11), 0, 0, 0);

        service.sendTrialEnded(trial, I18nConfig.SPANISH);

        assertThat(sent().getFrom()[0].toString()).isEqualTo("PedidAI <hola@pedidai.test>");
    }
}
