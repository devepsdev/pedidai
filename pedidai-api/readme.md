# PedidAI — API REST

API de [PedidAI](https://pedidai.es), la aplicación para bares y restaurantes que lee sus albaranes, compara lo que cobra cada proveedor y prepara los pedidos al más barato.

| | |
| --- | --- |
| **Framework** | Spring Boot 3.5 |
| **Lenguaje** | Java 25 |
| **Base de datos** | MySQL 8 (H2 en memoria para los tests) |
| **Autenticación** | JWT (HS512, 1 hora) |
| **Idiomas** | Castellano y catalán (`Accept-Language`) |
| **Documentación interactiva** | [pedidai.es/swagger-ui.html](https://pedidai.es/swagger-ui.html) |

---

## Funcionalidades

- **Multiempresa:** cada consulta se limita a la empresa del usuario autenticado.
- **Registro con prueba gratuita de 14 días**, verificación de email y recuperación de contraseña.
- **Lectura de albaranes:** OCR con Tesseract en el propio servidor y estructuración del texto con IA (Mistral AI por defecto, cualquier API compatible con OpenAI).
- **Historial y comparativa de precios** entre proveedores, con avisos de subidas.
- **Pedidos:** creación, edición, cancelación y envío al proveedor por email (la respuesta llega al cliente).
- **Análisis de consumo** para las sugerencias de pedido y el asistente de chat (`orderflow`).
- **Informes** por periodo con exportación a PDF.
- **Panel SUPER_ADMIN:** empresas, usuarios, estadísticas, ampliar la prueba y activar el plan de pago.
- **Tareas programadas:** avisos de la prueba al cliente (9:00), borrado de datos de pruebas no contratadas a los 30 días (03:30) y limpieza de los límites de uso.
- **Endpoint interno** para las alertas de n8n (solo accesible desde el propio servidor).

---

## Stack

| Área | Tecnología |
| --- | --- |
| Web y seguridad | Spring Web, Spring Security, JJWT |
| Datos | Spring Data JPA, Hibernate, MySQL Connector/J |
| Validación | Jakarta Bean Validation |
| Email | Spring Mail (SMTP) |
| PDF | OpenPDF, PDFBox (PDF de albaranes a imagen) |
| IA | Tesseract OCR (`spa` + `cat`) + API de chat compatible con OpenAI vía WebClient |
| Documentación | SpringDoc OpenAPI (Swagger UI) |
| Utilidades | Lombok |
| Tests | JUnit 5, Mockito, AssertJ, H2 |

---

## Estructura

```text
src/main/java/com/pedidai/api/
├── config/          # Idiomas (MessageSource), traducción de respuestas, Swagger, recursos web
├── controllers/     # Endpoints REST; Pages acota la paginación
├── dto/             # Entrada/salida de la API con validaciones
├── entities/        # Company, User, Supplier, Product, PriceHistory, Order, OrderItem
├── exceptions/      # Excepciones con clave de traducción y manejador global
├── repositories/    # Spring Data JPA y Specifications de filtros
├── security/        # JWT, filtro de autenticación, CurrentUser, límites de uso, IP del cliente
└── services/impl/   # Lógica de negocio y tareas programadas
src/main/resources/
├── application.properties   # Sin secretos: todo lo sensible llega por variables de entorno
└── i18n/                    # messages.properties (es) y messages_ca.properties (ca)
```

Capas: `Petición → JwtAuthenticationFilter → Controller → Service → Repository → MySQL`.

---

## Configuración

Variables de entorno (en producción, en `/opt/apps/pedidai/.env`):

| Variable | Obligatoria | Uso |
| --- | --- | --- |
| `DB_USER_PEDIDAI`, `DB_PASS_PEDIDAI` | Sí | Credenciales de MySQL |
| `SPRING_DATASOURCE_URL` | No | URL de la base de datos (por defecto `localhost:3306/pedidai_db`) |
| `MAIL_USER_PEDIDAI`, `MAIL_PASS_PEDIDAI` | Sí | Cuenta SMTP de envío |
| `JWT_SECRET` | Sí | Clave de firma de los tokens |
| `AI_API_KEY` | Para la IA | Clave de la API de IA (Mistral AI) |
| `AI_API_URL`, `AI_MODEL` | No | Por defecto `https://api.mistral.ai/v1` y `mistral-small-latest` |
| `CORS_ALLOWED_ORIGINS` | No | Orígenes permitidos (por defecto los dominios públicos y `localhost:4200`) |
| `SERVER_ADDRESS` | No | `127.0.0.1` en producción: la API solo escucha en local, detrás de nginx |
| `N8N_NEW_COMPANY_WEBHOOK` | No | Webhook de n8n para el aviso de registro nuevo (vacío = desactivado) |

Otras propiedades útiles al arrancar: `--app.frontend.url=http://localhost:4200`, `--app.ocr.tesseract-command=/ruta/a/tesseract` y, para depurar, `--logging.level.com.pedidai.api=DEBUG --spring.jpa.show-sql=true`.

La base de datos se crea con los scripts de [`pedidai-db`](../pedidai-db/readme.md) (`ddl-auto=none`).

---

## Ejecución

```bash
cd pedidai-api
./mvnw spring-boot:run              # http://localhost:8085 (Swagger en /swagger-ui.html)
./mvnw test                         # tests con H2, sin MySQL
./mvnw -DskipTests package          # jar en target/
```

---

## API

Todas las rutas empiezan por `/api`. Salvo las públicas, necesitan `Authorization: Bearer <token>`. Las respuestas siguen el formato `{ success, message, data, timestamp }`, con `message` traducido según `Accept-Language` (`es` o `ca`). Los listados paginados aceptan `page`, `size` (máximo 200), `sortBy` y `sortDir`.

### Autenticación — `/api/auth` (públicos)

| Método | Ruta | Descripción |
| --- | --- | --- |
| POST | `/login` | Inicia sesión y devuelve el token |
| POST | `/verify-email` | Verifica el email con el token recibido |
| POST | `/resend-verification` | Reenvía el email de verificación |
| POST | `/forgot-password` | Envía el enlace de recuperación (misma respuesta exista o no la cuenta) |
| POST | `/reset-password` | Cambia la contraseña con el token |

### Empresas — `/api/companies`

| Método | Ruta | Descripción |
| --- | --- | --- |
| POST | `/register` | Alta de empresa y administrador; empieza la prueba de 14 días y devuelve la sesión (público) |
| GET | `/` | Datos de la empresa (ADMIN) |
| PUT | `/` | Actualiza los datos de la empresa (ADMIN) |
| GET | `/my-plan` | Plan y fin de la prueba |

```json
POST /api/companies/register
{
  "companyName": "Bar La Plaza",
  "adminFirstName": "Laura",
  "adminEmail": "laura@barlaplaza.es",
  "adminPassword": "Plaza2026",
  "acceptTerms": true
}
```

Opcionales: `adminLastName`, `taxId`, `companyPhone`, `companyAddress`, `companyCity`, `companyPostalCode`.

### Usuarios — `/api/users`

| Método | Ruta | Descripción |
| --- | --- | --- |
| GET | `/me` | Usuario de la sesión |
| PATCH | `/me/language` | Idioma del usuario (`es`/`ca`) para emails y PDF |
| POST | `/me/resend-verification` | Reenvía la verificación del propio email |
| PATCH | `/{uuid}/change-password` | Cambia la contraseña (solo la propia) |
| GET, POST | `/` | Listar y crear usuarios de la empresa (ADMIN) |
| GET, PUT, DELETE | `/{uuid}` | Ver, editar y borrar (baja lógica con email anonimizado) (ADMIN) |
| PATCH | `/{uuid}/status` | Activar o desactivar (ADMIN) |
| GET | `/search`, `/filter` | Búsquedas (ADMIN) |

### Proveedores — `/api/suppliers` y productos — `/api/products`

| Método | Ruta | Descripción |
| --- | --- | --- |
| GET, POST | `/api/suppliers` | Listar y crear proveedores |
| GET, PUT | `/api/suppliers/{uuid}` | Ver y editar |
| PATCH | `/api/suppliers/{uuid}/status` | Activar o desactivar |
| GET | `/api/suppliers/search`, `/filter` | Búsquedas |
| GET | `/api/products`, `/search`, `/filter` | Listar y buscar productos |
| POST | `/api/products/create` | Crear producto (el precio se guarda en el historial) |
| GET, PUT | `/api/products/{uuid}` | Ver y editar (sin `imageUrl` se conserva la imagen) |
| PATCH | `/api/products/deactivate/{uuid}` | Baja lógica |
| POST | `/api/products/upload/{uuid}` | Sube la imagen (campo `image`; JPEG, PNG o WebP, máx. 5 MB) |
| GET | `/api/products/compare-prices?productName=` | Comparativa de un producto entre proveedores |

### Albaranes y precios

| Método | Ruta | Descripción |
| --- | --- | --- |
| POST | `/api/invoices/scan` | Lee un albarán o factura (imagen o PDF) y devuelve las líneas para revisar (40 al día por empresa) |
| POST | `/api/invoices/confirm` | Guarda las líneas revisadas: productos e historial de precios |
| GET | `/api/prices/overview?days=` | Comparativa, subidas de precio y estimación de lo pagado de más |

### Pedidos — `/api/orders`

| Método | Ruta | Descripción |
| --- | --- | --- |
| POST | `/create` | Crea un pedido pendiente |
| GET | `/filter` (o `/list`) | Lista con filtros |
| GET | `/{uuid}` | Detalle |
| PUT | `/update/{uuid}` | Edita un pedido pendiente |
| POST | `/{uuid}/send` | Envía el pedido al proveedor por email (requiere email verificado) |
| PATCH | `/{uuid}/cancel` | Cancela un pedido pendiente |
| PATCH | `/delete/{uuid}` | Baja lógica |
| GET | `/consumption-analysis?days=` | Consumo por producto (base de las sugerencias) |

Estados en uso: `PENDING` → `SENT`, o `PENDING` → `CANCELLED`. `CONFIRMED`, `REJECTED` y `COMPLETED` están reservados.

### Informes — `/api/reports`

| Método | Ruta | Descripción |
| --- | --- | --- |
| GET | `/dashboard` | Resumen del panel de inicio (todos los usuarios) |
| GET | `/global`, `/global/pdf` | Informe por periodo en JSON o PDF (ADMIN) |

### Plataforma

| Método | Ruta | Descripción |
| --- | --- | --- |
| GET | `/api/superadmin/dashboard`, `/companies`, `/companies/{uuid}`, `/users`, `/stats/monthly` | Panel SUPER_ADMIN |
| PATCH | `/api/superadmin/companies/{uuid}/status` | Cambia el estado (`ACTIVE`, `INACTIVE`, `PENDING`, `SUSPENDED`) |
| PATCH | `/api/superadmin/companies/{uuid}/extend-trial` | `{ "days": 14 }`: suma días al final de la prueba (no en clientes de pago ni en la plataforma) |
| PATCH | `/api/superadmin/companies/{uuid}/activate` | Pasa a cliente de pago |
| GET | `/api/internal/daily-summary?hours=24` | Resumen para n8n (solo peticiones locales que no pasan por nginx) |

---

## Seguridad

- **Roles:** `USER` (pedidos), `ADMIN` (además informes, empresa y usuarios) y `SUPER_ADMIN` (plataforma). Nadie puede asignarse un rol superior.
- **JWT** con el email y el rol; en cada petición se comprueba en la base de datos que el usuario y la empresa siguen activos y que la prueba no ha terminado.
- **Contraseñas:** mínimo 8 caracteres con alguna letra y algún número, guardadas con BCrypt.
- **Límites de uso:** 8 intentos de login fallidos por cuenta y 40 por IP cada 15 minutos, 5 registros por IP y hora, recuperación de contraseña y lecturas de albaranes por día. La IP se toma de `X-Real-IP` (la fija nginx).
- **Errores** sin detalles internos (400, 401, 403, 404, 405, 409, 415, 429 y 500) y mensajes que no revelan si un email existe.
- **Emails** con el contenido del usuario escapado; los logs enmascaran las direcciones.
- **Imágenes** validadas por contenido y guardadas con nombre aleatorio.

---

## Emails

| Email | Cuándo |
| --- | --- |
| Bienvenida con verificación | Al registrarse (enlace válido 48 h) |
| Verificación | Usuarios nuevos del equipo o al pedir otro enlace |
| Recuperación de contraseña | Al pedirla (enlace válido 1 h) |
| Pedido al proveedor | Al enviar un pedido (respuesta al email del cliente) |
| La prueba acaba en 3 días | Tarea de las 9:00, con la actividad de la prueba |
| La prueba ha terminado | Tarea de las 9:00, con la fecha de borrado de los datos |

Todos se envían en el idioma del usuario.

---

## Autores

- **Daniel Garcia** — Backend Developer
- **Enrique Pérez** — Full Stack Developer

Contacto: [hola@pedidai.es](mailto:hola@pedidai.es) · [pedidai.es](https://pedidai.es)
