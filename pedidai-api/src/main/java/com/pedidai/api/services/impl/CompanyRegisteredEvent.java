package com.pedidai.api.services.impl;

/** S'ha registrat una empresa nova (es publica dins la transacció del registre). */
public record CompanyRegisteredEvent(Long companyId) {
}
