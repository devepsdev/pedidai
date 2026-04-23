# Pedidai - API REST Backend

API REST desarrollada con Spring Boot para la gestión integral de la cadena de suministro B2B para pymes.

**Versión:** 1.2
**Framework:** Spring Boot 3.5.6
**Lenguaje:** Java 21
**Base de datos:** MySQL 8.0+
**Autenticación:** JWT (JSON Web Tokens)

---

## Descripción

Pedidai Backend es una API REST que proporciona una plataforma completa para que las pymes gestionen sus proveedores, mantengan un catálogo de productos y envíen pedidos de forma eficiente. Incluye gestión multiempresa, autenticación JWT, verificación de email, recuperación de contraseña, sistema de notificaciones (email y WhatsApp), generación de informes en PDF e integración con IA para asistencia inteligente.

---

## Características Principales

- **Gestión multi-entidad:** Empresas, usuarios, proveedores, productos y pedidos
- **Catálogo de productos:** Gestión completa con imágenes y búsqueda avanzada
- **Sistema de pedidos:** Creación, edición y envío con notificaciones por email y WhatsApp
- **Notificaciones flexibles:** EMAIL, WHATSAPP o BOTH en el momento del envío
- **Verificación de email:** Tokens con caducidad de 24 horas
- **Autenticación JWT segura:** Tokens HS512 con caducidad de 1 hora
- **Recuperación de contraseña:** Sistema completo con tokens de 1 hora
- **Búsqueda avanzada:** Filtros múltiples con paginación y ordenación
- **Informes y estadísticas:** Dashboard, reportes globales por período con exportación a PDF
- **Integración con IA:** API DeepSeek Vision para asistencia inteligente y escaneo de facturas
- **Gestión de errores centralizada:** Respuestas uniformes con códigos HTTP estándar
- **Validaciones robustas:** Bean Validation con requisitos de contraseña compleja
- **Documentación interactiva:** Swagger UI disponible en producción

---

## Stack Tecnológico

| Área               | Tecnología                                          |
| ------------------ | --------------------------------------------------- |
| Framework          | Spring Boot 3.5.6, Java 21                          |
| Seguridad          | Spring Security, JWT HS512 (JJWT 0.11.5)            |
| Base de datos      | MySQL 8.0+, Spring Data JPA, Hibernate              |
| Validación         | Jakarta Bean Validation                             |
| Email              | Spring Mail (SMTP Gmail)                            |
| PDF                | OpenPDF 1.3.30, PDFBox 2.0.29                       |
| IA                 | DeepSeek Vision API (Spring WebFlux + Reactor)      |
| Documentación API  | SpringDoc OpenAPI 2.8.13 (Swagger UI)               |
| Utilidades         | Lombok, BCrypt                                      |
| Testing            | JUnit, Mockito, H2 (en memoria)                     |
| Build              | Maven 3.8+                                          |

---

## Requisitos Previos

- Java 21 o superior
- Maven 3.8+ (o usar el wrapper incluido)
- MySQL 8.0+
- Cuenta Gmail con App Password configurada
- Variable de entorno `JWT_SECRET` configurada
- Variable de entorno `DEEPSEEK_API_KEY` configurada (para funcionalidades de IA)

---

## Instalación y Configuración

### 1. Clonar el repositorio

```bash
git clone https://github.com/devepsdev/pedidai.git
cd pedidai
```

### 2. Crear la base de datos

```sql
CREATE USER 'pedidai_user'@'localhost' IDENTIFIED BY 'password_seguro';
CREATE DATABASE pedidai_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON pedidai_db.* TO 'pedidai_user'@'localhost';
FLUSH PRIVILEGES;
```

Ejecutar el script de esquema:

```bash
mysql -u pedidai_user -p < pedidai-db/pedidai_db_schema.sql
```

### 3. Configurar variables de entorno

**Windows (PowerShell como administrador):**

