# Pedidai - Base de Datos

Scripts SQL y documentación del esquema de base de datos de Pedidai.

**SGBD:** MySQL 8.0+
**Nombre BD:** `pedidai_db`
**Charset:** `utf8mb4`
**Collation:** `utf8mb4_unicode_ci`
**Motor:** InnoDB

---

## Estructura de Ficheros

```text
pedidai-db/
└── pedidai_db_schema.sql    # Script completo de creación del esquema
```

---

## Instalación

### 1. Crear usuario y base de datos

```sql
CREATE USER 'pedidai_user'@'localhost' IDENTIFIED BY 'password_seguro';
CREATE DATABASE pedidai_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON pedidai_db.* TO 'pedidai_user'@'localhost';
FLUSH PRIVILEGES;
```

### 2. Ejecutar el script de esquema

```bash
mysql -u pedidai_user -p < pedidai-db/pedidai_db_schema.sql
```

O desde la consola MySQL:

```sql
SOURCE /ruta/a/pedidai-db/pedidai_db_schema.sql;
```

### 3. Verificar las tablas

```sql
USE pedidai_db;
SHOW TABLES;
```

Resultado esperado: `companies`, `users`, `suppliers`, `products`, `orders`, `order_items`

---

## Diagrama Entidad-Relación

```text
┌──────────────┐
│   COMPANIES  │
└──────┬───────┘
       │ 1
       ├──────────────────┬────────────────┐
       │ N                │ N              │ N
  ┌────┴───┐        ┌─────┴─────┐    ┌────┴───┐
  │ USERS  │        │ SUPPLIERS │    │ ORDERS │
  └────────┘        └─────┬─────┘    └────┬───┘
                          │ 1             │ 1
                          │ N             │ N
                     ┌────┴─────┐  ┌─────┴──────┐
                     │ PRODUCTS │  │ ORDER_ITEMS │
                     └──────────┘  └────────────┘
```

---

## Descripción de Tablas

### `companies`

Almacena la información de las empresas clientes de la plataforma.

| Campo         | Tipo           | Restricciones         | Descripción                     |
| ------------- | -------------- | --------------------- | ------------------------------- |
| `id`          | BIGINT         | PK, AUTO_INCREMENT    | Identificador interno           |
| `uuid`        | VARCHAR(255)   | UNIQUE, NOT NULL      | Identificador universal         |
| `name`        | VARCHAR(255)   | NOT NULL              | Nombre de la empresa            |
| `tax_id`      | VARCHAR(50)    | UNIQUE, NOT NULL      | NIF/CIF                         |
| `email`       | VARCHAR(255)   |                       | Email de contacto               |
| `phone`       | VARCHAR(50)    |                       | Teléfono                        |
| `address`     | TEXT           |                       | Dirección física                |
| `city`        | VARCHAR(100)   |                       | Ciudad                          |
| `postal_code` | VARCHAR(20)    |                       | Código postal                   |
| `status`      | ENUM           | DEFAULT 'PENDING'     | ACTIVE \| INACTIVE \| PENDING   |
| `created_at`  | TIMESTAMP      | NOT NULL              | Fecha de creación               |
| `updated_at`  | TIMESTAMP      | NOT NULL              | Última modificación             |

### `users`

Usuarios asociados a las empresas.

| Campo                         | Tipo         | Restricciones    | Descripción                |
| ----------------------------- | ------------ | ---------------- | -------------------------- |
| `id`                          | BIGINT       | PK               | Identificador interno      |
| `uuid`                        | VARCHAR(255) | UNIQUE, NOT NULL | Identificador universal    |
| `company_id`                  | BIGINT       | FK, NOT NULL     | Referencia a COMPANIES     |
| `email`                       | VARCHAR(255) | UNIQUE, NOT NULL | Email único                |
| `password`                    | VARCHAR(255) | NOT NULL         | Hash BCrypt                |
| `first_name`                  | VARCHAR(100) | NOT NULL         | Nombre                     |
| `last_name`                   | VARCHAR(100) | NOT NULL         | Apellidos                  |
| `role`                        | ENUM         | DEFAULT 'USER'   | ADMIN \| USER              |
| `phone`                       | VARCHAR(50)  |                  | Teléfono                   |
| `is_active`                   | BOOLEAN      | DEFAULT TRUE     | Cuenta activa              |
| `is_deleted`                  | BOOLEAN      | DEFAULT FALSE    | Eliminado (soft delete)    |
| `email_verified`              | BOOLEAN      | DEFAULT FALSE    | Email verificado           |
| `email_verification_token`    | VARCHAR(255) |                  | Token de verificación      |
| `email_verification_expires`  | TIMESTAMP    |                  | Expiración token           |
| `password_reset_token`        | VARCHAR(255) |                  | Token de reset             |
| `password_reset_expires`      | TIMESTAMP    |                  | Expiración reset           |
| `last_login`                  | TIMESTAMP    |                  | Último login               |
| `created_at`                  | TIMESTAMP    | NOT NULL         | Fecha creación             |
| `updated_at`                  | TIMESTAMP    | NOT NULL         | Última modificación        |

**FK:** `company_id` → `companies(id)` ON DELETE CASCADE

### `suppliers`

Proveedores de las empresas.

