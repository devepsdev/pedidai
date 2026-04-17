# Pedidai - Frontend

Aplicación web desarrollada con **Angular 21** para la gestión integral de pedidos B2B para pymes.

**Versión:** 1.2.0
**Framework:** Angular 21.2.6
**Lenguaje:** TypeScript 5.9
**Estilos:** Tailwind CSS 4.2

---

## Descripción

El frontend de Pedidai ofrece una interfaz moderna y responsiva que permite a las empresas gestionar proveedores, productos, pedidos y usuarios desde un panel privado. Incluye internacionalización (castellano y catalán), integración con IA para asistencia inteligente y escaneo de facturas.

## Stack Tecnológico

- **Angular 21.2.6** — Framework principal (componentes standalone)
- **TypeScript 5.9** — Lenguaje de programación
- **Tailwind CSS 4.2** — Framework de estilos utility-first
- **Angular CDK 21** — Kit de desarrollo de componentes
- **@ngx-translate 17** — Internacionalización (i18n): castellano y catalán
- **RxJS 7.8** — Programación reactiva
- **Vitest 4** — Tests unitarios
- **Angular CLI 21.2.6** — Herramienta de construcción y desarrollo

## Requisitos Previos

- **Node.js** 22.x o superior
- **npm** 10.x o superior
- Backend Pedidai API arrancado en `http://localhost:8085`

## Instalación

```bash
cd pedidai-app
npm install
```

## Configurar entorno de desarrollo

Edita o crea el fichero `src/environments/environment.ts`:

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8085/api'
};
```

Para producción, edita `src/environments/environment.prod.ts`:

```typescript
export const environment = {
  production: true,
  apiUrl: 'https://pedidai.es/api'
};
```

## Servidor de Desarrollo

```bash
ng serve
```

La aplicación estará disponible en: **<http://localhost:4200/>**

El servidor recargará automáticamente al detectar cambios en los ficheros fuente.

## Scripts Disponibles

| Script     | Comando                                    | Descripción                               |
| ---------- | ------------------------------------------ | ----------------------------------------- |
| Desarrollo | `ng serve`                                 | Inicia el servidor de desarrollo          |
| Build      | `ng build`                                 | Compila para producción                   |
| Build dev  | `ng build --configuration development`     | Compila en modo desarrollo                |
| Tests      | `ng test`                                  | Ejecuta los tests unitarios con Vitest    |
| Tests e2e  | `ng e2e`                                   | Ejecuta los tests end-to-end              |
| Lint       | `npm run lint`                             | Analiza el código con ESLint              |
| Format     | `npm run format`                           | Formatea el código con Prettier           |

## Estructura del Proyecto

```text
pedidai-app/
├── public/
│   └── i18n/                    # Ficheros de traducción
│       ├── ca.json              # Catalán
│       └── es.json              # Castellano
├── src/
│   ├── app/
│   │   ├── app.ts               # Componente raíz
│   │   ├── app.routes.ts        # Definición de rutas
│   │   ├── app.config.ts        # Configuración Angular
│   │   ├── components/          # Componentes funcionales
│   │   │   ├── ai-chat/         # Chat IA y sugerencias IA
│   │   │   ├── auth/            # Login, registro, verificación
│   │   │   ├── company/         # Configuración empresa
│   │   │   ├── dashboard/       # Panel de control
│   │   │   ├── invoices/        # Escaneo de facturas con IA
│   │   │   ├── orders/          # Gestión de pedidos
│   │   │   ├── products/        # Catálogo de productos
│   │   │   ├── reports/         # Informes y estadísticas
│   │   │   ├── suppliers/       # Gestión de proveedores
│   │   │   └── users/           # Gestión de usuarios
│   │   ├── guards/              # Guards de autenticación
│   │   ├── interceptors/        # Interceptores HTTP (JWT)
│   │   ├── layouts/             # Layouts público y privado
│   │   │   ├── public-layout/
│   │   │   └── private-layout/
│   │   ├── models/              # Interfaces TypeScript
│   │   ├── pages/               # Páginas estáticas (landing)
│   │   ├── services/            # Servicios HTTP
│   │   └── shared/              # Componentes compartidos
│   │       ├── alert/
│   │       ├── confirm-modal/
│   │       ├── footer/
│   │       ├── navbar/
│   │       ├── page-header/
│   │       ├── pagination/
│   │       └── sidebar/
│   ├── assets/                  # Recursos estáticos
│   ├── environments/            # Configuración de entornos
│   └── styles.scss              # Estilos globales
├── angular.json                 # Configuración Angular CLI
├── package.json                 # Dependencias npm
└── tsconfig.json                # Configuración TypeScript
```

## Compilar para Producción

```bash
ng build
```

Los ficheros compilados se generarán en la carpeta `dist/`. Por defecto, el build de producción optimiza la aplicación para rendimiento y velocidad.

## Recursos Adicionales

- [Documentación Angular](https://angular.dev/)
- [Angular CLI Reference](https://angular.dev/tools/cli)
- [Tailwind CSS Docs](https://tailwindcss.com/docs)
- [ngx-translate Docs](https://github.com/ngx-translate/core)
- [API Swagger UI](https://pedidai.es/swagger-ui.html)
