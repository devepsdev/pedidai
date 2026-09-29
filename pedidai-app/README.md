# PedidAI — Frontend

Aplicación web de [PedidAI](https://pedidai.es) para **bares y restaurantes**: lectura de albaranes, comparativa de precios entre proveedores, pedidos por chat y sugerencias de pedido. Incluye la web pública (landing, legales) y la zona privada.

| | |
| --- | --- |
| **Framework** | Angular 21 (componentes standalone, *signals*, sin zone.js) |
| **Lenguaje** | TypeScript 5.9 |
| **Estilos** | Tailwind CSS 4 |
| **Idiomas** | Castellano y catalán (ngx-translate 17) |
| **Tests** | Vitest 4 |

---

## Funcionalidades

### Web pública

- **Landing** para campañas: propuesta de ahorro, cómo funciona, calculadora de ahorro de ejemplo, precio (39 €/mes, lanzamiento 29 €/mes), preguntas frecuentes y un único botón «Pruébalo gratis 14 días». En pantallas grandes cada frase ocupa su propia línea (pipe `lines`).
- **Registro rápido** (negocio, nombre, email y contraseña) que deja la sesión iniciada; login, verificación de email y recuperación de contraseña.
- **Sobre nosotros**, **contacto** y páginas **legales** bilingües: privacidad, cookies, términos y aviso legal.
- **Aviso de cookies** con Consent Mode v2 de Google: aceptar, rechazar o configurar por categorías; Analytics y Ads solo se cargan con consentimiento. Se puede reabrir desde el pie de página.

### Zona privada

| Ruta | Pantalla |
| --- | --- |
| `/dashboard` | Inicio: avisos de verificación de email y de fin de prueba, primeros pasos, resumen de precios, pedidos y gasto del mes, últimos pedidos y sugerencias de pedido |
| `/invoices/scan` | Leer albarán: foto o PDF, revisión y corrección línea a línea, alta de proveedor nuevo |
| `/prices` | Mis precios: comparativa del mismo producto entre proveedores, subidas de precio y estimación de lo pagado de más |
| `/ai` | Pedir por chat: el asistente prepara pedidos pendientes al proveedor más barato |
| `/ai/suggestions` | Sugerencias de pedido: productos habituales con urgencia, cantidad y proveedor más barato; crean un pedido pendiente con un clic |
| `/orders` | Pedidos: listado con filtros, creación, edición, detalle y envío al proveedor |
| `/suppliers`, `/products` | Proveedores y catálogo, con búsqueda y filtros |
| `/users` | Usuarios del equipo (administrador) |
| `/reports` | Informes por periodo con exportación a PDF |
| `/company` | Datos de la empresa y plan |
| `/superadmin/…` | Panel de la plataforma (solo SUPER_ADMIN): empresas, usuarios, estadísticas, ampliar la prueba 14 días o activar el plan de pago |

Todas las rutas se cargan bajo demanda (`loadComponent`). Las rutas desconocidas redirigen a la landing.

---

## Arquitectura

```text
Componente ──► Servicio (services/) ──► HttpClient ──► jwt-interceptor ──► API (/api) o asistente (/ai)
                                                        error-interceptor (401 → cierra sesión y va a /login)
```

- **Estado:** *signals* en los componentes; token JWT y usuario en `localStorage`.
- **Interceptores:** `jwt-interceptor` añade `Authorization: Bearer <token>` y `Accept-Language` (`es`/`ca`) a las peticiones a la API y al asistente; `error-interceptor` cierra la sesión si la API responde 401.
- **Guards:** `auth-guard` protege la zona privada y `super-admin.guard` el panel de la plataforma.
- **Idioma:** `LanguageService` guarda el idioma elegido, cambia las traducciones y lo sincroniza con el perfil del usuario (`PATCH /api/users/me/language`), para que emails y PDF lleguen en el mismo idioma.
- **Analítica:** `AnalyticsService` aplica el consentimiento de cookies y, al registrarse un usuario, envía el evento `sign_up` a Analytics y la conversión a Google Ads, cada uno solo si se ha aceptado su categoría.
- **Textos en varias líneas:** el pipe `lines` parte las traducciones por `\n` para mostrar frases completas por línea en pantallas grandes.

---

## Estructura

```text
pedidai-app/
├── public/
│   ├── i18n/                    # es.json y ca.json
│   └── …                        # logos, iconos y manifest
├── src/
│   ├── index.html
│   ├── styles.css               # Tailwind y estilos globales
│   ├── environments/            # environment.ts (local) y environment.prod.ts
│   └── app/
│       ├── app.routes.ts        # Rutas (lazy loading)
│       ├── app.config.ts        # Zoneless, router, HttpClient con interceptores, traducciones
│       ├── components/
│       │   ├── ai-chat/         # Chat de pedidos y sugerencias de pedido
│       │   ├── auth/            # Login, registro, verificación, recuperación de contraseña
│       │   ├── company/         # Datos de la empresa
│       │   ├── dashboard/       # Inicio
│       │   ├── invoices/        # Lectura de albaranes
│       │   ├── orders/          # Pedidos
│       │   ├── prices/          # Mis precios
│       │   ├── products/        # Catálogo
│       │   ├── reports/         # Informes y PDF
│       │   ├── suppliers/       # Proveedores
│       │   └── users/           # Usuarios del equipo
│       ├── guards/              # auth-guard, super-admin.guard
│       ├── interceptors/        # jwt-interceptor, error-interceptor
│       ├── layouts/             # public-layout y private-layout
│       ├── models/              # Interfaces TypeScript
│       ├── pages/               # Landing, sobre nosotros, contacto, legales, superadmin
│       ├── services/            # API, auth, pedidos, precios, albaranes, asistente, idioma, analítica…
│       └── shared/              # Sidebar, navbar, footer, alertas, modales, paginación,
│                                # aviso de cookies, pipe lines, utilidades de formato
├── proxy.conf.json              # /ai → asistente en 127.0.0.1:3201 (ng serve)
└── angular.json
```

---

## Configuración

`src/environments/environment.ts` (desarrollo):

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8085/api',
  aiUrl: '/ai',
  analytics: { gaId: '', adsId: '', adsSignupLabel: '' },
};
```

`src/environments/environment.prod.ts` (producción) usa `https://pedidai.es/api`, `/ai` y el identificador de Google Analytics. `adsId` y `adsSignupLabel` se rellenan cuando exista la campaña de Google Ads; vacíos, no se carga la etiqueta de Ads.

