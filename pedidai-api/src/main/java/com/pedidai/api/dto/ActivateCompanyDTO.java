package com.pedidai.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivateCompanyDTO {
    /** Plan identifier, e.g. "PRO". Reserved for future Stripe integration. */
    private String plan;
}