```powershell
[System.Environment]::SetEnvironmentVariable('DB_USER_PEDIDAI', 'pedidai_user', 'Machine')
[System.Environment]::SetEnvironmentVariable('DB_PASS_PEDIDAI', 'password_seguro', 'Machine')
[System.Environment]::SetEnvironmentVariable('MAIL_USER_PEDIDAI', 'correo@gmail.com', 'Machine')
[System.Environment]::SetEnvironmentVariable('MAIL_PASS_PEDIDAI', 'app_password_gmail', 'Machine')
[System.Environment]::SetEnvironmentVariable('JWT_SECRET', 'tu_secreto_jwt_muy_largo', 'Machine')
[System.Environment]::SetEnvironmentVariable('DEEPSEEK_API_KEY', 'tu_api_key_deepseek', 'Machine')
```

**Linux/macOS (añadir a `~/.bashrc` o `~/.zshrc`):**

```bash
export DB_USER_PEDIDAI=pedidai_user
export DB_PASS_PEDIDAI=password_seguro
export MAIL_USER_PEDIDAI=correo@gmail.com
export MAIL_PASS_PEDIDAI=app_password_gmail
export JWT_SECRET=tu_secreto_jwt_muy_largo
export DEEPSEEK_API_KEY=tu_api_key_deepseek
```

> **Nota Gmail:** Genera una "App Password" desde <https://myaccount.google.com/apppasswords> con la verificación en 2 pasos activada.

### 4. Compilar y ejecutar

```bash
cd pedidai-api

# Con Maven Wrapper (recomendado)
./mvnw clean install
./mvnw spring-boot:run

# Windows
mvnw.cmd clean install
mvnw.cmd spring-boot:run
```

La API estará disponible en: **<http://localhost:8085>**

Swagger UI: **<https://pedidai.es/swagger-ui.html>**

---

## Estructura del Proyecto

```text
pedidai-api/
└── src/main/java/com/pedidai/api/
    ├── config/                  # Configuración (Swagger, Web, CORS)
    ├── controllers/             # Controladores REST
    │   ├── AuthController       # Autenticación y recuperación de cuenta
    │   ├── CompanyController    # Gestión de empresa
    │   ├── UserController       # CRUD de usuarios
    │   ├── SupplierController   # CRUD de proveedores
    │   ├── ProductController    # CRUD de productos + imágenes
    │   ├── OrderController      # Gestión y envío de pedidos
    │   └── ReportController     # Dashboard y exportación PDF
    ├── dto/                     # Data Transfer Objects
    ├── entities/                # Entidades JPA
    ├── exceptions/              # Gestión de errores centralizada
    ├── repositories/            # Repositorios JPA + Specifications
    ├── security/                # JWT y configuración de seguridad
    └── services/                # Lógica de negocio + impl/
```

---

## API Endpoints

### Autenticación — `/api/auth`

| Método | Endpoint              | Descripción                          | Auth    |
| ------ | --------------------- | ------------------------------------ | ------- |
| POST   | `/login`              | Login con email y contraseña         | Público |
| POST   | `/verify-email`       | Verificar email tras registro        | Público |
| POST   | `/forgot-password`    | Solicitar recuperación de contraseña | Público |
| POST   | `/reset-password`     | Restablecer contraseña con token     | Público |
| POST   | `/resend-verification`| Reenviar email de verificación       | Público |

### Empresas — `/api/companies`

| Método | Endpoint    | Descripción                       | Auth    |
| ------ | ----------- | --------------------------------- | ------- |
| POST   | `/register` | Registrar empresa + administrador | Público |
| GET    | `/`         | Obtener datos de la empresa       | JWT     |
| PUT    | `/`         | Actualizar datos de la empresa    | JWT     |

**Ejemplo de registro:**

```json
POST /api/companies/register
{
  "companyName": "Ferretería El Martillo SL",
  "taxId": "B12345678",
  "companyEmail": "info@elmartillo.es",
  "adminEmail": "admin@elmartillo.es",
  "adminPassword": "Password123@",
  "adminFirstName": "Juan",
  "adminLastName": "García"
}
```

### Usuarios — `/api/users`

| Método | Endpoint                    | Descripción                           | Auth |
| ------ | --------------------------- | ------------------------------------- | ---- |
| GET    | `/`                         | Listar usuarios (paginado)            | JWT  |
| GET    | `/{uuid}`                   | Obtener usuario por UUID              | JWT  |
| GET    | `/search`                   | Búsqueda por texto                    | JWT  |
| GET    | `/filter`                   | Búsqueda avanzada con filtros         | JWT  |
| POST   | `/`                         | Crear nuevo usuario                   | JWT  |
| PUT    | `/{uuid}`                   | Actualizar usuario                    | JWT  |
| PATCH  | `/{uuid}/status`            | Activar/desactivar usuario            | JWT  |
| PATCH  | `/{uuid}/change-password`   | Cambiar contraseña                    | JWT  |
| DELETE | `/{uuid}`                   | Eliminar usuario (soft delete)        | JWT  |

