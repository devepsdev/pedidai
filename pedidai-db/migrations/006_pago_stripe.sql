-- ============================================================================
-- Migració 006 — Pagament amb Stripe (octubre 2026)
--   companies: client i subscripció de Stripe i el seu estat («active»,
--   «trialing», «past_due», «canceled»…), que manté al dia el webhook.
--   stripe_events: avisos de Stripe ja processats. Stripe pot repetir un
--   avís; si l'id ja hi és, no es torna a aplicar.
--   Només afegeix columnes buides i una taula nova: no modifica cap dada.
-- Aplicar amb còpia de seguretat prèvia:  mysqldump pedidai_db > backup.sql
-- ============================================================================

ALTER TABLE companies
  ADD COLUMN stripe_customer_id VARCHAR(64) NULL AFTER trial_end_notified_at,
  ADD COLUMN stripe_subscription_id VARCHAR(64) NULL AFTER stripe_customer_id,
  ADD COLUMN subscription_status VARCHAR(32) NULL AFTER stripe_subscription_id,
  ADD UNIQUE KEY uk_companies_stripe_customer (stripe_customer_id),
  ADD UNIQUE KEY uk_companies_stripe_subscription (stripe_subscription_id);

CREATE TABLE stripe_events (
  id          VARCHAR(255) NOT NULL COMMENT 'Id de l''esdeveniment de Stripe (evt_…)',
  type        VARCHAR(100) NOT NULL,
  received_at DATETIME     NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Avisos de Stripe ya procesados';
