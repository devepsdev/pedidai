# PedidAI — Base de datos

Esquema SQL, migraciones y documentación de la base de datos de PedidAI.

| | |
| --- | --- |
| **SGBD** | MySQL 8.0+ |
| **Base de datos** | `pedidai_db` |
| **Charset / collation** | `utf8mb4` / `utf8mb4_unicode_ci` |
| **Motor** | InnoDB |
| **JPA** | `ddl-auto=none`: el esquema se gestiona solo con estos scripts |

---

## Ficheros

```text
pedidai-db/
├── pedidai_db_schema.sql          # Esquema base (solo estructura, sin datos)
└── migrations/
    ├── 001_lanzamiento.sql        # CIF opcional, idioma del usuario, nombre genérico de producto, historial de precios
    └── 002_empresa_suspendida.sql # Estado SUSPENDED en companies.status
```

Las migraciones se aplican **en orden** sobre el esquema base. Cada una se aplica una sola vez.

> `pedidai_db_schema.sql` es **solo para bases nuevas y vacías**: no contiene `USE` ni `DROP TABLE`, se aplica a la base indicada en la línea de comandos y falla si las tablas ya existen. En una base con datos, aplica únicamente las migraciones que falten.

---

## Instalación

### 1. Usuario y base de datos

```sql
CREATE USER 'pedidai_user'@'localhost' IDENTIFIED BY 'password_seguro';
CREATE DATABASE pedidai_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON pedidai_db.* TO 'pedidai_user'@'localhost';
FLUSH PRIVILEGES;
```

### 2. Esquema y migraciones

```bash
mysql -u pedidai_user -p pedidai_db < pedidai-db/pedidai_db_schema.sql
mysql -u pedidai_user -p pedidai_db < pedidai-db/migrations/001_lanzamiento.sql
mysql -u pedidai_user -p pedidai_db < pedidai-db/migrations/002_empresa_suspendida.sql
```

### 3. Comprobación

```sql
SHOW TABLES;
```

Resultado esperado: `companies`, `order_items`, `orders`, `price_history`, `products`, `suppliers`, `users`.

### Actualizar una base existente

1. Copia de seguridad: `mysqldump --single-transaction pedidai_db | gzip > pedidai_db-$(date +%Y%m%d).sql.gz`
2. Aplica solo las migraciones que falten, en orden.

---

## Diagrama entidad-relación

```text
companies  1 ──< N  users
companies  1 ──< N  suppliers  1 ──< N  products
companies  1 ──< N  orders     1 ──< N  order_items  N >── 1  products
suppliers  1 ──< N  orders
users      1 ──< N  orders                  (usuario que crea el pedido)
companies  1 ──< N  price_history  N >── 1  suppliers
                    price_history  N >── 1  products
```

- Una empresa tiene usuarios, proveedores, pedidos e historial de precios.
- Un proveedor tiene productos; cada pedido es para un proveedor y lo crea un usuario.
- Cada línea de pedido apunta a un producto.
- Cada entrada del historial de precios apunta a la empresa, al proveedor y al producto.

---

## Tablas

Todas las tablas tienen `id` (`BIGINT`, clave interna, nunca expuesta en la API) y `uuid` (`VARCHAR`, identificador público). Salvo `price_history` y `order_items`, también tienen `created_at` y `updated_at`.

### `companies`

Empresas cliente (cada local es una empresa).

| Campo | Tipo | Restricciones | Descripción |
| --- | --- | --- | --- |
| `name` | VARCHAR(255) | NOT NULL | Nombre del negocio |
| `tax_id` | VARCHAR(50) | UNIQUE, NULL | NIF/CIF; opcional hasta contratar |
| `email` | VARCHAR(255) | | Email de contacto |
| `phone` | VARCHAR(50) | | Teléfono |
| `address` | TEXT | | Dirección |
| `city` | VARCHAR(100) | | Ciudad |
| `postal_code` | VARCHAR(20) | | Código postal |
| `status` | ENUM | DEFAULT `PENDING` | `ACTIVE`, `INACTIVE` (prueba acabada sin contratar), `PENDING`, `SUSPENDED` (bloqueada por la plataforma) |
| `trial_ends_at` | DATETIME | NULL | Fin de la prueba gratuita; `NULL` = cliente de pago |

### `users`

| Campo | Tipo | Restricciones | Descripción |
| --- | --- | --- | --- |
| `company_id` | BIGINT | FK → `companies` | Empresa del usuario |
| `email` | VARCHAR(255) | UNIQUE, NOT NULL | Email de acceso |
| `password` | VARCHAR(255) | NOT NULL | Hash BCrypt |
| `first_name` / `last_name` | VARCHAR(100) | NOT NULL | Nombre y apellidos |
| `role` | ENUM | DEFAULT `USER` | `USER`, `ADMIN` (gestiona su empresa), `SUPER_ADMIN` (plataforma) |
| `phone` | VARCHAR(50) | | Teléfono |
| `language` | VARCHAR(2) | NOT NULL, DEFAULT `es` | Idioma de la interfaz, los emails y los PDF (`es` / `ca`) |
| `is_active` | BOOLEAN | DEFAULT TRUE | Cuenta activa |
| `is_deleted` | BOOLEAN | DEFAULT FALSE | Baja lógica |
| `email_verified` | BOOLEAN | DEFAULT FALSE | Necesario para enviar pedidos a proveedores |
| `email_verification_token` / `_expires` | VARCHAR / TIMESTAMP | | Verificación de email (48 h) |
| `password_reset_token` / `_expires` | VARCHAR / TIMESTAMP | | Recuperación de contraseña |
| `last_login` | TIMESTAMP | | Último acceso |

