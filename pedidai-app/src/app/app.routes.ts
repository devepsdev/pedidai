import { Routes } from '@angular/router';
import { authGuard } from './guards/auth-guard';
import { superAdminGuard } from './guards/super-admin.guard';
import { adminGuard } from './guards/admin.guard';
import { PublicLayoutComponent } from './layouts/public-layout/public-layout';
import { Landing } from './pages/landing/landing';
import { Register } from './components/auth/register/register';
import { Login } from './components/auth/login/login';

// La landing, el registro y el login se cargan de inicio (es lo que ve quien llega desde un anuncio);
// el resto de pantallas se descargan solo cuando se visitan.
export const routes: Routes = [
  {
    path: '',
    component: PublicLayoutComponent,
    children: [
      { path: '', component: Landing, pathMatch: 'full' },
      { path: 'login', component: Login },
      { path: 'register', component: Register },
      { path: 'verify-email', loadComponent: () => import('./components/auth/verify-email/verify-email').then(m => m.VerifyEmail) },
      { path: 'recover-password', loadComponent: () => import('./components/auth/recover-password/recover-password').then(m => m.RecoverPassword) },
      { path: 'reset-password', loadComponent: () => import('./components/auth/reset-password/reset-password').then(m => m.ResetPassword) },
      { path: 'privacidad', loadComponent: () => import('./pages/legal/privacy-policy/privacy-policy').then(m => m.PrivacyPolicy) },
      { path: 'aviso-legal', loadComponent: () => import('./pages/legal/legal-notice/legal-notice').then(m => m.LegalNotice) },
      { path: 'cookies', loadComponent: () => import('./pages/legal/cookie-policy/cookie-policy').then(m => m.CookiePolicy) },
      { path: 'terminos', loadComponent: () => import('./pages/legal/terms-conditions/terms-conditions').then(m => m.TermsConditions) },
      { path: 'contacto', loadComponent: () => import('./pages/contact/contact.component').then(m => m.ContactComponent) },
      { path: 'contacte', redirectTo: 'contacto', pathMatch: 'full' },
      { path: 'sobre-nosotros', loadComponent: () => import('./pages/about/about.component').then(m => m.AboutComponent) },
      { path: 'sobre-nosaltres', redirectTo: 'sobre-nosotros', pathMatch: 'full' },
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
      { path: 'ai', loadComponent: () => import('./components/ai-chat/ai-chat/ai-chat').then(m => m.AiChat) },
      { path: 'ai/suggestions', loadComponent: () => import('./components/ai-chat/ai-suggestions/ai-suggestions').then(m => m.AiSuggestions) },
      { path: 'superadmin', canActivate: [superAdminGuard], loadComponent: () => import('./pages/superadmin/superadmin-dashboard/superadmin-dashboard').then(m => m.SuperadminDashboard) },
      { path: 'superadmin/companies', canActivate: [superAdminGuard], loadComponent: () => import('./pages/superadmin/superadmin-companies/superadmin-companies').then(m => m.SuperadminCompanies) },
      { path: 'superadmin/companies/:uuid', canActivate: [superAdminGuard], loadComponent: () => import('./pages/superadmin/superadmin-company-detail/superadmin-company-detail').then(m => m.SuperadminCompanyDetail) },
      { path: 'superadmin/users', canActivate: [superAdminGuard], loadComponent: () => import('./pages/superadmin/superadmin-users/superadmin-users').then(m => m.SuperadminUsers) },
    ]
  },
  // Una dirección desconocida lleva a la portada (antes llevaba al login)
  { path: '**', redirectTo: '' }
];
