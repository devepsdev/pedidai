package com.pedidai.api.services.impl;

import com.pedidai.api.config.Messages;
import com.pedidai.api.services.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import static org.springframework.web.util.HtmlUtils.htmlEscape;

/**
 * Correus de la plataforma en català o castellà segons l'idioma de l'usuari.
 * Tot el contingut que prové de l'usuari s'escapa abans d'inserir-lo a l'HTML.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    /** Adreça de contacte de PedidAI: rep les respostes als avisos de la prova. */
    static final String CONTACT_EMAIL = "hola@pedidai.es";

    private final JavaMailSender mailSender;
    private final Messages messages;

    /** Remitent dels correus (en producció, hola@pedidai.es autenticat amb DKIM a Brevo). */
    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name:PedidAI}")
    private String fromName = "PedidAI";

    @Value("${app.frontend.url:https://pedidai.es}")
    private String frontendUrl;

    // ───────────────────────── Correus de compte (asíncrons) ─────────────────────────

    @Async
    @Override
    public void sendPasswordResetEmail(String to, String token, String userName, Locale locale) {
        String link = frontendUrl + "/reset-password?token=" + token;
        String body = greeting(locale, userName)
                + paragraph(t(locale, "email.reset.intro"))
                + button(link, t(locale, "email.reset.button"))
                + notice(t(locale, "email.reset.expires"))
                + small(t(locale, "email.reset.ignore"));
        sendAccountEmail(to, t(locale, "email.reset.subject"), locale, t(locale, "email.reset.title"), body, link);
    }

    @Async
    @Override
    public void sendEmailVerification(String to, String token, String userName, Locale locale) {
        String link = frontendUrl + "/verify-email?token=" + token;
        String body = greeting(locale, userName)
                + paragraph(t(locale, "email.verify.intro"))
                + button(link, t(locale, "email.verify.button"))
                + notice(t(locale, "email.verify.expires"))
                + small(t(locale, "email.verify.ignore"));
        sendAccountEmail(to, t(locale, "email.verify.subject"), locale, t(locale, "email.verify.title"), body, link);
    }

    @Async
    @Override
    public void sendWelcomeVerification(String to, String token, String userName, String companyName, Locale locale) {
        String link = frontendUrl + "/verify-email?token=" + token;
        String body = greeting(locale, userName)
                + paragraph(t(locale, "email.welcome.intro", htmlEscape(companyName)))
                + paragraph(t(locale, "email.welcome.verifyWhy"))
                + button(link, t(locale, "email.welcome.button"))
                + infoBox(t(locale, "email.welcome.nextTitle"),
                        "1. " + t(locale, "email.welcome.step1") + "<br>"
                                + "2. " + t(locale, "email.welcome.step2") + "<br>"
                                + "3. " + t(locale, "email.welcome.step3"))
                + notice(t(locale, "email.verify.expires"))
                + small(t(locale, "email.welcome.ignore"));
        sendAccountEmail(to, t(locale, "email.welcome.subject"), locale, t(locale, "email.welcome.title"), body, link);
    }

    private void sendAccountEmail(String to, String subject, Locale locale, String headerTitle, String body, String link) {
        String footer = t(locale, "email.footer.buttonFallback") + "<br><span style=\"color:#06b6d4;word-break:break-all;\">"
                + htmlEscape(link) + "</span>";
        try {
            send(to, null, subject, layout(locale, headerTitle, null, body, footer));
        } catch (MessagingException | RuntimeException e) {
            // Un correu de compte que falla no ha de trencar el registre: l'usuari pot demanar-ne un altre
            log.error("No s'ha pogut enviar el correu '{}' a {}: {}", subject, maskEmail(to), e.getMessage());
        }
    }

    // ───────────────────────── Comanda al proveïdor (síncron) ─────────────────────────

    @Override
    public void sendOrderNotification(OrderEmail order, Locale locale) {
        StringBuilder rows = new StringBuilder();
        for (OrderEmailLine line : order.lines()) {
            String lineNotes = line.notes() == null || line.notes().isBlank() ? ""
                    : "<br><span style=\"font-size:12px;color:#999;font-style:italic;\">"
                    + htmlEscape(line.notes()) + "</span>";
            rows.append("""
                    <tr style="border-bottom:1px solid #eeeeee;">
                      <td style="padding:12px;color:#333333;"><strong>%s</strong>%s</td>
                      <td style="padding:12px;text-align:right;color:#333333;white-space:nowrap;">%s %s</td>
                    </tr>""".formatted(htmlEscape(line.productName()), lineNotes,
                    htmlEscape(line.quantity()), htmlEscape(nullToEmpty(line.unit()))));
        }

        String table = """
                <table width="100%%" cellpadding="0" cellspacing="0" style="border:1px solid #e0e0e0;border-radius:6px;margin:10px 0 20px;">
                  <tr style="background-color:#06b6d4;">
                    <th style="padding:10px 12px;text-align:left;color:#ffffff;font-size:14px;">%s</th>
                    <th style="padding:10px 12px;text-align:right;color:#ffffff;font-size:14px;">%s</th>
                  </tr>
                  %s
                </table>""".formatted(t(locale, "email.order.colProduct"), t(locale, "email.order.colQuantity"), rows);

        String client = infoBox(t(locale, "email.order.clientTitle"),
                "<strong>" + t(locale, "email.order.company") + ":</strong> " + htmlEscape(order.companyName())
                        + optionalLine(t(locale, "email.order.address"), order.companyAddress())
                        + optionalLine(t(locale, "email.order.phone"), order.companyPhone())
                        + optionalLine(t(locale, "email.order.email"), order.replyTo()));

        String notes = order.notes() == null || order.notes().isBlank() ? ""
                : infoBox(t(locale, "email.order.notesTitle"), htmlEscape(order.notes()).replace("\n", "<br>"));

        String supplierName = order.supplierContactName() == null || order.supplierContactName().isBlank()
                ? null : order.supplierContactName();
        String body = (supplierName != null ? greeting(locale, supplierName) : "")
                + paragraph(t(locale, "email.order.intro", "<strong>" + htmlEscape(order.companyName()) + "</strong>"))
                + client + table + notes
                + small(t(locale, "email.order.reply", htmlEscape(order.companyName())));

        String subject = t(locale, "email.order.subject", order.orderName(), order.companyName());
        try {
            send(order.to(), order.replyTo(), subject,
                    layout(locale, t(locale, "email.order.title"), htmlEscape(order.orderName()), body,
                            t(locale, "email.order.footer")));
        } catch (MessagingException e) {
            log.error("Error en enviar la comanda '{}' a {}: {}", order.orderName(), maskEmail(order.to()), e.getMessage());
            throw new IllegalStateException("No s'ha pogut enviar el correu de la comanda", e);
        }
    }

    // ───────────────────────── Avisos de la prova gratuïta (síncrons) ─────────────────────────

    @Override
    public void sendTrialEndingSoon(TrialEmail trial, Locale locale) {
        String end = longDate(trial.trialEnd(), locale);
        String activity = trial.invoiceLines() > 0
                ? infoBox(t(locale, "email.trial.activityTitle"),
                        t(locale, "email.trial.activity", trial.invoiceLines(), trial.suppliers(), trial.orders()))
                : paragraph(t(locale, "email.trial.noActivity"));
        String body = greeting(locale, trial.userName())
                + paragraph(t(locale, "email.trialSoon.intro", "<strong>" + htmlEscape(trial.companyName()) + "</strong>", end))
                + activity
                + paragraph(t(locale, "email.trial.price"))
                + button(contactLink(locale, trial.companyName()), t(locale, "email.trial.button"))
                + small(t(locale, "email.trial.noCharge"));
        sendTrialEmail(trial.to(), t(locale, "email.trialSoon.subject", end), locale, t(locale, "email.trialSoon.title"), body);
    }

    @Override
    public void sendTrialEnded(TrialEmail trial, Locale locale) {
        String body = greeting(locale, trial.userName())
                + paragraph(t(locale, "email.trialEnded.intro", "<strong>" + htmlEscape(trial.companyName()) + "</strong>",
                        longDate(trial.trialEnd(), locale)))
                + notice(t(locale, "email.trialEnded.deletion", longDate(trial.deletionDate(), locale)))
                + paragraph(t(locale, "email.trial.price"))
                + button(contactLink(locale, trial.companyName()), t(locale, "email.trial.button"))
                + small(t(locale, "email.trial.noCharge"));
        sendTrialEmail(trial.to(), t(locale, "email.trialEnded.subject"), locale, t(locale, "email.trialEnded.title"), body);
    }

    private void sendTrialEmail(String to, String subject, Locale locale, String title, String body) {
        try {
            send(to, CONTACT_EMAIL, subject, layout(locale, title, null, body, t(locale, "email.trial.footer")));
        } catch (MessagingException e) {
            log.error("No s'ha pogut enviar el correu '{}' a {}: {}", subject, maskEmail(to), e.getMessage());
            throw new IllegalStateException("No s'ha pogut enviar l'avís de la prova", e);
        }
    }

    /** Enllaç per contractar: un correu a PedidAI amb l'assumpte ja escrit. */
    private String contactLink(Locale locale, String companyName) {
        return "mailto:" + CONTACT_EMAIL + "?subject="
                + URLEncoder.encode(t(locale, "email.trial.mailSubject", companyName), StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String longDate(LocalDate date, Locale locale) {
        return date == null ? "" : date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale));
    }

    // ───────────────────────── Plantilla ─────────────────────────

    private void send(String to, String replyTo, String subject, String html) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        try {
            helper.setFrom(fromEmail, fromName);
        } catch (java.io.UnsupportedEncodingException e) {
            helper.setFrom(fromEmail);
        }
        helper.setTo(to);
        if (replyTo != null && !replyTo.isBlank()) {
            helper.setReplyTo(replyTo);
        }
        helper.setSubject(subject);
        helper.setText(html, true);
        mailSender.send(message);
        log.info("Correu '{}' enviat a {}", subject, maskEmail(to));
    }

    /** Als logs no hi van adreces completes: «p***@domini.cat». */
    static String maskEmail(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        return at <= 0 ? "***" : email.charAt(0) + "***" + email.substring(at);
    }

    private String layout(Locale locale, String headerTitle, String headerSubtitle, String body, String footerExtra) {
        String subtitle = headerSubtitle == null ? ""
                : "<p style=\"color:#ffffff;margin:10px 0 0;font-size:17px;opacity:0.9;\">" + headerSubtitle + "</p>";
        return """
                <!DOCTYPE html>
                <html lang="%s">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>%s</title></head>
                <body style="margin:0;padding:0;font-family:Arial,sans-serif;background-color:#f4f4f4;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f4f4;padding:20px;">
                    <tr><td align="center">
                      <table width="600" cellpadding="0" cellspacing="0" style="max-width:600px;background-color:#ffffff;border-radius:8px;overflow:hidden;">
                        <tr><td style="background:linear-gradient(135deg,#06b6d4 0%%,#0f172a 100%%);background-color:#0f172a;padding:28px 20px;text-align:center;">
                          <img src="%s/logo-email.png" alt="PedidAI" width="160" height="64" style="display:block;margin:0 auto 10px;border:0;">
                          <h1 style="color:#ffffff;margin:0;font-size:24px;font-weight:800;">%s</h1>%s
                        </td></tr>
                        <tr><td style="padding:34px 30px;">%s</td></tr>
                        <tr><td style="background-color:#f8f8f8;padding:18px 30px;text-align:center;border-top:1px solid #eeeeee;">
                          <p style="color:#999999;font-size:12px;margin:0;">© %d PedidAI · pedidai.es</p>
                          <p style="color:#999999;font-size:11px;margin:10px 0 0;">%s</p>
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>""".formatted(locale.getLanguage(), headerTitle, frontendUrl, headerTitle, subtitle,
                body, Year.now().getValue(), footerExtra);
    }

    private String greeting(Locale locale, String name) {
        return "<h2 style=\"color:#333333;margin-top:0;\">" + t(locale, "email.greeting", htmlEscape(name)) + "</h2>";
    }

    private String paragraph(String html) {
        return "<p style=\"color:#555555;font-size:16px;line-height:1.6;\">" + html + "</p>";
    }

    private String small(String html) {
        return "<p style=\"color:#999999;font-size:13px;line-height:1.6;margin-top:26px;padding-top:18px;border-top:1px solid #eeeeee;\">"
                + html + "</p>";
    }

    private String notice(String html) {
        return "<p style=\"margin:20px 0;padding:12px;background-color:#fff3cd;border-radius:6px;color:#856404;font-size:13px;text-align:center;\">"
                + html + "</p>";
    }

    private String infoBox(String title, String html) {
        return "<div style=\"margin:20px 0;padding:16px 18px;background-color:#ecfeff;border-left:4px solid #06b6d4;border-radius:6px;\">"
                + "<p style=\"margin:0 0 8px;color:#333333;font-size:15px;font-weight:bold;\">" + title + "</p>"
                + "<p style=\"margin:0;color:#444444;font-size:14px;line-height:1.6;\">" + html + "</p></div>";
    }

    private String button(String link, String label) {
        return "<p style=\"text-align:center;margin:30px 0;\"><a href=\"" + htmlEscape(link)
                + "\" style=\"display:inline-block;padding:15px 38px;background-color:#0891b2;color:#ffffff;text-decoration:none;border-radius:6px;font-weight:bold;font-size:16px;\">"
                + label + "</a></p>";
    }

    private String optionalLine(String label, String value) {
        return value == null || value.isBlank() ? "" : "<br><strong>" + label + ":</strong> " + htmlEscape(value);
    }

    private String t(Locale locale, String key, Object... args) {
        return messages.get(locale, key, args);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