En desarrollo, `ng serve` usa `proxy.conf.json` para redirigir `/ai` al asistente (`orderflow/assistant`) en `127.0.0.1:3201`.

---

## Desarrollo

Requisitos: Node.js 22 y npm 10, con la API de PedidAI en `http://localhost:8085` (y, opcionalmente, el asistente en `127.0.0.1:3201`).

```bash
cd pedidai-app
npm install
npx ng serve                   # http://localhost:4200
```

| Comando | Descripción |
| --- | --- |
| `npx ng serve` | Servidor de desarrollo con recarga automática y proxy de `/ai` |
| `npx ng build` | Build de producción en `dist/pedidai-app/browser` |
| `npm run watch` | Build de desarrollo en modo observación |
| `npx ng test --watch=false` | Tests unitarios con Vitest |

---

## Traducciones

- Todos los textos están en `public/i18n/es.json` y `public/i18n/ca.json`, con las mismas claves.
- Los errores de la API llegan ya traducidos (la API responde en el idioma de `Accept-Language`); las claves `ERRORS.*` cubren los errores propios del frontend.
- Para una frase por línea en pantallas grandes, separa las frases con `\n` en la traducción y usa el pipe `lines` en la plantilla.

---

## Despliegue

```bash
npx ng build
```

El contenido de `dist/pedidai-app/browser/` se copia a `/var/www/pedidai.es` en el servidor (sin borrar `.well-known/`). nginx sirve `index.html` y `/i18n/` con `Cache-Control: no-cache`, de modo que los cambios de textos se ven sin forzar la recarga; los JS y CSS llevan hash en el nombre.

---

## Recursos

- [Documentación de Angular](https://angular.dev/)
- [Tailwind CSS](https://tailwindcss.com/docs)
- [ngx-translate](https://github.com/ngx-translate/core)
- [API de PedidAI (Swagger)](https://pedidai.es/swagger-ui.html)
