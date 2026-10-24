#!/usr/bin/env bash
# Despliega PedidAI en el VPS desde este PC (Git Bash). Si falla un test, no sube nada.
#
#   scripts/desplegar.sh api             tests + jar + copia de seguridad + cambio + comprobación
#   scripts/desplegar.sh web             tests + build de Angular + copia de seguridad + cambio + comprobación
#   scripts/desplegar.sh api --simular   solo tests y build, sin tocar el servidor (igual con web)
#
# Si la API no arranca con el jar nuevo, vuelve sola al anterior.
set -euo pipefail

OBJETIVO=${1:-}
SIMULAR=${2:-}
SSH=/c/Windows/System32/OpenSSH/ssh.exe   # el ssh de Git Bash no tiene las claves cargadas
HOST=vps
RAIZ=$(cd "$(dirname "$0")/.." && pwd)
FECHA=$(date +%Y%m%d-%H%M%S)
COPIA=/root/backups/despliegue-$FECHA

paso() { printf '\n\033[1;36m==> %s\033[0m\n' "$*"; }
falla() { printf '\n\033[1;31mERROR: %s\033[0m\n' "$*" >&2; exit 1; }

if [[ "$OBJETIVO" != api && "$OBJETIVO" != web ]]; then sed -n '2,8p' "$0"; exit 1; fi
if [[ -n "$SIMULAR" && "$SIMULAR" != --simular ]]; then falla "opción desconocida: $SIMULAR"; fi

origen() {
  printf 'Rama %s · commit %s\n' "$(git -C "$RAIZ" branch --show-current)" "$(git -C "$RAIZ" rev-parse --short HEAD)"
  if [[ -n "$(git -C "$RAIZ" status --porcelain -- "pedidai-$OBJETIVO" | grep -v '^??' || true)" ]]; then
    printf 'Aviso: hay cambios sin commit; se despliega lo que hay en disco.\n'
  fi
}

desplegar_api() {
  local jar=target/pedidai-api-0.0.1-SNAPSHOT.jar
  cd "$RAIZ/pedidai-api"
  paso "Tests y jar de la API (si falla un test, se para aquí)"
  ./mvnw -q -B clean package
  if unzip -l "$jar" | grep -q 'application-local.properties'; then
    falla "el jar lleva application-local.properties (ajustes locales)"
  fi
  if [[ -n "$SIMULAR" ]]; then paso "Simulación: tests y jar correctos; no se toca el servidor"; return; fi

  paso "Subiendo el jar"
  "$SSH" "$HOST" "cat > /tmp/pedidai-api-$FECHA.jar" < "$jar"

  paso "Copia de seguridad, cambio de jar y comprobación (en el servidor)"
  "$SSH" "$HOST" "sudo -n bash -s -- $FECHA $COPIA" <<'REMOTO'
set -euo pipefail
FECHA=$1; COPIA=$2
APP=/opt/apps/pedidai/pedidai-api/target/pedidai-api-0.0.1-SNAPSHOT.jar
NUEVO=/tmp/pedidai-api-$FECHA.jar
mkdir -p "$COPIA"; chmod 700 "$COPIA"
mysqldump --single-transaction --routines pedidai_db | gzip > "$COPIA/pedidai_db.sql.gz"
cp -p "$APP" "$COPIA/"
echo "Copia de seguridad en $COPIA"

sana() {  # sin token, la API arrancada responde 401
  for _ in $(seq 1 45); do
    [[ "$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8085/api/users/me)" == 401 ]] && return 0
    sleep 2
  done
  return 1
}

# Se para antes de copiar: cambiar el jar en marcha provoca NoClassDefFoundError al apagarse
systemctl stop pedidai-api
install -o root -g root -m 644 "$NUEVO" "$APP"
systemctl start pedidai-api
if sana; then
  rm -f "$NUEVO"
  echo "API arrancada con el jar nuevo"
else
  echo "La API no responde: se vuelve al jar anterior" >&2
  systemctl stop pedidai-api
  install -o root -g root -m 644 "$COPIA/$(basename "$APP")" "$APP"
  systemctl start pedidai-api
  sana && echo "Jar anterior restaurado y funcionando" >&2
  exit 1
fi
REMOTO
}

desplegar_web() {
  local dist=dist/pedidai-app/browser
  cd "$RAIZ/pedidai-app"
  paso "Tests y build de Angular (si falla un test, se para aquí)"
  npx ng test --watch=false
  npx ng build
  [[ -f "$dist/index.html" ]] || falla "no se ha generado $dist/index.html"
  if [[ -n "$SIMULAR" ]]; then paso "Simulación: tests y build correctos; no se toca el servidor"; return; fi

  paso "Subiendo la web"
  tar czf - -C "$dist" . | "$SSH" "$HOST" "cat > /tmp/pedidai-web-$FECHA.tgz"

  paso "Copia de seguridad y cambio de la web (en el servidor)"
  "$SSH" "$HOST" "sudo -n bash -s -- $FECHA $COPIA" <<'REMOTO'
set -euo pipefail
FECHA=$1; COPIA=$2
WEB=/var/www/pedidai.es
PAQUETE=/tmp/pedidai-web-$FECHA.tgz
mkdir -p "$COPIA"; chmod 700 "$COPIA"
tar czf "$COPIA/web.tgz" -C /var/www pedidai.es
echo "Copia de seguridad en $COPIA (para volver atrás: tar xzf $COPIA/web.tgz -C /var/www)"
NUEVA=$(mktemp -d)
tar xzf "$PAQUETE" -C "$NUEVA"
# Sin --delete: los JS antiguos siguen sirviendo a quien tenga la página abierta; .well-known no se toca
rsync -a --exclude .well-known "$NUEVA"/ "$WEB"/
chown -R www-data:www-data "$WEB"
rm -rf "$NUEVA" "$PAQUETE"
echo "Web actualizada"
REMOTO
}

comprobar() {
  paso "Comprobación desde fuera"
  local web api js principal
  web=$(curl -s -o /dev/null -w '%{http_code}' https://pedidai.es/)
  api=$(curl -s -o /dev/null -w '%{http_code}' https://pedidai.es/api/users/me)
  principal=$(curl -s https://pedidai.es/ | grep -oE 'main-[A-Z0-9]+\.js' | head -1)
  js=$(curl -s -o /dev/null -w '%{http_code}' "https://pedidai.es/$principal")
  printf 'web %s (espera 200) · api sin token %s (espera 401) · %s %s (espera 200)\n' "$web" "$api" "$principal" "$js"
  [[ "$web" == 200 && "$api" == 401 && "$js" == 200 ]] || falla "algo no responde como debería; copia de seguridad en $COPIA"
}

origen
"desplegar_$OBJETIVO"
if [[ -z "$SIMULAR" ]]; then
  comprobar
  paso "Listo. Copia de seguridad en el servidor: $COPIA"
fi
