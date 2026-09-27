-- ============================================================================
-- Migració 001 — Llançament (setembre 2026)
--   · CIF opcional al registre (es demana en contractar)
--   · Idioma preferit de l'usuari (correus i PDF en català o castellà)
--   · Nom genèric dels productes per comparar-los entre proveïdors
--   · Historial de preus (albarans) per detectar pujades i el proveïdor més barat
-- Aplicar amb còpia de seguretat prèvia:  mysqldump pedidai_db > backup.sql
-- ============================================================================

ALTER TABLE companies
  MODIFY tax_id varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'NIF/CIF (opcional fins a la contractació)';

ALTER TABLE users
  ADD COLUMN language varchar(2) NOT NULL DEFAULT 'es' AFTER phone;

ALTER TABLE products
  ADD COLUMN canonical_name varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL AFTER name,
  ADD INDEX idx_products_canonical_name (canonical_name);

CREATE TABLE price_history (
  id            bigint NOT NULL AUTO_INCREMENT,
  company_id    bigint NOT NULL,
  supplier_id   bigint NOT NULL,
  product_id    bigint NOT NULL,
  unit_price    decimal(12,4) NOT NULL,
  unit          varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  quantity      decimal(12,3) DEFAULT NULL,
  document_date date NOT NULL,
  source        varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  document_ref  varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  created_at    timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_price_history_company_product (company_id, product_id, document_date),
  KEY idx_price_history_supplier (supplier_id),
  CONSTRAINT fk_price_history_company  FOREIGN KEY (company_id)  REFERENCES companies (id) ON DELETE CASCADE,
  CONSTRAINT fk_price_history_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id) ON DELETE CASCADE,
  CONSTRAINT fk_price_history_product  FOREIGN KEY (product_id)  REFERENCES products (id)  ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Els productes existents parteixen del seu preu actual com a primera observació
INSERT INTO price_history (company_id, supplier_id, product_id, unit_price, unit, document_date, source)
SELECT s.company_id, p.supplier_id, p.id, p.price, p.unit, DATE(COALESCE(p.updated_at, p.created_at)), 'MANUAL'
FROM products p JOIN suppliers s ON s.id = p.supplier_id;
