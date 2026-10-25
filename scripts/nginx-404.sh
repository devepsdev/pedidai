#!/usr/bin/env bash
# Cambio de nginx para que PedidAI devuelva un 404 de verdad. Se ejecuta UNA vez, después de desplegar la web
# con la página /404 prerenderizada:
#   tr -d '\r' < scripts/nginx-404.sh | ssh vps 'sudo bash -s'
#
# Hasta ahora cualquier dirección (pedidai.es/lo-que-sea) devolvía 200 con la app, y Google podía guardarla
# como página. Con este cambio:
# - Las páginas y ficheros que existen se sirven igual que antes.
# - Las rutas del panel y de la web (lista de abajo) reciben index.csr.html, como antes.
# - Cualquier otra dirección recibe el HTML de /404/index.html con código 404.
# La lista debe coincidir con las rutas de src/app/app.routes.ts (lo comprueba un test de app.spec.ts).
#
# Volver atrás (desde la copia que se indica al final):
#   cp -a <copia>/pedidai /etc/nginx/sites-available/pedidai && cp -a <copia>/01-pedidai.conf /etc/nginx/conf.d/ \
#     && nginx -t && systemctl reload nginx
set -euo pipefail
SITIO=/etc/nginx/sites-available/pedidai
MAPA=/etc/nginx/conf.d/01-pedidai.conf
WEB=/var/www/pedidai.es
COPIA=/root/backups/nginx-404-$(date +%Y%m%d-%H%M%S)

[[ -f $WEB/404/index.html ]] || { echo "Falta $WEB/404/index.html: despliega antes la web" >&2; exit 1; }
! grep -q pedidai_ruta_app "$MAPA" || { echo "El cambio ya está aplicado" >&2; exit 1; }
[[ $(grep -c 'try_files $uri $uri/index.html /index.csr.html;' "$SITIO") == 1 ]] || { echo "location / no es la esperada; no se toca nada" >&2; exit 1; }

mkdir -p "$COPIA"; chmod 700 "$COPIA"
cp -a "$SITIO" "$MAPA" "$COPIA/"

cat >> "$MAPA" <<'EOF'

# PedidAI: rutas de la app Angular (primer tramo de la URL). Cualquier otra dirección es un 404.
# Debe coincidir con app.routes.ts (test en app.spec.ts).
map $uri $pedidai_ruta_app {
    default 0;
    ~^/(login|register|verify-email|recover-password|reset-password|privacidad|aviso-legal|cookies|terminos|contacto|contacte|sobre-nosotros|sobre-nosaltres|404|dashboard|account|prices|suppliers|products|orders|users|reports|company|invoices|chat|suggestions|superadmin)(/|$) 1;
}
EOF

python3 - "$SITIO" <<'EOF'
import sys
p = sys.argv[1]
c = open(p, encoding='utf-8').read()
viejo = "        try_files $uri $uri/index.html /index.csr.html;\n    }\n"
nuevo = """        try_files $uri $uri/index.html @app;
    }

    # Ni fichero ni página prerenderizada: si es una ruta de la app, la app vacía; si no, la página 404
    location @app {
        include snippets/pedidai-security-headers.conf;
        include snippets/pedidai-csp.conf;
        add_header Cache-Control "no-cache" always;
        if ($pedidai_ruta_app = 0) {
            return 404;
        }
        rewrite ^ /index.csr.html break;
    }

    error_page 404 /404/index.html;
    location = /404/index.html {
        internal;
        include snippets/pedidai-security-headers.conf;
        include snippets/pedidai-csp.conf;
        add_header Cache-Control "no-cache" always;
    }
"""
assert c.count(viejo) == 1
open(p, 'w', encoding='utf-8').write(c.replace(viejo, nuevo))
EOF

if nginx -t; then
  systemctl reload nginx
  echo "nginx actualizado. Copia de seguridad en $COPIA"
else
  cp -a "$COPIA/pedidai" "$SITIO"; cp -a "$COPIA/01-pedidai.conf" "$MAPA"
  echo "nginx -t ha fallado: configuración restaurada, no se ha recargado nada" >&2
  exit 1
fi
