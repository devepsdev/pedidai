#!/usr/bin/env bash
# Cambio de nginx para servir PedidAI prerenderizado. Se ejecuta UNA vez, justo después de
# «scripts/desplegar.sh web» con la versión prerenderizada:
#   ssh vps 'sudo bash -s' < scripts/nginx-prerender.sh
#
# Qué cambia en location / de /etc/nginx/sites-available/pedidai:
#   antes:   try_files $uri $uri/ /index.html;
#   después: try_files $uri $uri/index.html /index.csr.html;
#            + Cache-Control: no-cache solo en las respuestas HTML
# - /register, /contacto… sirven su página ya pintada (register/index.html…).
# - El panel y el resto de rutas reciben index.csr.html (la app vacía de siempre). Si siguieran recibiendo
#   index.html, ahora sería la portada pintada y Angular intentaría «hidratar» el panel sobre la portada.
# - El HTML se revalida siempre (como ya pasaba con index.html); los JS/CSS con hash se siguen guardando en caché.
#
# Volver atrás (desde la copia que se indica al final):
#   cp -a <copia>/pedidai /etc/nginx/sites-available/pedidai && rm -f /etc/nginx/conf.d/01-pedidai.conf \
#     && nginx -t && systemctl reload nginx
set -euo pipefail
SITIO=/etc/nginx/sites-available/pedidai
MAPA=/etc/nginx/conf.d/01-pedidai.conf
WEB=/var/www/pedidai.es
COPIA=/root/backups/nginx-prerender-$(date +%Y%m%d-%H%M%S)

[[ -f $WEB/index.csr.html ]] || { echo "Falta $WEB/index.csr.html: despliega antes la web prerenderizada" >&2; exit 1; }
[[ ! -e $MAPA ]] || { echo "$MAPA ya existe: el cambio ya está aplicado" >&2; exit 1; }
[[ $(grep -c 'try_files $uri $uri/ /index.html;' "$SITIO") == 1 ]] || { echo "location / no es la esperada; no se toca nada" >&2; exit 1; }

mkdir -p "$COPIA"; chmod 700 "$COPIA"
cp -a "$SITIO" "$COPIA/"

cat > "$MAPA" <<'EOF'
# PedidAI: las páginas HTML (prerenderizadas e index.csr.html) siempre se revalidan; los JS/CSS llevan hash
map $sent_http_content_type $pedidai_cache_html {
    ~^text/html "no-cache";
    default     "";
}
EOF

sed -i 's|^\(\s*\)try_files \$uri \$uri/ /index.html;|\1add_header Cache-Control $pedidai_cache_html always;\n\1try_files $uri $uri/index.html /index.csr.html;|' "$SITIO"

if nginx -t; then
  systemctl reload nginx
  echo "nginx actualizado. Copia de seguridad en $COPIA"
else
  cp -a "$COPIA/pedidai" "$SITIO"; rm -f "$MAPA"
  echo "nginx -t ha fallado: configuración restaurada, no se ha recargado nada" >&2
  exit 1
fi
