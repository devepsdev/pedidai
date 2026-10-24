# Pago con Stripe

PedidAI cobra la suscripción (29 €/mes + IVA por local, precio de lanzamiento) con **Stripe Checkout**: el cliente paga en la página de Stripe y la tarjeta nunca pasa por nuestro servidor.

- **Sin claves de Stripe configuradas, el pago en línea está desactivado** y la web sigue ofreciendo «Activar mi plan» por email, como antes.
- **Claves `sk_test_…` = modo de pruebas** (tarjetas falsas, sin dinero real). **Claves `sk_live_…` = cobros reales.**

## Cómo funciona

| Paso | Qué pasa |
| --- | --- |
| Durante la prueba gratuita | El administrador pulsa «Activar mi plan» en el panel y va a la página de pago de Stripe (tarjeta, CIF y número de locales, de 1 a 5). **No pierde los días de prueba que le quedan:** el primer cobro se hace cuando termina la prueba (si quedan menos de 49 horas, se cobra ya). |
| Prueba ya terminada | No puede iniciar sesión. En el login, bajo el aviso «Tu prueba gratuita ha terminado», aparece **«Contratar ahora»**: con el mismo email y contraseña va a la página de pago. |
| Después de pagar | Stripe avisa a `POST /api/billing/webhook` (aviso firmado). La empresa pasa a cliente de pago (`ACTIVE`, sin fecha de fin de prueba). La web lo muestra en unos segundos. |
| Gestión | En «Datos de la empresa», «Gestionar suscripción» abre el portal de Stripe: cambiar la tarjeta, descargar facturas, darse de baja. |
| Baja o impago | Con la suscripción `canceled`, `unpaid` o `incomplete_expired`, la empresa pasa a `INACTIVE`: no puede entrar y el login le dice «Tu suscripción a PedidAI ha terminado» con el botón «Volver a contratar» (a quien viene de la prueba le habla de la prueba). Con `past_due` (Stripe reintenta el cobro) sigue activa. |

Una empresa `SUSPENDED` por el superadministrador no se reactiva nunca con un pago. Cada aviso de Stripe se guarda en `stripe_events` y no se aplica dos veces.

## 1. Crear la cuenta de Stripe en modo de pruebas

No pide datos fiscales ni bancarios mientras solo se use el modo de pruebas.

1. Regístrate en [stripe.com](https://stripe.com) con tu email y activa el **modo de prueba** (interruptor «Modo de prueba» arriba a la derecha).
2. **Producto y precio:** *Catálogo de productos* → *Añadir producto*.
   - Nombre: `PedidAI`
   - Precio: `29,00 EUR`, **recurrente**, **mensual**.
   - Impuestos incluidos en el precio: **No** (el IVA se suma aparte).
   - Copia el **ID del precio** (`price_…`).
3. **IVA:** *Catálogo de productos* → *Tasas de impuestos* → *Nueva*: nombre `IVA`, `21 %`, España, **no incluida en el precio** (exclusiva). Copia el ID (`txr_…`).
4. **Portal del cliente:** *Configuración* → *Facturación* → *Portal del cliente*: activa cambiar el método de pago, ver el historial de facturas y cancelar suscripciones (mejor al final del periodo de facturación). Guarda.
5. **Clave secreta:** *Desarrolladores* → *Claves de API* → **Clave secreta** (`sk_test_…`).

### Sandbox ya configurado (octubre 2026)

En el sandbox de pruebas de la cuenta ya están creados el producto `PedidAI` (precio `price_1UU88ICn0kLIsOJRLbwfD1fy`, 29 €/mes), la tasa `IVA 21 % España` (`txr_1UU87hCn0kLIsOJRNbIHTup6`) y el portal del cliente (facturas, tarjeta, datos y CIF, baja al final del periodo). El flujo completo se probó en local el 24/10/2026: alta en prueba, pago, portal, baja inmediata y nueva contratación desde el login.

## 2. Probarlo en este PC

1. Añade a `pedidai-api/config/application-local.properties` (no se sube a git):

   ```properties
   stripe.secret-key=sk_test_...
   stripe.price-id=price_...
   stripe.tax-rate-id=txr_...
   stripe.webhook-secret=whsec_...
   ```

2. Instala la CLI de Stripe e inicia sesión (se abre el navegador para autorizarla):

   ```bash
   winget install Stripe.StripeCli
   stripe login
   ```

3. Reenvía los avisos de Stripe a la API local. La primera línea que muestra es el `whsec_…` del paso 1:

   ```bash
   stripe listen --forward-to localhost:8085/api/billing/webhook --events checkout.session.completed,customer.subscription.created,customer.subscription.updated,customer.subscription.deleted
   ```

4. Arranca la API y la web (`pedidai-api` y `pedidai-app` en `.claude/launch.json`), regístrate con una empresa de prueba y pulsa «Activar mi plan».

**Tarjetas de prueba** (cualquier fecha futura y cualquier CVC):

| Tarjeta | Resultado |
| --- | --- |
| `4242 4242 4242 4242` | Pago correcto |
| `4000 0025 0000 3155` | Pide autenticación (3D Secure) |
| `4000 0000 0000 0341` | Se guarda, pero los cobros fallan (para probar impagos) |

**Qué comprobar:** la empresa pasa a cliente de pago; «Gestionar suscripción» abre el portal; al cancelar la suscripción desde el portal (o con `stripe subscriptions cancel sub_…`), la empresa pasa a `INACTIVE` y el login ofrece «Contratar ahora».

## 3. Pasar a cobros reales

1. **Alta fiscal:** antes de la primera factura (autónomo o sociedad). Consulta con tu gestoría también los requisitos de las facturas (numeración, NIF) y del sistema de facturación (Verifactu).
2. **Activa la cuenta de Stripe:** datos fiscales, DNI/NIF e IBAN donde recibirás las transferencias. En *Configuración* → *Facturación* → *Facturas*, añade tu NIF y dirección para que salgan en las facturas. Pon también **`PedidAI` como nombre de la cuenta** en *Configuración* → *Empresa* → *Datos de la cuenta*: sin él, la página de pago dice «autorizas a a cobrarte» y el portal muestra «Nueva empresa» (en el sandbox ya está puesto).
3. **Repite en modo real** el producto, la tasa de IVA y el portal del cliente (son independientes de los de pruebas).
4. **Webhook de producción:** *Desarrolladores* → *Webhooks* → *Añadir endpoint* → `https://pedidai.es/api/billing/webhook` con los eventos `checkout.session.completed`, `customer.subscription.created`, `customer.subscription.updated` y `customer.subscription.deleted`. Copia su secreto (`whsec_…`).
5. **Servidor:** aplica la migración `pedidai-db/migrations/006_pago_stripe.sql` **antes** de desplegar la API (la API lee las columnas nuevas), y añade a `/opt/apps/pedidai/.env` `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`, `STRIPE_PRICE_ID` y `STRIPE_TAX_RATE_ID`. Después, `scripts/desplegar.sh api` y `scripts/desplegar.sh web`.
6. **Textos legales:** los términos dicen «Para contratar, escríbenos a hola@pedidai.es»; cámbialo por la contratación desde el panel.

> **Nunca pongas claves `sk_test_` en el servidor de producción:** cualquiera podría «pagar» con una tarjeta de prueba y quedar activado gratis.
