# Pruebas de lectura de albaranes

Albaranes inventados (pescado, carne, fruta, mayorista y los dos documentos de la demo) para comprobar la lectura con
la IA real. Cada línea tiene su resultado correcto en `esperado.json`.

```bash
python casos.py        # genera docs/*.html y esperado.json
node render.mjs        # los convierte en JPG/PDF con el Chrome instalado
python leer.py         # los pasa por la API local (perfil local) y compara; python leer.py 01 05 para algunos
```

Necesita la API local en marcha (`.claude/launch.json` → `pedidai-api`) y el usuario de la demo
(`marketing/demo/acceso.txt`). La API admite unas 30 lecturas al día por empresa (se reinicia al reiniciar la API):
una vuelta completa son 16.
