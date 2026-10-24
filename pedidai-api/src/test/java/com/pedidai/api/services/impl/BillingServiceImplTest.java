package com.pedidai.api.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidai.api.dto.LoginRequestDTO;
import com.pedidai.api.entities.Company;
import com.pedidai.api.entities.StripeEvent;
import com.pedidai.api.entities.User;
import com.pedidai.api.exceptions.BadRequestException;
import com.pedidai.api.exceptions.ForbiddenException;
import com.pedidai.api.repositories.CompanyRepository;
import com.pedidai.api.repositories.StripeEventRepository;
import com.pedidai.api.security.CurrentUser;
import com.pedidai.api.services.StripeGateway;
import com.pedidai.api.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BillingServiceImpl: pagament amb Stripe")
class BillingServiceImplTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Mock private StripeGateway stripe;
    @Mock private CurrentUser currentUser;
    @Mock private UserService userService;
    @Mock private CompanyRepository companyRepository;
    @Mock private StripeEventRepository stripeEventRepository;

    private BillingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BillingServiceImpl(stripe, currentUser, userService, companyRepository, stripeEventRepository);
        ReflectionTestUtils.setField(service, "frontendUrl", "https://pedidai.es");
    }

    private static Company company(LocalDateTime trialEndsAt) {
        return Company.builder().id(1L).uuid("emp-1").name("Bar La Plaza").email("bar@plaza.test")
                .status(Company.CompanyStatus.ACTIVE).trialEndsAt(trialEndsAt).build();
    }

    private static User admin(Company company) {
        return User.builder().email("admin@plaza.test").role(User.UserRole.ADMIN).company(company).build();
    }

    private StripeGateway.CheckoutRequest checkoutRequest() {
        ArgumentCaptor<StripeGateway.CheckoutRequest> captor = ArgumentCaptor.forClass(StripeGateway.CheckoutRequest.class);
        verify(stripe).createCheckoutSession(captor.capture());
        return captor.getValue();
    }

    // ───────────────────────── Pàgina de pagament ─────────────────────────

    @Test
    @DisplayName("sense Stripe configurat, contractar en línia no està disponible")
    void notConfigured() {
        when(stripe.isConfigured()).thenReturn(false);

        assertThatThrownBy(() -> service.startCheckout())
                .isInstanceOf(BadRequestException.class).hasMessageContaining("error.billing.notConfigured");
        verify(stripe, never()).createCheckoutSession(any());
    }

    @Test
    @DisplayName("durant la prova, el primer cobrament es fa quan acaba (no perd dies)")
    void checkoutKeepsRemainingTrial() {
        LocalDateTime trialEnd = LocalDateTime.now().plusDays(10);
        Company company = company(trialEnd);
        when(stripe.isConfigured()).thenReturn(true);
        when(currentUser.requireAdmin()).thenReturn(admin(company));
        when(stripe.createCheckoutSession(any())).thenReturn("https://checkout.stripe.test/s1");

        assertThat(service.startCheckout().getUrl()).isEqualTo("https://checkout.stripe.test/s1");

        StripeGateway.CheckoutRequest request = checkoutRequest();
        assertThat(request.companyUuid()).isEqualTo("emp-1");
        assertThat(request.email()).isEqualTo("admin@plaza.test");
        assertThat(request.trialEndEpochSeconds())
                .isEqualTo(trialEnd.atZone(java.time.ZoneId.systemDefault()).toEpochSecond());
        assertThat(request.successUrl()).isEqualTo("https://pedidai.es/dashboard?pago=ok");
        assertThat(request.cancelUrl()).isEqualTo("https://pedidai.es/dashboard?pago=cancelado");
    }

    @Test
    @DisplayName("si a la prova li queden menys de 49 hores, es cobra ja (Stripe exigeix 48 h)")
    void checkoutChargesNowWhenTrialAlmostOver() {
        Company company = company(LocalDateTime.now().plusHours(20));
        when(stripe.isConfigured()).thenReturn(true);
        when(currentUser.requireAdmin()).thenReturn(admin(company));
        when(stripe.createCheckoutSession(any())).thenReturn("https://checkout.stripe.test/s2");

        service.startCheckout();

        assertThat(checkoutRequest().trialEndEpochSeconds()).isNull();
    }

    @Test
    @DisplayName("una empresa amb la subscripció activa no pot tornar a contractar")
    void alreadySubscribed() {
        Company company = company(null);
        company.setSubscriptionStatus("active");
        when(stripe.isConfigured()).thenReturn(true);
        when(currentUser.requireAdmin()).thenReturn(admin(company));

        assertThatThrownBy(() -> service.startCheckout())
                .isInstanceOf(BadRequestException.class).hasMessageContaining("error.billing.alreadySubscribed");
    }

    @Test
    @DisplayName("amb la prova acabada, l'administrador contracta amb email i contrasenya i torna al login")
    void checkoutAfterTrial() {
        Company company = company(LocalDateTime.now().minusDays(3));
        company.setStatus(Company.CompanyStatus.INACTIVE);
        company.setStripeCustomerId("cus_antic");
        LoginRequestDTO credentials = new LoginRequestDTO("admin@plaza.test", "secret123");
        when(stripe.isConfigured()).thenReturn(true);
        when(userService.verifyCredentials(credentials, "1.2.3.4")).thenReturn(admin(company));
        when(stripe.createCheckoutSession(any())).thenReturn("https://checkout.stripe.test/s3");

        service.startCheckoutAfterTrial(credentials, "1.2.3.4");

        StripeGateway.CheckoutRequest request = checkoutRequest();
        assertThat(request.trialEndEpochSeconds()).isNull();
        assertThat(request.existingCustomerId()).isEqualTo("cus_antic");
        assertThat(request.successUrl()).isEqualTo("https://pedidai.es/login?pago=ok");
    }

    @Test
    @DisplayName("amb la prova acabada, un usuari que no és administrador no pot contractar")
    void checkoutAfterTrialRequiresAdmin() {
        Company company = company(LocalDateTime.now().minusDays(3));
        User user = User.builder().email("u@plaza.test").role(User.UserRole.USER).company(company).build();
        LoginRequestDTO credentials = new LoginRequestDTO("u@plaza.test", "secret123");
        when(stripe.isConfigured()).thenReturn(true);
        when(userService.verifyCredentials(credentials, "1.2.3.4")).thenReturn(user);

        assertThatThrownBy(() -> service.startCheckoutAfterTrial(credentials, "1.2.3.4"))
                .isInstanceOf(ForbiddenException.class).hasMessageContaining("error.user.adminRequired");
    }

    @Test
    @DisplayName("una empresa suspesa no pot contractar pel seu compte")
    void checkoutAfterTrialSuspended() {
        Company company = company(null);
        company.setStatus(Company.CompanyStatus.SUSPENDED);
        LoginRequestDTO credentials = new LoginRequestDTO("admin@plaza.test", "secret123");
        when(stripe.isConfigured()).thenReturn(true);
        when(userService.verifyCredentials(credentials, "1.2.3.4")).thenReturn(admin(company));

        assertThatThrownBy(() -> service.startCheckoutAfterTrial(credentials, "1.2.3.4"))
                .isInstanceOf(ForbiddenException.class).hasMessageContaining("error.auth.companySuspended");
    }

    @Test
    @DisplayName("el portal del client només s'obre si ja hi ha client de Stripe")
    void portal() {
        Company company = company(null);
        when(stripe.isConfigured()).thenReturn(true);
        when(currentUser.requireAdmin()).thenReturn(admin(company));

        assertThatThrownBy(() -> service.openPortal())
                .isInstanceOf(BadRequestException.class).hasMessageContaining("error.billing.noSubscription");

        company.setStripeCustomerId("cus_1");
        when(stripe.createPortalSession("cus_1", "https://pedidai.es/company")).thenReturn("https://billing.stripe.test/p1");
        assertThat(service.openPortal().getUrl()).isEqualTo("https://billing.stripe.test/p1");
    }

    // ───────────────────────── Avisos de Stripe ─────────────────────────

    private void receive(String json) throws Exception {
        JsonNode event = JSON.readTree(json);
        when(stripe.isConfigured()).thenReturn(true);
        when(stripe.verifyWebhook("payload", "sig")).thenReturn(event);
        service.handleWebhook("payload", "sig");
    }

    @Test
    @DisplayName("pagament completat: l'empresa passa a client de pagament i es guarden client i subscripció")
    void checkoutCompleted() throws Exception {
        Company company = company(LocalDateTime.now().plusDays(5));
        when(stripeEventRepository.existsById("evt_1")).thenReturn(false);
        when(companyRepository.findByUuid("emp-1")).thenReturn(Optional.of(company));
        when(stripe.subscriptionStatus("sub_1")).thenReturn("trialing");

        receive("""
                {"id":"evt_1","type":"checkout.session.completed","data":{"object":{
                  "mode":"subscription","client_reference_id":"emp-1","customer":"cus_1","subscription":"sub_1"}}}""");

        assertThat(company.getStatus()).isEqualTo(Company.CompanyStatus.ACTIVE);
        assertThat(company.getTrialEndsAt()).isNull();
        assertThat(company.getStripeCustomerId()).isEqualTo("cus_1");
        assertThat(company.getStripeSubscriptionId()).isEqualTo("sub_1");
        assertThat(company.getSubscriptionStatus()).isEqualTo("trialing");
        verify(companyRepository).save(company);
        ArgumentCaptor<StripeEvent> saved = ArgumentCaptor.forClass(StripeEvent.class);
        verify(stripeEventRepository).save(saved.capture());
        assertThat(saved.getValue().getId()).isEqualTo("evt_1");
    }

    @Test
    @DisplayName("un avís repetit no es torna a aplicar")
    void duplicateEvent() throws Exception {
        when(stripeEventRepository.existsById("evt_1")).thenReturn(true);

        receive("""
                {"id":"evt_1","type":"checkout.session.completed","data":{"object":{
                  "mode":"subscription","client_reference_id":"emp-1","customer":"cus_1","subscription":"sub_1"}}}""");

        verifyNoInteractions(companyRepository);
        verify(stripeEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("subscripció cancel·lada: l'empresa passa a INACTIVE")
    void subscriptionCanceled() throws Exception {
        Company company = company(null);
        company.setStripeSubscriptionId("sub_1");
        company.setSubscriptionStatus("active");
        when(companyRepository.findByStripeSubscriptionId("sub_1")).thenReturn(Optional.of(company));
        when(stripe.subscriptionStatus("sub_1")).thenReturn("canceled");

        receive("""
                {"id":"evt_2","type":"customer.subscription.deleted","data":{"object":{"id":"sub_1","customer":"cus_1"}}}""");

        assertThat(company.getStatus()).isEqualTo(Company.CompanyStatus.INACTIVE);
        assertThat(company.getSubscriptionStatus()).isEqualTo("canceled");
    }

    @Test
    @DisplayName("una empresa suspesa pel superadministrador no es reactiva amb un pagament")
    void suspendedStaysSuspended() throws Exception {
        Company company = company(null);
        company.setStatus(Company.CompanyStatus.SUSPENDED);
        company.setStripeSubscriptionId("sub_1");
        when(companyRepository.findByStripeSubscriptionId("sub_1")).thenReturn(Optional.of(company));
        when(stripe.subscriptionStatus("sub_1")).thenReturn("active");

        receive("""
                {"id":"evt_3","type":"customer.subscription.updated","data":{"object":{"id":"sub_1"}}}""");

        assertThat(company.getStatus()).isEqualTo(Company.CompanyStatus.SUSPENDED);
        assertThat(company.getSubscriptionStatus()).isEqualTo("active");
    }

    @Test
    @DisplayName("la subscripció que arriba abans que el pagament es troba per les metadades")
    void subscriptionBeforeCheckout() throws Exception {
        Company company = company(LocalDateTime.now().plusDays(5));
        when(companyRepository.findByStripeSubscriptionId("sub_9")).thenReturn(Optional.empty());
        when(companyRepository.findByUuid("emp-1")).thenReturn(Optional.of(company));
        when(stripe.subscriptionStatus("sub_9")).thenReturn("active");

        receive("""
                {"id":"evt_4","type":"customer.subscription.created","data":{"object":{
                  "id":"sub_9","customer":"cus_9","metadata":{"company_uuid":"emp-1"}}}}""");

        assertThat(company.getStripeSubscriptionId()).isEqualTo("sub_9");
        assertThat(company.getStripeCustomerId()).isEqualTo("cus_9");
        assertThat(company.getStatus()).isEqualTo(Company.CompanyStatus.ACTIVE);
        assertThat(company.getTrialEndsAt()).isNull();
    }

    @Test
    @DisplayName("la baixa d'una subscripció antiga no desactiva l'empresa que n'ha contractat una altra")
    void oldSubscriptionIgnored() throws Exception {
        Company company = company(null);
        company.setStripeSubscriptionId("sub_nova");
        company.setSubscriptionStatus("active");
        when(companyRepository.findByStripeSubscriptionId("sub_antiga")).thenReturn(Optional.empty());
        when(companyRepository.findByUuid("emp-1")).thenReturn(Optional.of(company));
        when(stripe.subscriptionStatus("sub_antiga")).thenReturn("canceled");

        receive("""
                {"id":"evt_5","type":"customer.subscription.deleted","data":{"object":{
                  "id":"sub_antiga","metadata":{"company_uuid":"emp-1"}}}}""");

        assertThat(company.getStatus()).isEqualTo(Company.CompanyStatus.ACTIVE);
        assertThat(company.getStripeSubscriptionId()).isEqualTo("sub_nova");
        verify(companyRepository, never()).save(any());
    }
}