### Proveedores — `/api/suppliers`

| Método | Endpoint          | Descripción                        | Auth |
| ------ | ----------------- | ---------------------------------- | ---- |
| GET    | `/`               | Listar proveedores (paginado)      | JWT  |
| GET    | `/{uuid}`         | Obtener proveedor por UUID         | JWT  |
| GET    | `/search`         | Búsqueda básica por texto          | JWT  |
| GET    | `/filter`         | Búsqueda avanzada con filtros      | JWT  |
| POST   | `/`               | Crear nuevo proveedor              | JWT  |
| PUT    | `/{uuid}`         | Actualizar proveedor               | JWT  |
| PATCH  | `/{uuid}/status`  | Activar/desactivar proveedor       | JWT  |

**Parámetros de paginación (todos los GET):** `page`, `size`, `sortBy`, `sortDir`

### Productos — `/api/products`

| Método | Endpoint                     | Descripción                            | Auth |
| ------ | ---------------------------- | -------------------------------------- | ---- |
| GET    | `/`                          | Listar productos (paginado)            | JWT  |
| GET    | `/{uuid}`                    | Obtener producto por UUID              | JWT  |
| GET    | `/search`                    | Búsqueda básica                        | JWT  |
| GET    | `/filter`                    | Búsqueda avanzada con filtros          | JWT  |
| POST   | `/create`                    | Crear nuevo producto                   | JWT  |
| PUT    | `/{uuid}`                    | Actualizar producto                    | JWT  |
| PATCH  | `/deactivate/{uuid}`         | Desactivar producto (soft delete)      | JWT  |
| POST   | `/upload/{productUuid}`      | Subir imagen a producto existente      | JWT  |
| POST   | `/upload-temp`               | Subir imagen temporal                  | JWT  |

**Filtros disponibles:** `supplierUuid`, `name`, `category`, `description`, `volume`, `unit`, `minPrice`, `maxPrice`, `isActive`, `createdAfter`, `createdBefore`

### Pedidos — `/api/orders`

| Método | Endpoint            | Descripción                                    | Auth |
| ------ | ------------------- | ---------------------------------------------- | ---- |
| GET    | `/filter`           | Listar/filtrar pedidos con búsqueda avanzada   | JWT  |
| GET    | `/{uuid}`           | Obtener pedido por UUID                        | JWT  |
| POST   | `/create`           | Crear nuevo pedido con ítems                   | JWT  |
| PUT    | `/update/{uuid}`    | Actualizar pedido existente                    | JWT  |
| POST   | `/{uuid}/send`      | Enviar pedido al proveedor (email/WhatsApp)    | JWT  |
| PATCH  | `/delete/{uuid}`    | Cancelar pedido                                | JWT  |

**Estados del pedido:** `PENDING` → `SENT` → `CONFIRMED` / `REJECTED` / `COMPLETED` / `CANCELLED`

**Métodos de notificación:** `EMAIL`, `WHATSAPP`, `BOTH`

**Ejemplo de creación:**

```json
POST /api/orders/create
{
  "name": "Pedido Semanal #42",
  "supplierUuid": "550e8400-e29b-41d4-a716-446655440000",
  "deliveryDate": "2026-05-10",
  "notes": "Entregar antes de las 10h",
  "notificationMethod": "EMAIL",
  "items": [
    {
      "productUuid": "660e8400-e29b-41d4-a716-446655440001",
      "quantity": 10,
      "notes": "Preferiblemente ecológico"
    }
  ]
}
```

### Informes — `/api/reports`

| Método | Endpoint        | Descripción                                  | Auth |
| ------ | --------------- | -------------------------------------------- | ---- |
| GET    | `/dashboard`    | Datos del dashboard (último mes)             | JWT  |
| GET    | `/global`       | Informe global por período personalizado     | JWT  |
| GET    | `/global/pdf`   | Informe global en formato PDF                | JWT  |

