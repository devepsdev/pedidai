# PedidAI — contexto para Claude Code

Plataforma de compras para bares y restaurantes (https://pedidai.es): leer albaranes y tarifas, comparar
proveedores, historial de precios y pedidos por chat. Castellano y catalán. Repo `devepsdev/pedidai`. El detalle
completo está en [`readme.md`](readme.md), las reglas generales en `~/.claude/CLAUDE.md` y el contexto de todos los
proyectos en `../CLAUDE.md`.

## Stack y estructura

| Carpeta | Qué es |
|---|---|
| `pedidai-api/` | Java 25, Spring Boot 3.5 (Web, JPA, Security, Validation, Mail), JWT, OpenPDF, Swagger. Lectura de albaranes con Tesseract (spa + cat) y Mistral. Código en `src/main/java/com/pedidai/api/` (`controllers`, `services/impl`, `repositories`, `entities`, `dto`, `security`, `config`, `exceptions`); textos en `resources/i18n/` |
| `pedidai-app/` | Angular 21 (standalone, signals, sin zone.js), Tailwind 4, ngx-translate (`public/i18n/es.json` y `ca.json`), Vitest. Páginas públicas prerenderizadas (SSR estático) |
| `pedidai-db/` | `pedidai_db_schema.sql` y `migrations/` (001 a 006; la 006 es la del pago con Stripe) |
| `scripts/` | `desplegar.sh`, `nginx-404.sh`, `nginx-prerender.sh` (cambios de nginx ya aplicados, de una vez) y `pruebas-lectura/` (albaranes inventados con su resultado esperado, para probar la IA real) |
| `docs/` | `pago-stripe.md`; `google-ads/` (sin commit todavía) |
| `marketing/` | Material de vídeos y redes |
| `../orderflow/` | Repo aparte: asistente de pedidos por chat (Express + Mistral) y n8n, en Docker |

Tests: `cd pedidai-api && ./mvnw test` (H2 en memoria) y `cd pedidai-app && npx ng test`. En local:
`.claude/launch.json` de `Proyectos/` → `pedidai-api` (perfil `local`, puerto 8085) y `pedidai-app` (4200).

### Reglas al cambiar el código

- **Ruta nueva en `app.routes.ts` → añadirla también a la lista de nginx** (`scripts/nginx-404.sh`, map
  `$pedidai_ruta_app` en `conf.d/01-pedidai.conf`); un test de `app.spec.ts` lo avisa. Las rutas no pueden empezar por
  `/api`, `/ai` ni `/n8n`.
- Título, descripción y canónica por ruta en `data.seo` de `app.routes.ts` (`SeoService`). El catalán no debe
  parpadear (`lang-gate.js`); cookies y analítica solo en el navegador.
- Cada consulta de la API se limita a la empresa del usuario. Migraciones nuevas en `pedidai-db/migrations/` y se
  aplican antes de desplegar la API.

## Servidor (VPS `vps`, Ubuntu 24.04) — sin claves

| Pieza | Dónde |
|---|---|
| Web | `/var/www/pedidai.es` (nginx sirve `<ruta>/index.html` prerenderizado y el resto `index.csr.html`; `/404` real) |
| API | `pedidai-api.service`, `127.0.0.1:8085`, jar en `/opt/apps/pedidai/pedidai-api/target/` |
| Secretos (solo nombres) | `/opt/apps/pedidai/.env` (600): credenciales de MySQL, `JWT_SECRET`, `AI_API_KEY`, `SMTP_*`/`MAIL_FROM*` (Brevo), `CORS_ALLOWED_ORIGINS`, `N8N_NEW_COMPANY_WEBHOOK`; las de Stripe **no están** (pago desactivado) |
| Asistente y n8n | `/opt/apps/orderflow` con `docker compose` (`orderflow-assistant-1` en `127.0.0.1:3201`, `orderflow-n8n-1` en `127.0.0.1:5678`) |
| Base de datos | MySQL local, `pedidai_db` |
| nginx | `sites-enabled/pedidai` (`/` → web, `/api` → 8085, `/ai/` → asistente, `/n8n/` → n8n; bloquea `/api/internal/`), `conf.d/01-pedidai.conf` (lista de rutas de la app), `snippets/pedidai-security-headers.conf` y `snippets/pedidai-csp.conf` (la CSP permite Google Ads: `www.googleadservices.com` y `ad.doubleclick.net`) |
| Comandos del servidor | `sudo pedidai-set-ai-key` y `sudo pedidai-set-smtp` (piden la clave oculta y la validan), `sudo pedidai-borrar-empresa <email>` (pide escribir `BORRAR`). Se lanzan con `ssh -t vps …` |

## Desplegar y volver atrás (siempre con permiso)

```bash
scripts/desplegar.sh api --simular   # solo tests y build, sin tocar el servidor (igual con web)
scripts/desplegar.sh api             # tests + jar + copia + cambio + comprobación
scripts/desplegar.sh web             # tests + build de Angular + copia + cambio + comprobación
```

- Si falla un test no sube nada. Guarda copia en `/root/backups/despliegue-<fecha>/` (base de datos y jar, o `web.tgz`).
- **API**: si no responde en 90 s, vuelve sola al jar anterior. A mano: parar el servicio, copiar el jar de la copia
  y arrancar.
- **Web**: volver atrás con `sudo tar xzf /root/backups/despliegue-<fecha>/web.tgz -C /var/www`.
- **Asistente y n8n**: en `/opt/apps/orderflow`, `git fetch && git merge --ff-only origin/main` (sin sudo) y
  `sudo docker compose up -d --build assistant` o `sudo docker compose up -d n8n`.
- Comprobar: `https://pedidai.es/api/users/me` → 401 sin token y `https://pedidai.es/ai/health` → `{"status":"ok"}`.
- Pasos completos en la sección 9 de [`readme.md`](readme.md). Pago con Stripe: [`docs/pago-stripe.md`](docs/pago-stripe.md).

## Estado (2026-11-07)

- **`main` = GitHub = producción** (`7c9acac`): web desplegada el 2026-10-30 y API el 2026-10-29 (`d238d58`).
- **Ramas**: `feat/pagina-404`, `feat/prerender-publicas` y `appmod/java-upgrade-20260927132009` ya están juntas con
  `main` (se pueden borrar). `docs/traspaso` lleva este archivo.
- **Sin commit**: `docs/google-ads/negativas-2026-11-07.md` (51 negativas aplicadas en la campaña «PedidAI ·
  Búsqueda»; **revisar el efecto hacia el 2026-11-21**) y `.claude/launch.json`.
- **Pendiente**:
  - Pago con Stripe desplegado pero **desactivado** (faltan las claves en el `.env`): ver `docs/pago-stripe.md`.
  - Modelo de IA fijado (rama `chore/fijar-version-mistral`, sin desplegar): la API usa `mistral-small-2603`
    (Mistral Small 4; valor por defecto de `ai.model`, el `.env` no tiene `AI_MODEL`) y el asistente
    `mistral-medium-2604` (Mistral Medium 3.5). Son las versiones a las que apuntaban los alias `-latest` el
    2026-11-07 (consultado en `GET /v1/models`).
  - Google Ads: para probar conversiones, desactivar Adblock en pedidai.es (detalles en `../CLAUDE.md`).

## Decisiones tomadas

- **Mistral** (empresa europea, **datos en Francia**) para estructurar los albaranes (después del OCR con Tesseract en
  el servidor) y para el chat. El asistente usa el token del propio usuario y los pedidos siempre los confirma el
  usuario desde la web.
- **Telegram sin datos personales** (regla de DevEps para cualquier aviso nuevo).
- **Nada de correos en frío** ni scraping. Los correos de n8n son solo avisos internos al equipo.
- No se publica en redes de forma automática: Enrique revisa y publica cada pieza.
