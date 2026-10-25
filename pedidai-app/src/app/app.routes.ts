import { Routes } from '@angular/router';
import { authGuard } from './guards/auth-guard';
import { superAdminGuard } from './guards/super-admin.guard';
import { adminGuard } from './guards/admin.guard';
import { PublicLayoutComponent } from './layouts/public-layout/public-layout';
import { Landing } from './pages/landing/landing';
import { Register } from './components/auth/register/register';
import { Login } from './components/auth/login/login';

const NOT_FOUND_SEO = { title: 'Página no encontrada · PedidAI', description: 'Esta dirección no existe en PedidAI.', index: false };
const notFound = () => import('./pages/not-found/not-found').then(m => m.NotFound);

// La landing, el registro y el login se cargan de inicio (es lo que ve quien llega desde un anuncio);
// el resto de pantallas se descargan solo cuando se visitan.
// `data.seo`: título y descripción para buscadores (SeoService). Sin `seo`, la ruta queda en noindex.
export const routes: Routes = [
  {
    path: '',
    component: PublicLayoutComponent,
    children: [
      { path: '', component: Landing, pathMatch: 'full', data: { seo: { title: 'PedidAI · Descubre cuánto pagas de más a tus proveedores', description: 'Para bares y restaurantes: haz una foto a tus albaranes y PedidAI compara lo que te cobra cada proveedor y te avisa de las subidas. Prueba 14 días gratis.' } } },
      { path: 'login', component: Login, data: { seo: { title: 'Acceder · PedidAI', description: 'Entra en tu cuenta de PedidAI.', index: false } } },
      { path: 'register', component: Register, data: { seo: { title: 'Prueba PedidAI gratis 14 días · Para bares y restaurantes', description: 'Crea tu cuenta en un minuto y sin tarjeta. Sube tus albaranes y descubre qué proveedor te cobra de más y dónde comprar más barato.' } } },
      { path: 'verify-email', loadComponent: () => import('./components/auth/verify-email/verify-email').then(m => m.VerifyEmail) },
      { path: 'recover-password', loadComponent: () => import('./components/auth/recover-password/recover-password').then(m => m.RecoverPassword) },
      { path: 'reset-password', loadComponent: () => import('./components/auth/reset-password/reset-password').then(m => m.ResetPassword) },
      { path: 'privacidad', loadComponent: () => import('./pages/legal/privacy-policy/privacy-policy').then(m => m.PrivacyPolicy), data: { seo: { title: 'Política de privacidad · PedidAI', description: 'Cómo trata PedidAI tus datos y los de tu negocio: qué guardamos, para qué, durante cuánto tiempo y cómo ejercer tus derechos.' } } },
      { path: 'aviso-legal', loadComponent: () => import('./pages/legal/legal-notice/legal-notice').then(m => m.LegalNotice), data: { seo: { title: 'Aviso legal · PedidAI', description: 'Datos del titular de PedidAI y condiciones de uso del sitio web.' } } },
      { path: 'cookies', loadComponent: () => import('./pages/legal/cookie-policy/cookie-policy').then(m => m.CookiePolicy), data: { seo: { title: 'Política de cookies · PedidAI', description: 'Qué cookies usa PedidAI: las técnicas, siempre activas, y las de analítica y publicidad, solo con tu consentimiento.' } } },
      { path: 'terminos', loadComponent: () => import('./pages/legal/terms-conditions/terms-conditions').then(m => m.TermsConditions), data: { seo: { title: 'Términos y condiciones · PedidAI', description: 'Condiciones de uso de PedidAI: prueba gratuita, precio, pago y baja del servicio.' } } },
      { path: 'contacto', loadComponent: () => import('./pages/contact/contact.component').then(m => m.ContactComponent), data: { seo: { title: 'Contacto · PedidAI', description: 'Escribe a Enrique, fundador de PedidAI, por email o WhatsApp. Respuesta en menos de 24 horas laborables.' } } },
      { path: 'contacte', redirectTo: 'contacto', pathMatch: 'full' },
      { path: 'sobre-nosotros', loadComponent: () => import('./pages/about/about.component').then(m => m.AboutComponent), data: { seo: { title: 'Sobre nosotros · PedidAI', description: '24 años llevando fruta y verdura a restaurantes. Ahora ayudamos a bares y restaurantes a no pagar de más a sus proveedores.' } } },
      { path: 'sobre-nosaltres', redirectTo: 'sobre-nosotros', pathMatch: 'full' },
      // Se prerenderiza: es el HTML que nginx devuelve con código 404 (scripts/nginx-404.sh)
      { path: '404', loadComponent: notFound, data: { seo: NOT_FOUND_SEO } },
    ]
  },
  {
    path: '',
    loadComponent: () => import('./layouts/private-layout/private-layout').then(m => m.PrivateLayoutComponent),
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', loadComponent: () => import('./components/dashboard/dashboard/dashboard').then(m => m.Dashboard) },
      { path: 'account', loadComponent: () => import('./components/account/account').then(m => m.Account) },
      { path: 'prices', loadComponent: () => import('./components/prices/prices').then(m => m.Prices) },
      { path: 'suppliers', loadComponent: () => import('./components/suppliers/supplier-list/supplier-list').then(m => m.SupplierList) },
      { path: 'suppliers/new', loadComponent: () => import('./components/suppliers/supplier-form/supplier-form').then(m => m.SupplierForm) },
      { path: 'suppliers/:uuid', loadComponent: () => import('./components/suppliers/supplier-form/supplier-form').then(m => m.SupplierForm) },
      { path: 'products', loadComponent: () => import('./components/products/product-list/product-list').then(m => m.ProductList) },
      { path: 'products/new', loadComponent: () => import('./components/products/product-form/product-form').then(m => m.ProductForm) },
      { path: 'products/:uuid', loadComponent: () => import('./components/products/product-form/product-form').then(m => m.ProductForm) },
      { path: 'orders', loadComponent: () => import('./components/orders/order-list/order-list').then(m => m.OrderList) },
      { path: 'orders/new', loadComponent: () => import('./components/orders/order-create/order-create').then(m => m.OrderCreate) },
      { path: 'orders/:uuid/edit', loadComponent: () => import('./components/orders/order-create/order-create').then(m => m.OrderCreate) },
      { path: 'orders/:uuid', loadComponent: () => import('./components/orders/order-detail/order-detail').then(m => m.OrderDetail) },
      { path: 'users', canActivate: [adminGuard], loadComponent: () => import('./components/users/user-list/user-list').then(m => m.UserList) },
      { path: 'users/new', canActivate: [adminGuard], loadComponent: () => import('./components/users/user-form/user-form').then(m => m.UserForm) },
      { path: 'users/:uuid', canActivate: [adminGuard], loadComponent: () => import('./components/users/user-form/user-form').then(m => m.UserForm) },
      { path: 'reports', canActivate: [adminGuard], loadComponent: () => import('./components/reports/reports/reports').then(m => m.Reports) },
      { path: 'company', canActivate: [adminGuard], loadComponent: () => import('./components/company/company-config/company-config').then(m => m.CompanyConfig) },
      { path: 'invoices/scan', loadComponent: () => import('./components/invoices/invoice-scan/invoice-scan').then(m => m.InvoiceScan) },
      { path: 'chat', loadComponent: () => import('./components/ai-chat/ai-chat/ai-chat').then(m => m.AiChat) },
      { path: 'suggestions', loadComponent: () => import('./components/ai-chat/ai-suggestions/ai-suggestions').then(m => m.AiSuggestions) },
      { path: 'superadmin', canActivate: [superAdminGuard], loadComponent: () => import('./pages/superadmin/superadmin-dashboard/superadmin-dashboard').then(m => m.SuperadminDashboard) },
      { path: 'superadmin/companies', canActivate: [superAdminGuard], loadComponent: () => import('./pages/superadmin/superadmin-companies/superadmin-companies').then(m => m.SuperadminCompanies) },
      { path: 'superadmin/companies/:uuid', canActivate: [superAdminGuard], loadComponent: () => import('./pages/superadmin/superadmin-company-detail/superadmin-company-detail').then(m => m.SuperadminCompanyDetail) },
      { path: 'superadmin/users', canActivate: [superAdminGuard], loadComponent: () => import('./pages/superadmin/superadmin-users/superadmin-users').then(m => m.SuperadminUsers) },
    ]
  },
  // Dirección desconocida: la misma página 404, dentro de la web pública (así hidrata sobre el HTML de /404).
  // Si añades una ruta, añádela también a la lista de nginx (scripts/nginx-404.sh) o recargarla dará 404.
  { path: '**', component: PublicLayoutComponent, children: [{ path: '', loadComponent: notFound, data: { seo: NOT_FOUND_SEO } }] }
];
