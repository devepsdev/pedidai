package com.pedidai.api.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.pedidai.api.dto.BillingUrlDTO;
import com.pedidai.api.dto.LoginRequestDTO;
import com.pedidai.api.entities.Company;
import com.pedidai.api.entities.StripeEvent;
import com.pedidai.api.entities.User;
import com.pedidai.api.exceptions.BadRequestException;
import com.pedidai.api.exceptions.ForbiddenException;
import com.pedidai.api.repositories.CompanyRepository;
import com.pedidai.api.repositories.StripeEventRepository;
import com.pedidai.api.security.CurrentUser;
import com.pedidai.api.services.BillingService;
import com.pedidai.api.services.StripeGateway;
import com.pedidai.api.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;

/**
 * Subscripció amb Stripe. L'estat de l'empresa el decideix sempre el webhook (no la tornada del navegador):
 * amb la subscripció «active», «trialing» o «past_due» l'empresa és de pagament; amb «canceled», «unpaid» o
 * «incomplete_expired» passa a INACTIVE. Una empresa SUSPENDED pel superadministrador no es toca mai.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillingServiceImpl implements BillingService {

    /** Estats de Stripe amb què l'empresa pot fer servir el servei («past_due»: Stripe encara reintenta el cobrament). */
    static final Set<String> PAID = Set.of("active", "trialing", "past_due");
    /** Estats de Stripe en què la subscripció s'ha acabat. */
    static final Set<String> ENDED = Set.of("canceled", "unpaid", "incomplete_expired");
    /** Stripe exigeix que la prova acabi com a mínim d'aquí a 48 hores; amb menys temps es cobra ja. */
    static final Duration MIN_TRIAL_LEFT = Duration.ofHours(49);

    private final StripeGateway stripe;
    private final CurrentUser currentUser;
    private final UserService userService;
    private final CompanyRepository companyRepository;
    private final StripeEventRepository stripeEventRepository;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public boolean isEnabled() {
        return stripe.isConfigured();
    }

    @Override
    @Transactional(readOnly = true)
    public BillingUrlDTO startCheckout() {
        requireEnabled();
        User admin = currentUser.requireAdmin();
        return checkout(CurrentUser.companyOf(admin), admin.getEmail(), "/dashboard?pago=ok", "/dashboard?pago=cancelado");
    }

    @Override
    @Transactional(readOnly = true)
    public BillingUrlDTO startCheckoutAfterTrial(LoginRequestDTO credentials, String clientIp) {
        requireEnabled();
        User user = userService.verifyCredentials(credentials, clientIp);
        if (user.getRole() != User.UserRole.ADMIN) {
            throw new ForbiddenException("error.user.adminRequired");
        }
        Company company = CurrentUser.companyOf(user);
        if (company.getStatus() == Company.CompanyStatus.SUSPENDED) {
            throw new ForbiddenException("error.auth.companySuspended");
        }
        return checkout(company, user.getEmail(), "/login?pago=ok", "/login");
    }

    @Override
    @Transactional(readOnly = true)
    public BillingUrlDTO openPortal() {
        requireEnabled();
        Company company = CurrentUser.companyOf(currentUser.requireAdmin());
        if (company.getStripeCustomerId() == null) {
            throw new BadRequestException("error.billing.noSubscription");
        }
        return new BillingUrlDTO(stripe.createPortalSession(company.getStripeCustomerId(), frontendUrl + "/company"));
    }

    @Override
    @Transactional
    public void handleWebhook(String payload, String signatureHeader) {
        requireEnabled();
        JsonNode event = stripe.verifyWebhook(payload, signatureHeader);
        String eventId = event.path("id").asText();
        String type = event.path("type").asText();
        if (stripeEventRepository.existsById(eventId)) {
            log.info("Stripe: avís {} ({}) ja processat", eventId, type);
            return;
        }

        JsonNode object = event.path("data").path("object");
        switch (type) {
            case "checkout.session.completed" -> onCheckoutCompleted(object);
            case "customer.subscription.created", "customer.subscription.updated", "customer.subscription.deleted" ->
                    onSubscriptionChanged(object);
            default -> log.debug("Stripe: avís {} ignorat", type);
        }
        stripeEventRepository.save(new StripeEvent(eventId, type, LocalDateTime.now()));
    }

    // ───────────────────────── Pàgina de pagament ─────────────────────────

    private BillingUrlDTO checkout(Company company, String email, String successPath, String cancelPath) {
        if (company.getSubscriptionStatus() != null && PAID.contains(company.getSubscriptionStatus())) {
            throw new BadRequestException("error.billing.alreadySubscribed");
        }
        String url = stripe.createCheckoutSession(new StripeGateway.CheckoutRequest(
                company.getUuid(), email, company.getStripeCustomerId(), remainingTrialEnd(company),
                frontendUrl + successPath, frontendUrl + cancelPath));
        return new BillingUrlDTO(url);
    }

    /** Qui contracta durant la prova no perd els dies que li queden: el primer cobrament es fa quan acaba. */
    private static Long remainingTrialEnd(Company company) {
        LocalDateTime trialEndsAt = company.getTrialEndsAt();
        if (trialEndsAt == null || trialEndsAt.isBefore(LocalDateTime.now().plus(MIN_TRIAL_LEFT))) {
            return null;
        }
        return trialEndsAt.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    // ───────────────────────── Avisos de Stripe ─────────────────────────

    private void onCheckoutCompleted(JsonNode session) {
        if (!"subscription".equals(session.path("mode").asText())) {
            return;
        }
        String companyUuid = text(session, "client_reference_id");
        String subscriptionId = text(session, "subscription");
        Optional<Company> found = companyUuid == null ? Optional.empty() : companyRepository.findByUuid(companyUuid);
        if (found.isEmpty() || subscriptionId == null) {
            log.warn("Stripe: pagament completat sense empresa coneguda (client_reference_id={})", companyUuid);
            return;
        }
        Company company = found.get();
        company.setStripeCustomerId(text(session, "customer"));
        company.setStripeSubscriptionId(subscriptionId);
        // L'estat es llegeix de Stripe en aquest moment: així no importa l'ordre en què arriben els avisos
        applyStatus(company, stripe.subscriptionStatus(subscriptionId));
    }

    private void onSubscriptionChanged(JsonNode subscription) {
        String subscriptionId = text(subscription, "id");
        Optional<Company> found = companyRepository.findByStripeSubscriptionId(subscriptionId);
        if (found.isEmpty()) {
            // Pot arribar abans que «checkout.session.completed»: l'empresa va a les metadades de la subscripció
            String companyUuid = text(subscription.path("metadata"), "company_uuid");
            found = companyUuid == null ? Optional.empty() : companyRepository.findByUuid(companyUuid);
        }
        if (found.isEmpty()) {
            log.warn("Stripe: subscripció {} sense empresa coneguda", subscriptionId);
            return;
        }
        Company company = found.get();
        String status = stripe.subscriptionStatus(subscriptionId);
        boolean otherSubscription = company.getStripeSubscriptionId() != null
                && !company.getStripeSubscriptionId().equals(subscriptionId);
        if (otherSubscription && !PAID.contains(status)) {
            // Avís d'una subscripció antiga (l'empresa ja n'ha contractat una altra): no la desactiva
            log.info("Stripe: subscripció antiga {} ({}) ignorada", subscriptionId, status);
            return;
        }
        company.setStripeSubscriptionId(subscriptionId);
        if (company.getStripeCustomerId() == null) {
            company.setStripeCustomerId(text(subscription, "customer"));
        }
        applyStatus(company, status);
    }

    private void applyStatus(Company company, String status) {
        company.setSubscriptionStatus(status);
        if (company.getStatus() != Company.CompanyStatus.SUSPENDED) {
            if (PAID.contains(status)) {
                // Client de pagament: sense límit de prova (com quan l'activa el superadministrador)
                company.setStatus(Company.CompanyStatus.ACTIVE);
                company.setTrialEndsAt(null);
            } else if (ENDED.contains(status)) {
                company.setStatus(Company.CompanyStatus.INACTIVE);
            }
        }
        companyRepository.save(company);
        log.info("Stripe: empresa {} → subscripció «{}», estat {}", company.getUuid(), status, company.getStatus());
    }

    private void requireEnabled() {
        if (!stripe.isConfigured()) {
            throw new BadRequestException("error.billing.notConfigured");
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() && !value.asText().isBlank() ? value.asText() : null;
    }
}
