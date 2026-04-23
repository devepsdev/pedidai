-- ============================================================
-- PedidAI — Migración SUPER_ADMIN
-- Base de datos: pedidai_db
-- Ejecutar UNA SOLA VEZ
-- ============================================================

USE pedidai_db;

-- 1. Añadir columna trial_ends_at a companies (IF NOT EXISTS evita el error si ya existe)
ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS trial_ends_at DATETIME NULL;

-- 2. Rellenar trial_ends_at para empresas existentes (3 meses desde created_at)
UPDATE companies
SET trial_ends_at = DATE_ADD(created_at, INTERVAL 3 MONTH)
WHERE trial_ends_at IS NULL;

-- 3. Dar rol SUPER_ADMIN al usuario de desarrollo
UPDATE users
SET role = 'SUPER_ADMIN'
WHERE email = 'devepsdev@gmail.com';

-- 4. (Opcional) Asegurarse de que su empresa esté ACTIVE
UPDATE companies c
    JOIN users u ON u.company_id = c.id
SET c.status = 'ACTIVE'
WHERE u.email = 'devepsdev@gmail.com';

-- ============================================================
-- Verificación
-- ============================================================
SELECT id, name, status, trial_ends_at FROM companies LIMIT 10;
SELECT id, email, role FROM users WHERE email = 'devepsdev@gmail.com';