| Campo          | Tipo         | Restricciones    | Descripción             |
| -------------- | ------------ | ---------------- | ----------------------- |
| `id`           | BIGINT       | PK               | Identificador interno   |
| `uuid`         | VARCHAR(255) | UNIQUE, NOT NULL | Identificador universal |
| `company_id`   | BIGINT       | FK, NOT NULL     | Referencia a COMPANIES  |
| `name`         | VARCHAR(255) | NOT NULL         | Nombre del proveedor    |
| `contact_name` | VARCHAR(255) |                  | Persona de contacto     |
| `email`        | VARCHAR(255) |                  | Email                   |
| `phone`        | VARCHAR(50)  |                  | Teléfono                |
| `address`      | TEXT         |                  | Dirección               |
| `notes`        | TEXT         |                  | Observaciones           |
| `is_active`    | BOOLEAN      | DEFAULT TRUE     | Activo                  |
| `created_at`   | TIMESTAMP    | NOT NULL         | Fecha creación          |
| `updated_at`   | TIMESTAMP    | NOT NULL         | Última modificación     |

### `products`

Catálogo de productos por proveedor.

| Campo         | Tipo           | Restricciones    | Descripción              |
| ------------- | -------------- | ---------------- | ------------------------ |
| `id`          | BIGINT         | PK               | Identificador interno    |
| `uuid`        | VARCHAR(255)   | UNIQUE, NOT NULL | Identificador universal  |
| `supplier_id` | BIGINT         | FK, NOT NULL     | Referencia a SUPPLIERS   |
| `category`    | VARCHAR(255)   |                  | Categoría                |
| `name`        | VARCHAR(255)   | NOT NULL         | Nombre del producto      |
| `description` | TEXT           |                  | Descripción              |
| `price`       | DECIMAL(10,2)  | NOT NULL         | Precio unitario          |
| `volume`      | DECIMAL(10,2)  |                  | Volumen                  |
| `unit`        | VARCHAR(50)    |                  | Unidad (kg, L, uds, ...) |
| `image_url`   | VARCHAR(500)   |                  | URL de la imagen         |
| `is_active`   | BOOLEAN        | DEFAULT TRUE     | Activo                   |
| `created_at`  | TIMESTAMP      | NOT NULL         | Fecha creación           |
| `updated_at`  | TIMESTAMP      | NOT NULL         | Última modificación      |

### `orders`

Pedidos de las empresas a sus proveedores.

| Campo                 | Tipo           | Restricciones     | Descripción                                                            |
| --------------------- | -------------- | ----------------- | ---------------------------------------------------------------------- |
| `id`                  | BIGINT         | PK                | Identificador interno                                                  |
| `uuid`                | VARCHAR(255)   | UNIQUE, NOT NULL  | Identificador universal                                                |
| `company_id`          | BIGINT         | FK, NOT NULL      | Referencia a COMPANIES                                                 |
| `supplier_id`         | BIGINT         | FK, NOT NULL      | Referencia a SUPPLIERS                                                 |
| `user_id`             | BIGINT         | FK, NOT NULL      | Usuario que crea el pedido                                             |
| `name`                | VARCHAR(255)   | NOT NULL          | Nombre del pedido                                                      |
| `status`              | ENUM           | DEFAULT 'PENDING' | PENDING, SENT, CONFIRMED, REJECTED, COMPLETED, CANCELLED, DELETED             |
| `total_amount`        | DECIMAL(10,2)  | DEFAULT 0         | Total del pedido                                                       |
| `notes`               | TEXT           |                   | Observaciones                                                          |
| `delivery_date`       | DATE           |                   | Fecha de entrega prevista                                              |
| `notification_method` | ENUM           |                   | EMAIL \| WHATSAPP \| BOTH                                              |
| `created_at`          | TIMESTAMP      | NOT NULL          | Fecha creación                                                         |
| `updated_at`          | TIMESTAMP      | NOT NULL          | Última modificación                                                    |

### `order_items`

Líneas de productos dentro de los pedidos.

| Campo        | Tipo          | Restricciones    | Descripción                     |
| ------------ | ------------- | ---------------- | ------------------------------- |
| `id`         | BIGINT        | PK               | Identificador interno           |
| `uuid`       | VARCHAR(255)  | UNIQUE, NOT NULL | Identificador universal         |
| `order_id`   | BIGINT        | FK, NOT NULL     | Referencia a ORDERS             |
| `product_id` | BIGINT        | FK, NOT NULL     | Referencia a PRODUCTS           |
| `quantity`   | DECIMAL(10,2) | NOT NULL         | Cantidad                        |
| `unit_price` | DECIMAL(10,2) | NOT NULL         | Precio unitario en el momento   |
| `subtotal`   | DECIMAL(10,2) | NOT NULL         | Subtotal (quantity × unit_price)|
| `notes`      | TEXT          |                  | Observaciones del ítem          |
| `created_at` | TIMESTAMP     | NOT NULL         | Fecha creación                  |

---

## Relaciones entre Tablas

| Origen        | Destino       | Cardinalidad | Cascade                  |
| ------------- | ------------- | ------------ | ------------------------ |
| COMPANIES     | USERS         | 1:N          | DELETE CASCADE           |
| COMPANIES     | SUPPLIERS     | 1:N          | —                        |
| COMPANIES     | ORDERS        | 1:N          | —                        |
| SUPPLIERS     | PRODUCTS      | 1:N          | —                        |
| USERS         | ORDERS        | 1:N          | —                        |
| ORDERS        | ORDER_ITEMS   | 1:N          | CASCADE + orphanRemoval  |
| PRODUCTS      | ORDER_ITEMS   | N:1          | —                        |

---

## Convenciones

- **ID numérico (BIGINT):** Clave primaria interna, no expuesta en la API.
- **UUID (VARCHAR):** Identificador para uso externo (API/frontend).
- **Timestamps de auditoría:** `created_at` y `updated_at` se gestionan automáticamente.
- **Soft delete:** USERS con `is_deleted`, ORDERS con estado `DELETED`, PRODUCTS con `is_active`.
- **Estrategia JPA:** `ddl-auto=none` — el esquema se crea y gestiona manualmente con este script.
