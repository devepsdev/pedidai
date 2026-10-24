package com.pedidai.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyPlanDTO {
    private String status;
    private LocalDateTime trialEndsAt;
    /** El pagament en línia (Stripe) està configurat; si no, la web mostra el contacte per email. */
    private boolean billingEnabled;
    /** Estat de la subscripció a Stripe; null si mai ha contractat en línia. */
    private String subscriptionStatus;
}
