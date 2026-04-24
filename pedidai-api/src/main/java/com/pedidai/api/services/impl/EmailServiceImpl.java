package com.pedidai.api.services.impl;

import com.pedidai.api.services.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url:https://pedidai.es}")
    private String frontendUrl;

    @Override
    public void sendPasswordResetEmail(String to, String token, String userName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Recuperació de Contrasenya - PedidAI");
            helper.setText(buildPasswordResetEmailBody(token, userName), true); // true = HTML

            mailSender.send(message);
            log.info("Email de recuperació enviat a: {}", to);
        } catch (MessagingException e) {
            log.error("Error en enviar l'email de recuperació: {}", e.getMessage());
            throw new RuntimeException("Error en enviar l'email de recuperació");
        }
    }

    @Override
    public void sendEmailVerification(String to, String token, String userName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Verifica el teu compte de PedidAI");
            helper.setText(buildEmailVerificationBody(token, userName), true); // true = HTML

            mailSender.send(message);
            log.info("Email de verificació enviat a: {}", to);
        } catch (MessagingException e) {
            log.error("Error en enviar l'email de verificació: {}", e.getMessage());
            throw new RuntimeException("Error en enviar l'email de verificació");
        }
    }

    @Override
    public void sendCompanyAdminVerification(String to, String token, String userName, String companyName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("¡Benvingut a PedidAI! - Verifica la teva empresa");
            helper.setText(buildCompanyAdminVerificationBody(token, userName, companyName), true);

            mailSender.send(message);
            log.info("Email de verificació d'empresa enviat a: {} per a l'empresa: {}", to, companyName);
        } catch (MessagingException e) {
            log.error("Error en enviar l'email de verificació de l'empresa: {}", e.getMessage());
            throw new RuntimeException("Error en enviar l'email de verificació d'empresa");
        }
    }

    @Override
    public void sendOrderNotification(String to, String supplierName, String companyName,
                                      String companyAddress, String companyPhone,
                                      String orderName, String orderDetails,
                                      String notes) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Nova Comanda - " + orderName + " - PedidAI");
            helper.setText(buildOrderNotificationBody(supplierName, companyName, companyAddress, companyPhone,
                    orderName, orderDetails, notes), true);

            mailSender.send(message);
            log.info("Email de comanda enviat a: {} per a la comanda: {}", to, orderName);
        } catch (MessagingException e) {
            log.error("Error en enviar l'email de comanda: {}", e.getMessage());
            throw new RuntimeException("Error en enviar l'email de comanda");
        }
    }

    private String buildPasswordResetEmailBody(String token, String userName) {
        String resetLink = frontendUrl + "/reset-password?token=" + token;

        return """
                 <!DOCTYPE html>
                        <html lang="ca">
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <title>Recuperació de Contrasenya</title>
                        </head>
                        <body style="margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;">
                            <table width="100%%" cellpadding="0" cellspacing="0" style="background-color: #f4f4f4; padding: 20px;">
                                <tr>
                                    <td align="center">
                                        <table width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                                            <!-- Header -->
                                            <tr>
                                                <td style="background: linear-gradient(135deg, #06b6d4 0%%, #0f172a 100%%); padding: 30px 20px; text-align: center;">
                                                    <img src="%s/logo-email.png" alt="PedidAI" width="160" height="64" style="display: block; margin: 0 auto 12px; border: 0;">
                                                    <h1 style="color: #ffffff; margin: 0; font-size: 26px; font-weight: 800; letter-spacing: -0.5px;">Pedid<span style="color: #67e8f9;">AI</span></h1>
                                                </td>
                                            </tr>
                
                                            <!-- Body -->
                                            <tr>
                                                <td style="padding: 40px 30px;">
                                                    <h2 style="color: #333333; margin-top: 0;">Hola %s,</h2>
                                                    <p style="color: #666666; font-size: 16px; line-height: 1.6;">
                                                        Has sol·licitat restablir la teva contrasenya. Fes clic al botó de sota per crear una contrasenya nova:
                                                    </p>
                
                                                    <!-- Button -->
                                                    <table width="100%%" cellpadding="0" cellspacing="0" style="margin: 30px 0;">
                                                        <tr>
                                                            <td align="center">
                                                                <a href="%s" style="display: inline-block; padding: 16px 40px; background: linear-gradient(135deg, #06b6d4 0%%, #0f172a 100%%); color: #ffffff; text-decoration: none; border-radius: 5px; font-weight: bold; font-size: 16px; box-shadow: 0 4px 15px rgba(6, 182, 212, 0.4);">
                                                                    Restablir Contrasenya
                                                                </a>
                                                            </td>
                                                        </tr>
                                                    </table>
                
                                                    <!-- Timer Info -->
                                                    <table width="100%%" cellpadding="0" cellspacing="0" style="margin: 20px 0; background-color: #fff3cd; border-radius: 6px;">
                                                        <tr>
                                                            <td style="padding: 15px; text-align: center;">
                                                                <p style="margin: 0; color: #856404; font-size: 13px;">
                                                                    ⏱️ Aquest enllaç expirarà en <strong>1 hora</strong>
                                                                </p>
                                                            </td>
                                                        </tr>
                                                    </table>
                
                                                    <p style="color: #999999; font-size: 13px; line-height: 1.6; margin-top: 30px; padding-top: 20px; border-top: 1px solid #eeeeee;">
                                                        Si no has sol·licitat restablir la teva contrasenya, pots ignorar aquest correu de forma segura. La teva contrasenya actual continuarà sent vàlida.
                                                    </p>
                                                </td>
                                            </tr>
                
                                            <!-- Footer -->
                                            <tr>
                                                <td style="background-color: #f8f8f8; padding: 20px 30px; text-align: center; border-top: 1px solid #eeeeee;">
                                                    <p style="color: #999999; font-size: 12px; margin: 0;">
                                                        © 2026 PedidAI. Tots els drets reservats.
                                                    </p>
                                                    <p style="color: #999999; font-size: 11px; margin: 10px 0 0 0;">
                                                        Si el botó no funciona, copia i enganxa aquest enllaç al teu navegador:<br>
                                                        <span style="color: #06b6d4;">%s</span>
                                                    </p>
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>
                            </table>
                        </body>
                        </html>
                \s""".formatted(frontendUrl, userName, resetLink, resetLink);
    }

    private String buildEmailVerificationBody(String token, String userName) {
        String verificationLink = frontendUrl + "/verify-email?token=" + token;

        return """
                 <!DOCTYPE html>
                        <html lang="ca">
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <title>Verifica el teu Email</title>
                        </head>
                        <body style="margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;">
                            <table width="100%%" cellpadding="0" cellspacing="0" style="background-color: #f4f4f4; padding: 20px;">
                                <tr>
                                    <td align="center">
                                        <table width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                                            <!-- Header -->
                                            <tr>
                                                <td style="background: linear-gradient(135deg, #06b6d4 0%%, #0f172a 100%%); padding: 30px 20px; text-align: center;">
                                                    <img src="%s/logo-email.png" alt="PedidAI" width="160" height="64" style="display: block; margin: 0 auto 12px; border: 0;">
                                                    <h1 style="color: #ffffff; margin: 0; font-size: 26px; font-weight: 800; letter-spacing: -0.5px;">Pedid<span style="color: #67e8f9;">AI</span></h1>
                                                </td>
                                            </tr>
                
                                            <!-- Body -->
                                            <tr>
                                                <td style="padding: 40px 30px;">
                                                    <h2 style="color: #333333; margin-top: 0;">Hola %s,</h2>
                                                    <p style="color: #666666; font-size: 16px; line-height: 1.6;">
                                                        Gràcies per registrar-te a PedidAI. Per completar el teu registre, si us plau verifica la teva adreça de correu electrònic.
                                                    </p>
                
                                                    <!-- Button -->
                                                    <table width="100%%" cellpadding="0" cellspacing="0" style="margin: 30px 0;">
                                                        <tr>
                                                            <td align="center">
                                                                <a href="%s" style="display: inline-block; padding: 16px 40px; background: linear-gradient(135deg, #06b6d4 0%%, #0f172a 100%%); color: #ffffff; text-decoration: none; border-radius: 5px; font-weight: bold; font-size: 16px; box-shadow: 0 4px 15px rgba(6, 182, 212, 0.4);">
                                                                    Verificar Email
                                                                </a>
                                                            </td>
                                                        </tr>
                                                    </table>
                
                                                    <!-- Timer Info -->
                                                    <table width="100%%" cellpadding="0" cellspacing="0" style="margin: 20px 0; background-color: #fff3cd; border-radius: 6px;">
                                                        <tr>
                                                            <td style="padding: 15px; text-align: center;">
                                                                <p style="margin: 0; color: #856404; font-size: 13px;">
                                                                    ⏱️ Aquest enllaç expirarà en <strong>24 hores</strong>
                                                                </p>
                                                            </td>
                                                        </tr>
                                                    </table>
                
                                                    <p style="color: #999999; font-size: 13px; line-height: 1.6; margin-top: 30px; padding-top: 20px; border-top: 1px solid #eeeeee;">
                                                        Si no t'has registrat a PedidAI, pots ignorar aquest correu de forma segura.
                                                    </p>
                                                </td>
                                            </tr>
                
                                            <!-- Footer -->
                                            <tr>
                                                <td style="background-color: #f8f8f8; padding: 20px 30px; text-align: center; border-top: 1px solid #eeeeee;">
                                                    <p style="color: #999999; font-size: 12px; margin: 0;">
                                                        © 2026 PedidAI. Tots els drets reservats.
                                                    </p>
                                                    <p style="color: #999999; font-size: 11px; margin: 10px 0 0 0;">
                                                        Si el botó no funciona, copia i enganxa aquest enllaç al teu navegador:<br>
                                                        <span style="color: #06b6d4;">%s</span>
                                                    </p>
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>
                            </table>
                        </body>
                        </html>
                \s""".formatted(frontendUrl, userName, verificationLink, verificationLink);
    }

    private String buildCompanyAdminVerificationBody(String token, String userName, String companyName) {
        String verificationLink = frontendUrl + "/verify-email?token=" + token;

        return """
                 <!DOCTYPE html>
                        <html lang="ca">
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <title>Verifica la teva Empresa a PedidAI</title>
                        </head>
                        <body style="margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;">
                            <table width="100%%" cellpadding="0" cellspacing="0" style="background-color: #f4f4f4; padding: 20px;">
                                <tr>
                                    <td align="center">
                                        <table width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                                            <!-- Header -->
                                            <tr>
                                                <td style="background: linear-gradient(135deg, #06b6d4 0%%, #0f172a 100%%); padding: 30px 20px; text-align: center;">
                                                    <img src="%s/logo-email.png" alt="PedidAI" width="160" height="64" style="display: block; margin: 0 auto 12px; border: 0;">
                                                    <h1 style="color: #ffffff; margin: 0; font-size: 26px; font-weight: 800; letter-spacing: -0.5px;">🎉 Benvingut a Pedid<span style="color: #67e8f9;">AI</span>!</h1>
                                                </td>
                                            </tr>

                                            <!-- Body -->
                                            <tr>
                                                <td style="padding: 40px 30px;">
                                                    <h2 style="color: #333333; margin-top: 0;">Hola %s,</h2>
                
                                                    <p style="color: #666666; font-size: 16px; line-height: 1.6;">
                                                        Gràcies per registrar la teva empresa a PedidAI!
                                                    </p>
                
                                                    <p style="color: #666666; font-size: 16px; line-height: 1.6;">
                                                        Estem emocionats que formis part de la nostra plataforma. Per completar el registre de la teva empresa i poder començar a gestionar el teu equip, necessitem que verificis la teva adreça de correu electrònic.
                                                    </p>
                
                                                    <!-- Info Box -->
                                                    <table width="100%%" cellpadding="0" cellspacing="0" style="margin: 25px 0; background-color: #ecfeff; border-radius: 8px; border-left: 4px solid #06b6d4;">
                                                        <tr>
                                                            <td style="padding: 20px;">
                                                                <p style="margin: 0; color: #555555; font-size: 14px; line-height: 1.6;">
                                                                    <strong>✓ Com a administrador, podràs:</strong><br>
                                                                    • Convidar membres al teu equip<br>
                                                                    • Gestionar usuaris i permisos<br>
                                                                    • Configurar la teva empresa<br>
                                                                    • Accedir a totes les funcionalitats
                                                                </p>
                                                            </td>
                                                        </tr>
                                                    </table>
                
                                                    <p style="color: #666666; font-size: 16px; line-height: 1.6; margin-top: 25px;">
                                                        Fes clic al botó per verificar el teu compte i activar la teva empresa:
                                                    </p>
                
                                                    <!-- Button -->
                                                    <table width="100%%" cellpadding="0" cellspacing="0" style="margin: 30px 0;">
                                                        <tr>
                                                            <td align="center">
                                                                <a href="%s" style="display: inline-block; padding: 16px 45px; background: linear-gradient(135deg, #06b6d4 0%%, #0f172a 100%%); color: #ffffff; text-decoration: none; border-radius: 5px; font-weight: bold; font-size: 16px; box-shadow: 0 4px 15px rgba(6, 182, 212, 0.4);">
                                                                    Verificar i Activar Empresa
                                                                </a>
                                                            </td>
                                                        </tr>
                                                    </table>
                
                                                    <!-- Timer Info -->
                                                    <table width="100%%" cellpadding="0" cellspacing="0" style="margin: 20px 0; background-color: #fff3cd; border-radius: 6px;">
                                                        <tr>
                                                            <td style="padding: 15px; text-align: center;">
                                                                <p style="margin: 0; color: #856404; font-size: 13px;">
                                                                    ⏱️ Aquest enllaç expirarà en <strong>24 hores</strong>
                                                                </p>
                                                            </td>
                                                        </tr>
                                                    </table>
                
                                                    <p style="color: #666666; font-size: 14px; line-height: 1.6; margin-top: 30px;">
                                                        Un cop verificat el teu compte, podràs accedir al teu panell d'administració i convidar els membres del teu equip.
                                                    </p>
                
                                                    <p style="color: #999999; font-size: 13px; line-height: 1.6; margin-top: 30px; padding-top: 20px; border-top: 1px solid #eeeeee;">
                                                        Si no has registrat aquesta empresa a PedidAI, pots ignorar aquest correu de forma segura.
                                                    </p>
                                                </td>
                                            </tr>
                
                                            <!-- Footer -->
                                            <tr>
                                                <td style="background-color: #f8f8f8; padding: 20px 30px; text-align: center; border-top: 1px solid #eeeeee;">
                                                    <p style="color: #999999; font-size: 12px; margin: 0;">
                                                        © 2026 PedidAI. Tots els drets reservats.
                                                    </p>
                                                    <p style="color: #999999; font-size: 11px; margin: 10px 0 0 0;">
                                                        Si el botó no funciona, copia i enganxa aquest enllaç al teu navegador:<br>
                                                        <span style="color: #06b6d4;">%s</span>
                                                    </p>
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>
                            </table>
                        </body>
                        </html>
                \s""".formatted(frontendUrl, userName, companyName, verificationLink, verificationLink);
    }

    private String buildOrderNotificationBody(String supplierName, String companyName,
                                              String companyAddress, String companyPhone,
                                              String orderName, String orderDetails,
                                              String notes) {

        // Construir la secció de notes si existeix
        String notesSection = "";
        if (notes != null && !notes.isEmpty()) {
            notesSection = """
                    <tr>
                        <td style="padding: 15px 30px; border-bottom: 1px solid #eeeeee;">
                            <p style="margin: 0 0 5px 0; color: #333333; font-size: 15px; font-weight: bold;">
                                📝 Notes addicionals:
                            </p>
                            <p style="margin: 0; color: #666666; font-size: 14px; line-height: 1.6;">
                                %s
                            </p>
                        </td>
                    </tr>
                    """.formatted(notes);
        }

        // Construir la secció de dades del client
        String clientDataSection = """
                <tr>
                    <td style="padding: 20px 30px; background-color: #f8f9ff; border-bottom: 1px solid #eeeeee;">
                        <p style="margin: 0 0 8px 0; color: #333333; font-size: 15px; font-weight: bold;">
                            🧾 Dades del client:
                        </p>
                
                        <p style="margin: 0; color: #444444; font-size: 14px; line-height: 1.5;">
                            <strong>Empresa:</strong> %s
                        </p>
                
                        <p style="margin: 0; color: #444444; font-size: 14px; line-height: 1.5;">
                            <strong>Adreça:</strong> %s
                        </p>
                
                        <p style="margin: 0; color: #444444; font-size: 14px; line-height: 1.5;">
                            <strong>Telèfon:</strong> %s
                        </p>
                    </td>
                </tr>
                """.formatted(companyName, companyAddress, companyPhone);

        return """
                <!DOCTYPE html>
                <html lang="ca">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Nova Comanda - %s</title>
                </head>
                <body style="margin: 0; padding: 0; font-family: Arial, sans-serif; background-color: #f4f4f4;">
                    <table width="100%%" cellpadding="0" cellspacing="0" style="background-color: #f4f4f4; padding: 20px;">
                        <tr>
                            <td align="center">
                                <table width="650" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                                    <!-- Header -->
                                    <tr>
                                        <td style="background: linear-gradient(135deg, #06b6d4 0%%, #0f172a 100%%); padding: 30px 20px; text-align: center;">
                                            <img src="%s/logo-email.png" alt="PedidAI" width="160" height="64" style="display: block; margin: 0 auto 12px; border: 0;">
                                            <h1 style="color: #ffffff; margin: 0; font-size: 26px; font-weight: 800; letter-spacing: -0.5px;">🛒 Nova Comanda</h1>
                                            <p style="color: #ffffff; margin: 10px 0 0 0; font-size: 18px; opacity: 0.9;">%s</p>
                                        </td>
                                    </tr>
                
                                    <!-- Body -->
                                    <tr>
                                        <td style="padding: 40px 30px 20px 30px;">
                                            <h2 style="color: #333333; margin-top: 0;">Hola %s,</h2>
                
                                            <p style="color: #666666; font-size: 16px; line-height: 1.6;">
                                                Has rebut una nova comanda de <strong style="color: #06b6d4;">%s</strong> a través de la plataforma PedidAI.
                                                 A continuació trobaràs els detalls:
                                            </p>
                                        </td>
                                    </tr>
                
                                    <!-- Client Data -->
                                    %s
                
                                    <!-- Order Details -->
                                    <tr>
                                        <td style="padding: 0 30px;">
                                            %s
                                        </td>
                                    </tr>
                
                                    %s  <!-- notes -->
                
                                    <!-- Contact Info -->
                                    <tr>
                                        <td style="padding: 30px; background-color: #ecfeff;">
                                            <p style="margin: 0 0 10px 0; color: #333333; font-size: 15px; font-weight: bold;">
                                                ℹ️ Informació important:
                                            </p>
                                            <p style="margin: 0; color: #666666; font-size: 14px; line-height: 1.6;">
                                                Si tens qualsevol dubte sobre aquesta comanda, si us plau contacta amb nosaltres a través de la plataforma PedidAI
                                                 o respon a aquest correu electrònic.
                                            </p>
                                        </td>
                                    </tr>
                
                                    <!-- Footer -->
                                    <tr>
                                        <td style="background-color: #f8f8f8; padding: 20px 30px; text-align: center; border-top: 1px solid #eeeeee;">
                                            <p style="color: #999999; font-size: 12px; margin: 0;">
                                                © 2026 PedidAI - Plataforma de Gestió de Comandes
                                            </p>
                                            <p style="color: #999999; font-size: 11px; margin: 10px 0 0 0;">
                                                Aquest és un correu automàtic generat pel sistema PedidAI
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(
                orderName,
                frontendUrl,
                orderName,
                supplierName,
                companyName,
                clientDataSection,
                orderDetails,
                notesSection
        );
    }
}