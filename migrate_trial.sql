-- ============================================================
-- PedidAI — Migración trial_ends_at para empresas existentes
-- Base de datos: pedidai_db
-- Ejecutar UNA SOLA VEZ
-- ============================================================

USE pedidai_db;

-- Rellenar trial_ends_at para empresas ACTIVE que no tengan fecha asignada
-- (3 meses desde su fecha de creación)
UPDATE companies
SET trial_ends_at = DATE_ADD(created_at, INTERVAL 3 MONTH)
WHERE trial_ends_at IS NULL
  AND status = 'ACTIVE';

-- ============================================================
-- Verificación
-- ============================================================
SELECT id, name, status, created_at, trial_ends_at FROM companies ORDER BY created_at DESC;
