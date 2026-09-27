-- ============================================================================
-- Migració 002 — Estat SUSPENDED de les empreses (setembre 2026)
--   El codi (panell SUPER_ADMIN) ja permet suspendre una empresa, però l'ENUM de
--   la base de dades no acceptava el valor i MySQL (mode estricte) rebutjava el canvi.
--   Afegir un valor al final d'un ENUM no modifica les dades existents.
-- Aplicar amb còpia de seguretat prèvia:  mysqldump pedidai_db > backup.sql
-- ============================================================================

ALTER TABLE companies
  MODIFY status enum('ACTIVE','INACTIVE','PENDING','SUSPENDED')
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'PENDING';
