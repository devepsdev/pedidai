-- ============================================================================
-- Migració 003 — Avisos al client sobre la prova gratuïta (setembre 2026)
--   Es desa quan s'ha enviat cada correu per no repetir-lo:
--   · trial_reminder_sent_at: «la teva prova acaba d'aquí a 3 dies»
--   · trial_end_notified_at:  «la teva prova ha acabat»
--   Només afegeix columnes buides (NULL): no modifica cap dada existent.
-- Aplicar amb còpia de seguretat prèvia:  mysqldump pedidai_db > backup.sql
-- ============================================================================

ALTER TABLE companies
  ADD COLUMN trial_reminder_sent_at DATETIME NULL AFTER trial_ends_at,
  ADD COLUMN trial_end_notified_at DATETIME NULL AFTER trial_reminder_sent_at;
