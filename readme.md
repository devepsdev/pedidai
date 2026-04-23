# DOCUMENTACIÓN TÉCNICA — PROYECTO PEDIDAI

**Versión:** 1.2.0
**Fecha:** 13 de abril de 2026
**Autores:** Equipo de desarrollo Pedidai

## Enlaces del Proyecto

- **Web App:** [pedidai.es](https://pedidai.es/)
- **API Docs (Swagger):** [pedidai.es/swagger-ui.html](https://pedidai.es/swagger-ui.html)
- **Repositorio GitHub:** [github.com/devepsdev/pedidai](https://github.com/devepsdev/pedidai)

---

## ÍNDICE

1. [Descripción del Producto](#1-descripción-del-producto)
2. [Tecnologías y Lenguajes](#2-tecnologías-y-lenguajes)
3. [Diseño de Pantallas (UI)](#3-diseño-de-pantallas-ui)
4. [Diseño de la Base de Datos](#4-diseño-de-la-base-de-datos)
5. [Documentación del Código Fuente](#5-documentación-del-código-fuente)
6. [Instalación y Configuración](#6-instalación-y-configuración)

---

## 1. DESCRIPCIÓN DEL PRODUCTO

### 1.1. Visión General

**Pedidai** es una plataforma web de gestión empresarial B2B diseñada para que las pymes centralicen y optimicen sus operaciones de compra y gestión de proveedores. Proporciona una interfaz moderna e intuitiva para gestionar pedidos, productos, proveedores y usuarios, con soporte de inteligencia artificial para asistencia inteligente y automatización de tareas.

### 1.2. Funcionalidades

#### Autenticación y Usuarios

- **Registro de empresas** con verificación por correo electrónico
- **Autenticación JWT** con tokens de 1 hora (HS512)
- **Recuperación de contraseña** mediante enlaces temporales
- **Gestión de usuarios** con roles (ADMIN, USER)
- **Multi-tenancy:** cada empresa gestiona sus propios recursos de forma aislada

#### Gestión de Proveedores

- Creación y edición de proveedores con información de contacto completa
- Búsqueda avanzada con múltiples filtros y paginación
- Activación/desactivación de proveedores
- Asociación automática a la empresa autenticada

#### Gestión de Productos

- Catálogo de productos organizado por proveedor
- Información detallada: nombre, categoría, precio, volumen, unidad
- Carga de imágenes de productos (máx. 10 MB)
- Búsqueda básica y avanzada con filtros por categoría, proveedor, precio
- Desactivación de productos (soft delete)

#### Gestión de Pedidos

- Creación de pedidos con múltiples productos (ítems)
- Seguimiento de estado: PENDING → SENT → CONFIRMED / REJECTED / COMPLETED / CANCELLED
- Envío automático al proveedor por **email** o **WhatsApp** (o ambos)
- Filtrado avanzado por estado, proveedor y fechas
- Cálculo automático de totales y subtotales
- Visualización responsive (tablas en escritorio, tarjetas en móvil)

#### Dashboard e Informes

- Panel de control con estadísticas clave del mes actual:
  - Total de pedidos del mes
  - Gasto total del mes (€)
  - Pedidos pendientes
- Últimos 5 pedidos del mes actual
- Generación de informes globales por período con **exportación a PDF**
- Visualización de datos con gráficos y tablas

#### Inteligencia Artificial

- **AI Chat:** asistente inteligente integrado para consultas y sugerencias
- **AI Suggestions:** sugerencias automáticas de productos basadas en el historial
- **Invoice Scan:** escaneo y extracción de datos de facturas mediante visión por computadora (DeepSeek Vision API)

#### Funcionalidades Adicionales

- **Internacionalización (i18n):** interfaz disponible en castellano y catalán
- Paginación y ordenación en todos los listados
- Modales de confirmación para acciones críticas
- Alertas y notificaciones de éxito/error
- Diseño responsive adaptado a todos los dispositivos
- Breadcrumbs para navegación intuitiva

---

## 2. TECNOLOGÍAS Y LENGUAJES

### 2.1. Frontend (pedidai-app)

#### Lenguaje

- **TypeScript 5.9** — Superset de JavaScript con tipado estático
- **HTML5 y CSS3** — Estructura y estilos web

#### Framework y Librerías Principales

- **Angular 21.2.6** — Framework principal para construcción de interfaces (componentes standalone)
- **Angular Router** — Gestión de rutas y navegación con guards
- **Angular Reactive Forms** — Gestión de formularios con validación reactiva
- **Angular CDK 21** — Kit de desarrollo de componentes

#### Estilos

- **Tailwind CSS 4.2.2** — Framework CSS utility-first
- **PostCSS 8.5** — Procesador de CSS

#### Internacionalización

- **@ngx-translate/core 17** — Soporte i18n: castellano (`es.json`) y catalán (`ca.json`)

#### Reactividad

- **RxJS 7.8** — Programación reactiva con Observables

#### Herramientas de Calidad y Build

- **Angular CLI 21.2.6** — Herramienta de construcción y generación de código
- **Vitest 4** — Tests unitarios
- **Prettier 3.8** — Formatador de código automático

### 2.2. Backend (pedidai-api)

#### Lenguaje del Backend

- **Java 21** — Última versión LTS (Long Term Support)

#### Framework Principal

- **Spring Boot 3.5.6** — Framework para aplicaciones Java empresariales
  - `spring-boot-starter-web` — API REST
  - `spring-boot-starter-data-jpa` — Persistencia JPA/Hibernate
  - `spring-boot-starter-security` — Seguridad y autenticación
  - `spring-boot-starter-validation` — Validación de entrada
  - `spring-boot-starter-mail` — Envío de correos electrónicos
  - `spring-webflux` + `reactor-netty-http` — Llamadas reactivas a API de IA

#### ORM y Base de Datos

- **Hibernate** — Implementación JPA para mapeo objeto-relacional
- **MySQL Connector/J** — Driver JDBC para MySQL

#### Seguridad

- **Spring Security** — Framework de seguridad
- **JJWT 0.11.5** — Generación y validación de JWT (HS512)

#### Generación de Documentos

- **OpenPDF 1.3.30** — Generación de documentos PDF
- **PDFBox 2.0.29** — Procesamiento de PDF

#### Integración con IA

- **DeepSeek Vision API** — Visión por computadora para escaneo de facturas y asistencia

#### Documentación API

- **SpringDoc OpenAPI 2.8.13** — Swagger UI interactivo

#### Utilidades

- **Lombok** — Generación automática de código (getters, setters, builders)

#### Testing

- **Spring Boot Starter Test** — JUnit, Mockito
- **Spring Security Test** — Tests de seguridad
- **H2 Database** — Base de datos en memoria para tests

#### Build

- **Maven 3.8+** — Gestión de dependencias y construcción

### 2.3. Base de Datos (pedidai-db)

- **MySQL 8.0+** — SGBD relacional
- **Charset:** `utf8mb4` con collation `utf8mb4_unicode_ci`
- **Motor:** InnoDB (transacciones ACID, integridad referencial)

### 2.4. Infraestructura y Herramientas

| Categoría              | Herramienta                                 |
| ---------------------- | ------------------------------------------- |
| Control de versiones   | Git + GitHub                                |
| IDE Backend            | IntelliJ IDEA                               |
| IDE Frontend           | Visual Studio Code                          |
| Servidor de aplicación | Apache Tomcat (embebido en Spring Boot)     |
| Servidor web           | Apache HTTP Server (proxy inverso)          |
| Gestión de paquetes    | npm (frontend), Maven (backend)             |

---

## 3. DISEÑO DE PANTALLAS (UI)

### 3.1. Paleta de Colores

Definida en los estilos globales de Tailwind:

| Variable    | Color     | Uso                    |
| ----------- | --------- | ---------------------- |
| Primary     | `#1c2792` | Azul corporativo       |
| Secondary   | `#8c00ff` | Violeta secundario     |
| Success     | `#28a745` | Acciones exitosas      |
| Warning     | `#ffc107` | Advertencias           |
| Danger      | `#dc3545` | Errores                |

### 3.2. Tipografía

- **Fuente principal:** Poppins
- **Fuente secundaria:** Inter
- **Fallback:** system fonts (Segoe UI, Roboto, Arial)

### 3.3. Estructura de Páginas

#### Páginas Públicas (`PublicLayout`)

Estructura: Navbar + Contenido + Footer

| Ruta              | Componente         | Descripción                                         |
| ----------------- | ------------------ | --------------------------------------------------- |
| `/`               | Landing            | Página de inicio con hero, beneficios y CTA         |
| `/login`          | Login              | Formulario de autenticación con validación          |
| `/register`       | Register           | Registro de empresa + usuario administrador         |
| `/recover`        | RecoverPassword    | Solicitud de restablecimiento de contraseña         |
| `/reset-password` | ResetPassword      | Formulario para establecer nueva contraseña         |
| `/verify-email`   | VerifyEmail        | Verificación automática con token por email         |

#### Páginas Privadas (`PrivateLayout`)

Estructura: Sidebar + Contenido principal

| Ruta          | Componente           | Descripción                                      |
| ------------- | -------------------- | ------------------------------------------------ |
| `/dashboard`  | Dashboard            | Panel con estadísticas del mes y últimos pedidos |
| `/suppliers`  | Supplier List/Form   | Listado y formulario de proveedores              |
| `/products`   | Product List/Form    | Catálogo de productos con búsqueda avanzada      |
| `/orders`     | Order List/Create    | Gestión completa de pedidos                      |
| `/reports`    | Reports              | Informes globales con exportación a PDF          |
| `/company`    | CompanyConfig        | Configuración de datos de la empresa             |
| `/users`      | User List/Form       | Gestión de usuarios de la empresa                |
| `/ai-chat`    | AiChat               | Asistente de IA para consultas y sugerencias     |
| `/invoices`   | InvoiceScan          | Escaneo de facturas con IA                       |

### 3.4. Componentes Compartidos (`shared/`)

| Componente       | Descripción                                        |
| ---------------- | -------------------------------------------------- |
| `navbar`         | Barra de navegación superior (zona pública)        |
| `sidebar`        | Menú lateral (zona privada), colapsable en móvil   |
| `footer`         | Pie de página con enlaces legales                  |
| `page-header`    | Cabecera de página con título y breadcrumbs        |
| `alert`          | Notificaciones de éxito/error/advertencia          |
| `confirm-modal`  | Modal de confirmación para acciones críticas       |
| `pagination`     | Componente de paginación reutilizable              |

### 3.5. Diseño Responsive

| Breakpoint   | Layout                                    |
| ------------ | ----------------------------------------- |
| ≥ 1024px     | Sidebar visible, tablas, multicolomna     |
| < 1024px     | Sidebar colapsable, tarjetas, monocolomna |

---

## 4. DISEÑO DE LA BASE DE DATOS

### 4.1. Información General

- **SGBD:** MySQL 8.0+
- **Nombre de la BD:** `pedidai_db`
- **Charset:** `utf8mb4`
- **Collation:** `utf8mb4_unicode_ci`
- **Motor:** InnoDB
- **Estrategia JPA:** `ddl-auto=none` (esquema gestionado manualmente)

### 4.2. Diagrama Entidad-Relación

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

### 4.3. Tablas Principales

#### `companies`

| Campo         | Tipo          | Descripción                      |
| ------------- | ------------- | -------------------------------- |
| `id`          | BIGINT PK     | Identificador interno            |
| `uuid`        | VARCHAR UNIQUE| Identificador externo            |
| `name`        | VARCHAR       | Nombre de la empresa             |
| `tax_id`      | VARCHAR UNIQUE| NIF/CIF                          |
| `email`       | VARCHAR       | Email de contacto                |
| `phone`       | VARCHAR       | Teléfono                         |
| `address`     | TEXT          | Dirección física                 |
| `city`        | VARCHAR       | Ciudad                           |
| `postal_code` | VARCHAR       | Código postal                    |
| `status`      | ENUM          | ACTIVE, INACTIVE, PENDING        |
| `created_at`  | TIMESTAMP     | Fecha de creación                |
| `updated_at`  | TIMESTAMP     | Última modificación              |

#### `users`

| Campo                        | Tipo          | Descripción                   |
| ---------------------------- | ------------- | ----------------------------- |
| `id`                         | BIGINT PK     | Identificador interno         |
| `uuid`                       | VARCHAR UNIQUE| Identificador externo         |
| `company_id`                 | BIGINT FK     | Referencia a COMPANIES        |
| `email`                      | VARCHAR UNIQUE| Email único                   |
| `password`                   | VARCHAR       | Hash BCrypt                   |
| `first_name` / `last_name`   | VARCHAR       | Nombre y apellidos            |
| `role`                       | ENUM          | ADMIN, USER                   |
| `is_active`                  | BOOLEAN       | Cuenta activa                 |
| `is_deleted`                 | BOOLEAN       | Soft delete                   |
| `email_verified`             | BOOLEAN       | Email verificado              |
| `email_verification_token`   | VARCHAR       | Token de verificación (24h)   |
| `password_reset_token`       | VARCHAR       | Token de reset (1h)           |
| `last_login`                 | TIMESTAMP     | Último login                  |

**FK:** `company_id` → `companies(id)` ON DELETE CASCADE

#### `suppliers`

| Campo          | Tipo          | Descripción                |
| -------------- | ------------- | -------------------------- |
| `id`           | BIGINT PK     | Identificador interno      |
| `uuid`         | VARCHAR UNIQUE| Identificador externo      |
| `company_id`   | BIGINT FK     | Referencia a COMPANIES     |
| `name`         | VARCHAR       | Nombre del proveedor       |
| `contact_name` | VARCHAR       | Persona de contacto        |
| `email`        | VARCHAR       | Email                      |
| `phone`        | VARCHAR       | Teléfono                   |
| `address`      | TEXT          | Dirección                  |
| `notes`        | TEXT          | Observaciones              |
| `is_active`    | BOOLEAN       | Activo/inactivo            |

#### `products`

| Campo         | Tipo          | Descripción               |
| ------------- | ------------- | ------------------------- |
| `id`          | BIGINT PK     | Identificador interno     |
| `uuid`        | VARCHAR UNIQUE| Identificador externo     |
| `supplier_id` | BIGINT FK     | Referencia a SUPPLIERS    |
| `category`    | VARCHAR       | Categoría                 |
| `name`        | VARCHAR       | Nombre del producto       |
| `description` | TEXT          | Descripción               |
| `price`       | DECIMAL(10,2) | Precio unitario           |
| `volume`      | DECIMAL(10,2) | Volumen                   |
| `unit`        | VARCHAR       | Unidad (kg, L, uds, ...)  |
| `image_url`   | VARCHAR       | URL de la imagen          |
| `is_active`   | BOOLEAN       | Activo/inactivo           |

#### `orders`

| Campo                 | Tipo          | Descripción                                       |
| --------------------- | ------------- | ------------------------------------------------- |
| `id`                  | BIGINT PK     | Identificador interno                             |
| `uuid`                | VARCHAR UNIQUE| Identificador externo                             |
| `company_id`          | BIGINT FK     | Referencia a COMPANIES                            |
| `supplier_id`         | BIGINT FK     | Referencia a SUPPLIERS                            |
| `user_id`             | BIGINT FK     | Referencia a USERS                                |
| `name`                | VARCHAR       | Nombre del pedido                                 |
| `status`              | ENUM          | PENDING, SENT, CONFIRMED, REJECTED, COMPLETED, CANCELLED          |
| `total_amount`        | DECIMAL(10,2) | Total del pedido                                  |
| `notes`               | TEXT          | Observaciones                                     |
| `delivery_date`       | DATE          | Fecha de entrega prevista                         |
| `notification_method` | ENUM          | EMAIL, WHATSAPP, BOTH                             |

#### `order_items`

| Campo        | Tipo          | Descripción                      |
| ------------ | ------------- | -------------------------------- |
| `id`         | BIGINT PK     | Identificador interno            |
| `uuid`       | VARCHAR UNIQUE| Identificador externo            |
| `order_id`   | BIGINT FK     | Referencia a ORDERS              |
| `product_id` | BIGINT FK     | Referencia a PRODUCTS            |
| `quantity`   | DECIMAL(10,2) | Cantidad                         |
| `unit_price` | DECIMAL(10,2) | Precio unitario en el momento    |
| `subtotal`   | DECIMAL(10,2) | Subtotal (quantity × unit_price) |
| `notes`      | TEXT          | Observaciones del ítem           |

### 4.4. Relaciones

| Origen    | Destino     | Cardinalidad | Cascade                 |
| --------- | ----------- | ------------ | ----------------------- |
| COMPANIES | USERS       | 1:N          | DELETE CASCADE          |
| COMPANIES | SUPPLIERS   | 1:N          | —                       |
| COMPANIES | ORDERS      | 1:N          | —                       |
| SUPPLIERS | PRODUCTS    | 1:N          | —                       |
| USERS     | ORDERS      | 1:N          | —                       |
| ORDERS    | ORDER_ITEMS | 1:N          | CASCADE + orphanRemoval |
| PRODUCTS  | ORDER_ITEMS | N:1          | —                       |

### 4.5. Características Especiales

- **Doble identificador:** `id` interno (BIGINT) + `uuid` externo (VARCHAR) para no exponer claves internas en la API.
- **Timestamps de auditoría:** `created_at` y `updated_at` gestionados automáticamente por JPA.
- **Soft delete:** `is_deleted` en usuarios, `is_active` en productos y proveedores, estado `DELETED` en pedidos.

---

## 5. DOCUMENTACIÓN DEL CÓDIGO FUENTE

### 5.1. Estructura General del Proyecto

```text
pedidai/
├── pedidai-api/    # Backend (Spring Boot + Java 21)
├── pedidai-app/    # Frontend (Angular 21 + Tailwind CSS)
└── pedidai-db/     # Scripts SQL de base de datos
```

### 5.2. Frontend (pedidai-app)

#### 5.2.1. Estructura de Carpetas

```text
pedidai-app/
├── public/
│   └── i18n/                      # Traducciones
│       ├── ca.json                # Catalán
│       └── es.json                # Castellano
├── src/
│   ├── app/
│   │   ├── app.ts                 # Componente raíz
│   │   ├── app.routes.ts          # Definición de rutas
│   │   ├── app.config.ts          # Configuración Angular
│   │   ├── components/            # Componentes funcionales
│   │   │   ├── ai-chat/           # Chat IA + sugerencias IA
│   │   │   │   ├── ai-chat/
│   │   │   │   └── ai-suggestions/
│   │   │   ├── auth/              # Autenticación
│   │   │   │   ├── login/
│   │   │   │   ├── register/
│   │   │   │   ├── verify-email/
│   │   │   │   ├── recover-password/
│   │   │   │   └── reset-password/
│   │   │   ├── company/           # Configuración empresa
│   │   │   │   └── company-config/
│   │   │   ├── dashboard/         # Panel de control
│   │   │   │   └── dashboard/
│   │   │   ├── invoices/          # Escaneo de facturas con IA
│   │   │   │   └── invoice-scan/
│   │   │   ├── orders/            # Gestión de pedidos
│   │   │   │   ├── order-create/
│   │   │   │   ├── order-detail/
│   │   │   │   └── order-list/
│   │   │   ├── products/          # Catálogo de productos
│   │   │   │   ├── product-form/
│   │   │   │   └── product-list/
│   │   │   ├── reports/           # Informes y estadísticas
│   │   │   │   └── reports/
│   │   │   ├── suppliers/         # Gestión de proveedores
│   │   │   │   ├── supplier-form/
│   │   │   │   └── supplier-list/
│   │   │   └── users/             # Gestión de usuarios
│   │   │       ├── user-form/
│   │   │       └── user-list/
│   │   ├── guards/                # Guards de autenticación
│   │   ├── interceptors/          # Interceptores HTTP (JWT)
│   │   ├── layouts/               # Layouts de la app
│   │   │   ├── public-layout/
│   │   │   └── private-layout/
│   │   ├── models/                # Interfaces TypeScript
│   │   ├── pages/                 # Páginas estáticas
│   │   │   └── landing/
│   │   ├── services/              # Servicios HTTP
│   │   └── shared/                # Componentes compartidos
│   │       ├── alert/
│   │       ├── confirm-modal/
│   │       ├── footer/
│   │       ├── navbar/
│   │       ├── page-header/
│   │       ├── pagination/
│   │       └── sidebar/
│   ├── assets/                    # Recursos estáticos
│   ├── environments/              # Configuración de entornos
│   └── styles.scss                # Estilos globales
├── angular.json                   # Configuración Angular CLI
├── package.json                   # Dependencias npm
└── tsconfig.json                  # Configuración TypeScript
```

#### 5.2.2. Patrones y Arquitectura

**Componentes Standalone (Angular 17+):** No se usan NgModules; cada componente declara sus propias dependencias.

**Arquitectura en capas:**

```text
Page/Component → Service → HttpClient (con interceptor JWT) → Backend API
                    │
               Observable (RxJS)
```

**Gestión de estado:**

- `localStorage` para token JWT
- Guards para protección de rutas privadas
- Interceptor HTTP para inyección automática del token

**Internacionalización:**

- `@ngx-translate` con ficheros JSON por idioma en `public/i18n/`
- Idiomas soportados: castellano (`es`) y catalán (`ca`)

#### 5.2.3. Ficheros Clave

| Fichero                 | Descripción                                        |
| ----------------------- | -------------------------------------------------- |
| `app.routes.ts`         | Definición de todas las rutas (públicas/privadas)  |
| `app.config.ts`         | Proveedores Angular: HttpClient, i18n, router      |
| `guards/`               | AuthGuard para proteger rutas privadas             |
| `interceptors/`         | Inyección automática de token JWT en cada request  |
| `environments/`         | URLs de la API según entorno (dev/prod)            |

### 5.3. Backend (pedidai-api)

#### 5.3.1. Estructura de Carpetas

```text
pedidai-api/
└── src/main/java/com/pedidai/api/
    ├── PedidaiApplication.java      # Punto de entrada Spring Boot
    ├── config/
    │   ├── SecurityConfig.java      # Configuración de seguridad y CORS
    │   ├── SwaggerConfig.java       # Configuración Swagger/OpenAPI
    │   └── WebConfig.java           # Configuración web general
    ├── controllers/
    │   ├── AuthController.java      # /api/auth — autenticación y cuentas
    │   ├── CompanyController.java   # /api/companies — gestión de empresa
    │   ├── UserController.java      # /api/users — CRUD de usuarios
    │   ├── SupplierController.java  # /api/suppliers — CRUD de proveedores
    │   ├── ProductController.java   # /api/products — CRUD + imágenes
    │   ├── OrderController.java     # /api/orders — pedidos y notificaciones
    │   └── ReportController.java    # /api/reports — dashboard y PDF
    ├── dto/                         # Data Transfer Objects (request/response)
    ├── entities/                    # Entidades JPA (Company, User, Supplier, Product, Order, OrderItem)
    ├── exceptions/
    │   ├── GlobalExceptionHandler.java
    │   ├── ResourceNotFoundException.java
    │   ├── DuplicateResourceException.java
    │   └── BadRequestException.java
    ├── repositories/                # Repositorios JPA + Specifications para filtros
    ├── security/
    │   ├── JwtUtil.java             # Generación y validación de JWT
    │   ├── JwtAuthenticationFilter.java
    │   └── SecurityConfig.java
    └── services/
        ├── AuthService.java
        ├── CompanyService.java
        ├── UserService.java
        ├── SupplierService.java
        ├── ProductService.java
        ├── OrderService.java
        ├── ReportService.java
        ├── EmailService.java
        ├── NotificationService.java # Email + WhatsApp
        └── impl/                    # Implementaciones
```

#### 5.3.2. Patrones y Arquitectura

**Arquitectura en capas:**

```text
HTTP Request → JwtAuthenticationFilter → Controller → Service → Repository → Database
                      │                      │             │
               SecurityContext           Validación   Lógica negocio
```

**Patrones implementados:**

- **MVC REST:** separación Controller / Service / Repository
- **DTO Pattern:** separación entre objetos de transferencia y entidades JPA
- **Repository + Specification Pattern:** filtros dinámicos con JPA Criteria API
- **Dependency Injection:** Spring IoC
- **Builder:** Lombok `@Builder` para construcción de objetos

#### 5.3.3. Endpoints Principales

##### `AuthController` (`/api/auth`)

| Endpoint               | Método | Descripción                          |
| ---------------------- | ------ | ------------------------------------ |
| `/login`               | POST   | Autenticación → devuelve token JWT   |
| `/forgot-password`     | POST   | Solicitud de recuperación            |
| `/reset-password`      | POST   | Restablecer contraseña con token     |
| `/verify-email`        | POST   | Verificar email de registro          |
| `/resend-verification` | POST   | Reenviar correo de verificación      |

##### `CompanyController` (`/api/companies`)

| Endpoint    | Método | Descripción                             |
| ----------- | ------ | --------------------------------------- |
| `/register` | POST   | Registrar empresa + admin (público)     |
| `/`         | GET    | Obtener empresa del usuario autenticado |
| `/`         | PUT    | Actualizar datos de la empresa          |

##### `UserController` (`/api/users`)

| Endpoint                  | Método | Descripción                      |
| ------------------------- | ------ | -------------------------------- |
| `/`                       | GET    | Listar usuarios (paginado)       |
| `/{uuid}`                 | GET    | Obtener usuario por UUID         |
| `/search`                 | GET    | Búsqueda por texto               |
| `/filter`                 | GET    | Búsqueda avanzada con filtros    |
| `/`                       | POST   | Crear usuario                    |
| `/{uuid}`                 | PUT    | Actualizar usuario               |
| `/{uuid}/status`          | PATCH  | Activar/desactivar               |
| `/{uuid}/change-password` | PATCH  | Cambiar contraseña               |
| `/{uuid}`                 | DELETE | Eliminar (soft delete)           |

##### `SupplierController` (`/api/suppliers`)

| Endpoint         | Método | Descripción                   |
| ---------------- | ------ | ----------------------------- |
| `/`              | GET    | Listar proveedores (paginado) |
| `/{uuid}`        | GET    | Obtener proveedor por UUID    |
| `/search`        | GET    | Búsqueda por texto            |
| `/filter`        | GET    | Búsqueda avanzada             |
| `/`              | POST   | Crear proveedor               |
| `/{uuid}`        | PUT    | Actualizar proveedor          |
| `/{uuid}/status` | PATCH  | Activar/desactivar            |

##### `ProductController` (`/api/products`)

| Endpoint               | Método | Descripción                          |
| ---------------------- | ------ | ------------------------------------ |
| `/`                    | GET    | Listar productos (paginado)          |
| `/{uuid}`              | GET    | Obtener producto por UUID            |
| `/search`              | GET    | Búsqueda por texto                   |
| `/filter`              | GET    | Búsqueda avanzada                    |
| `/create`              | POST   | Crear producto                       |
| `/{uuid}`              | PUT    | Actualizar producto                  |
| `/deactivate/{uuid}`   | PATCH  | Desactivar (soft delete)             |
| `/upload/{productUuid}`| POST   | Subir imagen a producto existente    |
| `/upload-temp`         | POST   | Subir imagen temporal                |

##### `OrderController` (`/api/orders`)

| Endpoint          | Método | Descripción                            |
| ----------------- | ------ | -------------------------------------- |
| `/filter`         | GET    | Listar/filtrar pedidos                 |
| `/{uuid}`         | GET    | Obtener pedido por UUID                |
| `/create`         | POST   | Crear pedido con ítems                 |
| `/update/{uuid}`  | PUT    | Actualizar pedido                      |
| `/{uuid}/send`    | POST   | Enviar al proveedor (email/WhatsApp)   |
| `/delete/{uuid}`  | PATCH  | Cancelar pedido                        |

##### `ReportController` (`/api/reports`)

| Endpoint       | Método | Descripción                               |
| -------------- | ------ | ----------------------------------------- |
| `/dashboard`   | GET    | Datos del dashboard (último mes)          |
| `/global`      | GET    | Informe global por período (JSON)         |
| `/global/pdf`  | GET    | Informe global por período (PDF)          |

#### 5.3.4. Ficheros Clave

| Fichero                       | Descripción                                      |
| ----------------------------- | ------------------------------------------------ |
| `pom.xml`                     | Dependencias y configuración Maven               |
| `PedidaiApplication.java`     | Punto de entrada de Spring Boot                  |
| `SecurityConfig.java`         | Configuración de seguridad, JWT y CORS           |
| `JwtUtil.java`                | Generación y validación de tokens JWT            |
| `GlobalExceptionHandler.java` | Gestión centralizada de errores HTTP             |
| `ApiResponseDTO.java`         | Formato estándar de respuesta de la API          |
| `application.properties`      | Configuración de la aplicación y variables       |

### 5.4. Base de Datos (pedidai-db)

```text
pedidai-db/
├── pedidai_db_schema.sql    # Esquema completo de producción
└── readme.md                # Documentación de tablas y relaciones
```

---

## 6. INSTALACIÓN Y CONFIGURACIÓN

### 6.1. Requisitos Previos

| Componente  | Versión mínima  | Propósito             |
| ----------- | --------------- | --------------------- |
| Java JDK    | 21 (LTS)        | Backend               |
| Maven       | 3.8+            | Build backend         |
| Node.js     | 22.x            | Frontend              |
| npm         | 10.x            | Paquetes frontend     |
| MySQL       | 8.0+            | Base de datos         |
| Git         | Cualquiera      | Control de versiones  |

### 6.2. Instalación de la Base de Datos

#### Instalar MySQL

**Windows:** descargar desde <https://dev.mysql.com/downloads/installer/>

**Linux (Ubuntu/Debian):**

```bash
sudo apt update && sudo apt install mysql-server
sudo mysql_secure_installation
```

**macOS:**

```bash
brew install mysql && brew services start mysql
```

#### Crear usuario y esquema

```bash
mysql -u root -p
```

```sql
CREATE USER 'pedidai_user'@'localhost' IDENTIFIED BY 'password_seguro';
CREATE DATABASE pedidai_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON pedidai_db.* TO 'pedidai_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

```bash
mysql -u pedidai_user -p < pedidai-db/pedidai_db_schema.sql
```

### 6.3. Instalación del Backend

#### Instalar Java 21

**Windows:** descargar desde <https://www.oracle.com/java/technologies/downloads/#java21>

**Linux:**

```bash
sudo apt install openjdk-21-jdk
```

**macOS:**

```bash
brew install openjdk@21
```

#### Configurar variables de entorno

**Windows (PowerShell como administrador):**

```powershell
[System.Environment]::SetEnvironmentVariable('DB_USER_PEDIDAI', 'pedidai_user', 'Machine')
[System.Environment]::SetEnvironmentVariable('DB_PASS_PEDIDAI', 'password_seguro', 'Machine')
[System.Environment]::SetEnvironmentVariable('MAIL_USER_PEDIDAI', 'correo@gmail.com', 'Machine')
[System.Environment]::SetEnvironmentVariable('MAIL_PASS_PEDIDAI', 'app_password', 'Machine')
[System.Environment]::SetEnvironmentVariable('JWT_SECRET', 'secreto_jwt_largo_y_seguro', 'Machine')
[System.Environment]::SetEnvironmentVariable('DEEPSEEK_API_KEY', 'tu_api_key', 'Machine')
```

**Linux/macOS** (añadir a `~/.bashrc` o `~/.zshrc`):

```bash
export DB_USER_PEDIDAI=pedidai_user
export DB_PASS_PEDIDAI=password_seguro
export MAIL_USER_PEDIDAI=correo@gmail.com
export MAIL_PASS_PEDIDAI=app_password
export JWT_SECRET=secreto_jwt_largo_y_seguro
export DEEPSEEK_API_KEY=tu_api_key
source ~/.bashrc
```

#### Compilar y ejecutar

```bash
cd pedidai-api

# Linux/macOS
./mvnw clean install
./mvnw spring-boot:run

# Windows
mvnw.cmd clean install
mvnw.cmd spring-boot:run
```

El backend estará disponible en: **<http://localhost:8085>**

Swagger UI: **<http://localhost:8085/swagger-ui.html>**

### 6.4. Instalación del Frontend

#### Instalar Node.js

**Windows/macOS:** descargar desde <https://nodejs.org/> (versión LTS 22.x)

**Linux:**

```bash
curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash -
sudo apt-get install -y nodejs
```

#### Instalar dependencias y configurar entorno

```bash
cd pedidai-app
npm install
```

Editar `src/environments/environment.ts` para desarrollo:

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8085/api'
};
```

#### Ejecutar en desarrollo

```bash
ng serve
```

El frontend estará disponible en: **<http://localhost:4200>**

#### Compilar para producción

```bash
ng build
```

Los ficheros compilados se generan en `dist/`.

### 6.5. Configuración de Correo (Gmail)

1. Acceder a <https://myaccount.google.com/security>
2. Activar **Verificación en 2 pasos**
3. Ir a <https://myaccount.google.com/apppasswords>
4. Crear una contraseña de aplicación (16 caracteres)
5. Usarla como valor de la variable `MAIL_PASS_PEDIDAI`

### 6.6. Scripts Disponibles

#### Frontend

| Acción       | Comando                               | Descripción                            |
| ------------ | ------------------------------------- | -------------------------------------- |
| Desarrollo   | `ng serve`                            | Servidor de desarrollo con hot reload  |
| Build prod   | `ng build`                            | Compilación optimizada para producción |
| Tests        | `ng test`                             | Tests unitarios con Vitest             |
| Lint         | `npm run lint`                        | Análisis de código con ESLint          |
| Format       | `npm run format`                      | Formateo con Prettier                  |

#### Backend

| Acción      | Comando                              | Descripción                      |
| ----------- | ------------------------------------ | -------------------------------- |
| Compilar    | `./mvnw clean install`               | Compila el proyecto              |
| Ejecutar    | `./mvnw spring-boot:run`             | Ejecuta la aplicación            |
| Tests       | `./mvnw test`                        | Ejecuta los tests                |
| Empaquetar  | `./mvnw clean package -DskipTests`   | Genera el JAR ejecutable         |

### 6.7. Despliegue en Producción

#### Backend — Ejecutar como servicio (Linux)

```bash
cd pedidai-api
./mvnw clean package -DskipTests
```

Crear `/etc/systemd/system/pedidai-api.service`:

```ini
[Unit]
Description=Pedidai API
After=syslog.target

[Service]
User=pedidai
ExecStart=/usr/bin/java -jar /opt/pedidai/pedidai-api.jar
SuccessExitStatus=143
Environment="DB_USER_PEDIDAI=pedidai_user"
Environment="DB_PASS_PEDIDAI=password_seguro"
Environment="MAIL_USER_PEDIDAI=correo@gmail.com"
Environment="MAIL_PASS_PEDIDAI=app_password"
Environment="JWT_SECRET=secreto_jwt"
Environment="DEEPSEEK_API_KEY=api_key"

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl enable pedidai-api
sudo systemctl start pedidai-api
```

#### Frontend — Apache con proxy inverso

Compilar:

```bash
cd pedidai-app
ng build
sudo cp -r dist/pedidai-app/browser/* /var/www/pedidai/
```

Configuración Apache (`/etc/apache2/sites-available/pedidai.conf`):

```apache
<VirtualHost *:80>
    ServerName pedidai.cat
    DocumentRoot /var/www/pedidai

    <Directory /var/www/pedidai>
        Options -Indexes +FollowSymLinks
        AllowOverride All
        Require all granted
        FallbackResource /index.html
    </Directory>

    ProxyPass /api http://localhost:8085/api
    ProxyPassReverse /api http://localhost:8085/api
</VirtualHost>
```

```bash
sudo a2ensite pedidai
sudo systemctl reload apache2
```

### 6.8. Verificación de la Instalación

#### Backend

```bash
# Comprobar que el servidor responde
curl http://localhost:8085/v3/api-docs

# Acceder a Swagger UI
# http://localhost:8085/swagger-ui.html
```

#### Frontend

Abrir en el navegador: <http://localhost:4200>

Verificar que se cargan la página de inicio, el formulario de login y el formulario de registro.

#### Base de Datos

```bash
mysql -u pedidai_user -p pedidai_db -e "SHOW TABLES;"
```

Resultado esperado: `companies`, `users`, `suppliers`, `products`, `orders`, `order_items`

### 6.9. Solución de Problemas Comunes

**Backend no arranca — `Access denied for user`:**
Verificar credenciales MySQL y variables de entorno `DB_USER_PEDIDAI` / `DB_PASS_PEDIDAI`.

**Backend no arranca — `Port 8085 is already in use`:**
Cambiar el puerto en `application.properties` o detener el proceso que ocupa el puerto.

**Frontend no conecta con el backend — errores CORS:**
Verificar que `environment.ts` apunta a `http://localhost:8085/api` y que el backend tiene CORS configurado para `http://localhost:4200`.

**Frontend — `Module not found`:**
Ejecutar `npm install` de nuevo.

**Errores de TypeScript:**
Verificar que las versiones de TypeScript y Angular son compatibles con `ng version`.

### 6.10. Recomendaciones de IDE

**Visual Studio Code** (frontend):

- Angular Language Service
- Tailwind CSS IntelliSense
- ESLint
- Prettier

**IntelliJ IDEA** (backend):

- Lombok Plugin
- Spring Boot Plugin
- Database Navigator

---

## APÉNDICE

### A. Contacto y Soporte

- **Email:** <devepsdev@gmail.com>
- **Issue Tracker:** [GitHub Issues](https://github.com/devepsdev/pedidai/issues)

### B. Licencia

Este proyecto es propiedad de DevEps. Todos los derechos reservados.

---

Última actualización: 13 de abril de 2026
