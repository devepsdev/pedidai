# PedidAI

**Descubre cuánto pagas de más a tus proveedores.**

PedidAI es una aplicación web para **bares y restaurantes** (de 1 a 5 locales) que hacen sus propias compras:

1. **Haces una foto a tus albaranes** y PedidAI lee productos, cantidades y precios.
2. **Compara lo que te cobra cada proveedor** por el mismo producto y te avisa cuando te suben un precio.
3. **Pides por chat** («10 kg de tomates y 5 garrafas de agua»): el asistente prepara el pedido al proveedor más barato y tú lo envías por email con un clic.

- **Web:** [pedidai.es](https://pedidai.es/)
- **API (Swagger):** [pedidai.es/swagger-ui.html](https://pedidai.es/swagger-ui.html)
- **Repositorios:** [devepsdev/pedidai](https://github.com/devepsdev/pedidai) (web, API y base de datos) y [devepsdev/orderflow](https://github.com/devepsdev/orderflow) (asistente de pedidos por chat)

**Versión:** 2.0.0 · **Última actualización:** 27 de septiembre de 2026

---

## Índice

- [PedidAI](#pedidai)
  - [Índice](#índice)
  - [1. Arquitectura](#1-arquitectura)
  - [2. Funcionalidades](#2-funcionalidades)
    - [Para el cliente (bar o restaurante)](#para-el-cliente-bar-o-restaurante)
    - [Para la plataforma](#para-la-plataforma)
  - [3. Tecnologías](#3-tecnologías)
  - [4. Estructura del código](#4-estructura-del-código)
    - [4.1. Frontend (`pedidai-app/src/app`)](#41-frontend-pedidai-appsrcapp)
    - [4.2. Backend (`pedidai-api/src/main/java/com/pedidai/api`)](#42-backend-pedidai-apisrcmainjavacompedidaiapi)
  - [5. API REST](#5-api-rest)
  - [6. Base de datos](#6-base-de-datos)
  - [7. Seguridad y privacidad](#7-seguridad-y-privacidad)
  - [8. Instalación en local](#8-instalación-en-local)
    - [Requisitos](#requisitos)
    - [8.1. Base de datos](#81-base-de-datos)
    - [8.2. Backend](#82-backend)
    - [8.3. Frontend](#83-frontend)
    - [8.4. Asistente (opcional)](#84-asistente-opcional)
  - [9. Despliegue en producción](#9-despliegue-en-producción)
    - [Pasos de una actualización](#pasos-de-una-actualización)
  - [10. Solución de problemas](#10-solución-de-problemas)

---

## 1. Arquitectura

```text
                        Navegador (Angular)
                               │  https://pedidai.es
                               ▼
                    ┌─────────────────────┐
                    │        nginx        │
                    └──┬───────┬───────┬──┘
          páginas web  │  /api │   /ai │
                       ▼       ▼       ▼
   /var/www/pedidai.es     Spring Boot      Asistente (Docker, Node.js)
   (ficheros de Angular)   :8085            :3201 ──► DeepSeek
                            │  ▲               │
                            │  └───────────────┘  REST con el token
                            ▼                     del propio usuario
                          MySQL

   n8n (Docker, :5678, /n8n/)
```

| Componente | Responsabilidad |
| --- | --- |
| `pedidai-app` (Angular) | Interfaz web. Se sirve como ficheros estáticos y consume la API; el interceptor añade el JWT y el idioma (`Accept-Language`). |
| nginx | HTTPS y proxy inverso: `/` → Angular, `/api` → Spring Boot, `/ai/` → asistente, `/n8n/` → n8n. |
| `pedidai-api` (Spring Boot) | Lógica de negocio, autenticación JWT, aislamiento por empresa, emails (SMTP), PDF y lectura de albaranes (Tesseract + DeepSeek). Único acceso a MySQL. |
| MySQL | Persistencia (`pedidai_db`). |
| `orderflow/mcp-server` | Asistente de pedidos por chat: Express + *function calling* de DeepSeek. Cada herramienta llama a la API con el token del usuario, por lo que tiene sus mismos permisos. Crea pedidos en estado `PENDING`; el envío siempre lo confirma el usuario desde la web. Sigue el enfoque de MCP, pero no usa el SDK oficial. |
| Docker | Ejecuta el asistente y n8n (`docker compose` en `orderflow`). |
| n8n | Automatización de flujos. |

## 2. Funcionalidades

### Para el cliente (bar o restaurante)

- **Registro en 1 minuto** (nombre del negocio, nombre, email y contraseña), sin tarjeta. Empieza una **prueba gratuita de 14 días**.
- **Verificación de email** necesaria para enviar pedidos a proveedores.
- **Lectura de albaranes y facturas** (foto o PDF) con OCR + IA, con revisión línea a línea antes de guardar.
- **Mis precios:** comparativa del mismo producto entre proveedores, avisos de subidas (≥ 2 %) y estimación de lo pagado de más.
- **Asistente IA por chat:** prepara pedidos al proveedor más barato y sugiere pedidos según el consumo.
- **Pedidos:** creación manual o por chat, envío al proveedor **por email** (la respuesta del proveedor llega al email del cliente) y seguimiento de estado (`PENDING → SENT → CONFIRMED / REJECTED / COMPLETED / CANCELLED`).
- **Proveedores, productos y usuarios** del equipo, con búsqueda y filtros.
- **Informes** por periodo con exportación a PDF.
- **Bilingüe castellano/catalán en todo:** pantallas, errores de la API, emails y PDF, según el idioma de cada usuario.

### Para la plataforma

- **Panel SUPER_ADMIN:** empresas, usuarios, estadísticas; ampliar la prueba o activar el plan de pago.
- **Fin de la prueba:** al acabar, la cuenta queda inactiva hasta que se contrata (no se cobra nada automáticamente).
- **Borrado automático:** cada día a las 03:30 (hora de Madrid) se eliminan las empresas cuya prueba terminó hace **más de 30 días** sin contratar, con todos sus datos e imágenes. Nunca se borran clientes de pago ni la cuenta SUPER_ADMIN (`DataRetentionServiceImpl`).
- **Landing para campañas:** precio visible (39 €/mes, lanzamiento 29 €/mes), calculadora de ahorro de ejemplo, FAQ y un único botón «Pruébalo gratis 14 días».
- **Cookies con consentimiento** (Consent Mode v2 de Google): Analytics y Ads solo se cargan si el usuario acepta.
- **Páginas legales** bilingües: privacidad, cookies, términos y aviso legal.

---

## 3. Tecnologías

| Capa | Tecnología |
| --- | --- |
| Frontend | Angular 21 (standalone, *signals*, sin zone.js), TypeScript 5.9, Tailwind CSS 4, ngx-translate 17, Vitest |
| Backend | Java 25, Spring Boot 3.5 (Web, Data JPA, Security, Validation, Mail), JJWT, OpenPDF, SpringDoc (Swagger) |
| Base de datos | MySQL 8 (InnoDB, `utf8mb4`); H2 en memoria para los tests |
| IA | Tesseract OCR (castellano + catalán) en el servidor; DeepSeek para estructurar albaranes y para el chat |
| Asistente | Node.js 20 + Express 5 (`orderflow/mcp-server`), en Docker |
| Automatización | n8n en Docker (instalado, sin flujos en uso) |
| Servidor | VPS Ubuntu, nginx (HTTPS con Let's Encrypt), systemd, ufw |

---

## 4. Estructura del código

```text
pedidai/
├── pedidai-app/          # Frontend Angular
├── pedidai-api/          # Backend Spring Boot
└── pedidai-db/           # Esquema SQL, migraciones y documentación de tablas

orderflow/                # Repositorio aparte
├── mcp-server/           # Asistente de chat (Express + DeepSeek + herramientas)
├── n8n-workflows/        # Flujos de ejemplo para n8n (no importados en producción)
└── docker-compose.yml    # Levanta el asistente y n8n
```

### 4.1. Frontend (`pedidai-app/src/app`)

```text
app.routes.ts            # Rutas; cada página se carga bajo demanda (lazy loading)
app.config.ts            # HttpClient, router, traducciones
interceptors/            # jwt-interceptor: añade el token y el idioma (Accept-Language)
guards/                  # Protegen la zona privada y el panel SUPER_ADMIN
services/                # Llamadas HTTP a la API y al asistente; idioma; analítica
layouts/                 # public-layout (web pública) y private-layout (aplicación)
pages/                   # Landing, sobre nosotros, contacto, legales, superadmin
components/              # Pantallas de la aplicación: dashboard, prices, invoices,
                         # ai-chat, orders, suppliers, products, users, reports, company, auth
shared/                  # Sidebar, alertas, modales, paginación, aviso de cookies,
                         # pipe `lines` (una frase por línea en pantallas grandes)
public/i18n/             # es.json y ca.json
```

Rutas principales:

| Zona | Rutas |
| --- | --- |
| Pública | `/`, `/login`, `/register`, `/verify-email`, `/recover-password`, `/reset-password`, `/sobre-nosotros`, `/contacto`, `/privacidad`, `/cookies`, `/terminos`, `/aviso-legal` |
| Privada | `/dashboard`, `/prices`, `/invoices/scan`, `/ai`, `/ai/suggestions`, `/orders`, `/suppliers`, `/products`, `/users`, `/reports`, `/company` |
| SUPER_ADMIN | `/superadmin`, `/superadmin/companies`, `/superadmin/users` |

### 4.2. Backend (`pedidai-api/src/main/java/com/pedidai/api`)

```text
controllers/    # Endpoints REST (ver sección 5)
services/impl/  # Lógica de negocio; cada consulta se limita a la empresa del usuario
repositories/   # Acceso a datos (Spring Data JPA)
entities/       # Company, User, Supplier, Product, PriceHistory, Order, OrderItem
dto/            # Objetos de entrada/salida de la API (con validaciones)
security/       # JWT, filtro de autenticación, CurrentUser, límites de uso, política de contraseñas
config/         # Idiomas (MessageSource), traducción de respuestas, Swagger, recursos web
exceptions/     # Errores con clave de traducción y manejador global (sin detalles internos)
resources/i18n/ # messages.properties (castellano) y messages_ca.properties (catalán)
```

Capas: `Petición HTTP → JwtAuthenticationFilter → Controller → Service → Repository → MySQL`.

---

## 5. API REST

Todas las rutas empiezan por `/api` y, salvo las marcadas como públicas, necesitan la cabecera `Authorization: Bearer <token>`. Las respuestas siguen el formato `{ success, message, data }`, con el mensaje en el idioma de la cabecera `Accept-Language` (`es` o `ca`). Documentación interactiva en `/swagger-ui.html`.

| Recurso | Endpoints |
| --- | --- |
| `/auth` (públicos) | `POST /login`, `/forgot-password`, `/reset-password`, `/verify-email`, `/resend-verification` |
| `/companies` | `POST /register` (público), `GET /`, `PUT /`, `GET /my-plan` |
| `/users` | `GET /me`, `PATCH /me/language`, `POST /me/resend-verification`, CRUD (`GET`, `POST`, `PUT /{uuid}`, `PATCH /{uuid}/status`, `PATCH /{uuid}/change-password`, `DELETE /{uuid}`), `GET /search`, `GET /filter` |
| `/suppliers` | CRUD, `GET /search`, `GET /filter`, `PATCH /{uuid}/status` |
| `/products` | `POST /create`, `GET /{uuid}`, `PUT /{uuid}`, `PATCH /deactivate/{uuid}`, `GET /search`, `GET /filter`, `GET /compare-prices`, `POST /upload/{uuid}`, `POST /upload-temp` |
| `/invoices` | `POST /scan` (imagen o PDF), `POST /confirm` |
| `/prices` | `GET /overview?days=` — comparativa, alertas y estimación de ahorro |
| `/orders` | `POST /create`, `GET /` (filtros), `GET /{uuid}`, `PUT /update/{uuid}`, `POST /{uuid}/send`, `PATCH /delete/{uuid}`, `GET /consumption-analysis` |
| `/reports` | `GET /dashboard`, `GET /global`, `GET /global/pdf` |
| `/superadmin` | `GET /dashboard`, `/companies`, `/companies/{uuid}`, `/users`, `/stats/monthly`; `PATCH /companies/{uuid}/status`, `/extend-trial`, `/activate` |

Asistente (`/ai`, servido por `orderflow`): `GET /health`, `POST /process-order`, `POST /suggest-orders`. Todos menos `/health` exigen el token del usuario.

---

## 6. Base de datos

Tablas: `companies`, `users`, `suppliers`, `products`, `price_history`, `orders`, `order_items`.

```text
companies ─┬─< users
           ├─< suppliers ─< products ─< price_history
           ├─< price_history
           └─< orders ─< order_items >─ products
```

- Cada tabla tiene un `id` interno y un `uuid` público (la API nunca expone los `id`).
- `companies.trial_ends_at`: fin de la prueba (`NULL` = cliente de pago).
- `users.language`: idioma del usuario (`es`/`ca`) para emails y PDF.
- `products.canonical_name`: nombre genérico para comparar el mismo producto entre proveedores.
- `price_history`: cada precio leído de un albarán (o introducido a mano), con su fecha.
- El esquema lo gestionan los scripts (`ddl-auto=none`): `pedidai-db/pedidai_db_schema.sql` y, en orden, las migraciones de `pedidai-db/migrations/`.

Detalle de columnas en [`pedidai-db/readme.md`](pedidai-db/readme.md).

---

## 7. Seguridad y privacidad

- **Aislamiento por empresa:** cada consulta se filtra por la empresa del usuario autenticado (`CurrentUser`); no se puede leer ni modificar nada de otra empresa.
- **Roles:** `USER`, `ADMIN` (gestiona su empresa) y `SUPER_ADMIN` (plataforma). Nadie puede asignarse un rol superior al suyo.
- **JWT de 1 hora**; en cada petición se vuelve a comprobar en la base de datos que el usuario y la empresa siguen activos.
- **Límites de uso:** intentos de login, registros por IP, recuperación de contraseña y lecturas de albaranes por día.
- **Contraseñas:** mínimo 8 caracteres con letras y números, guardadas con BCrypt.
- **Errores sin detalles internos** y mensajes que no revelan si un email existe.
- **Emails a proveedores** sin HTML inyectable; imágenes validadas por contenido y guardadas con nombre aleatorio.
- **IA:** a DeepSeek solo se envía el texto necesario (el OCR se hace en nuestro servidor). Está explicado en la política de privacidad.
- **Logs** en nivel INFO, sin consultas SQL y con los emails enmascarados.
- **Conservación:** datos de pruebas no contratadas borrados automáticamente a los 30 días.

---

## 8. Instalación en local

### Requisitos

| Componente | Versión |
| --- | --- |
| Java JDK | 25 |
| Node.js / npm | 22 / 10 |
| MySQL | 8 (o Docker) |
| Tesseract OCR | 5, con los idiomas `spa` y `cat` |
| Docker | Opcional, para el asistente y n8n |

### 8.1. Base de datos

```sql
CREATE USER 'pedidai_user'@'localhost' IDENTIFIED BY 'password_seguro';
CREATE DATABASE pedidai_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON pedidai_db.* TO 'pedidai_user'@'localhost';
```

```bash
mysql -u pedidai_user -p pedidai_db < pedidai-db/pedidai_db_schema.sql
mysql -u pedidai_user -p pedidai_db < pedidai-db/migrations/001_lanzamiento.sql
mysql -u pedidai_user -p pedidai_db < pedidai-db/migrations/002_empresa_suspendida.sql
```

### 8.2. Backend

Variables de entorno necesarias (nunca las subas al repositorio):

```bash
export DB_USER_PEDIDAI=pedidai_user
export DB_PASS_PEDIDAI=password_seguro
export MAIL_USER_PEDIDAI=correo@ejemplo.com
export MAIL_PASS_PEDIDAI=contraseña_de_aplicación
export JWT_SECRET=un_secreto_largo_y_aleatorio
export DEEPSEEK_API_KEY=tu_api_key
```

```bash
cd pedidai-api
./mvnw spring-boot:run          # http://localhost:8085  (Swagger en /swagger-ui.html)
./mvnw test                     # tests (usan H2, no necesitan MySQL)
```

Opciones útiles al arrancar: `--app.frontend.url=http://localhost:4200`, `--app.ocr.tesseract-command=/ruta/a/tesseract` y, para depurar, `--logging.level.com.pedidai.api=DEBUG --spring.jpa.show-sql=true`.

### 8.3. Frontend

```bash
cd pedidai-app
npm install
npx ng serve                    # http://localhost:4200
npx ng test --watch=false       # tests
npx ng build                    # producción → dist/pedidai-app/browser
```

`src/environments/environment.ts` apunta a `http://localhost:8085/api`. `proxy.conf.json` redirige `/ai` al asistente en `127.0.0.1:3201`.

### 8.4. Asistente (opcional)

```bash
cd orderflow/mcp-server
cp .env.example .env            # PEDIDAI_API_URL=http://localhost:8085/api y DEEPSEEK_API_KEY
npm install && npm start        # http://127.0.0.1:3201/health
```

---

## 9. Despliegue en producción

Todo corre en un VPS Ubuntu:

| Pieza | Dónde |
| --- | --- |
| Web (Angular) | `/var/www/pedidai.es`, servida por nginx |
| API (Spring Boot) | `/opt/apps/pedidai/pedidai-api/target/*.jar`, servicio `pedidai-api.service` (puerto 8085, solo local) |
| Secretos de la API | `/opt/apps/pedidai/.env` (permisos `600`, cargado con `EnvironmentFile=`) |
| Imágenes de productos | `/opt/apps/pedidai/pedidai-api/img/productes/` |
| Asistente y n8n | `/opt/apps/orderflow`, con `docker compose` (asistente en `127.0.0.1:3201`) |
| Base de datos | MySQL local, `pedidai_db` |

nginx publica `/` (Angular, con `index.html` y `/i18n/` en `no-cache`), `/api` → `localhost:8085`, `/ai/` → `localhost:3201/` y `/n8n/` → `localhost:5678/`. El cortafuegos (`ufw`) solo deja pasar HTTP/HTTPS y SSH.

### Pasos de una actualización

1. **Copia de seguridad** de la base de datos (`mysqldump`), del jar y de la web.
2. **Migraciones nuevas** de `pedidai-db/migrations/`, si las hay.
3. **API:** `./mvnw -DskipTests package`, copiar el jar a `target/` y `sudo systemctl restart pedidai-api`.
4. **Web:** `npx ng build` y copiar `dist/pedidai-app/browser/` a `/var/www/pedidai.es` (sin borrar `.well-known/`).
5. **Asistente:** en `/opt/apps/orderflow`, `git fetch && git merge --ff-only origin/main` y `sudo docker compose up -d --build mcp-bridge`.
6. **Comprobar:** `https://pedidai.es/api/users/me` debe responder 401 sin token y `https://pedidai.es/ai/health` `{"status":"ok"}`.

---

## 10. Solución de problemas

| Síntoma | Causa probable |
| --- | --- |
| La API no arranca: `Access denied for user` | Credenciales de MySQL o variables `DB_*` incorrectas. |
| `Port 8085 is already in use` | Ya hay otra instancia de la API en marcha. |
| La lectura de albaranes falla | Tesseract no instalado o sin los idiomas `spa`/`cat`; revisa `app.ocr.tesseract-command`. |
| El chat responde 401 | Sesión caducada (el token dura 1 hora): vuelve a entrar. |
| Tras desplegar se ven textos antiguos | Caché del navegador: recarga forzando (Ctrl+F5). En producción `index.html` e `/i18n/` ya se sirven con `no-cache`. |
| No llegan los emails | Revisa `MAIL_USER_PEDIDAI` / `MAIL_PASS_PEDIDAI` (contraseña de aplicación) y los logs: `journalctl -u pedidai-api`. |

---

Proyecto de DevEps. Todos los derechos reservados. Contacto: [hola@pedidai.es](mailto:hola@pedidai.es) · [GitHub Issues](https://github.com/devepsdev/pedidai/issues)