### `suppliers`

| Campo | Tipo | Restricciones | Descripción |
| --- | --- | --- | --- |
| `company_id` | BIGINT | FK → `companies` | Empresa propietaria |
| `name` | VARCHAR(255) | NOT NULL | Nombre del proveedor |
| `contact_name` | VARCHAR(255) | | Persona de contacto |
| `email` | VARCHAR(255) | | Email al que se envían los pedidos |
| `phone` | VARCHAR(50) | | Teléfono |
| `address` / `notes` | TEXT | | Dirección y observaciones |
| `is_active` | BOOLEAN | DEFAULT TRUE | Baja lógica |

### `products`

| Campo | Tipo | Restricciones | Descripción |
| --- | --- | --- | --- |
| `supplier_id` | BIGINT | FK → `suppliers` | Proveedor que lo vende |
| `name` | VARCHAR(255) | NOT NULL | Nombre tal como aparece en el albarán |
| `canonical_name` | VARCHAR(255) | INDEX | Nombre genérico para comparar el mismo producto entre proveedores |
| `category` | VARCHAR(255) | | Categoría |
| `description` | TEXT | | Descripción |
| `price` | DECIMAL(10,2) | NOT NULL | Último precio conocido |
| `volume` | DECIMAL(10,2) | | Volumen o peso por unidad |
| `unit` | VARCHAR(50) | | Unidad (kg, L, ud., caja…) |
| `image_url` | VARCHAR(500) | | Imagen subida |
| `is_active` | BOOLEAN | DEFAULT TRUE | Baja lógica |

### `price_history`

Cada precio observado de un producto: la base de la comparativa entre proveedores y de los avisos de subidas.

| Campo | Tipo | Restricciones | Descripción |
| --- | --- | --- | --- |
| `company_id` | BIGINT | FK → `companies`, ON DELETE CASCADE | Empresa |
| `supplier_id` | BIGINT | FK → `suppliers`, ON DELETE CASCADE | Proveedor |
| `product_id` | BIGINT | FK → `products`, ON DELETE CASCADE | Producto |
| `unit_price` | DECIMAL(12,4) | NOT NULL | Precio unitario |
| `unit` | VARCHAR(50) | | Unidad |
| `quantity` | DECIMAL(12,3) | | Cantidad del albarán |
| `document_date` | DATE | NOT NULL | Fecha del albarán o factura |
| `source` | VARCHAR(20) | NOT NULL | `INVOICE` (albarán leído) o `MANUAL` |
| `document_ref` | VARCHAR(100) | | Número del documento |
| `created_at` | TIMESTAMP | | Fecha de registro |

Índice principal: `(company_id, product_id, document_date)`.

### `orders`

| Campo | Tipo | Restricciones | Descripción |
| --- | --- | --- | --- |
| `company_id` | BIGINT | FK → `companies` | Empresa |
| `supplier_id` | BIGINT | FK → `suppliers` | Proveedor |
| `user_id` | BIGINT | FK → `users` | Usuario que lo crea |
| `name` | VARCHAR(255) | NOT NULL | Nombre del pedido |
| `status` | ENUM | DEFAULT `PENDING` | `PENDING`, `SENT`, `CONFIRMED`, `REJECTED`, `COMPLETED`, `CANCELLED`, `DELETED` |
| `total_amount` | DECIMAL(10,2) | DEFAULT 0 | Total |
| `notes` | TEXT | | Observaciones para el proveedor |
| `delivery_date` | DATE | | Fecha de entrega prevista |

### `order_items`

| Campo | Tipo | Restricciones | Descripción |
| --- | --- | --- | --- |
| `order_id` | BIGINT | FK → `orders` | Pedido |
| `product_id` | BIGINT | FK → `products` | Producto |
| `quantity` | DECIMAL(10,2) | NOT NULL | Cantidad |
| `unit_price` | DECIMAL(10,2) | NOT NULL | Precio en el momento del pedido |
| `subtotal` | DECIMAL(10,2) | NOT NULL | `quantity × unit_price` |
| `notes` | TEXT | | Observaciones de la línea |
| `created_at` | TIMESTAMP | | Fecha de creación |

---

## Claves foráneas y borrado

| Origen | Destino | Al borrar el destino |
| --- | --- | --- |
| `users.company_id` | `companies` | Restringido |
| `suppliers.company_id` | `companies` | Restringido |
| `products.supplier_id` | `suppliers` | Restringido |
| `orders.company_id` / `supplier_id` / `user_id` | `companies` / `suppliers` / `users` | Restringido |
| `order_items.order_id` / `product_id` | `orders` / `products` | Restringido |
| `price_history.*` | `companies` / `suppliers` / `products` | CASCADE |

La aplicación no borra registros en el uso diario (bajas lógicas con `is_active`, `is_deleted` o el estado `DELETED`). El único borrado físico es el **borrado automático de conservación**: cada día a las 03:30 la API elimina las empresas cuya prueba terminó hace más de 30 días sin contratar, en el orden que exigen las claves (`price_history` → `order_items` → `orders` → `products` → `suppliers` → `users` → `companies`). Nunca borra clientes de pago ni la empresa del SUPER_ADMIN.