**Parámetros de informe:** `startDate`, `endDate` (formato `YYYY-MM-DD`)

---

## Base de Datos

El esquema completo está en `pedidai-db/pedidai_db_schema.sql`. Consulta [pedidai-db/readme.md](../pedidai-db/readme.md) para la documentación detallada de tablas y relaciones.

---

## Seguridad

### JWT (JSON Web Tokens)

- **Algoritmo:** HS512 (HMAC-SHA512)
- **Caducidad:** 1 hora
- **Claims:** `sub` (email), `role` (ADMIN/USER), `uuid`, `companyId`

### Requisitos de contraseña

- Mínimo 8 caracteres
- Al menos 1 letra mayúscula
- Al menos 1 letra minúscula
- Al menos 1 número
- Al menos 1 carácter especial (`@#$%^&+=...`)

### Multi-tenancy

Cada usuario solo puede acceder a los datos de su empresa. El `companyId` se extrae automáticamente del token JWT — no es manipulable desde el cliente.

---

## Sistema de Emails

| Tipo                       | Caducidad     | Descripción                                             |
| -------------------------- | ------------- | ------------------------------------------------------- |
| Verificación de email      | 24 horas      | Se envía tras el registro con enlace de verificación    |
| Recuperación de contraseña | 1 hora        | Se envía al solicitar reset con enlace temporal         |
| Notificación de pedido     | Sin caducidad | Se envía al proveedor al confirmar el envío del pedido  |

El email de pedido incluye: datos de la empresa, tabla de productos con cantidades, notas y fecha de entrega prevista.

---

## Flujo de Registro

```text
POST /api/companies/register
        │
        ▼
  Validar datos
        │
        ▼
  Crear Company (PENDING) + User ADMIN (email_verified=false)
        │
        ▼
  Enviar email de verificación (token 24h)
        │
  [Usuario hace clic en el enlace]
        │
        ▼
  POST /api/auth/verify-email
        │
        ▼
  email_verified=true → Company status=ACTIVE
```

## Flujo de Pedido

```text
POST /api/orders/create → Order (PENDING) + OrderItems
        │
  [Usuario revisa y confirma]
        │
        ▼
POST /api/orders/{uuid}/send
        │
        ▼
  Enviar notificación al proveedor (EMAIL/WhatsApp)
        │
        ▼
  Order status = SENT
```

---

## Formato de Respuesta

Todas las respuestas siguen el formato `ApiResponseDTO`:

```json
{
  "success": true,
  "message": "Operación realizada correctamente",
  "data": { },
  "timestamp": "2026-04-13T14:30:00"
}
```

Las respuestas paginadas siguen el formato `PagedResponseDTO`:

```json
{
  "success": true,
  "message": "Búsqueda completada",
  "data": {
    "content": [],
    "pageable": {
      "page": 0,
      "size": 10,
      "totalPages": 5,
      "totalElements": 48,
      "first": true,
      "last": false
    }
  },
  "timestamp": "2026-04-13T14:30:00"
}
```

---

## Gestión de Errores

| Código | Descripción                               |
| ------ | ----------------------------------------- |
| 200    | Operación correcta                        |
| 201    | Recurso creado correctamente              |
| 400    | Petición incorrecta o validación fallida  |
| 401    | No autorizado (token inválido o expirado) |
| 404    | Recurso no encontrado                     |
| 409    | Conflicto (recurso duplicado)             |
| 500    | Error interno del servidor                |

---

## Scripts Maven

| Acción    | Comando                              |
| --------- | ------------------------------------ |
| Compilar  | `./mvnw clean install`               |
| Ejecutar  | `./mvnw spring-boot:run`             |
| Tests     | `./mvnw test`                        |
| Empaquetar| `./mvnw clean package -DskipTests`   |

---

## Autores

- **Daniel Garcia** — Backend Developer
- **Enrique Pérez** — Full Stack Developer

## Contacto

- **Email:** <devepsdev@gmail.com>
- **Web:** <https://pedidai.es>
- **Swagger UI:** <https://pedidai.es/swagger-ui.html>

---

*Documentación completa: consulta Swagger UI para todos los endpoints y sus parámetros.*
