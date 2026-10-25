import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

// Página de dirección inexistente. Se prerenderiza en /404 y nginx la sirve con código 404
// para cualquier ruta que no sea de la web ni del panel (scripts/nginx-404.sh).
@Component({
  selector: 'app-not-found',
  imports: [RouterLink, TranslateModule],
  templateUrl: './not-found.html',
})
export class NotFound {}
