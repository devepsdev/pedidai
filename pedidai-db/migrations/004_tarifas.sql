-- ============================================================================
-- Migració 004 — Tarifes i pressupostos de proveïdors (octubre 2026)
--   Els preus d'una tarifa es desen a price_history amb source = 'QUOTE'
--   (preu ofert, no pagat) i, si el document la porta, la data de validesa.
--   Només afegeix una columna buida (NULL): no modifica cap dada existent.
-- Aplicar amb còpia de seguretat prèvia:  mysqldump pedidai_db > backup.sql
-- ============================================================================

ALTER TABLE price_history
  ADD COLUMN valid_until date NULL AFTER document_ref;
