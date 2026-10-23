-- ============================================================================
-- Migració 005 — Sessions de 30 dies que es poden revocar (octubre 2026)
--   token_version va dins del token (claim «tv»). En canviar o restablir la
--   contrasenya puja una unitat i els tokens anteriors deixen de servir.
--   Només afegeix una columna amb valor 0: no modifica cap dada existent.
-- Aplicar amb còpia de seguretat prèvia:  mysqldump pedidai_db > backup.sql
-- ============================================================================

ALTER TABLE users
  ADD COLUMN token_version INT NOT NULL DEFAULT 0 AFTER last_login;
