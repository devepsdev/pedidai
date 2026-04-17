import { Routes } from '@angular/router';
import { authGuard } from './guards/auth-guard';
import { PublicLayoutComponent } from './layouts/public-layout/public-layout';
import { PrivateLayoutComponent } from './layouts/private-layout/private-layout';
import { Login } from './components/auth/login/login';
import { Register } from './components/auth/register/register';
import { VerifyEmail } from './components/auth/verify-email/verify-email';
import { RecoverPassword } from './components/auth/recover-password/recover-password';
import { ResetPassword } from './components/auth/reset-password/reset-password';
import { Dashboard } from './components/dashboard/dashboard/dashboard';
import { SupplierList } from './components/suppliers/supplier-list/supplier-list';
import { SupplierForm } from './components/suppliers/supplier-form/supplier-form';
import { ProductList } from './components/products/product-list/product-list';
import { ProductForm } from './components/products/product-form/product-form';
import { OrderList } from './components/orders/order-list/order-list';
import { OrderCreate } from './components/orders/order-create/order-create';
import { OrderDetail } from './components/orders/order-detail/order-detail';
import { UserList } from './components/users/user-list/user-list';
import { UserForm } from './components/users/user-form/user-form';
import { Reports } from './components/reports/reports/reports';
import { CompanyConfig } from './components/company/company-config/company-config';
import { AiChat } from './components/ai-chat/ai-chat/ai-chat';
import { AiSuggestions } from './components/ai-chat/ai-suggestions/ai-suggestions';
import { InvoiceScan } from './components/invoices/invoice-scan/invoice-scan';
import { Landing } from './pages/landing/landing';
import { PrivacyPolicy } from './pages/legal/privacy-policy/privacy-policy';
import { LegalNotice } from './pages/legal/legal-notice/legal-notice';
import { CookiePolicy } from './pages/legal/cookie-policy/cookie-policy';
import { TermsConditions } from './pages/legal/terms-conditions/terms-conditions';

export const routes: Routes = [
  {
    path: '',
    component: PublicLayoutComponent,
    children: [
      { path: '', component: Landing, pathMatch: 'full' },
      { path: 'login', component: Login },
      { path: 'register', component: Register },
      { path: 'verify-email', component: VerifyEmail },
      { path: 'recover-password', component: RecoverPassword },
      { path: 'reset-password', component: ResetPassword },
      { path: 'privacidad', component: PrivacyPolicy },
      { path: 'aviso-legal', component: LegalNotice },
      { path: 'cookies', component: CookiePolicy },
      { path: 'terminos', component: TermsConditions },
    ]
  },
  {
    path: '',
    component: PrivateLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', component: Dashboard },
      { path: 'suppliers', component: SupplierList },
      { path: 'suppliers/new', component: SupplierForm },
      { path: 'suppliers/:uuid', component: SupplierForm },
      { path: 'products', component: ProductList },
      { path: 'products/new', component: ProductForm },
      { path: 'products/:uuid', component: ProductForm },
      { path: 'orders', component: OrderList },
      { path: 'orders/new', component: OrderCreate },
      { path: 'orders/:uuid/edit', component: OrderCreate },
      { path: 'orders/:uuid', component: OrderDetail },
      { path: 'users', component: UserList },
      { path: 'users/new', component: UserForm },
      { path: 'users/:uuid', component: UserForm },
      { path: 'reports', component: Reports },
      { path: 'company', component: CompanyConfig },
      { path: 'invoices/scan', component: InvoiceScan },
      { path: 'ai', component: AiChat },
      { path: 'ai/suggestions', component: AiSuggestions },
    ]
  },
  { path: '**', redirectTo: 'login' }
];
